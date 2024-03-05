package com.lebenlab.core.simulacion;

import com.lebenlab.ModelSerializer;
import com.lebenlab.core.FilePath;
import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.tbmaster.PG1;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import smile.data.DataFrame;
import smile.regression.RandomForest;

import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.FilePath.modelsDir;
import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.simulacion.LongleyTest.longleyForest;
import static com.lebenlab.core.simulacion.LongleyTest.ntrees;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.Quarter.fromIntToQuarter;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.asStrings;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.concepto;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.diasRegistro;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.mercado;
import static com.lebenlab.core.simulacion.RndForestSmile.instance;
import static com.lebenlab.core.simulacion.RndForestSmile.rndForestSmile;
import static com.lebenlab.core.tbmaster.ConceptoTaller.fromIntToConcepto;
import static com.lebenlab.core.tbmaster.Incentivo.fromIntToIncentivo;
import static com.lebenlab.core.tbmaster.Incentivo.tarjeta_regalo;
import static com.lebenlab.core.tbmaster.Mercado.fromIntToMercado;
import static com.lebenlab.core.tbmaster.PG1.PG1_0;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_10;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static com.lebenlab.core.util.DataTestExperiment.insert2Participantes;
import static com.lebenlab.core.util.DataTestSimulation.cleanSimTables;
import static com.lebenlab.core.util.DataTestSimulation.rndModelTestDataDf;
import static com.lebenlab.core.util.DataTestSimulation.rndPredictionTestDataDf;
import static com.lebenlab.smile.Smiler.dfPredictoresPg1;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.notExists;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 26/03/2020
 * Time: 16:53
 */
public class RndForestSmileTest {

    private RndForestSmile rndForest;

    @After
    public void tearDown() throws IOException
    {
        if (rndForest == null) {
            rndForest = rndForestSmile(modelSimulateDao);
        }

        cleanSimTables();
        for (Path file : newDirectoryStream(modelsDir)) {
            deleteIfExists(file);
        }
        for (PG1 pg1 : rndForest.modelsMap.keySet()) {
            rndForest.modelsMap.get(pg1).set(null);
        }
        instance.set(null);
    }

    // =====================  Instance members ===================

    @Test
    public void test_constructor()
    {
        RandomForest longley = longleyForest();
        FilePath filePath = new ModelFilePath(PG1_1);
        new ModelSerializer<>(longley).write(filePath);
        // initial state.
        assertThat(isReadable(filePath.path())).isTrue();
        // test.
        rndForest = rndForestSmile(modelSimulateDao);
        assertThat(rndForest.modelsMap.get(PG1_1).get()).isNotNull();
        assertThat(rndForest.modelsMap.get(PG1_1).get().trees()).hasSize(ntrees);
        assertThat(rndForest.modelsMap.get(PG1_2).get()).isNull();
    }

    @Test
    public void test_aggregateResultSimulation_1()
    {
        rndForest = rndForestSmile(modelSimulateDao);
        // NO hay modelo.
        ResultAggregSimulation result = rndForest.aggregateResultSimulation(promoIn());
        assertThat(result.resultAvgPG1s).hasSize(2).containsExactly(
                new ResultAggregSimulation.ResultAvgPG1(fromIntPg1(promoIn().pg1s.get(0)), 0d, 0),
                new ResultAggregSimulation.ResultAvgPG1(fromIntPg1(promoIn().pg1s.get(1)), 0d, 0));
        assertThat(result.numParticipantes).isEqualTo(0);
        assertThat(result.mensaje).contains(no_data_for_prediction);
    }

    @Test
    public void test_aggregateResultSimulation_2()
    {
        rndForest = rndForestMock();
        final Function<Integer, List<ModelRowDf>> dfFunction = pg1Id -> rndModelTestDataDf(1000, random -> PG1_4);
        rndForest.upDateModel(dfFunction);
        // Insertamos dos participantes, uno en mercado en promoción, para testar inicialización del número de participantes.
        insert2Participantes();
        // Test
        ResultAggregSimulation result = rndForest.aggregateResultSimulation(promoIn());
        assertThat(result.numParticipantes).isEqualTo(1);
        assertThat(result.resultAvgPG1s).hasSize(2);
        assertThat(result.resultAvgPG1s.get(0).pg1).isEqualTo(PG1_4);
        assertThat(result.resultAvgPG1s.get(0).numEstimaciones).isEqualTo(1);
        assertThat(result.resultAvgPG1s.get(0).mediaGlobalTalleres).isGreaterThan(0d);
        assertThat(result.resultAvgPG1s.get(1).pg1).isEqualTo(PG1_10);
        assertThat(result.resultAvgPG1s.get(1).numEstimaciones).isEqualTo(1);
        assertThat(result.resultAvgPG1s.get(1).mediaGlobalTalleres).isGreaterThan(0d);
        assertThat(result.mensaje).isNull();
    }

