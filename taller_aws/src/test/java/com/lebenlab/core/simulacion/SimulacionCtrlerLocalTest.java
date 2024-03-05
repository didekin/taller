package com.lebenlab.core.simulacion;


import com.lebenlab.core.util.FileTestUtil;

import org.jetbrains.annotations.NotNull;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.Promocion.FieldLabel.cod_promo;
import static com.lebenlab.core.Promocion.FieldLabel.conceptos;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_fin;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_inicio;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.mercados;
import static com.lebenlab.core.Promocion.FieldLabel.pg1s;
import static com.lebenlab.core.UrlPath.sim_pg1_cluster_path;
import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.ViewPathTest.checkMenuInHtml;
import static com.lebenlab.core.mediocom.DataTestMedioCom.cleanMedCommTables;
import static com.lebenlab.core.mediocom.DataTestMedioCom.medioThree;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text;
import static com.lebenlab.core.mediocom.MedioComunicacion.fromIdToInstance;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.tbmaster.ConceptoTaller.fromIntToConcepto;
import static com.lebenlab.core.tbmaster.Incentivo.fromIntToIncentivo;
import static com.lebenlab.core.tbmaster.Mercado.fromIntToMercado;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.core.util.DataTestSimulation.cleanSimTables;
import static com.lebenlab.core.util.WebConnTestUtils.doHtmlGetWithTk;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttp;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttpStr;
import static com.lebenlab.core.util.WebConnTestUtils.initJavalinInTest;
import static com.lebenlab.jwt.JwtTestUtil.doBasicClaims;
import static com.lebenlab.jwt.JwtTestUtil.doHeaderToken;
import static com.lebenlab.jwt.JwtTestUtil.putFechasOk;
import static java.lang.String.valueOf;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Arrays.asList;
import static kong.unirest.Unirest.post;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:19
 */
public class SimulacionCtrlerLocalTest {

    private final static String textMsg_1 = "email_text_1";

    @ClassRule
    public static final ExternalResource resource = initJavalinInTest();

    @Before
    public void setUp() throws IOException
    {
        assertThat(isDirectory(FileTestUtil.downloadDir) && list(FileTestUtil.downloadDir).count() == 0).isTrue();

    }

    @After
    public void clean() throws IOException
    {
        cleanSimTables();
        cleanMedCommTables();
        for (Path file : newDirectoryStream(FileTestUtil.downloadDir)) {
            deleteIfExists(file);
        }
    }

    // ========================= Tests ========================

    @Test
    public void test_serveFormPg1Results() throws JoseException, InvalidJwtException
    {
        final var bodyResp = doHtmlGetWithTk(getLocalHttp(simulacionPath)).asString().getBody();
        // Menu
        checkMenuInHtml(bodyResp);
        assertThat(bodyResp).contains("action=" + "\"" + simulacionPath.actualPath() + "\"");
        System.out.println(bodyResp);
    }

    @Test
    public void test_handlePG1Results() throws InvalidJwtException, JoseException
    {
        // Dicccionario en BD.
        runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 1, 0.25), ('w1', 2, 0.1), ('w1', 3, 0.9);");

        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));
        String bodyResp = post(getLocalHttp(simulacionPath))
                .header(tokenHeader[0], tokenHeader[1])
                .fields(formSimulacion())
                .field(mercados.name(), asList("1", "2"))
                .field(conceptos.name(), asList("1", "2"))
                .asString()
                .getBody();
        String codPromoStr = formSimulacion().get(cod_promo.name()).toString();
        assertThat(bodyResp).contains(codPromoStr)
                .contains(fromIntToIncentivo(7).name())
                .contains(30 + "</span> días")
                .contains(fromIntToMercado(1).nombre).contains(fromIntToMercado(2).nombre)
                .contains(fromIntToConcepto(1).getNombre()).contains(fromIntToConcepto(2).getNombre())
                // medioComunicacion.
                .contains(fromIdToInstance(medioThree) + "</span>")
                .contains(textMsg_1)
                .contains(PG1_1.name())
                .contains(0 + "</span>")
                .contains(0 + "</span>")
                .contains(sim_pg1_cluster_path.actualPath(modelSimulateDao.promoSimulacionId(), PG1_1.idPg1));
        // Menu
        checkMenuInHtml(bodyResp);
        System.out.println(bodyResp);
    }

    @Test
    public void test_handleEstimatesPg1Clusters() throws JoseException, InvalidJwtException
    {
        long promoIdIn = modelSimulateDao.insertPromoSimulacion(promocion1);

        final var bodyResp =
                doHtmlGetWithTk(getLocalHttpStr(sim_pg1_cluster_path.actualPath(promoIdIn, promocion1.pg1s.get(0)))).asString().getBody();
        assertThat(bodyResp).contains(SimPlotter.scatter_clusters_div)
                .contains(no_data_for_prediction);
        // Menu
        checkMenuInHtml(bodyResp);
    }

    // ....... Utilities ..........

    @NotNull
    static Map<String, Object> formSimulacion()
    {
        Map<String, Object> formParams = new HashMap<>();
        formParams.put(cod_promo.name(), "codPromo1");
        formParams.put(fecha_inicio.name(), "23-04-2020");
        formParams.put(fecha_fin.name(), "22-05-2020");
        formParams.put(incentivo_id.name(), "7");
        formParams.put(medio_id.name(), valueOf(medioThree));
        formParams.put(promo_medio_text.name(), textMsg_1);
        formParams.put(pg1s.name(), "1");
        return formParams;
    }
}