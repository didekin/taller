package com.lebenlab.core.simulacion;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.smile.Smiler;
import com.lebenlab.tablesaw.TableSawer;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;

import static com.lebenlab.ProcessArgException.error_simulation_plot_ordinal_wrong;
import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.Plotter.doClustersLabels;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.vtaMediaDiariaPg1Exp;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.estimacion;
import static com.lebenlab.core.simulacion.RndForestSmile.rndForestSmile;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 24/08/2020
 * Time: 19:33
 */
public final class SimPlotter {

    private static final Logger logger = getLogger(SimPlotter.class);

    public static final String scatter_clusters_div = "scatter_plot";
    public static final String scatter_plot_title = "Clusters de las estimaciones de la simulación ";

    // ======= Singleton =======
    static final AtomicReference<SimPlotter> instance = new AtomicReference<>(null);

    public static SimPlotter simPlotter(ModelForSimulationDaoIf modelDaoIn)
    {
        instance.compareAndSet(null, new SimPlotter(modelDaoIn));
        return instance.get();
    }

    private final ModelForSimulationDaoIf modelDao;
    public final int index_vta_media_exp;
    public final int index_estimacion_vta_media;

    private SimPlotter(ModelForSimulationDaoIf modelDaoIn)
    {
        modelDao = modelDaoIn;
        index_vta_media_exp = 0;
        index_estimacion_vta_media = 1;
    }

    public String doClustersScatter(Pg1Promocion pg1PromoIn, int plotOrdinal)
    {
        logger.info("doClustersScatter()");
        if (plotOrdinal != 1 && plotOrdinal != 2) {
            throw new ProcessArgException(error_simulation_plot_ordinal_wrong);
        }

        DataFrame df = Smiler.dfPredictoresPg1(modelDao.rowsForPredictionDf(pg1PromoIn));
        final var estimaciones = rndForestSmile(modelDao).predictionPg1Arr(pg1PromoIn, df);

        if (estimaciones.length == 1 && estimaciones[0] == 0d) {
            logger.info("doClustersScatter(): estimaciones.length == 1 && estimaciones[0] == 0d");
            return no_data_for_prediction;
        }

        DoubleVector estimacionesVector = DoubleVector.of(estimacion.name(), estimaciones);
        double[][] arrData = df.select(vtaMediaDiariaPg1Exp.name()).merge(estimacionesVector).toArray();
        return TableSawer.doScatterSimClusters(arrData, doClustersLabels(arrData), pg1PromoIn.pg1);
    }
}
