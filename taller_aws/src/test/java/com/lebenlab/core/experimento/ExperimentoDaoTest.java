package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.Promocion.PromoBuilder;
import com.lebenlab.core.experimento.Experimento.ExperimentoBuilder;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteSample;
import com.lebenlab.core.experimento.PromoVariante.VarianteBuilder;
import com.lebenlab.core.mediocom.MedioComunicacion;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.mediocom.TextClassifier;
import com.lebenlab.core.util.DataTestExperiment;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static com.lebenlab.ProcessArgException.experimento_overlapping;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.core.experimento.ExperimentoDao.deleteExperiment;
import static com.lebenlab.core.experimento.ExperimentoDao.getExperimentoById;
import static com.lebenlab.core.experimento.ExperimentoDao.getExperimentos;
import static com.lebenlab.core.experimento.ExperimentoDao.getSamples;
import static com.lebenlab.core.experimento.ExperimentoDao.insertOnePromo;
import static com.lebenlab.core.experimento.ExperimentoDao.insertPromoParticipProducto;
import static com.lebenlab.core.experimento.ExperimentoDao.insertPromoParticipante;
import static com.lebenlab.core.experimento.ExperimentoDao.isOverlapConceptos;
import static com.lebenlab.core.experimento.ExperimentoDao.isOverlapDates;
import static com.lebenlab.core.experimento.ExperimentoDao.isOverlapExperiment;
import static com.lebenlab.core.experimento.ExperimentoDao.isOverlapMercados;
import static com.lebenlab.core.experimento.ExperimentoDao.isOverlapProductos;
import static com.lebenlab.core.experimento.ExperimentoDao.prPartPg1ForClusterPlot;
import static com.lebenlab.core.experimento.ExperimentoDao.promoParticipantes;
import static com.lebenlab.core.experimento.ExperimentoDao.promoParticipantesPg1;
import static com.lebenlab.core.experimento.ExperimentoDao.promocionById;
import static com.lebenlab.core.experimento.ExperimentoDao.promosByExperiment;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertExperimento;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertPromo;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertPromoConceptos;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertPromoIncentivo;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertPromoMedioCom;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertPromoMercados;
import static com.lebenlab.core.experimento.ExperimentoDao.txInsertPromoProd;
import static com.lebenlab.core.experimento.ExperimentoDao.txPromosExperiment;
import static com.lebenlab.core.experimento.ExperimentoDao.updatePromoParticipProducto;
import static com.lebenlab.core.experimento.ExperimentoDao.updatePromoParticipante;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.sortPg1s;
import static com.lebenlab.core.tbmaster.ConceptoTaller.allConceptos;
import static com.lebenlab.core.tbmaster.Mercado.allMercados;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.experimento1;
import static com.lebenlab.core.util.DataTestExperiment.experimento2;
import static com.lebenlab.core.util.DataTestExperiment.insert2Participantes;
import static com.lebenlab.core.util.DataTestExperiment.insertExp1Promos2;
import static com.lebenlab.core.util.DataTestExperiment.promo_1A;
import static com.lebenlab.core.util.DataTestExperiment.promo_2A;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.core.util.DataTestExperiment.variante_1A;
import static com.lebenlab.core.util.DataTestExperiment.variante_2B;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.lang.String.*;
import static java.lang.String.valueOf;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Arrays.deepToString;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Set.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * User: pedro@didekin.es
 * Date: 07/02/2020
 * Time: 13:34
 */
public class ExperimentoDaoTest {

    private final Jdbi jdbi = jdbiFactory.getJdbi();

    @After
    public void clean()
    {
        cleanExpTables();
    }

    // ===========================  Experimento síncrono ========================

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void test_GetSamples_1()
    {
        runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, email, tfno, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 'hola@gmmail.com', NULL, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, NULL, 34615671811, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, NULL, NULL, 1, '2019-01-04', '2020-06-01:12:32:11');");  //(2,1)
        var promoIn = new PromoBuilder().copyPromo(promo_1A).mercados(asList(1, 2)).conceptos(asList(2, 7)).build();
        // Selecciona los 2 registros impares en BD: (1,7) y (2,2).
        final var recordQuery = getSamples(asList(promoIn, promoIn)).get(1);
        final Function<ParticipanteSample, String> writeRecord =
                p -> join(";", p.idFiscal, p.email, p.tfno, valueOf(p.mercadoId), valueOf(p.provinciaId), valueOf(p.conceptoId));
        assertThat(recordQuery.stream()
                .map(writeRecord)
                .findFirst().get())
                .isEqualTo("B12345X;hola@gmmail.com;null;1;12;7");
        assertThat(recordQuery.stream().map(p -> p.idFiscal).collect(toUnmodifiableList()))
                .containsExactly("B12345X", "C98345Z");

