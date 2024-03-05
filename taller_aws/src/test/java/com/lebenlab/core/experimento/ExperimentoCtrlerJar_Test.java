package com.lebenlab.core.experimento;

import com.lebenlab.AfterJarTest;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;

import kong.unirest.HttpResponse;

import static com.lebenlab.core.UrlPath.experimentoPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.experimento.ExperimentoCtrlerLocalTest.checkZipNonEmptyFiles;
import static com.lebenlab.core.experimento.ExperimentoCtrlerLocalTest.insideHandleFileVentas;
import static com.lebenlab.core.experimento.ExperimentoCtrlerLocalTest.insideTestHandleExpProdClusters;
import static com.lebenlab.core.experimento.ExperimentoCtrlerLocalTest.mapParams;
import static com.lebenlab.core.experimento.ExperimentoCtrlerLocalTest.responseHandleNewExp;
import static com.lebenlab.core.mediocom.DataTestMedioCom.cleanMedCommTables;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id_variante;
import static com.lebenlab.core.util.DataTestExperiment.experimento1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.core.util.FileTestUtil.cleanTablesFiles;
import static com.lebenlab.core.util.FileTestUtil.csvParticipantes_11577;
import static com.lebenlab.core.util.FileTestUtil.downloadDir;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttp;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttpStr;
import static com.lebenlab.core.util.WebConnTestUtils.initLocalJar;
import static com.lebenlab.core.util.WebConnTestUtils.upLoadDir;
import static com.lebenlab.csv.CsvConstant.point_zip;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 20/10/2019
 * Time: 13:09
 * <p>
 * Comment <excludedGroups>AfterJarTest</excludedGroups>
 */
@Category(AfterJarTest.class)
public class ExperimentoCtrlerJar_Test {

    @ClassRule
    public static final ExternalResource resource = initLocalJar();

    @Before
    public void setUp() throws IOException
    {
        assertThat(isDirectory(downloadDir) && list(downloadDir).count() == 0).isTrue();
        // Diccionario en BD.
        runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 1, 0.25), ('w1', 2, 0.1), ('w1', 3, 0.9);");
    }

    @After
    public void clean() throws IOException
    {
        cleanTablesFiles();
        cleanMedCommTables();
    }

    @Test
    public void test_newExperimentOffline() throws IOException, JoseException, InvalidJwtException
    {
        // Long file.
        final var fields = mapParams(getLocalHttp(experimentoPath));
        assertThat(fields.getField(medio_id.name()).getValue()).isEqualTo("3");
        assertThat(fields.getField(medio_id_variante.name()).getValue()).isEqualTo("2");
        HttpResponse<File> samplesFileResp = responseHandleNewExp(csvParticipantes_11577, fields)
                .asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());
        checkZipNonEmptyFiles(samplesFileResp, 1);
        assertThat((int) jdbiFactory.getJdbi().withHandle(handle -> handle.execute(SqlUpdate.particip_delete_all.statement))).isEqualTo(1000);
    }

    @Test
    public void test_handleFileVentas() throws InvalidJwtException, JoseException, IOException
    {
        insideHandleFileVentas(getLocalHttpStr(ventas_file.actualPath()), upLoadDir.resolve("ventas_1000_1.csv"), 1000 * 5);
    }

    @Test
    public void test_handleExpProdClusters() throws JoseException, InvalidJwtException
    {
        insideTestHandleExpProdClusters();
    }
}
