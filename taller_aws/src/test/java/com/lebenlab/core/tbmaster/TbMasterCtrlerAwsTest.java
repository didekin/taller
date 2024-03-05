package com.lebenlab.core.tbmaster;

import com.lebenlab.AwsTest;

import kong.unirest.HttpResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.lebenlab.core.mediocom.DataTestMedioCom.medioComunicaTb;
import static com.lebenlab.core.mediocom.DataTestMedioCom.mediosComunicaJson;
import static com.lebenlab.core.util.DataTestExperiment.*;
import static com.lebenlab.core.util.DataTestExperiment.tbMasterPathParam;
import static com.lebenlab.core.util.WebConnTestUtils.doJsonGet;
import static com.lebenlab.core.util.WebConnTestUtils.getArecordHttpStr;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 09/05/2020
 * Time: 19:46
 *
 * Comment <excludedGroups>AwsTest</excludedGroups>
 */
@Category(AwsTest.class)
public class TbMasterCtrlerAwsTest {

    @Test  // GET  https://www.lebendata1.net/conceptos
    public void test_conceptosTb()
    {
        doTbTest(conceptoTb, conceptosJson);
    }

    @Test  // GET  https://www.lebendata1.net/incentivos
    public void test_incentivosTb()
    {
        doTbTest(incentivoTb, incentivosJson);
    }

    @Test  // GET  https://www.lebendata1.net/incentivos
    public void test_mediosComunicaTb()
    {
        doTbTest(medioComunicaTb, mediosComunicaJson);
    }

    @Test  // GET https://www.lebendata1.net/mercados
    public void test_mercados()
    {
        doTbTest(mercadoTb, mercadosJson);
    }

    @Test  // GET  https://www.lebendata1.net/pg1s
    public void test_pg1s()
    {
        doTbTest(pg1Tb, pg1sJson);
    }

    @Test  // GET  https://www.lebendata1.net/provincias
    public void test_provincias()
    {
        assertThat(
                doJsonGet(getArecordHttpStr(tbMastersUrl))
                        .routeParam(tbMasterPathParam, provinciaTb).asString().getBody()
        ).contains(provinciaJson);
    }

    // ================== static utilities ======================

    void doTbTest(String pathTable, String jsonTable)
    {
        HttpResponse<String> response = doJsonGet(getArecordHttpStr(tbMastersUrl))
                .routeParam(tbMasterPathParam, pathTable).asString();
        assertThat(response.getBody()).isEqualTo(jsonTable);
    }
}
