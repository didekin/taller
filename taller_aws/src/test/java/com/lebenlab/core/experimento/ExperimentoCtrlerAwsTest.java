package com.lebenlab.core.experimento;

import com.lebenlab.AwsTest;
import com.lebenlab.core.util.FileTestUtil;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.file.Path;

import static com.lebenlab.core.UrlPath.exp_list_path;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.experimentoPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.ViewPathTest.checkMenuInHtml;
import static com.lebenlab.core.util.WebConnTestUtils.doHtmlGetWithTk;
import static com.lebenlab.core.util.WebConnTestUtils.getArecordHttp;
import static com.lebenlab.core.util.WebConnTestUtils.getArecordHttpStr;
import static java.lang.String.valueOf;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static java.nio.file.Files.newDirectoryStream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 20/10/2019
 * Time: 13:09
 * <p>
 * Comment <excludedGroups>AwsTest</excludedGroups>
 */
@Category(AwsTest.class)
public class ExperimentoCtrlerAwsTest {

    @Before
    public void setUp() throws IOException
    {
        assertThat(isDirectory(FileTestUtil.downloadDir) && list(FileTestUtil.downloadDir).count() == 0).isTrue();
    }

    @After
    public void clean() throws IOException
    {
        for (Path file : newDirectoryStream(FileTestUtil.downloadDir)) {
            delete(file);
        }
    }

    @Test  // GET https://lebendata1.net/close/experimento
    public void test_serveNewExperiment() throws JoseException, InvalidJwtException
    {
        // Provisional: devuelve 0, porque no hay registros en BD.
        final var bodyResp = doHtmlGetWithTk(getArecordHttp(experimentoPath)).asString().getBody();
        assertThat(bodyResp).contains("0 participantes").contains(experimentoPath.actualPath());
        checkMenuInHtml(bodyResp);
    }

    @Test  // POST https://lebendata1.net/close/experimento
    public void test_handleNewExperiment() throws IOException, JoseException, InvalidJwtException
    {
        // Script manual insert_datatest.sql

        /*assertThat(isDirectory(FileTestUtil.downloadDir) && list(FileTestUtil.downloadDir).count() == 0).isTrue();
        // Long file.
        HttpResponse<File> samplesFileResp = ExperimentoCtrlerLocalTest.responseHandleNewExp(getArecordHttp(experimentoPath), FileTestUtil.csvParticipantes_11577)
                .asFile(FileTestUtil.downloadDir.resolve(experimento1.nombre + point_zip).toString());
        ExperimentoCtrlerLocalTest.checkZipNonEmptyFiles(samplesFileResp, 1);*/  // TODO: rehacer.

        // Script delete_aws_db_test.sql
    }

    @Test   // GET https://lebendata1.net/close/experimento/list
    public void test_handleExperimentList() throws JoseException, InvalidJwtException
    {
        // Script manual insert_datatest.sql

        String respBody = doHtmlGetWithTk(getArecordHttpStr(exp_list_path.actualPath())).asString().getBody();
        assertThat(respBody).contains("class=\"nombreExperimento\">" + "exp_1");
        /*assertThat(respBody).contains("class=\"fechaInicio\">019-01-01");
        assertThat(respBody).contains("class=\"fechaFin\">2019-02-28");
        assertThat(respBody).contains("class=\"campanaA\">cod_pr_1_A");
        assertThat(respBody).contains("class=\"campanaB\">cod_pr_1_B");*/
        // Enlace a Conslta de Experimento.
        assertThat(respBody).contains("location = '" + exp_statistics_path.actualPath(Long.toString(1)) + "';");
        checkMenuInHtml(respBody);

        // Script delete_aws_db_test.sql
    }

    @Test  // GET https://lebendata1.net/close/experimento/experimento_id
    public void test_handleStatisticsExperiment() throws JoseException, InvalidJwtException
    {
        // Script manual insert_datatest.sql

        // Método.
        String bodyResp = doHtmlGetWithTk(getArecordHttpStr(exp_statistics_path.urlBracesPathParam()))
                .routeParam(exp_statistics_path.pathParamName(), valueOf(1L))
                .asString()
                .getBody();

        assertThat(bodyResp).contains(Long.toString(11));

        // Script delete_aws_db_test.sql
    }

    @Test
    public void test_handleServeFormFileVentas() throws JoseException, InvalidJwtException
    {
        final var bodyResp = doHtmlGetWithTk(getArecordHttpStr(ventas_file.actualPath()))
                .asString().getBody();
        assertThat(bodyResp).contains("Carga de fichero de ventas");
        checkMenuInHtml(bodyResp);
    }

    @Test  // POST  https://lebendata1.net/close/experimento/ventas
    public void test_handleFileVentas() throws InvalidJwtException, JoseException, IOException
    {
        ExperimentoCtrlerLocalTest.insideHandleFileVentas(getArecordHttpStr(ventas_file.actualPath()), FileTestUtil.csvVentas_2, 2);
        // Script delete_aws_db_test.sql
    }
}
