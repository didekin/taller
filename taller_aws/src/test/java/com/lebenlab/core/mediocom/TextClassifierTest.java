package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.mediocom.TextClassifier.WordDictionary;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lebenlab.ProcessArgException.error_in_dictionnary;
import static com.lebenlab.core.mediocom.DataTestMedioCom.cleanMedCommTables;
import static com.lebenlab.core.mediocom.DataTestMedioCom.testStr500;
import static com.lebenlab.core.mediocom.DataTestMedioCom.upCsvCommunications;
import static com.lebenlab.core.mediocom.TextClassifier.CsvCommHeader.text;
import static com.lebenlab.core.mediocom.TextClassifier.TextClass.constantForLog0;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.NA;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.celebracion;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.fromIdToInstance;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.negocio;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.vacacion;
import static com.lebenlab.core.mediocom.TextClassifier.okDictWordList;
import static com.lebenlab.core.mediocom.TextClassifier.readTextToClasify;
import static com.lebenlab.core.mediocom.TextClassifier.textClassDao;
import static com.lebenlab.core.mediocom.TextClassifier.wordFrequencies;
import static com.lebenlab.core.mediocom.TextClassifier.wordLogPostProbs;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.lang.Math.log;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * User: pedro@didekin
 * Date: 23/01/2021
 * Time: 16:33
 */
public class TextClassifierTest {

    @After
    public void clean()
    {
        cleanMedCommTables();
    }


    @Test
    public void test_classifyText()
    {
        runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 3, 0.05), ('w1', 2, 0.01), ('w1', 1, 0.75), ('w0', 3, 0.29), ('w0', 1, 0.05), ('w0', 2, 0.65);");
        assertThat(textClassDao.classifyText("w1 w0")).isEqualTo(fromIdToInstance(1));
        assertThat(textClassDao.classifyText("w1")).isEqualTo(fromIdToInstance(1));
        assertThat(textClassDao.classifyText("w0")).isEqualTo(fromIdToInstance(2));
        assertThat(textClassDao.classifyText("w3")).isEqualTo(fromIdToInstance(0));
    }

    @Test
    public void test_textClass()
    {
        assertThat(textClassDao.textClassLogProb()).containsKeys(NA, vacacion, negocio, celebracion)
                .containsValues(log(constantForLog0), log(.3333), log(.3333), log(.3333));
    }

    @Test
    public void test_insertTextToClasify()
    {
        final var csvRecords = readTextToClasify.apply(new ByteArrayInputStream(upCsvCommunications.getBytes(UTF_8)));
        assertThat(TextClassifier.textClassDao.insertTextToClasify(csvRecords)).isEqualTo(2);
    }

    @Test
    public void test_okDictWordList()
    {
        List<WordDictionary> dictionary = asList(
                new WordDictionary("w1", celebracion, 0.05),
                new WordDictionary("w1", negocio, 0.01),
                new WordDictionary("w0", negocio, 0.9)
        );
        assertThat(okDictWordList(dictionary)).isFalse();
        final var newDict = new ArrayList<>(dictionary);
        newDict.remove(2);
        newDict.add(new WordDictionary("w1", negocio, 0.22));
        assertThat(okDictWordList(newDict)).isFalse();
        newDict.remove(2);
        newDict.add(new WordDictionary("w1", vacacion, 0.23));
        assertThat(okDictWordList(newDict)).isTrue();
    }

    @Test
    public void test_readTextToClasify()
    {
        System.out.println(upCsvCommunications);
        final var csvRecords = readTextToClasify.apply(new ByteArrayInputStream(upCsvCommunications.getBytes(UTF_8)));
        assertThat(csvRecords).hasSize(2);
        assertThat(csvRecords.get(0)).contains(testStr500, "1");
        assertThat(csvRecords.get(1)).contains(testStr500.substring(0, 100), "2");
        assertThat(csvRecords.get(0).get(text)).isEqualTo(testStr500);
    }

    @Test
    public void test_wordDictionary()
    {
        runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 1, 0.25), ('w1', 2, 0.1);");
        assertThatThrownBy(textClassDao::wordDictionary).isInstanceOf(ProcessArgException.class).hasMessage(error_in_dictionnary);

        runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 3, 0.9);");
        assertThat(textClassDao.wordDictionary()).containsExactly(
                new WordDictionary("w1", fromIdToInstance(1), 0.25),
                new WordDictionary("w1", fromIdToInstance(2), 0.1),
                new WordDictionary("w1", fromIdToInstance(3), 0.9)
        );
    }

    @Test
    public void test_wordFrequency()
    {
        assertThat(wordFrequencies.apply("no bb123 .. , jarra_b")).containsKeys("no", "bb123", "jarra_b").containsValues(1, 1, 1);
        assertThat(wordFrequencies.apply("234 34 hola hola __")).containsKeys("hola").containsValues(2);
    }

    @Test
    public void test_wordLogPostProbs()
    {
        List<WordDictionary> dictionary = asList(
                new WordDictionary("w1", celebracion, 0.5),
                new WordDictionary("w1", negocio, 0.3),
                new WordDictionary("w1", vacacion, 0.01),
                new WordDictionary("w0", negocio, 0.2),
                new WordDictionary("w0", vacacion, 0.8),
                new WordDictionary("w0", celebracion, 0.01),
                new WordDictionary("w2", vacacion, 0.25),
                new WordDictionary("w2", negocio, 0.6),
                new WordDictionary("w2", celebracion, 0.01)
        );

        Map<String, Integer> wordFrecuencies = Map.of("w1", 1);
        assertThat(wordLogPostProbs.apply(wordFrecuencies, dictionary)).containsKeys(vacacion, celebracion, negocio)
                .containsValues(log(0.01), log(0.5), log(0.3));

        wordFrecuencies = Map.of("w3", 3);
        assertThat(wordLogPostProbs.apply(wordFrecuencies, dictionary)).isEmpty();

        wordFrecuencies = Map.of("w1", 1, "w3", 3, "w2", 2);
        assertThat(wordLogPostProbs.apply(wordFrecuencies, dictionary)).containsKeys(vacacion, celebracion, negocio)
                .containsValues(log(.01) + 2 * log(.25), log(0.5) + 2 * log(.01), log(0.3) + 2 * log(0.6));
    }
}