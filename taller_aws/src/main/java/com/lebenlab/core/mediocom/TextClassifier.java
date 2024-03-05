package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.lebenlab.ProcessArgException.error_in_dictionnary;
import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.ProcessArgException.file_error_reading_msg;
import static com.lebenlab.core.mediocom.TextClassifier.CsvCommHeader.text;
import static com.lebenlab.core.mediocom.TextClassifier.CsvCommHeader.text_class;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.NA;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.fromIdToInstance;
import static com.lebenlab.core.mediocom.TextClassifier.WordDictionary.ofToLogPower;
import static com.lebenlab.csv.CsvConstant.delimiter;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.lang.Math.log;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.max;
import static java.util.EnumSet.allOf;
import static java.util.Map.Entry.comparingByValue;
import static java.util.Map.of;
import static java.util.Objects.hash;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.csv.CSVFormat.RFC4180;
import static org.apache.commons.csv.CSVParser.parse;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 23/01/2021
 * Time: 15:45
 */
public enum TextClassifier {

    textClassDao,
    ;

    public TextClassEnum classifyText(String textToClass)
    {
        logger.info("classifyText()");
        Map<TextClassEnum, Double> logProbClasses = textClassLogProb();
        Map<TextClassEnum, Double> logPostProbs = wordLogPostProbs.apply(wordFrequencies.apply(textToClass), wordDictionary());
        logPostProbs.replaceAll((textClass, logWordProb) -> logWordProb + logProbClasses.get(textClass));
        return logPostProbs.isEmpty() ? NA : max(logPostProbs.entrySet(), comparingByValue()).getKey();
    }