        // Selecciona el registro par en BD: (2,1)
        promoIn = new PromoBuilder().copyPromo(promoIn).conceptos(asList(7, 1)).build();
        assertThat(getSamples(asList(promoIn, promoIn)).get(0).stream()
                .map(writeRecord)
                .findFirst().get())
                .isEqualTo("H98895J;null;null;2;101;1");
    }

    @Test
    public void test_GetSamples_2() throws IOException
    {
        DataTestExperiment.insertCsvParticip150();
        final var promoIn = new PromoBuilder().copyPromo(promo_1A).mercados(asList(1, 2)).conceptos(asList(1, 2, 7)).build();
        // Samples: no contienen elementos en común.
        List<List<ParticipanteSample>> samples = getSamples(asList(promoIn, promoIn));
        final var sample0 = samples.get(0);
        final var sample1 = samples.get(1);
        assertThat(sample0.removeAll(sample1)).isFalse();
    }

    // =====================  Experimento síncrono: inserción componentes experimento ===================

    @Test
    public void test_PromosByExperiment()
    {
        // No experiment in BD. Throws exception for checking invariant about the composition of the list returned.
        assertThatThrownBy(() -> promosByExperiment(1L))
                .isInstanceOf(ProcessArgException.class).hasMessageContaining(result_experiment_wrongly_initialized);

        // Insertamos experimento.
        final var promosExpect = txPromosExperiment(experimento1);
        final var promoExpA = promosExpect.get(0);
        final var promoExpB = promosExpect.get(1);
        final var experimentId = promoExpA.experimentoId;
        // Método.
        final var promosAct = promosByExperiment(experimentId);
        // Assertions.
        assertThat(promosAct).extracting("idPromo", "codPromo", "fechaInicio", "fechaFin", "experimentoId")
                .containsExactlyInAnyOrder(
                        tuple(promoExpA.idPromo, promoExpA.codPromo, promoExpA.fechaInicio, promoExpA.fechaFin, promoExpA.experimentoId),
                        tuple(promoExpB.idPromo, promoExpB.codPromo, promoExpB.fechaInicio, promoExpB.fechaFin, promoExpB.experimentoId));
        assertThat(promosAct).extracting("mercados", "conceptos", "pg1s", "incentivo", "promoMedioComunica")
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        tuple(promoExpA.mercados, promoExpA.conceptos, sortPg1s(promoExpA), promoExpA.incentivo, promoExpA.promoMedioComunica),
                        tuple(promoExpB.mercados, promoExpB.conceptos, sortPg1s(promoExpB), promoExpB.incentivo, promoExpB.promoMedioComunica));
    }

    // =====================  Experimento síncrono: inserción componentes experimento ===================

    @Test
    public void test_IsOverlapConceptos()
    {
        insertExp1Promos2();
        // Añado concepto a promoción 11.
        runScript("INSERT INTO  promo_concepto(promo_id, concepto_id) VALUES (11, 1);");

        try (Handle h = jdbiFactory.getJdbi().open()) {
            // CASO 1: no solape.
            assertThat(isOverlapConceptos(singletonList(2), h)).isFalse();
            // CASO 2: solape.
            assertThat(isOverlapConceptos(singletonList(1), h)).isTrue();
            // Todos los conceptos.
            List<Integer> all = allConceptos.stream().mapToInt(concept -> concept.conceptoId).boxed().collect(toList());
            assertThat(isOverlapConceptos(all, h)).isTrue();
        }
    }

    @Test
    public void test_IsOverlapDates()
    {
        insertExp1Promos2(); // Fechas: '2020-01-05' -> '2020-01-06'.
        try (Handle h = jdbiFactory.getJdbi().open()) {
            // CASO 1: no solapes.
            assertThat(isOverlapDates(parse("2020-01-01"), parse("2020-01-02"), h)).isFalse();
            assertThat(isOverlapDates(parse("2020-01-07"), parse("2020-01-08"), h)).isFalse();
            // CASO 2: solape.
            assertThat(isOverlapDates(parse("2020-01-06"), parse("2020-01-06"), h)).isTrue();
            assertThat(isOverlapDates(parse("2020-01-04"), parse("2020-01-05"), h)).isTrue();
            assertThat(isOverlapDates(parse("2020-01-06"), parse("2020-01-07"), h)).isTrue();
        }
    }

    @Test
    public void test_IsOverlapExperiment()
    {
        insertExp1Promos2(); // Fechas: '2020-01-05' -> '2020-01-06'.
        // Añado concepto a promoción 11.
        runScript("INSERT INTO  promo_concepto(promo_id, concepto_id) VALUES (11, 1);");
        // Añado mercado a promoción 11.
        runScript("INSERT INTO  promo_mercado (promo_id, mercado_id) VALUES (11, 2);");
        // Añado PG1s.
        runScript("INSERT INTO  promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) " +
                " VALUES (11, 1, 2, 0), (11, 2, 1, 0), (12, 1, 0, 0);");
        // Nuevo experimento.
        var promo21 = new PromoBuilder()
                .codPromo("promo21")
                .fechaInicio(parse("2020-01-05"))
                .fechaFin(parse("2020-01-06"))
                .conceptos(singletonList(1))
                .mercados(singletonList(2))
                .incentivo(11)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(2).build())
                .pg1s(asList(3, 4))
                .build();
        var variante21 = new VarianteBuilder(
                "promo22",
                singletonList(3),
                10,
                new PromoMedioComunica.PromoMedComBuilder().medioId(2).build()
        ).build();
        Experimento experiment = new ExperimentoBuilder()
                .nombre("experimento2")
                .promocion(promo21)
                .variante(variante21)
                .build();

        // CASO 1: no solape. Fallan PG1s: (1,2) vs. (3,4)
        assertThat(isOverlapExperiment(experiment)).isFalse();
        // CASO 2: solape: cambiamos PG1s en promo y en variante of experiment from (3,4) to (2,3)
        promo21 = new PromoBuilder().copyPromo(promo21).pg1s(asList(2, 3)).build();
        variante21 = new VarianteBuilder(
                "promo22",
                singletonList(3),
                10,
                new PromoMedioComunica.PromoMedComBuilder().medioId(2).build()
        ).build();
        experiment = new ExperimentoBuilder(experiment).promocion(promo21).variante(variante21).build();
        assertThat(isOverlapExperiment(experiment)).isTrue();
    }

    @Test
    public void test_IsOverlapMercados()
    {
        insertExp1Promos2();
        // Añado mercado a promoción 11.
        runScript("INSERT INTO  promo_mercado (promo_id, mercado_id) VALUES (11, 2);");
        try (Handle h = jdbiFactory.getJdbi().open()) {
            // CASO 1: no solape.
            assertThat(isOverlapMercados(singletonList(1), h)).isFalse();
            // CASO 2: solape.
            assertThat(isOverlapMercados(singletonList(2), h)).isTrue();
            // Todos los mercados.
            List<Integer> all = allMercados.stream().mapToInt(mercado -> mercado.id).boxed().collect(toList());
            assertThat(isOverlapMercados(all, h)).isTrue();
        }
    }

    @Test
    public void test_OverlapProductos()
    {
        insertExp1Promos2();
        runScript("INSERT INTO  promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) " +
                " VALUES (11, 1, 2, 0), (11, 2, 1, 0), (12, 1, 0, 0);");
        try (Handle h = jdbiFactory.getJdbi().open()) {
            // CASO 1: no solape.
            assertThat(isOverlapProductos(of(3, 4), h)).isFalse();
            // CASO 2: solape.
            assertThat(isOverlapProductos(of(1, 3), h)).isTrue();
            assertThat(isOverlapProductos(of(2, 3), h)).isTrue();
        }
    }

    @Test
    public void test_txInsertExperimento()
    {
        try (Handle h = jdbi.open()) {
            assertThat(txInsertExperimento(experimento1.nombre, h)).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    public void test_txInsertPromo()
    {
        long pk;
        Promocion promoExp;
        try (Handle h = jdbi.open()) {
            promoExp = new PromoBuilder()
                    .copyPromo(promocion1)
                    .experimentoId(txInsertExperimento("expTest", h))
                    .build();
            pk = txInsertPromo(promoExp, h);
        }
        assertThat(pk).isGreaterThan(0);
        assertThat(promocionById(pk)).usingRecursiveComparison()
                .ignoringFields("mercados", "conceptos", "pg1s", "incentivo", "promoMedioComunica", "experimentoId")
                .isEqualTo(new PromoBuilder().copyPromo(promoExp).idPromo(pk).buildForSummary());
    }

    @Test
    public void test_txInsertPromoConceptos()
    {
        try (Handle h = jdbi.open()) {
            Promocion promoPk = insertOnePromo(h, promocion1, "expTest");
            assertThat(txInsertPromoConceptos(promoPk, h)).isEqualTo(promoPk.conceptos.size());
        }
    }

    @Test
    public void test_txInsertPromoIncentivo()
    {
        try (Handle h = jdbi.open()) {
            Promocion promoPk = insertOnePromo(h, promocion1, "expTest");
            assertThat(txInsertPromoIncentivo(promoPk, h)).isEqualTo(1);
        }
    }

    @Test
    public void test_txInsertPromoMedioCom()
    {
        final var promoIn = new Promocion.PromoBuilder().copyPromo(promocion1)
                .medio(
                        new PromoMedioComunica.PromoMedComBuilder()
                                .medioId(MedioComunicacion.email.id)
                                .textMsg("text_msg_1")
                                .msgClassifier(TextClassifier.TextClassEnum.negocio.codigoNum)
                                .build()
                ).build();
        try (Handle h = jdbi.open()) {
            Promocion promoPk = insertOnePromo(h, promoIn, "experiment_t");
            assertThat(txInsertPromoMedioCom(promoPk, h)).usingRecursiveComparison().isEqualTo(promoPk.promoMedioComunica);
        }
    }

    @Test
    public void test_txInsertPromoMercados()
    {
        try (Handle h = jdbi.open()) {
            Promocion promoPk = insertOnePromo(h, promocion1, "expTest");
            assertThat(txInsertPromoMercados(promoPk, h)).isEqualTo(promoPk.mercados.size());
        }
    }

    @Test
    public void test_txInsertPromoProd()
    {
        try (Handle h = jdbi.open()) {
            Promocion promoPk = insertOnePromo(h, promocion1, "expTest");
            assertThat(txInsertPromoProd(promoPk, h)).isEqualTo(promoPk.pg1s.size());
        }
    }

    @Test
    public void test_txPromosExperiment_1()
    {
        final var promosPk = txPromosExperiment(experimento1);
        assertThat(promosPk.stream().mapToLong(value -> value.idPromo).toArray()).doesNotContain(0L).hasSize(2);
        assertThat(promosPk.get(0).conceptos).containsExactly(experimento1.promocion.conceptos.toArray(Integer[]::new));
        assertThat(promosPk.get(1).conceptos).containsExactly(experimento1.promocion.conceptos.toArray(Integer[]::new));
        assertThat(promosPk.get(0).mercados).containsExactly(experimento1.promocion.mercados.toArray(Integer[]::new));
        assertThat(promosPk.get(1).mercados).containsExactly(experimento1.promocion.mercados.toArray(Integer[]::new));
        assertThat(promosPk.stream().mapToInt(promo -> promo.incentivo).toArray())
                .containsExactly(experimento1.promocion.incentivo, experimento1.variante.incentivo);

        final var arrActual = promosPk.stream().map(promo -> promo.promoMedioComunica).toArray(PromoMedioComunica[]::new);
        assertThat(arrActual).usingElementComparatorIgnoringFields("promoId")
                .containsExactly(experimento1.promocion.promoMedioComunica, experimento1.variante.promoMedioComunica);
        assertThat(stream(arrActual.clone()).mapToLong(proMed -> proMed.promoId).toArray()).doesNotContain(0L).hasSize(2);

        assertThat(promosPk.get(0).pg1s.toArray(Integer[]::new)).containsExactly(experimento1.promocion.pg1s.toArray(Integer[]::new));
        assertThat(promosPk.get(1).pg1s.toArray(Integer[]::new)).containsExactly(experimento1.variante.pg1s.toArray(Integer[]::new));
    }

    @Test
    public void test_txPromosExperiment_2()
    {
        final var promosPk = txPromosExperiment(experimento1);
        // Experimento with overlap.
        Experimento expIn = new ExperimentoBuilder()
                .promocion(promosPk.get(0)).variante(promosPk.get(1).asVariante()).nombre("experimento2").build();
        assertThatThrownBy(() -> txPromosExperiment(expIn)).isInstanceOf(ProcessArgException.class)
                .hasMessage(experimento_overlapping + expIn.nombre);
    }

    // ===========================  Experimento asíncrono: inserción participantes ===========================

    @Test
    public void test_insertPromoParticipante()
    {
        // La tabla solo tiene restricciones de integridad con promociones.
        Promocion promoPk;
        try (Handle h = jdbi.open()) {
            promoPk = insertOnePromo(h, promocion1, "expTest");
        }
        final var promoParticip =
                new ParticipanteSample(11L, "H9876BB", "3", "101", "7");
        // Tres PG1s en una sola promo y con un solo participante.
        assertThat(insertPromoParticipante(promoPk.idPromo, singletonList(promoParticip))).isEqualTo(1);
        assertThat(promoParticipantes(promoPk.idPromo))
                .extracting("promoId", "participanteId", "conceptoId", "provinciaId", "diasRegistro")
                .containsExactly(tuple(promoPk.idPromo, 11L, 0, 0, 0));
    }

    @Test
    public void test_updatePromoParticipante()
    {
        insert2Participantes(); // participantes 1 y 3; registros: '2004-12-31', '2019-01-04'; provincias: 12 y 101; conceptos: 7 y 2.
        insertExp1Promos2();   // promos 11 y 12.
        // Participantes.
        runScript("INSERT INTO  promo_participante (promo_id, participante_id)" +
                " VALUES (11, 1), (12, 3);");
        assertThat(promoParticipantes(11L))
                .extracting("promoId", "participanteId", "conceptoId", "provinciaId", "diasRegistro")
                .containsExactly(tuple(11L, 1L, 0, 0, 0));
        // Execute and check
        assertThat(updatePromoParticipante(11L)).isEqualTo(1);
        assertThat(promoParticipantes(11L))
                .extracting("promoId", "participanteId", "conceptoId", "provinciaId", "diasRegistro")
                .containsExactly(tuple(11L, 1L, 7, 12, 5483));
    }

    @Test
    public void test_insertPromoParticipProducto()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (22, 'promo22', '2020-01-05', '2020-01-06', 10, 1);" +
                " INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) " +
                " VALUES (22, 2, 17, 0), (22, 17, 2, 0);");
        insertPromoParticipante(22L,
                singletonList(new ParticipanteSample(11L, null, null, null, null)));
        final var inserted = insertPromoParticipProducto(22L);
        runScript("SET FOREIGN_KEY_CHECKS = 1;");
        assertThat(inserted).isEqualTo(2);
        assertThat(promoParticipantesPg1(22L)).hasSize(2)
                .extracting("promoId", "participanteId", "pg1Id")
                .containsExactlyInAnyOrder(tuple(22L, 11L, 2), tuple(22L, 11L, 17));

    }

    @Test
    public void test_updatePromoParticipProducto_1()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        // promos 11 y 12. Promo 12 tiene resultados previos.
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 11, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 12, 2)," +
                "        (13, 'promo13', '2020-01-10', '2020-01-11', 13, 3)," +
                "        (14, 'promo14', '2020-01-12', '2020-01-13', 0, 4) ;");  // Nueva promoción.
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                " VALUES (11, 1, 17, 0, 110)," +
                "        (12, 1, 17, 110, 0)," +
                "        (13, 2, 15, 0, 130)," +
                "        (14, 1, 17, 0, 0)," +  // Participante con vtas. previas en PG1_17.
                "        (14, 2, 17, 0, 0);");   // Participante sin vtas. previas en PG1_17.
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        assertThat(updatePromoParticipProducto(14L)).isEqualTo(1); // Actualiza solo participante con resultados previos.
        assertThat(promoParticipantesPg1(14L)).hasSize(2)
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1Exp", "vtaMediaDiariaPg1")
                // 55/23d = (110 + 0) / (11 + 12) * (1/2)
                .containsExactlyInAnyOrder(tuple(14L, 1L, 17, 55 / 23d, 0d), tuple(14L, 2L, 17, 0d, 0d));
    }

    @Test
    public void test_updatePromoParticipProducto_2()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        // promos 11 y 12. Promo 12 tiene resultados previos.
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 0, 1)," +
                "        (14, 'promo14', '2020-01-12', '2020-01-13', 0, 4) ;");  // Nueva promoción.
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                " VALUES (11, 1, 17, 0, 0)," +
                "        (14, 1, 17, 0, 0);");   // Participante sin vtas. previas en PG1_17.
        assertThat(updatePromoParticipProducto(14L)).isEqualTo(1); // Actualiza con 0s al participante.
        assertThat(promoParticipantesPg1(14L)).hasSize(1)
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1Exp", "vtaMediaDiariaPg1")
                .containsExactlyInAnyOrder(tuple(14L, 1L, 17, 0d, 0d));
    }

    // ============  Plots  ============

    @Test
    public void test_prPartPg1ForClusterPlot()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id) " +
                "VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 22, 1);" +
                "INSERT INTO promo_participante_pg1" +
                " (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 10.5, 60), (11, 112, 2, 20.5, 21.5);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");
        final var arrTest = new double[][]{{10.5, 60 / 22d}};
        System.out.println(deepToString(arrTest));
        assertThat(prPartPg1ForClusterPlot(11L, 1)).isDeepEqualTo(arrTest);
    }

    @Test
    public void test_PromoParticipantesPg1()
    {
        // Caso: dos participantes en un experimento, diferentes promociones;
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id) " +
                "VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 22, 1);" +
                "INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 12.5, 46), (11, 112, 1, 0, 0), (12, 111, 1, 0, 0);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");
        List<ExperimentoDao.PromoParticipantePg1> list = promoParticipantesPg1(11L, PG1_1.idPg1);
        // Check.
        assertThat(list).extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1Exp", "vtaMediaDiariaPg1").hasSize(2)
                .containsExactly(tuple(11L, 111L, 1, 12.5, 46 / 22d), tuple(11L, 112L, 1, 0d, 0d));
    }

    // ============================= Utilities ==============================

    @Test
    public void test_DeleteExperiment()
    {
        // Promos and participants in DB.
        List<Promocion> promosDb = txPromosExperiment(experimento1);
        final var promoParticip =
                new ParticipanteSample(11L, "H9876BB", "3", "101", "7");
        insertPromoParticipante(promosDb.get(0).idPromo, singletonList(promoParticip));

        assertThat(deleteExperiment(promosDb.get(0).experimentoId)).isEqualTo(1);
    }

    @Test
    public void test_GetExperimentoById()
    {
        long expPk;
        try (Handle h = jdbi.open()) {
            expPk = txInsertExperimento(experimento1.nombre, h);
        }
        Experimento experimento = getExperimentoById(expPk);
        assertThat(experimento.experimentoId).isEqualTo(expPk);
        assertThat(experimento.nombre).isEqualTo(experimento1.nombre);
    }

    @Test
    public void test_GetExperimentos_1()
    {
        txPromosExperiment(experimento1);
        txPromosExperiment(experimento2);

        List<Experimento> experimentos = getExperimentos();
        assertThat(experimentos.stream().map(e -> e.nombre)).containsExactlyInAnyOrder(experimento1.nombre, experimento2.nombre);
        assertThat(experimentos.stream().map(e -> e.promocion))
                .extracting("codPromo", "fechaInicio", "fechaFin", "incentivo")
                .containsExactlyInAnyOrder(tuple(promo_1A.codPromo, promo_1A.fechaInicio, promo_1A.fechaFin, promo_1A.incentivo),
                        tuple(promo_2A.codPromo, promo_2A.fechaInicio, promo_2A.fechaFin, promo_2A.incentivo));
        assertThat(experimentos.stream().map(e -> e.variante))
                .extracting("codPromo", "incentivo")
                .contains(tuple(variante_1A.codPromo, variante_1A.incentivo),
                        tuple(variante_2B.codPromo, variante_2B.incentivo));
    }

    @Test
    public void test_GetExperimentos_2()
    {
        // Empty DB.
        assertThat(getExperimentos()).hasSize(0);
    }
}