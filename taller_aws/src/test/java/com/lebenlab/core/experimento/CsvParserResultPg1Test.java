package com.lebenlab.core.experimento;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static com.lebenlab.core.experimento.CsvParserResultPg1.parser;
import static com.lebenlab.core.tbmaster.PG1.fromInt;
import static com.lebenlab.core.util.DataTestExperiment.upCsvResult;
import static com.lebenlab.core.util.DataTestExperiment.upCsvResultWrong;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.parse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 27/02/2020
 * Time: 12:13
 */
public class CsvParserResultPg1Test {

    @Test
    public void test_GetCsvResultPg1() throws IOException
    {
        System.out.println(upCsvResult);
        List<CsvParserResultPg1.ResultPg1> resultPg1s = parser.readCsvResultPg1(new ByteArrayInputStream(upCsvResult.getBytes(UTF_8)));
        Assertions.assertThat(resultPg1s).containsExactlyInAnyOrder(
                new CsvParserResultPg1.ResultPg1(1, fromInt(11), 27, parse("2019-12-31")),
                new CsvParserResultPg1.ResultPg1(5, fromInt(8), 3, parse("2019-01-02"))
        );

        // PG1s inexistentes.
        resultPg1s = parser.readCsvResultPg1(new ByteArrayInputStream(upCsvResultWrong.getBytes(UTF_8)));
        Assertions.assertThat(resultPg1s).hasSize(0);
    }
}