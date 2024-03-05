package com.lebenlab.core.simulacion;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Pg1Promocion;

import org.junit.After;
import org.junit.Test;

import static com.lebenlab.ProcessArgException.error_simulation_plot_ordinal_wrong;
import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.RndForestSmile.rndForestSmile;
import static com.lebenlab.core.simulacion.RndForestSmileTest.modelSimulacionDaoMock;
import static com.lebenlab.core.simulacion.RndForestSmileTest.promoIn;
import static com.lebenlab.core.simulacion.SimPlotter.scatter_clusters_div;
import static com.lebenlab.core.simulacion.SimPlotter.simPlotter;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static com.lebenlab.core.util.DataTestSimulation.rndModelTestDataDf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * User: pedro@didekin
 * Date: 31/08/2020
 * Time: 15:04
 */
public class    SimPlotterTest {

    final Pg1Promocion pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promoIn()).pg1(PG1_4.idPg1).build();

    @After
    public void cleanUp()
    {
        SimPlotter.instance.set(null);
    }

    @Test
    public void test_DoClustersScatter_1()
    {
        SimPlotter thisPlotter = simPlotter(modelSimulateDao);
        // Ordinal erróneo.
        assertThatThrownBy(() -> thisPlotter.doClustersScatter(pg1Promocion, 3))
                .isInstanceOf(ProcessArgException.class).hasMessage(error_simulation_plot_ordinal_wrong);
    }

    @Test
    public void test_DoClustersScatter_2()
    {
        SimPlotter thisPlotter = simPlotter(modelSimulateDao);
        // Sin datos.
        assertThat(thisPlotter.doClustersScatter(pg1Promocion, 1)).isEqualTo(no_data_for_prediction);
    }

    @Test
    public void test_DoClustersScatter_3()
    {
        SimPlotter thisPlotter = simPlotter(modelSimulacionDaoMock());
        rndForestSmile(modelSimulacionDaoMock()).upDateModel(pg1Id -> rndModelTestDataDf(1000, random -> PG1_4));

        // Con datos y modelo.
        final var plotStr = thisPlotter.doClustersScatter(pg1Promocion, 2);
        System.out.println(plotStr);
        assertThat(plotStr).containsOnlyOnce(SimPlotter.scatter_plot_title + fromIntPg1(pg1Promocion.pg1).name())
                .contains(scatter_clusters_div)
                .doesNotContain(no_data_for_prediction);
    }
}