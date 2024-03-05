package com.lebenlab.core.simulacion;

import com.lebenlab.ModelSerializer;
import com.lebenlab.core.FilePath;
import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.tbmaster.PG1;
import com.lebenlab.smile.Smiler;

import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import smile.data.DataFrame;
import smile.regression.RandomForest;

import static com.lebenlab.core.experimento.ParticipanteDao.countParticipByPromo;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static java.nio.file.Files.isReadable;
import static java.util.Arrays.stream;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.range;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 26/03/2020
 * Time: 14:31
 */
public final class RndForestSmile {

    // =====================  static fields ===================

    private static final Logger logger = getLogger(RndForestSmile.class);

    // ....... Random forest parameters ..........
    public static final int numTrees = 500;
    // Alternative: square root of varCount.
    public static final int predictorsSampleSize = ModelRowDf.instanceFields.size() / 3;
    public static final int treeMaxDepth = 8;
    // A revisar, quizás.
    public static final int treeMaxNodes = treeMaxDepth * 2;
    public static final int minInstancesInNode = 5;
    public static final double baggingSampleFraction = 1.0;

    // ======= Singleton =======
    public static final AtomicReference<RndForestSmile> instance = new AtomicReference<>(null);

    public static RndForestSmile rndForestSmile(ModelForSimulationDaoIf modelDaoIn)
    {
        instance.compareAndSet(null, new RndForestSmile(modelDaoIn));
        return instance.get();
    }

    // =====================  Instance members ===================

    public final Map<PG1, AtomicReference<RandomForest>> modelsMap;
    final ModelForSimulationDaoIf modelDao;

    private RndForestSmile(ModelForSimulationDaoIf modelDaoIn)
    {
        logger.info("RndForestSmile() constructor");

        modelDao = modelDaoIn;
        final var allPg1s = allOf(PG1.class);
        modelsMap = new HashMap<>(allPg1s.size());

        FilePath filePath;
        for (PG1 pg1 : allPg1s) {
            modelsMap.put(pg1, new AtomicReference<>(null));
            filePath = new ModelFilePath(pg1);
            if (isReadable(filePath.path())) {
                modelsMap.get(pg1).set(new ModelSerializer<RandomForest>(filePath).model);
            }
        }
    }

    public ResultAggregSimulation aggregateResultSimulation(Promocion promoIn)
    {
        logger.info("aggregateResultSimulation function");
        List<ResultAggregSimulation.ResultsPG1> predictionsPg1 = promoIn.pg1s
                .stream()
                .map(pg1Id -> new ResultAggregSimulation.ResultsPG1(
                                new Pg1Promocion.Pg1PromoBuilder().promo(promoIn).pg1(pg1Id).build(),
                                this::predictionPg1Arr
                        )
                )
                .collect(toList());
        return new ResultAggregSimulation.ResultByPG1Builder(predictionsPg1)
                .numeroParticipantes(countParticipByPromo(promoIn))
                .build();
    }

    /**
     * @return an array with the predictions of a PG1. If there is no model for the PG1, return an array with a zero element.
     */
    double[] predictionPg1Arr(Pg1Promocion pg1Promocion)
    {

        DataFrame df = Smiler.dfPredictoresPg1(modelDao.rowsForPredictionDf(pg1Promocion));
        final var rndModel = modelsMap.get(fromIntPg1(pg1Promocion.pg1)).get();
        if (df.size() > 0 && rndModel != null) {
            logger.info("predictionPg1Arr(). dataframe size = {}", df.size());
            return Smiler.rndPredict.apply(df, rndModel);
        }
        logger.info("predictionPg1Arr(). dataframe size = {}. rndModel = {}", 0, rndModel);
        return new double[]{0d};
    }

    /**
     * @return an array with the predictions of a PG1. If there is no model for the PG1, return an array with a zero element.
     */
    public double[] predictionPg1Arr(Pg1Promocion pg1Promocion, DataFrame predictorsDf)
    {
        final var rndModel = modelsMap.get(fromIntPg1(pg1Promocion.pg1)).get();
        if (predictorsDf.size() > 0 && rndModel != null) {
            logger.info("predictionPg1Arr(). dataframe size = {}", predictorsDf.size());
            return Smiler.rndPredict.apply(predictorsDf, rndModel);
        }
        logger.info("predictionPg1Arr(). dataframe size = {}. rndModel = {}", 0, null);
        return new double[]{0d};
    }

    /**
     * It is called by ResultadoPg1Dao after processing file with new results.
     */
    public void upDateModel(Function<Integer, List<ModelRowDf>> toModefDfFromProdId)
    {
        RandomForest forest;
        for (PG1 pg1 : allOf(PG1.class)) {
            forest = Smiler.rndModel.apply(toModefDfFromProdId.apply(pg1.idPg1));
            if (forest != null) {
                logger.info("updateModel(): forest != null");
                new ModelSerializer<>(forest).write(new ModelFilePath(pg1));
                modelsMap.get(pg1).set(forest);
            }
        }
    }

    // =======================  Utilities =====================

    public enum DfOutLabels {
        mercado,
        concepto,
        diasRegistro,
        vtaMediaDiariaPg1Exp,
        duracionPromo,
        quarterPromo,
        incentivo,
        medioCom,
        txtMsgClass,
        ratioAperturas,
        pg1WithOne,
        pg1WithTwo,
        estimacion,
        ;

        public static Set<String> asStrings(DfOutLabels... labelsToExclude)
        {
            final var namesToExclude = range(labelsToExclude[0], labelsToExclude[labelsToExclude.length - 1]);
            return stream(values()).filter(name -> !namesToExclude.contains(name)).map(Enum::name).collect(toSet());
        }
    }
}