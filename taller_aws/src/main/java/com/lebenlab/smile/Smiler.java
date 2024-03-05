package com.lebenlab.smile;

import com.lebenlab.core.simulacion.ModelRowDf;
import com.lebenlab.core.simulacion.PredictorRowDf;

import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import smile.clustering.KMeans;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.RandomForest;
import smile.regression.RegressionTree;
import smile.stat.hypothesis.TTest;

import static com.lebenlab.core.simulacion.RndForestSmile.baggingSampleFraction;
import static com.lebenlab.core.simulacion.RndForestSmile.minInstancesInNode;
import static com.lebenlab.core.simulacion.RndForestSmile.numTrees;
import static com.lebenlab.core.simulacion.RndForestSmile.predictorsSampleSize;
import static com.lebenlab.core.simulacion.RndForestSmile.treeMaxDepth;
import static com.lebenlab.core.simulacion.RndForestSmile.treeMaxNodes;
import static org.apache.logging.log4j.LogManager.getLogger;
import static smile.clustering.KMeans.fit;
import static smile.clustering.PartitionClustering.run;
import static smile.data.DataFrame.of;
import static smile.data.formula.Formula.lhs;
import static smile.stat.hypothesis.TTest.test;

/**
 * User: pedro@didekin
 * Date: 23/08/2020
 * Time: 15:21
 *
 * Class to encapsulate dependencies from Smile library.
 */
public final class Smiler {

    private static final Logger logger = getLogger(Smiler.class);

    static final int Kmeans_RUNS = 10;
    static final int Kmeans_MAX_ITER = 20;
    static final double Kmeans_tolerance = 1E-4;

    public static final BiFunction<DataFrame, RandomForest, double[]> rndPredict = (df, rf) -> {

        logger.info("rndPredict");
        double[] preds = new double[df.size()];
        for (int i = 0; i < df.size(); ++i) {
            double pred = 0;
            for (RegressionTree tree : rf.trees()) {
                pred += tree.predict(df.get(i));
            }
            preds[i] = pred / rf.trees().length;
        }
        return preds;
    };

    public static final Function<List<ModelRowDf>, RandomForest> rndModel = modelDf -> {

        logger.info("rndModel.apply()");
        if (modelDf.size() < ModelRowDf.instanceFields.size() * 5) {
            logger.info("rndModel.apply(): respModelDfs.size() < varCount * 5");
            return null;
        }
        return RandomForest.fit(
                Formula.lhs(ModelRowDf.responseVar),
                of(modelDf, ModelRowDf.class),
                numTrees,
                predictorsSampleSize,
                treeMaxDepth,
                treeMaxNodes,
                minInstancesInNode,
                baggingSampleFraction
        );
    };

    private Smiler()
    {
    }

    public static TTest doTtest(double[] cantidadesA, double[] cantidadesB, boolean equalVariance)
    {
        logger.debug("doTtest()");
        return test(cantidadesA, cantidadesB, equalVariance);
    }

    public static KMeans doKmeans(double[][] data, int numClusters)
    {
        logger.debug("doKmeans()");
        return run(Kmeans_RUNS, () -> fit(data, numClusters, Kmeans_MAX_ITER, Kmeans_tolerance));
    }

    public static DataFrame dfPredictoresPg1(List<PredictorRowDf> recordsBd)
    {
        logger.debug("dfPredictoresPg1()");
        return of(recordsBd, PredictorRowDf.class);
    }
}
