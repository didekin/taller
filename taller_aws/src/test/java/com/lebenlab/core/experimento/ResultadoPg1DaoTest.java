package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ResultadoExp.Pg1ResultExp;
import com.lebenlab.core.simulacion.ModelFilePath;
import com.lebenlab.core.tbmaster.PG1;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import smile.stat.hypothesis.TTest;

import static com.lebenlab.ProcessArgException.experimento_wrongly_initialized;
import static com.lebenlab.ProcessArgException.num_promos_by_pg1_wrong;
import static com.lebenlab.ProcessArgException.result_experiment_more_3_pg1s;
import static com.lebenlab.ProcessArgException.result_experiment_no_pg1s;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.core.experimento.ExperimentoDao.promoDaysOn;
import static com.lebenlab.core.experimento.ExperimentoDao.promoParticipantesPg1;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.promosExp;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.resultPg1Dao;
import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.util.DataTestExperiment.insertCsvResults150;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.core.util.DataTestExperiment.upCsvResult;
import static com.lebenlab.core.util.DataTestSimulation.cleanRndForestRelated;
import static com.lebenlab.core.util.DataTestSimulation.rndModelTestDataDf;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.notExists;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.DoubleStream.generate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * User: pedro@didekin
 * Date: 27/02/2020
 * Time: 13:47
 */
public class ResultadoPg1DaoTest {

    @After
    public void tearDown() throws IOException
    {
        cleanRndForestRelated();
    }

    // ============================= Handle ventas file ==============================

    @Test
    public void test_HandleVentasFile_1() throws IOException
    {
        // Estado previo: insertamos 150 registros.
        insertCsvResults150();
        // Hacemos carga del fichero con check.
        Assertions.assertThat(resultPg1Dao.handleVentasFile(new ByteArrayInputStream(upCsvResult.getBytes(UTF_8)))).isEqualTo(2);
        // La carga ha borrado los registros previos en la tabla.
        final int inDb = jdbiFactory.getJdbi().withHandle(
                h -> h.select("SELECT COUNT(*) FROM resultado_pg1").mapTo(Integer.class).one());
        assertThat(inDb).isEqualTo(2);
    }

    @Test
    public void test_InsertResults_1()
    {
        Assertions.assertThat(resultPg1Dao.insertResults(new ByteArrayInputStream(upCsvResult.getBytes(UTF_8)))).isEqualTo(2);
        Assertions.assertThat(resultPg1Dao.deleteAllResults()).isEqualTo(2);
    }

    @Test
    public void test_InsertResults_2()
    {
        // Repetimos la carga del fichero: los datos se replican en BD.
        Assertions.assertThat(resultPg1Dao.insertResults(new ByteArrayInputStream(upCsvResult.getBytes(UTF_8)))).isEqualTo(2);
        Assertions.assertThat(resultPg1Dao.insertResults(new ByteArrayInputStream(upCsvResult.getBytes(UTF_8)))).isEqualTo(2);
        Assertions.assertThat(resultPg1Dao.deleteAllResults()).isEqualTo(4);
    }

    @Test
    public void test_InsertResults_3()
    {
        Assertions.assertThat(resultPg1Dao.insertResults(new ByteArrayInputStream(upCsvResult.getBytes(UTF_8)))).isEqualTo(2);
        Assertions.assertThat(resultPg1Dao.deleteAllResults()).isEqualTo(2);
    }

    // ...................  (asynchronous) ...................

    @Test
    public void test_handleVentasAsync()
    {
        // Caso: 1 promo, 1 participante, 1 resultado.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-20', 1);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id) VALUES (11, 1, 17);");
        runScript("INSERT INTO resultado_pg1(participante_id, pg1_id, cantidad, fecha_resultado)" +
                " VALUES (1, 17, 110, '2020-01-10');");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        // Estado inicial: no existe randomForest en fichero.
        assertThat(notExists(new ModelFilePath(PG1_4).path())).isTrue();
        // Run con datos de test para la construcción del modelo.
        resultPg1Dao.handleVentasAsync(pg1Id -> rndModelTestDataDf(100, random -> PG1_4));
        // Check con cambio de la situación inicial: ya existe el fichero con modelo.
        await().until(() -> promoParticipantesPg1(11).size() == 1 && exists(new ModelFilePath(PG1_4).path()));
        Assertions.assertThat(promoDaysOn().get(0).entrySet()).containsExactlyInAnyOrder(entry("dias_con_resultados", 6L), entry("promo_id", 11L));
        Assertions.assertThat(promoParticipantesPg1(11)).hasSize(1)
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1")
                .containsExactly(tuple(11L, 1L, 17, 110 / 6d));
    }

