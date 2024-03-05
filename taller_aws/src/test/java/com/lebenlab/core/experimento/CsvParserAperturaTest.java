package com.lebenlab.core.experimento;

import com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv;

import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.fecha_apertura;
import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.getHeaderStr;
import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.participante_id;
import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.promocion_id;
import static com.lebenlab.core.experimento.CsvParserApertura.csvRecords;
import static com.lebenlab.core.util.DataTestExperiment.upCsvApertura;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 30/12/2020
 * Time: 14:14
 */
public class CsvParserAperturaTest {

    @Test
    public void test_ReadCsvApertura_1() throws IOException
    {
        List<CSVRecord> csvRecords = csvRecords(new ByteArrayInputStream(upCsvApertura.getBytes(UTF_8)));
        assertThat(csvRecords).hasSize(2);
        final var map1 = csvRecords.get(0).toMap();
        assertThat(map1.keySet()).containsExactly(stream(HeaderAperturaCsv.values()).map(Enum::name).toArray(String[]::new));
        assertThat(map1.values()).containsExactly("1111", "11", "2020-12-31");
    }

    @Test
    public void test_HeaderAperturaCsv_doHeaderStr()
    {
        assertThat(getHeaderStr()).isEqualTo(participante_id.name() + ";" + promocion_id.name() + ";" + fecha_apertura.name());
    }
}