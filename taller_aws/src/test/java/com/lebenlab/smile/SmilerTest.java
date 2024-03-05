package com.lebenlab.smile;

import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.core.simulacion.ModelRowDf;
import com.lebenlab.core.simulacion.PredictorRowDf;
import com.lebenlab.core.tbmaster.PG1;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import smile.data.DataFrame;
import smile.regression.RandomForest;

import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.RndForestSmileTest.promoIn;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.tbmaster.PG1.PG1_6;
import static com.lebenlab.core.util.DataTestSimulation.cleanSimTables;
import static com.lebenlab.core.util.DataTestSimulation.rndModelTestDataDf;
import static com.lebenlab.core.util.DataTestSimulation.rndPredictionTestDataDf;
import static com.lebenlab.smile.Smiler.dfPredictoresPg1;
import static com.lebenlab.smile.Smiler.rndModel;
import static com.lebenlab.smile.Smiler.rndPredict;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static smile.data.DataFrame.of;

/**
 * User: pedro@didekin
 * Date: 23/08/2020
 * Time: 18:39
 */
public class SmilerTest {

    @After
    public void clean()
    {
        cleanSimTables();
    }

    @Test
    public void test_RndRModel_1()
    {
        // estado previo.
        assertThat(rndModelTestDataDf(200, PG1::randomInstance).size()).isGreaterThanOrEqualTo((int) (ModelRowDf.varCount * 5));
        // test.
        RandomForest forest = rndModel.apply(rndModelTestDataDf(200, PG1::randomInstance));
        assertThat(requireNonNull(forest).formula().response().variables()).containsExactly(ModelRowDf.responseVar);

        System.out.println("Error:" + forest.error());
        double[] importance = forest.importance();
        System.out.println("----- importance -----");
        for (int i = 0; i < importance.length; i++) {
            System.out.format("%-15s %.4f%n", forest.schema().fieldName(i), importance[i]);
        }
    }

    @Test
    public void test_RndModel_2()
    {
        // estado previo.
        assertThat(rndModelTestDataDf(10, PG1::randomInstance).size()).isLessThan((int) (ModelRowDf.varCount * 5));
        assertThat(rndModel.apply(rndModelTestDataDf(10, PG1::randomInstance))).isNull();
    }

    @Test
    public void test_RndPredict_1()
    {
        // Test para verificar que la compatibilidad de dos data frames: la del modelo y la obtenida con predictores desde la BD.
        RandomForest myForest = rndModel.apply(rndModelTestDataDf(200, PG1::randomInstance));
        Pg1Promocion pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promoIn()).pg1(PG1_4.idPg1).build();
        DataFrame dfNew = of(modelSimulateDao.rowsForPredictionDf(pg1Promocion), PredictorRowDf.class);

        // Check x.schemas of formula and data for prediction. Data frame con nuevos predictores vacía: no cargamos datos en tablas.
        assertThat(stream(myForest.formula().predictors()).flatMap(term -> term.variables().stream()).toArray(String[]::new))
                .containsExactly(stream(dfNew.schema().fields()).map(field -> field.name).toArray(String[]::new));
        assertThat(rndPredict.apply(dfNew, myForest)).hasSize(dfNew.size()).hasSize(0);
    }

    @Test
    public void test_dfPredictoresPg1_1()
    {
        final var predictorsDf = dfPredictoresPg1(rndPredictionTestDataDf(2, rnd -> PG1_6));
        assertThat(predictorsDf.size()).isEqualTo(2);
        assertThat(predictorsDf.column("pg1WithOne").get(0))
                .isEqualTo(predictorsDf.column("pg1WithOne").get(1))
                // Smile codifica PG1 desde 0 a 15, en tipo byte. Como falla el PG1_9, por encima de él, falla la comparación.
                .isEqualTo((byte) PG1_6.idPg1);
    }

    @Test
    public void test_DfPredictoresPg1_2()
    {
        final var predictorsDf = dfPredictoresPg1(rndPredictionTestDataDf(2, rnd -> PG1.fromIntPg1(rnd.nextInt())));
        Set<String> columnsDf = stream(predictorsDf.schema().fields()).map(structField -> structField.name).collect(toSet());
        System.out.println(columnsDf);
        final var instanceFields = PredictorRowDf.instanceFields.toArray(new String[0]);
        System.out.println(Arrays.toString(instanceFields));
        assertThat(columnsDf).containsExactlyInAnyOrder(instanceFields);
    }
}