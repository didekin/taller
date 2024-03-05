package com.lebenlab.core.simulacion;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.asStrings;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.concepto;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.diasRegistro;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.duracionPromo;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.estimacion;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.incentivo;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.medioCom;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.mercado;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.pg1WithOne;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.pg1WithTwo;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.quarterPromo;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.ratioAperturas;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.txtMsgClass;
import static com.lebenlab.core.simulacion.RndForestSmile.DfOutLabels.vtaMediaDiariaPg1Exp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 06/09/2020
 * Time: 18:28
 */
public class PredictorRowDfTest {

    @Test
    public void test_instanceFields_1()
    {
        Assertions.assertThat(PredictorRowDf.instanceFields).containsExactlyInAnyOrder(
                mercado.name(),
                concepto.name(),
                diasRegistro.name(),
                vtaMediaDiariaPg1Exp.name(),
                duracionPromo.name(),
                quarterPromo.name(),
                incentivo.name(),
                medioCom.name(),
                txtMsgClass.name(),
                ratioAperturas.name(),
                pg1WithOne.name(),
                pg1WithTwo.name()
        );
    }

    @Test
    public void test_instanceFields_2()
    {
        Assertions.assertThat(PredictorRowDf.instanceFields).containsExactlyInAnyOrder(
                "mercado",
                "concepto",
                "diasRegistro",
                "vtaMediaDiariaPg1Exp",
                "duracionPromo",
                "quarterPromo",
                "incentivo",
                "medioCom",
                "txtMsgClass",
                "ratioAperturas",
                "pg1WithOne",
                "pg1WithTwo"
        );
    }

    @Test
    public void test_instanceFields_3()
    {
        Assertions.assertThat(PredictorRowDf.instanceFields).containsExactlyInAnyOrder(asStrings(estimacion).toArray(String[]::new));
    }
}