    @Test
    public void test_predictionsPg1Arr_1()
    {
        rndForest = rndForestMock();
        rndForest.upDateModel(pg1Id -> rndModelTestDataDf(1000, random -> PG1_4));
        assertThat(rndForest.modelsMap.get(PG1_4).get()).isNotNull();

        // Test
        final var pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promoIn()).pg1(PG1_4.idPg1).build();
        assertThat(rndForest.predictionPg1Arr(pg1Promocion)).hasSize(1);
    }

    @Test
    public void test_predictionsPg1Arr_2()
    {
        rndForest = rndForestMock();
        rndForest.upDateModel(pg1Id -> rndModelTestDataDf(1000, random -> PG1_4));
        assertThat(rndForest.modelsMap.get(PG1_4).get()).isNotNull();

        // Test: segunda variante del método, creando df predictores externamente.
        final var pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promoIn()).pg1(PG1_4.idPg1).build();
        DataFrame predictorsDf = dfPredictoresPg1(rndPredictionTestDataDf(1, random -> PG1_10));
        assertThat(rndForest.predictionPg1Arr(pg1Promocion, predictorsDf)).hasSize(1);
    }

    @Test
    public void test_predictionsPg1Arr_3()
    {
        rndForest = rndForestSmile(modelSimulateDao);
        // Sin datos para confeccionar modelo.
        final var pg1Promocion = new Pg1Promocion.Pg1PromoBuilder().promo(promoIn()).pg1(PG1_4.idPg1).build();
        assertThat(rndForest.predictionPg1Arr(pg1Promocion)).hasSize(1).containsExactly(0d);
    }

    @Test
    public void test_UpDateModel()
    {
        rndForest = rndForestSmile(modelSimulateDao);
        // initial state.
        assertThat(rndForest.modelsMap.get(PG1_4).get()).isNull();
        assertThat(exists(modelsDir)).isTrue();
        assertThat(notExists(new ModelFilePath(PG1_4).path())).isTrue();
        // test.
        rndForest.upDateModel(pg1Id -> rndModelTestDataDf(100, random -> PG1_4));
        assertThat(rndForest.modelsMap.get(PG1_4).get()).isNotNull();
        assertThat(exists(new ModelFilePath(PG1_4).path())).isTrue();
    }

    // =======================  Utilities =====================

    @Test
    public void test_asStrings()
    {
        assertThat(asStrings(mercado, concepto, diasRegistro)).hasSize(RndForestSmile.DfOutLabels.values().length - 3)
                .doesNotContainAnyElementsOf(asList(mercado.name(), concepto.name(), diasRegistro.name()));
    }

    // ========================= Test utilities ========================

    @NotNull
    public static Promocion promoIn()
    {
        return new Promocion.PromoBuilder()
                .fechaInicio(parse("2019-09-29"))
                .fechaFin(parse("2019-10-30"))
                .codPromo("promo_test01")
                .incentivo(tarjeta_regalo.incentivoId)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(email.id).build())
                .experimentoId(0)
                .mercados(singletonList(1))
                .conceptos(singletonList(7))
                .pg1s(asList(4, 10))
                .build();
    }

    public static ModelForSimulationDaoIf modelSimulacionDaoMock()
    {
        return new ModelForSimulationDaoIf() {
            @Override
            public List<PredictorRowDf> rowsForPredictionDf(Pg1Promocion pg1Promocion)
            {
                final var promoIn = pg1Promocion.promo;
                final var pg1Ids = promoIn.getPg1IdsZeroPadded();
                assertThat(pg1Ids).containsExactlyInAnyOrder(PG1_0.idPg1, PG1_4.idPg1, PG1_10.idPg1);

                return singletonList(new PredictorRowDf.PredictorRowDfBuilder(
                        fromIntToMercado(promoIn.mercados.get(0)),
                        fromIntToConcepto(promoIn.conceptos.get(0)),
                        94,
                        135 / 2d,
                        promoIn.getDuracionDias(),
                        fromIntToQuarter(promoIn.getQuarter()),
                        fromIntToIncentivo(promoIn.incentivo),
                        promoIn.promoMedioComunica,
                        0.55,
                        fromIntPg1(PG1_10.idPg1),
                        fromIntPg1(PG1_0.idPg1)).build()
                );
            }
        };
    }

    static RndForestSmile rndForestMock()
    {
        return rndForestSmile(modelSimulacionDaoMock());
    }
}