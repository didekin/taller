package com.lebenlab.core.experimento;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static com.lebenlab.csv.CsvConstant.delimiter;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.fecha_resultado;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.participante_id;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.pg1_id;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.uds_pg1;
import static com.lebenlab.core.tbmaster.PG1.fromInt;
import static com.lebenlab.core.tbmaster.PG1.PG1_0;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
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
public class CsvParserResultPg1 {

    private static final Logger logger = getLogger(CsvParserResultPg1.class);
    public static final CsvParserResultPg1 parser = new CsvParserResultPg1();

    @NotNull
    public List<ResultPg1> readCsvResultPg1(InputStream inputStream) throws IOException
    {
        logger.debug("readCsvResultPg1()");
        return parse(
                inputStream,
                UTF_8,
                RFC4180.withFirstRecordAsHeader().withHeader(HeaderResultPg1.class).withDelimiter(delimiter.toString().charAt(0))
        ).getRecords()
                .stream()
                .map(csvRecord -> new ResultPg1(
                        parseLong(csvRecord.get(participante_id).trim()),
                        fromInt(parseInt(csvRecord.get(pg1_id).trim())),
                        parseInt(csvRecord.get(uds_pg1).trim()),
                        LocalDate.parse(csvRecord.get(fecha_resultado)))
                )
                .filter(resultPg1 -> resultPg1.pg1Id != PG1_0.idPg1)  // We leave out wrong pg1s.
                .collect(toList());
    }

    public enum HeaderResultPg1 {

        participante_id,
        pg1_id,
        uds_pg1,
        fecha_resultado,
        ;

        public static String getHeaderStr(){
            return stream(values()).map(Enum::name).collect(joining(delimiter.toString()));
        }
    }

    public static class ResultPg1 {

        public final long participanteId;
        public final int pg1Id;
        public final int udsPg1;
        public final LocalDate fechaResultado;


        public ResultPg1(long participIdIn, int pg1Id, int udsPg1, LocalDate fechaResultado)
        {
            this.participanteId = participIdIn;
            this.pg1Id = pg1Id;
            this.udsPg1 = udsPg1;
            this.fechaResultado = fechaResultado;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof ResultPg1) {
                return participanteId == ((ResultPg1) obj).participanteId
                        && pg1Id == ((ResultPg1) obj).pg1Id
                        && udsPg1 == ((ResultPg1) obj).udsPg1
                        && fechaResultado.equals(((ResultPg1) obj).fechaResultado);
            }
            return false;
        }
    }
}
