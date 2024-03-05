package com.lebenlab.core.experimento;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.participante_id;
import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.promocion_id;
import static com.lebenlab.csv.CsvConstant.delimiter;
import static java.lang.Long.parseLong;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.csv.CSVFormat.RFC4180;
import static org.apache.commons.csv.CSVParser.parse;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin.es
 * Date: 28/01/2020
 * Time: 15:52
 */
public class CsvParserApertura {

    private static final Logger logger = getLogger(CsvParserApertura.class);

    public static final Function<CSVRecord, PromoParticipante> csvRecordToPromoParticip =
            csvRecord -> new PromoParticipante(
                    parseLong(csvRecord.get(promocion_id).trim()),
                    parseLong(csvRecord.get(participante_id).trim())
            );

    public static List<CSVRecord> csvRecords(InputStream inputStream) throws IOException
    {
        logger.info("csvRecords()");
        return parse(
                inputStream,
                UTF_8,
                RFC4180.withFirstRecordAsHeader().withHeader(HeaderAperturaCsv.class).withDelimiter(delimiter.toString().charAt(0))
        ).getRecords();
    }

    public enum HeaderAperturaCsv {

        participante_id,
        promocion_id,
        fecha_apertura,
        ;

        public static String getHeaderStr()
        {
            return stream(values()).map(Enum::name).collect(joining(delimiter.toString()));
        }
    }
}
