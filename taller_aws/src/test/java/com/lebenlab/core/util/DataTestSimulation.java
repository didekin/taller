package com.lebenlab.core.util;

import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.simulacion.ModelRowDf;
import com.lebenlab.core.simulacion.PredictorRowDf;
import com.lebenlab.core.simulacion.Quarter;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Incentivo;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.PG1;
import com.lebenlab.core.mediocom.DataTestMedioCom;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import smile.math.Random;

import static com.lebenlab.core.FilePath.modelsDir;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.RndForestSmile.instance;
import static com.lebenlab.core.simulacion.RndForestSmile.rndForestSmile;
import static com.lebenlab.core.simulacion.SqlUpdate.delete_all_promo_simulacion;
import static com.lebenlab.core.tbmaster.PG1.randomInstance;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.newDirectoryStream;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 19:36
 */
public final class DataTestSimulation {

    private DataTestSimulation()
    {
    }

    public static void cleanSimTables()
    {
        cleanExpTables();
        jdbiFactory.getJdbi().withHandle(handle -> handle.execute(delete_all_promo_simulacion.statement));
    }

    public static void cleanRndForestRelated() throws IOException
    {
        final var rndForest = rndForestSmile(modelSimulateDao);

        cleanSimTables();
        for (Path file : newDirectoryStream(modelsDir)) {
            deleteIfExists(file);
        }
        for (PG1 pg1 : rndForest.modelsMap.keySet()) {
            rndForest.modelsMap.get(pg1).set(null);
        }
        instance.set(null);
    }

    public static List<ModelRowDf> rndModelTestDataDf(int numResp, Function<Random, PG1> pg1Supplier)
    {
        double vtaMediaDiariaPg1;
        Mercado mercado;
        ConceptoTaller concepto;
        int diasRegistro;
        double vtaMediaDiariaPg1Exp;
        int duracionPromo;
        Quarter quarterPromo;
        Incentivo incentivo;
        PromoMedioComunica promoMedCom;
        double ratioAperturas;
        PG1 pg1WithOne;
        PG1 pg1WithTwo;

        List<ModelRowDf> respRnd = new ArrayList<>(numResp);
        Random rnd = new Random(1337);

        for (int i = 0; i < numResp; i++) {
            vtaMediaDiariaPg1 = rnd.nextDouble(0d, 20d);
            mercado = Mercado.randomInstance(rnd);
            concepto = ConceptoTaller.randomInstance(rnd);
            diasRegistro = rnd.nextInt(2000) + 1; // Para evitar 0 días.
            vtaMediaDiariaPg1Exp = rnd.nextDouble(0d, 600d);
            duracionPromo = rnd.nextInt(62) + 1; // Dos meses máximo.
            quarterPromo = Quarter.randomInstance(rnd);
            incentivo = Incentivo.randomInstance(rnd);
            promoMedCom = DataTestMedioCom.randInstance(rnd);
            ratioAperturas = rnd.nextDouble(0d, 1d);
            pg1WithOne = pg1Supplier.apply(rnd);
            pg1WithTwo = randomInstance(pg1WithOne, rnd);
            respRnd.add(new ModelRowDf.ModelRowDfBuilder(
                            vtaMediaDiariaPg1, mercado, concepto, diasRegistro, vtaMediaDiariaPg1Exp, duracionPromo,
                            quarterPromo, incentivo, promoMedCom, ratioAperturas, pg1WithOne, pg1WithTwo
                    ).build()
            );
        }
        return respRnd;
    }

    public static List<PredictorRowDf> rndPredictionTestDataDf(int numResp, Function<Random, PG1> pg1Supplier)
    {
        Mercado mercado;
        ConceptoTaller concepto;
        int diasRegistro;
        double vtaMediaDiariaPg1Exp;
        int duracionPromo;
        Quarter quarterPromo;
        Incentivo incentivo;
        PromoMedioComunica proMedCom;
        double ratioAperturas;
        PG1 pg1WithOne;
        PG1 pg1WithTwo;

        List<PredictorRowDf> respRnd = new ArrayList<>(numResp);
        Random rnd = new Random(1337);

        for (int i = 0; i < numResp; i++) {
            mercado = Mercado.randomInstance(rnd);
            concepto = ConceptoTaller.randomInstance(rnd);
            diasRegistro = rnd.nextInt(2000) + 1; // Para evitar 0 días.
            vtaMediaDiariaPg1Exp = rnd.nextDouble(0d, 600d);
            duracionPromo = rnd.nextInt(62) + 1; // Dos meses máximo.
            quarterPromo = Quarter.randomInstance(rnd);
            incentivo = Incentivo.randomInstance(rnd);
            proMedCom = DataTestMedioCom.randInstance(rnd);
            ratioAperturas = rnd.nextDouble(0d, 1d);
            pg1WithOne = pg1Supplier.apply(rnd);
            pg1WithTwo = randomInstance(pg1WithOne, rnd);
            respRnd.add(new PredictorRowDf.PredictorRowDfBuilder(
                    mercado, concepto, diasRegistro, vtaMediaDiariaPg1Exp, duracionPromo,
                    quarterPromo, incentivo, proMedCom, ratioAperturas, pg1WithOne, pg1WithTwo)
                    .build()
            );
        }
        return respRnd;
    }
}


