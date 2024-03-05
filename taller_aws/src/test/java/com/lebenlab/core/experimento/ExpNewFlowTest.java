package com.lebenlab.core.experimento;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteSample;
import com.lebenlab.core.mediocom.PromoMedioComunica.PromoMedComBuilder;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.lebenlab.AsyncUtil.executor;
import static com.lebenlab.core.experimento.ExpNewFlow.allPromoParticipAsync;
import static com.lebenlab.core.experimento.ExpNewFlow.insertAllPromoParticip;
import static com.lebenlab.core.experimento.ExpNewFlow.medioComFlow;
import static com.lebenlab.core.experimento.ExpNewFlow.updateAllPromoParticip;
import static com.lebenlab.core.experimento.ExpNewFlow.writeSampleCsvStr;
import static com.lebenlab.core.experimento.ExpNewFlow.writeSampleToZipOut;
import static com.lebenlab.core.experimento.ExpNewFlow.zipSamplesFile;
import static com.lebenlab.core.experimento.ExperimentoDao.getSamples;
import static com.lebenlab.core.experimento.ExperimentoDao.promoParticipantes;
import static com.lebenlab.core.experimento.ExperimentoDao.promoParticipantesPg1;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.mediocom.SmsDao.promoMedioSms;
import static com.lebenlab.core.util.DataTestExperiment.alter_seq_promo;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.downSampleParticipCsvEven;
import static com.lebenlab.core.util.DataTestExperiment.downSampleParticipCsvOdd;
import static com.lebenlab.core.util.DataTestExperiment.promo_1A;
import static com.lebenlab.core.util.DataTestExperiment.promo_2A;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.csv.CsvConstant.newLine;
import static com.lebenlab.csv.CsvConstant.point_csv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * User: pedro@didekin
 * Date: 22/05/2021
 * Time: 12:46
 */
public class ExpNewFlowTest {

    @After
    public void clean()
    {
        cleanExpTables();
    }

    // ===============================  Main functions =============================

    @Test
    public void test_medioComFlow_1() throws IOException, InterruptedException
    {
        SECONDS.sleep(5);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  participante (participante_id, id_fiscal, provincia_id, email, tfno, concepto_id, fecha_registro, fecha_modificacion)" +
                "             VALUES (1,'H98895J', 19, NULL, NULL, 1, '2019-01-04', '2020-06-01:12:32:11')," +
                "                    (2,'B12345X', 20, 'hola@gmmail.com', '34615201910', 7, '2004-12-31', '2020-06-01:12:32:11');");   // rowNumber = 2
        runScript(alter_seq_promo);

