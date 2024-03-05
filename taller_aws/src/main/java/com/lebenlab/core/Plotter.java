package com.lebenlab.core;

import org.apache.logging.log4j.Logger;

import static com.lebenlab.smile.Smiler.doKmeans;
import static java.util.Arrays.stream;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 28/08/2020
 * Time: 16:18
 */
public final class Plotter {

    private static final Logger logger = getLogger(Plotter.class);

    // Column names.
    public static final String vta_media_diaria_particip_exp = "venta media diaria esperada por pg1_participante";
    public static final String vta_media_diaria_particip = "venta_media_diaria_pg1_por_participante";
    public static final String estimacion_vta_media_diaria_particip = "estimación venta media diaria por pg1_participante";

    public static final int Kmeans_numClusters = 3;
    public static final String k_cluster_label = "k_cluster_label";

    private Plotter()
    {
    }

    public static double[] doArrColumn(double[][] xArrData, int columnIndex)
    {
        return stream(xArrData).mapToDouble(xArrRow -> xArrRow[columnIndex]).toArray();
    }

    public static double[] multiplyByScalar(double[] arrToMultiply, double scalar)
    {
        return stream(arrToMultiply).map(in -> in * scalar).toArray();
    }

    public static int[] doClustersLabels(double[][] arrDataForCluster)
    {
        logger.debug("doClustersLabels()");
        return doKmeans(arrDataForCluster, Kmeans_numClusters).y;
    }
}
