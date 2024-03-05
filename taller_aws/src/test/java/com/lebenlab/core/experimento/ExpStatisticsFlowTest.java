package com.lebenlab.core.experimento;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static com.lebenlab.ProcessArgException.experimento_wrongly_initialized;
import static com.lebenlab.core.experimento.ExpStatisticsFlow.statisticsFlow;
import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.core.util.DataTestSimulation.cleanRndForestRelated;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * User: pedro@didekin
 * Date: 20/06/2021
 * Time: 15:10
 */
public class ExpStatisticsFlowTest {


    @After
    public void tearDown() throws IOException
    {
        cleanRndForestRelated();
    }

    @Test
    public void test_statisticsFlow_1()
    {
        // Wrong name.
        assertThatThrownBy(() -> statisticsFlow.apply("wrongId12345")).hasMessageContaining(experimento_wrongly_initialized + ": wrongId12345");
    }

    @Test
    public void test_statisticsFlow_2()
    {
        // Caso: dos participantes en un experimento, diferentes promociones.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  experimento (experimento_id, nombre) VALUES (1, 'exp_1');");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_mercado (promo_id, mercado_id) VALUES (11, 1), (12, 1);");
        runScript("INSERT INTO promo_incentivo (promo_id, incentivo_id) VALUES (11, 5), (12, 6);");

        runScript("insert into promo_mediocomunicacion (promo_id, medio_id) VALUES (11,3), (12, 1);");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg) " +
                "VALUES (11, 111, 3, TRUE, FALSE), (11, 112, 1, FALSE, FALSE);");

        runScript("INSERT INTO promo_concepto (promo_id, concepto_id) VALUES (11, 5), (12, 5);");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) VALUES (11, 1, 10, 11), (12, 1, 10, 0);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 100), (12, 112, 1, 200);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        ResultadoExp resultExp = statisticsFlow.apply("1");
        checkExperimentResult(resultExp);
    }

    // ========================  Static utilities ===========================

    public static void checkExperimentResult(ResultadoExp resultExp)
    {
        assertThat(resultExp.experimento.nombre).isEqualTo("exp_1");

        assertThat(resultExp.promocion.promoMedioComunica.medioId).isEqualTo(email.id);
        assertThat(resultExp.variante.promoMedioComunica.medioId).isEqualTo(ninguna.id);
        assertThat(resultExp.recibidosPercent.get(0)).isEqualTo(1);
        assertThat(resultExp.recibidosPercent.get(1)).isEqualTo(NO_MEDIO_IN_PROMO);
        assertThat(resultExp.aperturasPercent.get(0)).isEqualTo(0);
        assertThat(resultExp.aperturasPercent.get(1)).isEqualTo(NO_MEDIO_IN_PROMO);

        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(0).participantes).isEqualTo(1);
        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(0).mediaVtaMediaDiariaParticip).isEqualTo(100d / 21);
        assertThat(resultExp.resultsPg1.get(PG1_1).tTestPvalue).isEqualTo(-1);
        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(1).participantes).isEqualTo(1);
        assertThat(resultExp.resultsPg1.get(PG1_1).resultByPromos.get(1).mediaVtaMediaDiariaParticip).isEqualTo(200d / 22);
    }
}