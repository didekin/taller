package com.lebenlab.core.util;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Predicate;

import static com.lebenlab.core.experimento.ParticipanteDao.insertParticipantes;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.tbmaster.PG1.PG1_3;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.tbmaster.PG1.PG1_5;
import static com.lebenlab.core.tbmaster.PG1.PG1_6;
import static com.lebenlab.core.util.WebConnTestUtils.upLoadDir;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.util.EnumSet.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 28/06/2020
 * Time: 20:12
 */
public class FileTestUtilTest {

    static final Predicate<Long> filterPred_0 = participante_id -> true;
    static final Predicate<Long> filterPred_1 = participante_id -> (participante_id % 3 == 0) || (participante_id % 5 == 0);

    @Before
    public void setUp()
    {
        assertThat(isDirectory(upLoadDir)).isTrue();
    }

    @After
    public void clean() throws IOException
    {
        FileTestUtil.cleanTablesFiles();
    }

    @Test
    public void test_dateResult()
    {
        Assertions.assertThat(FileTestUtil.dateForFileVtsTest(1, 3, "2020-10")).isIn("2020-10-01", "2020-10-02");
        Assertions.assertThat(FileTestUtil.dateForFileVtsTest(11, 13, "2020-10")).isIn("2020-10-11", "2020-10-12");
    }

    @Test
    public void test_WriteVentasCsv_11577() throws IOException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_11577));
        assertThat(insertParticipantes(byteStream)).isEqualTo(11577);

        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_11577_1.csv"), range(PG1_1, PG1_2));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_11577_2.csv"), range(PG1_3, PG1_4));
    }

    @Test
    public void test_WriteVentasCsv_1000() throws IOException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_1000));
        assertThat(insertParticipantes(byteStream)).isEqualTo(1000);

        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_1000_1.csv"), range(PG1_1, PG1_2));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_1000_2.csv"), range(PG1_3, PG1_4));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_1000_3.csv"), range(PG1_5, PG1_6));
    }

    @Test
    public void test_WriteVentasCsv_150() throws IOException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_150));
        assertThat(insertParticipantes(byteStream)).isEqualTo(150);

        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_150.csv"), range(PG1_1, PG1_2));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_150_2.csv"), range(PG1_2, PG1_3));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_150_3.csv"), range(PG1_1, PG1_3));
    }

    @Test
    public void test_WriteVentasCsv_3() throws IOException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_3));
        assertThat(insertParticipantes(byteStream)).isEqualTo(3);

        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_3_1.csv"), range(PG1_1, PG1_2));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_3_2.csv"), range(PG1_3, PG1_4));
        FileTestUtil.writeVentasFileCsv(upLoadDir.resolve("ventas_3_3.csv"), range(PG1_5, PG1_6));
    }

    @Test
    public void test_writeMailAperturas_150() throws IOException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_150));
        assertThat(insertParticipantes(byteStream)).isEqualTo(150);
        FileTestUtil.writeMailAperturasCsv(upLoadDir.resolve("mail_aperturas_150_all.csv"), filterPred_0);
        FileTestUtil.writeMailAperturasCsv(upLoadDir.resolve("mail_aperturas_150.csv"), filterPred_1);
    }

    @Test
    public void test_writeMailAperturas_1000() throws IOException
    {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_1000));
        assertThat(insertParticipantes(byteStream)).isEqualTo(1000);
        FileTestUtil.writeMailAperturasCsv(upLoadDir.resolve("mail_aperturas_1000.csv"), filterPred_1);
    }
}