package com.lebenlab.core.experimento;

import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteNoPk;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.getHeaderStr;
import static com.lebenlab.core.experimento.CsvParserParticipantes.parser;
import static com.lebenlab.core.util.DataTestExperiment.upCsvParticip;
import static com.lebenlab.core.util.DataTestExperiment.upCsvParticipFull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.parse;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 28/01/2020
 * Time: 16:24
 */
public class CsvParserParticipantesTest {

    @Test
    public void test_GetCsvParticipantes_1() throws IOException
    {
        List<ParticipanteNoPk> participantes = parser.readCsvParticipantes(new ByteArrayInputStream(upCsvParticip.getBytes(UTF_8)), null);
        assertThat(participantes).containsExactlyInAnyOrder(
                new ParticipanteNoPk(null, "B12345X", 12, 7, null, null, parse("2004-12-31"), null),
                new ParticipanteNoPk(null, "C98345Z", 101, 2, null, null, parse("2019-01-02"), null)
        );
    }

    @Test
    public void test_GetCsvParticipantes_2() throws IOException
    {
        LocalDateTime nowTime = now();
        List<ParticipanteNoPk> participantes = parser.readCsvParticipantes(new ByteArrayInputStream(upCsvParticip.getBytes(UTF_8)), nowTime);
        assertThat(participantes).containsExactlyInAnyOrder(
                new ParticipanteNoPk(null, "B12345X", 12, 7, null, null, parse("2004-12-31"), nowTime),
                new ParticipanteNoPk(null, "C98345Z", 101, 2, null, null, parse("2019-01-02"), nowTime)
        );
    }

    @Test
    public void test_GetCsvParticipantes_3() throws IOException
    {
        List<ParticipanteNoPk> participantes = parser.readCsvParticipantes(new ByteArrayInputStream(upCsvParticipFull.getBytes(UTF_8)), null);
        assertThat(participantes).containsExactlyInAnyOrder(
                new ParticipanteNoPk(null, "B12345X", 12, 7, "mail1@lebenlab.com", "34600000100", parse("2004-12-31"), null),
                new ParticipanteNoPk(null, "C98345Z", 101, 2, "mail2@lebenlab.com", "34600000200", parse("2019-01-02"), null)
        );
    }

    @Test
    public void test_getHeaderStr()
    {
        assertThat(getHeaderStr()).isEqualTo("id_fiscal;provincia;concepto;email;tfno;fecha_registro");
    }
}