    @Test
    public void test_UpdatePromoDaysOn()
    {
        // Casos: dos promociones; añado un resultado a la segunda promoción.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        // promos 11 y 12.
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-20', 13, 1)," +
                "        (12, 'promo12', '2020-01-05', '2020-01-16', 1, 2);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id) VALUES (11, 1, 17), (12, 2, 17);");
        runScript("INSERT INTO resultado_pg1(participante_id, pg1_id, cantidad, fecha_resultado) VALUES (2, 17, 110, '2020-01-10');");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        Assertions.assertThat(resultPg1Dao.updatePromoDaysOn()).isEqualTo(2);
        final var promosOut = promoDaysOn();
        // Mantiene días con resultados = 13.
        assertThat(promosOut.get(0).entrySet()).containsExactlyInAnyOrder(entry("dias_con_resultados", 13L), entry("promo_id", 11L));
        // Incrementa días con resultados = (10 - 5 + 1).
        assertThat(promosOut.get(1).entrySet()).containsExactlyInAnyOrder(entry("dias_con_resultados", 6L), entry("promo_id", 12L));
    }

    @Test
    public void test_UpdatePromoParticipPg1Act_1()
    {
        // Estado inicial: no hay vtas en ninguno de los dos participantes en BD. Añado un resultado a uno; ninguno al otro.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript(" INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-04', '2020-01-06', 11, 1);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id) VALUES (11, 1, 17), (11, 2, 17);");
        runScript("INSERT INTO resultado_pg1(participante_id, pg1_id, cantidad, fecha_resultado)" +
                " VALUES (1, 17, 110, '2020-01-05');");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        Assertions.assertThat(resultPg1Dao.updatePromoParticipPg1Act()).isEqualTo(2);
        Assertions.assertThat(promoParticipantesPg1(11)).hasSize(2)
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1")
                .containsExactly(tuple(11L, 1L, 17, 110 / 11d), tuple(11L, 2L, 17, 0d));
    }

    @Test
    public void test_UpdatePromoParticipPg1Act_2()
    {
        // Estado inicial: hay vtas en participante en BD. Añado dos resultados.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 11, 1);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1) VALUES (11, 1, 17, 33);");
        runScript("INSERT INTO resultado_pg1(participante_id, pg1_id, cantidad, fecha_resultado)" +
                " VALUES (1, 17, 110, '2020-01-05'), (1, 17, 300, '2020-01-06');");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        Assertions.assertThat(resultPg1Dao.updatePromoParticipPg1Act()).isEqualTo(1);
        Assertions.assertThat(promoParticipantesPg1(11)).hasSize(1)
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1")
                .containsExactly(tuple(11L, 1L, 17, (33 + 110 + 300) / 11d));
    }

    // ============================= Handle statistics experiment ==============================

    @Test
    public void test_ResultsByPg1_1()
    {
        final List<Double> random1 = new Random().doubles(30).map(value -> value * 100).boxed().collect(toList());
        double randomAvg = random1.stream().mapToDouble(value -> value).average().orElseThrow();

        // Map<promoId, List<vtaMediaDiariaPg1>. Dos promoId. Para cada promo, una lista de la vtaMediaDiariaPg1 de cada participante en la promo.
        Map<Long, List<Double>> resultsTwoPromos_1 = new HashMap<>(2);
        resultsTwoPromos_1.put(1L, random1);
        resultsTwoPromos_1.put(2L, random1);
        Map<Long, List<Double>> resultsTwoPromos_2 = new HashMap<>(2);
        resultsTwoPromos_2.put(1L, random1);
        resultsTwoPromos_2.put(2L, random1);
        //  Map<pg1Id, Map<promoId, List<vtaMediaDiariaPg1>>. 2 PG1s.
        Map<Integer, Map<Long, List<Double>>> resultsByPg1s = new HashMap<>(2);
        resultsByPg1s.put(PG1_1.idPg1, resultsTwoPromos_1);
        resultsByPg1s.put(PG1_2.idPg1, resultsTwoPromos_2);

        Map<PG1, Pg1ResultExp> resultsPg1 = resultPg1Dao.resultsByPg1(resultsByPg1s);
        assertThat(resultsPg1).hasSize(2);
        assertThat(resultsPg1.get(PG1_1).tTestPvalue).isEqualTo(1d);
        assertThat(resultsPg1.get(PG1_1).resultByPromos).extracting("promoId", "participantes").containsExactly(
                tuple(1L, 30),
                tuple(2L, 30));
        // Checking that the avg are +- 0.5 around the randomAvg.
        assertThat(resultsPg1.get(PG1_2).resultByPromos.stream()
                .mapToDouble(result -> result.mediaVtaMediaDiariaParticip)
                .filter(avg -> Math.abs(avg - randomAvg) < 0.5)
                .count()).isEqualTo(2);
    }

    @Test
    public void test_ResultsByPg1_2()
    {
        // Caso: no hay resultados en DB.
        // Map<promoId, List<cantidad>. Dos promoId. Para cada promo, una lista de resultados como cantidad (double).
        Map<Long, List<Double>> resultsTwoPromos_1 = new HashMap<>(2);
        resultsTwoPromos_1.put(1L, generate(() -> 0d).limit(25).boxed().collect(toList()));
        resultsTwoPromos_1.put(2L, generate(() -> 0d).limit(26).boxed().collect(toList()));
        //  Map<pg1Id, Map<promoId, List<cantidad>>. Un solo PG1: pg1_1.
        Map<Integer, Map<Long, List<Double>>> resultsByPg1s = new HashMap<>(2);
        resultsByPg1s.put(PG1_1.idPg1, resultsTwoPromos_1);
        // Check
        Map<PG1, Pg1ResultExp> resultsPg1 = resultPg1Dao.resultsByPg1(resultsByPg1s);
        assertThat(resultsPg1).hasSize(1);
        assertThat(resultsPg1.get(PG1_1).tTestPvalue).isEqualTo(-1d);
        assertThat(resultsPg1.get(PG1_1).resultByPromos).extracting("promoId", "participantes").containsExactly(
                tuple(1L, 25),
                tuple(2L, 26));
    }

    @Test
    public void test_ResultsByPromoPg1Particip()
    {
        // Caso: dos participantes en un experimento, diferentes promociones; un participante en otra promoción.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 12, 1);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 0), (12, 112, 1, 25), (22, 111, 1, 5);");
        Promocion[] promos = new Promocion[]{
                new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).pg1s(singletonList(PG1_1.idPg1)).build(),
                new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(12L).pg1s(singletonList(PG1_1.idPg1)).build()
        };
        List<ResultadoPg1Dao.PromoPg1ParticipResult> results = resultPg1Dao.resultsByPromoPg1Particip(asList(promos[0], promos[1]));
        runScript("SET FOREIGN_KEY_CHECKS = 1;");
        // Check.
        assertThat(results).extracting("promoId", "pg1Id", "participId", "vtaMediaDiariaPg1")
                .containsExactlyInAnyOrder(tuple(11L, 1, 111L, 0d), tuple(12L, 1, 112L, 25d / 12));
    }

    @Test
    public void test_resultsByPromoPg1()
    {
        // Caso: dos participantes en un experimento, diferentes promociones.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 100), (12, 112, 1, 200);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");
        Promocion[] promos = new Promocion[]{
                new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).pg1s(singletonList(PG1_1.idPg1)).build(),
                new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(12L).pg1s(singletonList(PG1_1.idPg1)).build()
        };

        // Map<pg1Id, Map<promoId, List<cantidad>>.
        Map<Integer, Map<Long, List<Double>>> results = resultPg1Dao.resultsByPromoPg1(asList(promos[0], promos[1]));
        assertThat(results).isNotEmpty().hasSize(1).containsKey(PG1_1.idPg1);
        // Un único PG1 con dos promociones.
        assertThat(results.get(PG1_1.idPg1)).isNotEmpty().hasSize(2);
        assertThat(results.get(PG1_1.idPg1).get(promos[0].idPromo)).containsExactly(100d / 21);
        assertThat(results.get(PG1_1.idPg1).get(promos[1].idPromo)).containsExactly(200d / 22);
    }


    @Test
    public void test_StatisticsExperiment_1()
    {
        // Caso: dos participantes en un experimento, diferentes promociones.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_mercado (promo_id, mercado_id) VALUES (11, 1), (12, 1);");
        runScript("INSERT INTO promo_incentivo (promo_id, incentivo_id) VALUES (11, 5), (12, 6);");
        runScript("INSERT INTO promo_concepto (promo_id, concepto_id) VALUES (11, 5), (12, 5);");
        runScript("insert into promo_mediocomunicacion (promo_id, medio_id) VALUES (11,3), (12, 1);");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) VALUES (11, 1, 10, 11), (12, 1, 10, 0);");

        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg) " +
                "VALUES (11, 111, 3, TRUE, TRUE), (11, 112, 1, FALSE, FALSE);");

        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 100), (12, 112, 1, 200);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        ResultadoExp resultExp = resultPg1Dao.statisticsExperiment(promosExp(valueOf(1L))).buildForTest();

        assertThat(resultExp.aperturasPercent.get(0)).isEqualTo(1);
        assertThat(resultExp.aperturasPercent.get(1)).isEqualTo(NO_MEDIO_IN_PROMO);

        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(0).participantes).isEqualTo(1);
        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(0).mediaVtaMediaDiariaParticip).isEqualTo(100d / 21);
        assertThat(resultExp.resultsPg1.get(PG1_1).tTestPvalue).isEqualTo(-1);
        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(1).participantes).isEqualTo(1);
        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(1).mediaVtaMediaDiariaParticip).isEqualTo(200d / 22);
    }

    // ============================= Static utilities ==============================

    @Test
    public void test_CheckInvariantsMapResults()
    {
        Map<Long, List<Double>> resultsTwoPromosByPg1 = new HashMap<>(2);
        resultsTwoPromosByPg1.put(1L, asList(2.5, 3d, 3.5));
        resultsTwoPromosByPg1.put(2L, asList(4.5, 5d, 5.5));

        Map<Integer, Map<Long, List<Double>>> resultsByPg1s = new HashMap<>(4);
        // No entries.
        assertThatThrownBy(() -> ResultadoPg1Dao.checkInvariantsMapResults(resultsByPg1s))
                .isInstanceOf(ProcessArgException.class).hasMessage(result_experiment_wrongly_initialized + result_experiment_no_pg1s);
        // Two entries.
        resultsByPg1s.put(11, resultsTwoPromosByPg1);
        resultsByPg1s.put(22, resultsTwoPromosByPg1);
        assertThatCode(() -> ResultadoPg1Dao.checkInvariantsMapResults(resultsByPg1s)).doesNotThrowAnyException();
        // Four entries.
        resultsByPg1s.put(33, resultsTwoPromosByPg1);
        resultsByPg1s.put(44, resultsTwoPromosByPg1);
        assertThatThrownBy(() -> ResultadoPg1Dao.checkInvariantsMapResults(resultsByPg1s))
                .isInstanceOf(ProcessArgException.class).hasMessage(result_experiment_wrongly_initialized + result_experiment_more_3_pg1s);

        // More than 2 promos by PG1.
        resultsByPg1s.remove(44);
        assertThat(resultsByPg1s.entrySet().size()).isLessThanOrEqualTo(3);
        resultsTwoPromosByPg1.put(3L, asList(2.5, 3d, 3.5));
        assertThat(resultsTwoPromosByPg1.entrySet().size()).isNotEqualTo(2);
        assertThatThrownBy(() -> ResultadoPg1Dao.checkInvariantsMapResults(resultsByPg1s))
                .isInstanceOf(ProcessArgException.class).hasMessage(result_experiment_wrongly_initialized + num_promos_by_pg1_wrong);
    }

    @Test
    public void test_promosExp_1()
    {
        // Wrong name.
        assertThatThrownBy(() -> promosExp("wrongId12345"))
                .isInstanceOf(ProcessArgException.class).hasMessage(experimento_wrongly_initialized + ": wrongId12345");
    }

    @Test
    public void test_promosExp_2()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  experimento (experimento_id, nombre) VALUES (1, 'exp_1');");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_mercado (promo_id, mercado_id) VALUES (11, 1), (12, 1);");
        runScript("INSERT INTO promo_incentivo (promo_id, incentivo_id) VALUES (11, 5), (12, 6);");
        runScript("insert into promo_mediocomunicacion (promo_id, medio_id) VALUES (11,3), (12, 1);");
        runScript("INSERT INTO promo_concepto (promo_id, concepto_id) VALUES (11, 5), (12, 5);");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) VALUES (11, 1, 10, 11), (12, 1, 10, 0);");
        // RUN
        final var promosOut = promosExp(valueOf(1L));
        // Checks.
        assertThat(promosOut).hasSize(2).extracting("idPromo", "codPromo", "fechaInicio", "fechaFin", "incentivo", "promoMedioComunica.medioId", "experimentoId")
                .containsExactlyInAnyOrder(
                        tuple(11L, "promo11", parse("2020-01-05"), parse("2020-01-06"), 5, 3, 1L),
                        tuple(12L, "promo12", parse("2020-01-08"), parse("2020-01-09"), 6, 1, 1L));
    }

    @Test
    public void test_TTest_1()
    {
        // No entries in map.
        Map<Long, List<Double>> resultsTwoPromosByPg1 = new HashMap<>(2);
        assertThatThrownBy(() -> ResultadoPg1Dao.tTest(resultsTwoPromosByPg1))
                .isInstanceOf(ProcessArgException.class).hasMessage(result_experiment_wrongly_initialized + num_promos_by_pg1_wrong);
    }

    @Test
    public void test_TTest_2()
    {
        // All entries are 0.
        Map<Long, List<Double>> resultsTwoPromosByPg1 = new HashMap<>(2);
        List<Double> zeroResults = generate(() -> 0d).limit(25).boxed().collect(toList());
        resultsTwoPromosByPg1.put(1L, zeroResults);
        resultsTwoPromosByPg1.put(2L, zeroResults);
        Assertions.assertThat(ResultadoPg1Dao.tTest(resultsTwoPromosByPg1)).isNull();
    }

    @Test
    public void test_TTest_3()
    {
        // Less than 25 results in one promo.
        Map<Long, List<Double>> resultsTwoPromosByPg1 = new HashMap<>(2);
        resultsTwoPromosByPg1.put(1L, generate(() -> 0d).limit(25).boxed().collect(toList()));
        resultsTwoPromosByPg1.put(2L, asList(2d, 2d));
        Assertions.assertThat(ResultadoPg1Dao.tTest(resultsTwoPromosByPg1)).isNull();
    }

    @Test
    public void test_TTest_4()
    {
        // Caso OK with unequal number of results.
        Map<Long, List<Double>> resultsTwoPromosByPg1 = new HashMap<>(2);
        resultsTwoPromosByPg1.put(1L, new Random().doubles(100).map(value -> value * 100).boxed().collect(toList()));
        resultsTwoPromosByPg1.put(2L, new Random().doubles(80).map(value -> value * 100).boxed().collect(toList()));
        TTest tTest = ResultadoPg1Dao.tTest(resultsTwoPromosByPg1);
        assertThat(requireNonNull(tTest).pvalue).isBetween(0d, 1d);
        assertThat(tTest.method).isEqualTo("Equal Variance Two Sample");
    }

    @Test
    public void test_TTest_5()
    {
        // Two equal list of values: perfect correlation.
        final var random1 = new Random().doubles(25).map(value -> value * 100).boxed().collect(toList());
        Map<Long, List<Double>> resultsTwoPromosByPg1 = new HashMap<>(2);
        resultsTwoPromosByPg1.put(1L, random1);
        resultsTwoPromosByPg1.put(2L, random1);
        Assertions.assertThat(requireNonNull(ResultadoPg1Dao.tTest(resultsTwoPromosByPg1)).pvalue).isCloseTo(1d, byLessThan(0.001));
    }
}