package com.lebenlab.core.experimento;

import com.lebenlab.core.Promocion;
import com.lebenlab.tablesaw.TableSawer;

import org.apache.logging.log4j.Logger;

import static com.lebenlab.core.Plotter.doClustersLabels;
import static com.lebenlab.core.experimento.ExperimentoDao.prPartPg1ForClusterPlot;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 24/08/2020
 * Time: 19:33
 */
public final class ExpPlotter {

    private static final Logger logger = getLogger(ExpPlotter.class);

    public static final int index_vta_media_exp = 0;
    public static final int index_vta_media = 1;

    public static final String scatter_clusters_div = "scatter_promo_";
    public static final String scatter_plot_title = "Clusters de los resultados promoción ";

    public static String doClustersScatter(Promocion promoIn, int pg1Id)
    {
        logger.info("doClustersScatter()");
        double[][] arrData = prPartPg1ForClusterPlot(promoIn.idPromo, pg1Id);
        return TableSawer.doScatterExpClusters(arrData, doClustersLabels(arrData), promoIn);
    }
}
