package com.lebenlab.core;

import com.lebenlab.core.util.WebConnTestUtils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.lebenlab.core.UrlPath.aperturas_file;
import static com.lebenlab.core.UrlPath.communications_file;
import static com.lebenlab.core.UrlPath.exp_list_path;
import static com.lebenlab.core.UrlPath.exp_pg1_clusters_path;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.experimentoPath;
import static com.lebenlab.core.UrlPath.ficherosPath;
import static com.lebenlab.core.UrlPath.landingPgPath;
import static com.lebenlab.core.UrlPath.pg1_id_param;
import static com.lebenlab.core.UrlPath.sim_pg1_cluster_path;
import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.UrlPath.smsPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.tbmaster.PG1.PG1_11;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 18:38
 */
public class UrlPathTest {

    @Test
    public void test_GetArecordHttp()
    {
        assertThat(WebConnTestUtils.getArecordHttp(simulacionPath)).isEqualTo("https://lebendata1.net/close/simulacion");
    }

    @Test
    public void test_GetCnameHttpUrl()
    {
        assertThat(WebConnTestUtils.getCnameHttp(exp_list_path)).isEqualTo("https://www.lebendata1.net/close/experimento/list");
    }

    @Test
    public void test_GetLocalHttpUrl()
    {
        assertThat(getLocalHttp(simulacionPath)).isEqualTo("http://localhost:8080/close/simulacion");
        assertThat(getLocalHttp(exp_list_path)).isEqualTo("http://localhost:8080/close/experimento/list");
    }

    @Test
    public void test_urlBracketPathParam()
    {
        assertThat(exp_statistics_path.urlBracesPathParam()).isEqualTo("/close/experimento/{experimento_id}");
    }

    @Test
    public void test_PathParamName()
    {
        assertThat(exp_statistics_path.pathParamName()).isEqualTo("experimento_id");
        assertThat(exp_pg1_clusters_path.pathParamName()).isEqualTo("experimento_id");
        assertThat(sim_pg1_cluster_path.pathParamName()).isEqualTo("promo_id");
        assertThat(landingPgPath.pathParamName()).isEqualTo("substitution_id");
    }

    @Test
    public void test_urlNoPathParam()
    {
        assertThat(exp_statistics_path.urlNoPathParam()).isEqualTo("/close/experimento/");
        assertThat(exp_pg1_clusters_path.urlNoPathParam()).isEqualTo("/close/experimento/clusters/");
        assertThat(sim_pg1_cluster_path.urlNoPathParam()).isEqualTo("/close/simulacion/");
        assertThat(landingPgPath.urlNoPathParam()).isEqualTo("/open/sms/");
    }

    // =============== Experimentos ==================

    @Test
    public void test_experimentoPath()
    {
        assertThat(experimentoPath.actualPath()).isEqualTo("/close/experimento");
    }

    @Test
    public void test_exp_list_path()
    {
        assertThat(exp_list_path.actualPath()).isEqualTo("/close/experimento/list");
    }

    @Test
    public void test_exp_pg1_clusters_path()
    {
        assertThat(exp_pg1_clusters_path.actualPath(11L, PG1_2.idPg1))
                .isEqualTo("/close/experimento/clusters/11?" + pg1_id_param + "=" + PG1_2.idPg1);
    }

    @Test
    public void test_exp_statistics_path()
    {
        assertThat(exp_statistics_path.actualPath(Long.toString(12345L))).isEqualTo("/close/experimento/12345");
    }

    // =============== Simulaciones ==================

    @Test
    public void test_simulacionPath()
    {
        assertThat(simulacionPath.actualPath()).isEqualTo("/close/simulacion");
    }

    @Test
    public void test_sim_pg1_cluster_path()
    {
        assertThat(sim_pg1_cluster_path.actualPath(1234L, PG1_11.idPg1)).isEqualTo("/close/simulacion/1234?" + pg1_id_param + "=11");
    }

    // =============== Ficheros ==================

    @Test
    public void test_ficheros_path()
    {
        assertThat(ficherosPath.actualPath()).isEqualTo("/close/ficheros");
    }

    @Test
    public void test_aperturas_file_path()
    {
        assertThat(aperturas_file.actualPath()).isEqualTo("/close/ficheros/aperturas");
    }

    @Test
    public void test_ventas_file_path()
    {
        assertThat(ventas_file.actualPath()).isEqualTo("/close/ficheros/ventas");
    }

    @Test
    public void test_comunicaciones_file_path()
    {
        assertThat(communications_file.actualPath()).isEqualTo("/close/ficheros/comunicaciones");
    }

    // =============== SMS ==================

    @Test
    public void test_smsPath(){
        assertThat(smsPath.actualPath()).isEqualTo("/open/sms");
        assertThat(landingPgPath.actualPath("123_98765")).isEqualTo("/open/sms/123_98765");
    }
}