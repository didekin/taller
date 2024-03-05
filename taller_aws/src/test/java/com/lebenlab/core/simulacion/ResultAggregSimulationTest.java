package com.lebenlab.core.simulacion;

import com.lebenlab.core.Pg1Promocion.Pg1PromoBuilder;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.Promocion.PromoBuilder;
import com.lebenlab.core.experimento.SqlUpdate;

import org.junit.After;
import org.junit.Test;

import java.util.List;

import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 31/03/2020
 * Time: 19:39
 */
public class ResultAggregSimulationTest {

    @After
    public void tearDown()
    {
        jdbiFactory.getJdbi().withHandle(handle -> handle.execute(SqlUpdate.particip_delete_all.statement));
    }

    @Test
    public void test_resultAvgPG1s()
    {
        runScript("INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                " VALUES (1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11');");

        Promocion promoIn = new PromoBuilder().copyPromo(promocion1).conceptos(singletonList(7)).mercados(singletonList(1)).build();
        List<ResultAggregSimulation.ResultsPG1> resultsPG1 = asList(
                new ResultAggregSimulation.ResultsPG1(new Pg1PromoBuilder().pg1(PG1_1.idPg1).promo(promoIn).build(), pg1Promocion -> new double[]{3.5, 6.5}),
                // Caso 1: sin resultados para PG1_2.
                new ResultAggregSimulation.ResultsPG1(new Pg1PromoBuilder().pg1(PG1_2.idPg1).promo(promoIn).build(), pg1Promocion -> new double[]{0})
        );

        ResultAggregSimulation aggRes = new ResultAggregSimulation.ResultByPG1Builder(resultsPG1).build(); // no inicializo numero participantes.
        assertThat(aggRes.numParticipantes).isEqualTo(0);
        assertThat(aggRes.resultAvgPG1s).usingRecursiveComparison()
                .isEqualTo(asList(new ResultAggregSimulation.ResultAvgPG1(PG1_1, 5d, 2), new ResultAggregSimulation.ResultAvgPG1(PG1_2, 0, 0)));
        // Caso 1: sin resultados para PG1_2.
        assertThat(aggRes.mensaje).isEqualTo(no_data_for_prediction + PG1_2.name());

        resultsPG1 = asList(
                // Caso 2: sin resultados para PG1_1.
                new ResultAggregSimulation.ResultsPG1(new Pg1PromoBuilder().pg1(PG1_1.idPg1).promo(promoIn).build(), pg1Promocion -> new double[]{0}),
                new ResultAggregSimulation.ResultsPG1(new Pg1PromoBuilder().pg1(PG1_2.idPg1).promo(promoIn).build(), pg1Promocion -> new double[]{0})
        );
        aggRes = new ResultAggregSimulation.ResultByPG1Builder(resultsPG1).build();
        final var expMsg = no_data_for_prediction + PG1_1.name() + "\n" + no_data_for_prediction + PG1_2.name();
        assertThat(aggRes.mensaje).isEqualTo(expMsg);
        System.out.println(expMsg);
    }
}