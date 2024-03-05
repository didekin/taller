package com.lebenlab.core.experimento;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.email;
import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.tfno;
import static com.lebenlab.csv.CsvConstant.delimiter;
import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.id_fiscal;
import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.concepto;
import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.fecha_registro;
import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.provincia;
import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.csv.CSVFormat.RFC4180;
import static org.apache.commons.csv.CSVParser.parse;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin.es
 * Date: 28/01/2020
 * Time: 15:52
 */
public class CsvParserParticipantes {

    private static final Logger logger = getLogger(CsvParserParticipantes.class);
    public static final CsvParserParticipantes parser = new CsvParserParticipantes();

    /**
     *  Formato csv fecha_registro: 2014-09-29.
     */
    @NotNull
    public List<ParticipanteAbs.ParticipanteNoPk> readCsvParticipantes(InputStream inputStream, LocalDateTime defaultDateTime) throws IOException
    {
        logger.info("readCsvParticipantes()");
        return parse(
                inputStream,
                UTF_8,
                RFC4180.withFirstRecordAsHeader().withHeader(HeaderParticipantes.class).withDelimiter(delimiter.toString().charAt(0))
        ).getRecords()
                .stream()
                .map(csvRecord -> new ParticipanteAbs.ParticipanteNoPk(
                        null,
                        csvRecord.get(id_fiscal).trim(),
                        parseInt(csvRecord.get(provincia).trim()),
                        parseInt(csvRecord.get(concepto).trim()),
                        csvRecord.get(email).trim(),
                        csvRecord.get(tfno).trim(),
                        LocalDate.parse(csvRecord.get(fecha_registro)),
                        defaultDateTime)
                ).collect(toList());
    }

    public enum HeaderParticipantes {

        id_fiscal,
        provincia,
        concepto,
        email,
        tfno,
        fecha_registro,
        ;

        public static String getHeaderStr(){
            return stream(values()).map(Enum::name).collect(joining(delimiter.toString()));
        }
    }
}
