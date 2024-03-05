package com.lebenlab.tablesaw;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ExpPlotter;
import com.lebenlab.core.simulacion.SimPlotter;

import org.junit.Test;

import tech.tablesaw.api.Table;

import static com.lebenlab.core.Plotter.Kmeans_numClusters;
import static com.lebenlab.core.Plotter.doClustersLabels;
import static com.lebenlab.core.Plotter.estimacion_vta_media_diaria_particip;
import static com.lebenlab.core.Plotter.k_cluster_label;
import static com.lebenlab.core.Plotter.vta_media_diaria_particip;
import static com.lebenlab.core.Plotter.vta_media_diaria_particip_exp;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static com.lebenlab.core.util.DataTestExperiment.getXDataCluster;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.smile.Smiler.doKmeans;
import static com.lebenlab.tablesaw.TableSawer.doScatterExpClusters;
import static com.lebenlab.tablesaw.TableSawer.doScatterSimClusters;
import static com.lebenlab.tablesaw.TableSawer.doTableExpClusters;
import static com.lebenlab.tablesaw.TableSawer.doTableSimClusters;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 26/08/2020
 * Time: 12:57
 */
public class TableSawerTest {

    @Test
    public void test_doTableExpClusters()
    {
        Table tableTest = doTableExpClusters(getXDataCluster(), doClustersLabels(getXDataCluster()));
        assertThat(tableTest.columnNames()).containsExactly(vta_media_diaria_particip_exp, vta_media_diaria_particip, k_cluster_label);
        assertThat(tableTest.column(vta_media_diaria_particip).size())
                .isEqualTo(tableTest.column(k_cluster_label).size())
                .isEqualTo(getXDataCluster().length);
    }

    @Test
    public void test_doTableSimClusters()
    {
        Table tableTest = doTableSimClusters(getXDataCluster(), doClustersLabels(getXDataCluster()));
        assertThat(tableTest.columnNames()).containsExactly(vta_media_diaria_particip_exp, estimacion_vta_media_diaria_particip, k_cluster_label);
        assertThat(tableTest.column(estimacion_vta_media_diaria_particip).size())
                .isEqualTo(tableTest.column(k_cluster_label).size())
                .isEqualTo(getXDataCluster().length);
    }

    @Test
    public void test_doScatterExpClusters()
    {
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(111L).codPromo("promo_111").build();
        double[][] xData = getXDataCluster();
        String scriptStr = doScatterExpClusters(xData, doKmeans(xData, Kmeans_numClusters).y, promoIn);
        assertThat(scriptStr).containsOnlyOnce(ExpPlotter.scatter_plot_title + "promo_111")
                .contains(ExpPlotter.scatter_clusters_div + 111L);
    }

    @Test
    public void test_doScatterSimClusters()
    {
        double[][] xData = getXDataCluster();
        String scriptStr = doScatterSimClusters(xData, doKmeans(xData, Kmeans_numClusters).y, 6);
        assertThat(scriptStr).containsOnlyOnce(SimPlotter.scatter_plot_title + fromIntPg1(6).name())
                .contains(SimPlotter.scatter_clusters_div);
    }
}