        final var promo1 = new Promocion.PromoBuilder().copyPromo(promo_1A).idPromo(1L)
                .medio(new PromoMedComBuilder().medioId(ninguna.id).build())
                .build();
        final var promo2 = new Promocion.PromoBuilder().copyPromo(promo_2A).idPromo(2L)
                .medio(new PromoMedComBuilder().medioId(sms.id).build())
                .build();
        final var samples = getSamples(asList(promo1, promo2));  // rowNumber 2 in sample 1
        // Execute.
        final var optionalStream = medioComFlow.apply(samples, asList(promo1, promo2));
        // Checks file.
        //noinspection OptionalGetWithoutIsPresent
        checkOneZipFile(promo1, new ZipInputStream(optionalStream.get(), UTF_8));
        // Checks SMS.
        await().atMost(10L, SECONDS).until(() -> promoMedioSms().size() > 0);
        final var promoMedioSms = promoMedioSms();
        assertThat(promoMedioSms).hasSize(1).extracting("promoId", "medioId")
                .containsExactly(tuple(promo2.idPromo, promo2.promoMedioComunica.medioId));
        assertThat(promoMedioSms.get(0).batchId).hasSizeBetween(10, 100);
    }

    // ===============================  Experimento off-line =============================

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void test_zipSamplesFile() throws IOException
    {
        final var promo1 = new Promocion.PromoBuilder().copyPromo(promo_1A).idPromo(111L).build();
        final var promo2 = new Promocion.PromoBuilder().copyPromo(promo_1A).idPromo(222L).build();
        final var samplesList = asList(
                singletonList(new ParticipanteSample(2L, "H98895J", "2", "101", "1")),
                singletonList(new ParticipanteSample(1L, "B12345X", "1", "12", "7"))
        );
        // RUN
        ZipInputStream zipInput = new ZipInputStream(zipSamplesFile(samplesList, asList(promo1, promo2)).get(), UTF_8);
        checkTwoZipFile(promo1, promo2, zipInput);

    }

    @Test
    public void test_WriteSampleToZipOut() throws IOException
    {
        runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, 1, '2019-01-04', '2020-06-01:12:32:11');");
        final var promoIn = new Promocion.PromoBuilder().copyPromo(promo_1A).idPromo(123412L).mercados(asList(1, 2)).conceptos(asList(1, 2, 7)).build();
        ByteArrayOutputStream byteArrayZip = new ByteArrayOutputStream(1024);
        ZipOutputStream zipOutput = new ZipOutputStream(byteArrayZip, UTF_8);
        // Run.
        writeSampleToZipOut(zipOutput, promoIn.idPromo, getSamples(asList(promoIn, promoIn)).get(0));
        // Check.
        ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(byteArrayZip.toByteArray()), UTF_8);
        assertThat(requireNonNull(zipInput.getNextEntry()).getName()).isEqualTo(123412L + point_csv.toString());
        assertThat(new String(zipInput.readAllBytes(), UTF_8)).isEqualTo(downSampleParticipCsvEven);
        // NO hay más entradas.
        assertThat(zipInput.getNextEntry()).isNull();

        zipOutput.close();
        zipInput.close();
    }

    @Test
    public void test_WriteSampleCsvStr()
    {
        runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, 1, '2019-01-04', '2020-06-01:12:32:11');");
        final var promoIn = new Promocion.PromoBuilder().copyPromo(promo_1A).mercados(asList(1, 2)).conceptos(asList(1, 2, 7)).build();
        assertThat(writeSampleCsvStr(getSamples(asList(promoIn, promoIn)).get(0)))
                .isEqualTo(downSampleParticipCsvEven);
        assertThat(writeSampleCsvStr(getSamples(asList(promoIn, promoIn)).get(1)))
                .isEqualTo(downSampleParticipCsvOdd);
    }

    // =======================================  SMS experiment ===============================

    // ======================================= FUTURES =======================================

    @Test
    public void test_allPromoParticipAsync()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id) " +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 2, 1), (12, 'promo12', '2020-01-10', '2020-01-15', 0, 2);");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'); ");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) " +
                " VALUES (11, 2, 17, 0), (11, 17, 2, 0), (12, 17, 0, 0); ");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                " VALUES (11, 1, 17, 0, 110)");
        runScript("SET FOREIGN_KEY_CHECKS = 1; ");

        // Execute
        final var promoOut = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(13L).build();
        final var promoIn = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(12L).build();
        final var participIn = new ParticipanteSample(1L, null, null, null, null);
        allPromoParticipAsync(asList(singletonList(participIn), emptyList()), asList(promoIn, promoOut));
        // Check 1.
        await().atMost(10L, SECONDS)
                .until(() -> promoParticipantes(12L).size() > 0 && promoParticipantesPg1(12L).size() > 0);
        assertThat(promoParticipantes(12L))
                .extracting("promoId", "participanteId", "conceptoId", "provinciaId", "diasRegistro")
                .containsExactly(tuple(12L, 1L, 7, 12, 5488));
        // Check 2.
        assertThat(promoParticipantesPg1(12L))
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1Exp", "vtaMediaDiariaPg1")
                .containsExactlyInAnyOrder(tuple(12L, 1L, 17, 110 / 2d, 0d));
        // No hay assertions para inserción de medio porque sólo se inserta cuando los mismos participantes tienen los mismos medios en promociones previas.
    }

    @Test
    public void test_InsertAllPromoParticip()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id) " +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 2, 1);");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'); ");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) " +
                " VALUES (11, 2, 17, 0), (11, 17, 2, 0); ");
        runScript("SET FOREIGN_KEY_CHECKS = 1; ");

        final var promoOut = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).build();
        final var sample = singletonList(new ParticipanteSample(1L, "B12345X", "1", "12", "7"));

        // Run.
        insertAllPromoParticip(promoOut, sample, executor(1));
        // Checks.
        await().until(() -> promoParticipantesPg1(11L).size() == 2);
        assertThat(promoParticipantesPg1(11L)).extracting("promoId", "participanteId", "pg1Id")
                .containsExactly(tuple(11L, 1L, 2), tuple(11L, 1L, 17));
    }

    @Test
    public void test_UpdateAllPromoParticip()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id) " +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 2, 1);");
        runScript(" INSERT INTO participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11');");
        runScript("INSERT INTO promo_participante (promo_id, participante_id) VALUES (11, 1);");
        // Para update promo_participante_pg1.
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                " VALUES (11, 1, 17, 0, 0);"); // No hay ventas previas a la promo 11. No actualiza ventas.

        final var promoOut = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).pg1s(singletonList(17)).build();

        // Run.
        updateAllPromoParticip(promoOut, executor(1));
        // Checks.
        await().until(() -> promoParticipantesPg1(11L).size() == 1);
        assertThat(promoParticipantes(11L))
                .extracting("promoId", "participanteId", "conceptoId", "provinciaId", "diasRegistro")
                .containsExactly(tuple(11L, 1L, 7, 12, 5483));

        assertThat(promoParticipantesPg1(11L))
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1Exp", "vtaMediaDiariaPg1")
                .containsExactlyInAnyOrder(tuple(11L, 1L, 17, 0d, 0d));
    }

    // ======================================= Utilities =======================================

    void checkTwoZipFile(Promocion promo1, Promocion promo2, ZipInputStream zipInput) throws IOException
    {
        // CHECK
        assertThat(requireNonNull(zipInput.getNextEntry()).getName()).isEqualTo(promo1.idPromo + point_csv.toString());
        assertThat(new String(zipInput.readAllBytes(), UTF_8)).isEqualTo(
                "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine + "2;H98895J;2;101;1");
        assertThat(zipInput.getNextEntry().getName()).isEqualTo(promo2.idPromo + point_csv.toString());
        assertThat(new String(zipInput.readAllBytes(), UTF_8)).isEqualTo(
                "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine + "1;B12345X;1;12;7");

        assertThat(zipInput.getNextEntry()).isNull();
        zipInput.close();
    }

    void checkOneZipFile(Promocion promo1, ZipInputStream zipInput) throws IOException
    {
        // CHECK
        assertThat(requireNonNull(zipInput.getNextEntry()).getName()).isEqualTo(promo1.idPromo + point_csv.toString());
        assertThat(new String(zipInput.readAllBytes(), UTF_8)).isEqualTo(
                "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine + "1;H98895J;1;19;1");

        assertThat(zipInput.getNextEntry()).isNull();
        zipInput.close();
    }
}