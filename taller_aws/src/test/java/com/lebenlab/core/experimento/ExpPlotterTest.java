package com.lebenlab.core.experimento;

import org.junit.After;
import org.junit.Test;

import static com.lebenlab.core.experimento.ExpPlotter.doClustersScatter;
import static com.lebenlab.core.experimento.ExpPlotter.scatter_plot_title;
import static com.lebenlab.core.experimento.ExperimentoDao.txPromosExperiment;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.experimento1;
import static com.lebenlab.core.util.DataTestExperiment.insertPromoParticipPg1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 25/08/2020
 * Time: 13:21
 */
public class ExpPlotterTest {

    @After
    public void clean()
    {
        cleanExpTables();
    }

    @Test
    public void test_DoClustersScatter()
    {
        // Insertamos experimento
        final var promos = txPromosExperiment(experimento1);
        final var promo_1 = promos.get(0);
        final var promo_2 = promos.get(1);
        final var pg1Id = experimento1.pg1sToExperiment().get(0);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        insertPromoParticipPg1(6000, promo_1.idPromo, promo_2.idPromo, pg1Id);
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        assertThat(doClustersScatter(promo_1, pg1Id))
                .containsOnlyOnce(scatter_plot_title + promo_1.codPromo);
    }
}