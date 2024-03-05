package com.lebenlab.tablesaw;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ExpPlotter;
import com.lebenlab.core.simulacion.SimPlotter;

import org.apache.logging.log4j.Logger;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.api.ScatterPlot;
import tech.tablesaw.plotly.components.Figure;

import static com.lebenlab.core.Plotter.doArrColumn;
import static com.lebenlab.core.Plotter.estimacion_vta_media_diaria_particip;
import static com.lebenlab.core.Plotter.k_cluster_label;
import static com.lebenlab.core.Plotter.vta_media_diaria_particip;
import static com.lebenlab.core.Plotter.vta_media_diaria_particip_exp;
import static com.lebenlab.core.experimento.ExpPlotter.index_vta_media;
import static com.lebenlab.core.experimento.ExpPlotter.index_vta_media_exp;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.SimPlotter.scatter_plot_title;
import static com.lebenlab.core.simulacion.SimPlotter.simPlotter;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static org.apache.logging.log4j.LogManager.getLogger;
import static tech.tablesaw.api.Table.create;

/**
 * User: pedro@didekin
 * Date: 08/06/2020
 * Time: 19:25
 */
public final class TableSawer {

    private static final Logger logger = getLogger(TableSawer.class);

    private TableSawer()
    {
    }

    public static Table doTableExpClusters(double[][] xArrData, int[] clusterLabels)
    {
        logger.info("doTableExpClusters()");

        DoubleColumn vta_media_exp = DoubleColumn.create(vta_media_diaria_particip_exp, doArrColumn(xArrData, index_vta_media_exp));
        DoubleColumn vtsAct = DoubleColumn.create(vta_media_diaria_particip, doArrColumn(xArrData, index_vta_media));
        IntColumn clusterLabel = IntColumn.create(k_cluster_label, clusterLabels);
        return create("cluster_table").addColumns(vta_media_exp, vtsAct, clusterLabel);
    }

    public static Table doTableSimClusters(double[][] xArrData, int[] clusterLabels)
    {
        logger.info("doTableSimClusters()");

        SimPlotter myPlotter = simPlotter(modelSimulateDao);
        DoubleColumn vta_media_exp = DoubleColumn.create(vta_media_diaria_particip_exp, doArrColumn(xArrData, myPlotter.index_vta_media_exp));
        DoubleColumn estimaciones = DoubleColumn.create(estimacion_vta_media_diaria_particip, doArrColumn(xArrData, myPlotter.index_estimacion_vta_media));
        IntColumn clusterLabel = IntColumn.create(k_cluster_label, clusterLabels);
        return create("cluster_table").addColumns(vta_media_exp, estimaciones, clusterLabel);
    }

    public static String doScatterExpClusters(double[][] arrData, int[] clusterLabels, Promocion promoIn)
    {
        logger.info("doScatterExpClusters()");

        Table tablePlot = doTableExpClusters(arrData, clusterLabels);
        return ScatterPlot.create(ExpPlotter.scatter_plot_title + promoIn.codPromo,
                tablePlot, vta_media_diaria_particip, vta_media_diaria_particip_exp, k_cluster_label)
                .asJavascript(ExpPlotter.scatter_clusters_div + promoIn.idPromo);
    }

    public static String doScatterSimClusters(double[][] arrData, int[] clusterLabels, int idPg1)
    {
        logger.info("doScatterSimClusters()");

        Table tablePlot = doTableSimClusters(arrData, clusterLabels);
        Figure scatterPlotFig = ScatterPlot.create(
                scatter_plot_title + fromIntPg1(idPg1).name(),
                tablePlot,
                estimacion_vta_media_diaria_particip,
                vta_media_diaria_particip_exp, k_cluster_label
        );
        return scatterPlotFig.asJavascript(SimPlotter.scatter_clusters_div);
    }
}
