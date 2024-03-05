package com.lebenlab.core.simulacion;

import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.mediocom.TextClassifier.TextClassEnum;
import com.lebenlab.core.util.DataTestSimulation;

import org.junit.After;
import org.junit.Test;

import java.time.LocalDate;

import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.fromIdToInstance;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.celebracion;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.Quarter.fromIntToQuarter;
import static com.lebenlab.core.tbmaster.ConceptoTaller.fromIntToConcepto;
import static com.lebenlab.core.tbmaster.Incentivo.IncentivoId.fromIntToIncentivoId;
import static com.lebenlab.core.tbmaster.Mercado.ES;
import static com.lebenlab.core.tbmaster.PG1.PG1_10;
import static com.lebenlab.core.tbmaster.PG1.PG1_11;
import static com.lebenlab.core.tbmaster.PG1.PG1_3;
import static com.lebenlab.core.tbmaster.PG1.PG1_5;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * User: pedro@didekin
 * Date: 21/03/2020
 * Time: 18:57
 */
public class ModelForSimulationDaoIfTest {

    @After
    public void tearDown()
    {
        DataTestSimulation.cleanSimTables();
    }

    @Test
    public void test_InsertPromoSimulacion()
    {
        assertThat(modelSimulateDao.insertPromoSimulacion(promocion1)).isGreaterThan(0L);
    }

    @Test
    public void test_PromoSimulation()
    {
        long promoIdIn = modelSimulateDao.insertPromoSimulacion(promocion1);
        assertThat(modelSimulateDao.promoSimulation(promoIdIn)).usingRecursiveComparison()
                .ignoringFields("idPromo").isEqualTo(promocion1);
    }

    @Test
    public void test_rowsForModelDf()
    {
        modelDfs();
        assertThat(modelSimulateDao.rowsForModelDf(PG1_11.idPg1))
                .hasSize(1)
                .extracting("vtaMediaDiariaPg1", "mercado", "concepto", "diasRegistro", "vtaMediaDiariaPg1Exp", "duracionPromo",
                        "quarterPromo", "incentivo", "medioCom", "txtMsgClass", "ratioAperturas", "pg1WithOne", "pg1WithTwo")
                .containsExactly(
                        tuple(25d, ES, fromIntToConcepto(2), 0, 0d, 3,
                                fromIntToQuarter(1), fromIntToIncentivoId(5), fromIdToInstance(3), TextClassEnum.fromIdToInstance(3), 0.24d, PG1_5, PG1_10)
                );

        // CASO 0: no hay registros para el producto 3.
        assertThat(modelSimulateDao.rowsForModelDf(PG1_3.idPg1))
                .hasSize(0);
    }

    @Test
    public void test_rowsForPredictionDf()
    {
        modelDfs();
        LocalDate fechaRegistroParticipante = parse("2019-12-15");
        final var promoIn = new Promocion.PromoBuilder()
                .codPromo("promo_3_A")
                .fechaInicio(parse("2020-01-18"))
                .fechaFin(parse("2020-01-20"))
                .experimentoId(2)
                .mercados(singletonList(1))
                .conceptos(singletonList(2))
                .incentivo(5)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(email.id).msgClassifier(celebracion.codigoNum).build())
                .pg1s(asList(5, 10))
                .build();

        // CASO 1: extraemos una instancia OK.
        Pg1Promocion pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promoIn).pg1(PG1_5.idPg1).build();

        var simulationDfs = modelSimulateDao.rowsForPredictionDf(pg1Promocion);
        assertThat(simulationDfs).hasSize(1)
                .usingRecursiveComparison()
                .isEqualTo(
                        singletonList(
                                new PredictorRowDf.PredictorRowDfBuilder(pg1Promocion.promo, fechaRegistroParticipante, 500 / 17d, 0.5)
                                        .build())
                );

        // CASO 2: no existen participantes con mercado 2.
        final var promo_2 = new Promocion.PromoBuilder().copyPromo(promoIn).mercados(singletonList(2)).build();
        pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promo_2).pg1(PG1_5.idPg1).build();
        assertThat(modelSimulateDao.rowsForPredictionDf(pg1Promocion)).hasSize(0);

        // CASO 3: no existen participantes con concepto 3.
        final var promo_3 = new Promocion.PromoBuilder().copyPromo(promoIn).conceptos(singletonList(3)).build();
        pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promo_3).pg1(PG1_5.idPg1).build();
        assertThat(modelSimulateDao.rowsForPredictionDf(pg1Promocion)).hasSize(0);

        // CASO 4: no hay ventas previas en producto 3.
        final var promo_4 = new Promocion.PromoBuilder().copyPromo(promoIn).pg1s(asList(3, 10)).build();
        pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promo_4).pg1(PG1_3.idPg1).build();
        simulationDfs = modelSimulateDao.rowsForPredictionDf(pg1Promocion);
        // Devuelve una venta media esperada de 0.
        assertThat(simulationDfs).hasSize(1);
        assertThat(simulationDfs.get(0).vtaMediaDiariaPg1Exp).isEqualTo(0d);

        // CASO 5: no existen promociones con medio_comunicación 2.
        final var promo_5 = new Promocion.PromoBuilder().copyPromo(promoIn)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(sms.id).msgClassifier(celebracion.codigoNum).build())
                .build();
        pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promo_5).pg1(PG1_5.idPg1).build();
        simulationDfs = modelSimulateDao.rowsForPredictionDf(pg1Promocion);
        // Devuelve un ratio de aperturas 0.
        assertThat(simulationDfs).hasSize(1);
        assertThat(simulationDfs.get(0).ratioAperturas).isEqualTo(0d);
    }

    // =============================  Static utilities ========================

    static void modelDfs()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript(
                "INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                        " VALUES (11, 'promo_1_A', '2020-01-12', '2020-01-14', 12, 1)," +
                        "       (12, 'promo_2_A', '2020-01-15', '2020-01-17', 5, 2);" +

                        " INSERT INTO promo_incentivo (promo_id, incentivo_id)" +
                        " VALUES (11, 5), (12, 5);" +

                        "INSERT INTO participante (participante_id, id_fiscal, concepto_id, provincia_id, fecha_registro, fecha_modificacion)" +
                        "VALUES (111, 'A12345', 2, 23, '2019-12-15', NOW());" +

                        " INSERT INTO promo_participante (promo_id, participante_id, concepto_id, provincia_id)" +
                        " VALUES (11, 111, 2, 23), (12, 111, 2, 23);" +

                        " INSERT INTO promo_mediocomunicacion (promo_id, medio_id, msg_classification)" +
                        " VALUES (11, 3, 3), (12, 3, 3);" +

                        "INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg) " +
                        "VALUES (11, 111, 3, TRUE, TRUE), (12, 111, 3, FALSE, FALSE);" +

                        "INSERT INTO promo_participante_medio_hist (promo_id, participante_id, medio_id, ratio_aperturas) " +
                        "VALUES (11, 111, 3, 0.24), (12, 111, 3, 0.5);" +

                        " INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                        " VALUES (11, 111, 5, 100)," +
                        "       (11, 111, 10, 200)," +
                        "       (11, 111, 11, 300)," +
                        "       (12, 111, 5, 400);" +

                        " INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2)" +
                        " VALUES (11, 5, 10, 11)," +
                        "       (11, 10, 5, 11)," +
                        "       (11, 11, 5, 10)," +
                        "       (12, 5, 0, 0);");

        runScript("SET FOREIGN_KEY_CHECKS = 1;");
    }
}