    public int insertTextToClasify(List<CSVRecord> records)
    {
        logger.info("insertTextToClasify()");
        try {
            return jdbiFactory.getJdbi().withHandle(
                    handle -> {
                        PreparedBatch batch = handle.prepareBatch(SqlComunicacion.insert_text_to_class.statement);
                        for (CSVRecord record : records) {
                            batch.bind("text", record.get(text.name()))
                                    .bind("text_class", record.get(text_class.name()))
                                    .add();
                        }
                        return stream(batch.execute()).sum();
                    }
            );
        } catch (JdbiException e) {
            logger.error("insertTextToClasify(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    Map<TextClassEnum, Double> textClassLogProb()
    {
        logger.info("textClassLogProb()");
        try {
            return jdbiFactory.getJdbi().withHandle(
                    h -> h.createQuery(SqlComunicacion.text_classes.statement)
                            .map((rs, ctx) -> new TextClass(
                                            fromIdToInstance(rs.getInt("class_id")),
                                            rs.getDouble("prob_prior")
                                    )
                            ).collect(toMap(TextClass::getClassEnum, TextClass::getLnPriorProb))
            );
        } catch (JdbiException e) {
            logger.error("textClassLogProb(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    public List<WordDictionary> wordDictionary()
    {
        logger.info("wordDictionary()");
        try {
            final var wordInDictList = jdbiFactory.getJdbi().withHandle(
                    h -> h.createQuery(SqlComunicacion.wordDictionary.statement)
                            .map(
                                    (rs, ctx) -> new WordDictionary(
                                            rs.getString("word"),
                                            fromIdToInstance(rs.getInt("text_class")),
                                            rs.getDouble("prior_prob")

                                    )
                            ).collect(toList())
            );
            if (okDictWordList(wordInDictList)) {
                return wordInDictList;
            }
            throw new ProcessArgException(error_in_dictionnary);
        } catch (JdbiException e) {
            logger.error("wordDictionary(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // ===================== static members ======================

    private static final Logger logger = getLogger(TextClassifier.class);

    public static final Function<InputStream, List<CSVRecord>> readTextToClasify = inStream -> {
        logger.info("readTextToClasify function");
        try {
            return parse(
                    inStream,
                    UTF_8,
                    RFC4180.withFirstRecordAsHeader()
                            .withHeader(CsvCommHeader.class)
                            .withDelimiter(delimiter.toString().charAt(0))
            ).getRecords();
        } catch (IOException e) {
            logger.error("readTextToClasify: {}", e.getMessage());
            throw new ProcessArgException(file_error_reading_msg);
        }
    };

    public static final Function<String, Map<String, Integer>> wordFrequencies = textStr -> {

        logger.info("wordFrequency function");
        if (textStr == null){
            return of();
        }
        return compile("\\W+").splitAsStream(textStr)
                .filter(str -> !(str.isEmpty() || str.matches("\\d+")))
                .collect(groupingBy(String::toLowerCase, summingInt(str -> 1)));
    };

    public static final BiFunction<Map<String, Integer>, List<WordDictionary>, Map<TextClassEnum, Double>> wordLogPostProbs =
            (frecuencies, wordDictionary) -> wordDictionary.stream()
                    .filter(wordInDict -> frecuencies.containsKey(wordInDict.word))
                    .map(wordInDict -> ofToLogPower(wordInDict, frecuencies.get(wordInDict.word)))
                    .collect(groupingBy(WordDictionary::getTextClass, summingDouble(WordDictionary::getProbability)));

    static boolean okDictWordList(List<WordDictionary> wordInDictList)
    {
        final var okThreeOccurr = wordInDictList.stream().collect(groupingBy(WordDictionary::getWord, counting())).values().stream().noneMatch(value -> value != 3);
        final var okThreeClass = wordInDictList.stream().collect(groupingBy(WordDictionary::getTextClass, counting())).values().stream().distinct().count() == 1L;
        return wordInDictList.size() % 3 == 0 && okThreeOccurr && okThreeClass;
    }

    public enum CsvCommHeader {
        text,
        text_class,
        ;

        public static String getHeaderStr()
        {
            return stream(values()).map(Enum::name).collect(joining(delimiter.toString()));
        }
    }

    public enum TextClassEnum {

        NA(0),
        vacacion(1),
        negocio(2),
        celebracion(3),
        ;

        public final int codigoNum;

        TextClassEnum(int codClasificacion)
        {
            codigoNum = codClasificacion;
        }

        public static final Map<Integer, TextClassEnum> idToInstance =
                allOf(TextClassEnum.class).stream().collect(toMap(cl -> cl.codigoNum, cl -> cl));

        public static TextClassEnum fromIdToInstance(int codNum)
        {
            return idToInstance.get(codNum);
        }
    }

    public final static class TextClass {

        public static final double constantForLog0 = 0.00001;
        final TextClassEnum classEnum;
        final double lnPriorProb;

        public TextClass(TextClassEnum classEnum, double priorProb)
        {
            this.classEnum = classEnum;
            lnPriorProb = priorProb > 0 ? log(priorProb) : log(constantForLog0);
        }

        public TextClassEnum getClassEnum()
        {
            return classEnum;
        }

        public double getLnPriorProb()
        {
            return lnPriorProb;
        }
    }

    public static final class WordDictionary {

        final String word;
        final TextClassEnum textClass;
        final double probability;

        public WordDictionary(String wordIn, TextClassEnum textClassIn, double probIn)
        {
            word = wordIn;
            textClass = textClassIn;
            probability = probIn;
        }

        public String getWord()
        {
            return word;
        }

        public TextClassEnum getTextClass()
        {
            return textClass;
        }

        public double getProbability()
        {
            return probability;
        }

        static WordDictionary ofToLogPower(WordDictionary wordIn, int powerIn)
        {
            return new WordDictionary(wordIn.word, wordIn.textClass, powerIn * log(wordIn.probability));
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof WordDictionary) {
                WordDictionary objectIn = (WordDictionary) obj;
                return word.equals(objectIn.word) && textClass == objectIn.textClass && probability == objectIn.probability;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return hash(word, textClass, probability);
        }
    }
}
