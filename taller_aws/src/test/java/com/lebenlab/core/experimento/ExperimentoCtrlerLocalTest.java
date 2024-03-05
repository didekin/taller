package com.lebenlab.core.experimento;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.AperturasFileDaoTest;
import com.lebenlab.core.mediocom.MedioComunicacion;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.util.FileTestUtil;

import org.jetbrains.annotations.NotNull;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;

import static com.lebenlab.HttpConstant.csv_mimetype;
import static com.lebenlab.ProcessArgException.upload_file_error_msg;
import static com.lebenlab.core.Promocion.FieldLabel.cod_promo;
import static com.lebenlab.core.Promocion.FieldLabel.conceptos;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_fin;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_inicio;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.mercados;
import static com.lebenlab.core.Promocion.FieldLabel.pg1s;
import static com.lebenlab.core.UrlPath.aperturas_file;
import static com.lebenlab.core.UrlPath.communications_file;
import static com.lebenlab.core.UrlPath.exp_list_path;
import static com.lebenlab.core.UrlPath.exp_pg1_clusters_path;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.experimentoPath;
import static com.lebenlab.core.UrlPath.landingPgPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.ViewPathTest.checkMenuInHtml;
import static com.lebenlab.core.experimento.ExpPlotter.scatter_clusters_div;
import static com.lebenlab.core.experimento.ExpPlotter.scatter_plot_title;
import static com.lebenlab.core.experimento.Experimento.FieldLabel.exp_nombre;
import static com.lebenlab.core.experimento.ExperimentoDao.txPromosExperiment;
import static com.lebenlab.core.experimento.ParticipanteDao.insertParticipantes;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.cod_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.incentivo_id_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.pg1s_variante;
import static com.lebenlab.core.experimento.SqlUpdate.particip_delete_all;
import static com.lebenlab.core.mediocom.DataTestMedioCom.cleanMedCommTables;
import static com.lebenlab.core.mediocom.DataTestMedioCom.msgForMedio;
import static com.lebenlab.core.mediocom.LandingPageFlowTest.dataTestSmsPageFlow;
import static com.lebenlab.core.mediocom.LandingPageFlowTest.sms_personal_message;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.tbmaster.ConceptoTaller.AutoCrew;
import static com.lebenlab.core.tbmaster.Mercado.AN;
import static com.lebenlab.core.util.DataTestExperiment.alter_seq_particip;
import static com.lebenlab.core.util.DataTestExperiment.downSampleParticipCsvEven;
import static com.lebenlab.core.util.DataTestExperiment.downSampleParticipCsvOdd;
import static com.lebenlab.core.util.DataTestExperiment.empty_inzip_file;
import static com.lebenlab.core.util.DataTestExperiment.experimento1;
import static com.lebenlab.core.util.DataTestExperiment.insertPromoParticipPg1;
import static com.lebenlab.core.util.DataTestExperiment.insert_particip_test;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.core.util.FileTestUtil.cleanTablesFiles;
import static com.lebenlab.core.util.FileTestUtil.csvParticipantes_1000;
import static com.lebenlab.core.util.FileTestUtil.csvParticipantes_11577;
import static com.lebenlab.core.util.FileTestUtil.csvParticipantes_3;
import static com.lebenlab.core.util.FileTestUtil.csvParticipantes_empty;
import static com.lebenlab.core.util.FileTestUtil.csvVentas_2;
import static com.lebenlab.core.util.FileTestUtil.downloadDir;
import static com.lebenlab.core.util.WebConnTestUtils.doHtmlGetWithTk;
import static com.lebenlab.core.util.WebConnTestUtils.doHtmlGetWithoutTk;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttp;
import static com.lebenlab.core.util.WebConnTestUtils.getLocalHttpStr;
import static com.lebenlab.core.util.WebConnTestUtils.initJavalinInTest;
import static com.lebenlab.core.util.WebConnTestUtils.upLoadDir;
import static com.lebenlab.csv.CsvConstant.multipart_form_file_param;
import static com.lebenlab.csv.CsvConstant.point_zip;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static com.lebenlab.jwt.JwtTestUtil.doBasicClaims;
import static com.lebenlab.jwt.JwtTestUtil.doHeaderToken;
import static com.lebenlab.jwt.JwtTestUtil.putFechasOk;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.readAllBytes;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static kong.unirest.ContentType.create;
import static kong.unirest.Unirest.post;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 12:38
 * <p>
 * Unit tests with a local Javaline instance.
 */
public class ExperimentoCtrlerLocalTest {

    @ClassRule
    public static final ExternalResource resource = initJavalinInTest();

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

    // ========================= Tests ========================

    // ......... Formulario nuevo experimento  .........

    @Test
    public void test_serveNewExperiment() throws JoseException, InvalidJwtException
    {
        runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, 1, '2019-01-04', '2020-06-01:12:32:11');");
        final var bodyResp = doHtmlGetWithTk(getLocalHttp(experimentoPath)).asString().getBody();
        assertThat(bodyResp).contains("3 participantes").contains(experimentoPath.actualPath());
        // Menu
        System.out.println(bodyResp);
        checkMenuInHtml(bodyResp);
    }

    @Test
    public void test_newExperiment_0() throws JoseException, InvalidJwtException
    {
        // Sin envío de fichero.
        final var fields = mapParams(getLocalHttp(experimentoPath));
        HttpResponse<String> samplesFileResp = fields.asString();
        assertThat(samplesFileResp.getStatus()).isEqualTo(500);
        assertThat(samplesFileResp.getBody()).isEqualTo(upload_file_error_msg);
    }

    @Test
    public void test_newExperiment_1() throws IOException, JoseException, InvalidJwtException
    {
        // Sin envío de fichero y con registros previos en BD.
        jdbiFactory.getJdbi().useHandle(
                h -> {
                    h.execute(alter_seq_particip);
                    assertThat(h.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", now())).isEqualTo(1);
                    assertThat(h.execute(insert_particip_test, "H98895J", 101, 1, "2018-04-01", now())).isEqualTo(1);
                    assertThat(h.execute(insert_particip_test, "C98345Z", 101, 2, "2018-05-13", now())).isEqualTo(1);
                }
        );
        final var fields = mapMedios(getLocalHttp(experimentoPath), asList(ninguna, email));
        HttpResponse<File> samplesFileResp = fields.asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());
        // Devuelve 2 ficheros con registros en BD: medios 'ninguno' y 'mail'.
        checkZipShortNonEmptyFiles(samplesFileResp, 2);
    }

    @Test
    public void test_newExperiment_2() throws JoseException, InvalidJwtException, IOException
    {
        // Envío de fichero vacío no nulo. Sin participantes en BD.
        final var fields = mapMedios(getLocalHttp(experimentoPath), asList(ninguna, email));
        HttpResponse<File> samplesFileResp = responseHandleNewExp(csvParticipantes_empty, fields)
                .asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());

        // Devuelve dos ficheros vacíos.
        checkZipEmptyFiles(samplesFileResp, 2);
    }

    @Test
    public void test_newExperiment_3() throws IOException, JoseException, InvalidJwtException
    {
        // Envío de fichero con 3 participantes.
        final var fields = mapParams(getLocalHttp(experimentoPath));
        assertThat(fields.getField(medio_id.name()).getValue()).isEqualTo("3");
        assertThat(fields.getField(medio_id_variante.name()).getValue()).isEqualTo("2");
        HttpResponse<File> samplesFileResp = responseHandleNewExp(csvParticipantes_3, fields)
                .asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());

        // Verifica 1 fichero para medio mail.
        checkZipShortNonEmptyFiles(samplesFileResp, 1);
        // Verifica la inserción de los 3 participantes.
        assertThat((int) jdbiFactory.getJdbi().withHandle(handle -> handle.execute(particip_delete_all.statement))).isEqualTo(3);
    }

    @Test
    public void test_newExperiment_4() throws IOException, JoseException, InvalidJwtException
    {
        // Envío de fichero con miles de participantes.
        final var fields = mapMedios(getLocalHttp(experimentoPath), asList(ninguna, email));
        HttpResponse<File> samplesFileResp = responseHandleNewExp(csvParticipantes_11577, fields)
                .asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());
        // Verifica 2 ficheros.
        checkZipNonEmptyFiles(samplesFileResp, 2);
        // Verifica la inserción de los 3 participantes.
        assertThat((int) jdbiFactory.getJdbi().withHandle(handle -> handle.execute(particip_delete_all.statement))).isEqualTo(11577);
    }

    @Test
    public void test_test_newExperiment_5() throws IOException, JoseException, InvalidJwtException, InterruptedException
    {
        // Long file repetido para ver cómo responde con la BD llena y muchas repeticiones.
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(csvParticipantes_11577));
        assertThat(insertParticipantes(byteStream)).isEqualTo(11577);
        SECONDS.sleep(1);

        final var fields = mapParams(getLocalHttp(experimentoPath));
        assertThat(fields.getField(medio_id.name()).getValue()).isEqualTo("3");
        assertThat(fields.getField(medio_id_variante.name()).getValue()).isEqualTo("2");

        HttpResponse<File> samplesFileResp = responseHandleNewExp(csvParticipantes_1000, fields)
                .asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());
        // Verifica 1 fichero para mail.
        checkZipNonEmptyFiles(samplesFileResp, 1);
        // 1000 es el tamaño del fichero con actualizaciones.
        assertThat((int) jdbiFactory.getJdbi().withHandle(handle -> handle.execute(SqlUpdate.particip_delete_all.statement))).isEqualTo(1000);
    }

    @Test
    public void test_newExperiment_6() throws JoseException, InvalidJwtException, IOException
    {
        // Envío de fichero con 3 participantes. Selección de mercados y conceptos sin público.
        final var fields = mapMercadosConceptos(getLocalHttp(experimentoPath), singletonList(AN), singletonList(AutoCrew));
        assertThat(fields.getField(medio_id.name()).getValue()).isEqualTo("3");
        assertThat(fields.getField(medio_id_variante.name()).getValue()).isEqualTo("2");
        HttpResponse<File> samplesFileResp = responseHandleNewExp(csvParticipantes_3, fields)
                .asFile(downloadDir.resolve(experimento1.nombre + point_zip).toString());

        // Check: 1 fichero vacío para 'mail'.
        checkZipEmptyFiles(samplesFileResp, 1);
    }

    @Test
    public void test_newExperiment_7() throws InvalidJwtException, JoseException, IOException
    {
        final var fields = mapMedios(getLocalHttp(experimentoPath), asList(sms, sms));
        final var bodyStr = responseHandleNewExp(csvParticipantes_3, fields).asString().getBody();
        assertThat(bodyStr).contains("Consulta de experimentos");
        checkMenuInHtml(bodyStr);

    }

    // ......... Resultados de experimentos .........

    @Test
    public void test_handleExperimentList_1() throws JoseException, InvalidJwtException
    {
        long experimento1_id = txPromosExperiment(experimento1).get(0).experimentoId;

        String respBody = doHtmlGetWithTk(getLocalHttpStr(exp_list_path.actualPath())).asString().getBody();
        assertThat(respBody).contains("class=\"nombreExperimento\">" + experimento1.nombre);
        assertThat(respBody).contains("class=\"fechaInicio\">" + experimento1.promocion.fechaInicio);
        assertThat(respBody).contains("class=\"fechaFin\">" + experimento1.promocion.fechaFin);
        assertThat(respBody).contains("class=\"campanaA\">" + experimento1.promocion.codPromo);
        assertThat(respBody).contains("class=\"campanaB\">" + experimento1.variante.codPromo);
        // Enlace a Consulta de Experimento.
        assertThat(respBody).contains("location = '" + exp_statistics_path.actualPath(Long.toString(experimento1_id)) + "';");
        checkMenuInHtml(respBody);
    }

    @Test
    public void test_handleExperimentList_2() throws JoseException, InvalidJwtException
    {
        // Sin experimentos en BD.
        String respBody = doHtmlGetWithTk(getLocalHttpStr(exp_list_path.actualPath())).asString().getBody();
        assertThat(respBody).contains("Consulta de experimentos");
        checkMenuInHtml(respBody);
    }

    @Test
    public void test_handleStatisticsExperiment_1() throws JoseException, InvalidJwtException
    {
        // Caso: dos participantes en un experimento, diferentes promociones.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  experimento (experimento_id, nombre) VALUES (1, 'exp_1');");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_mercado (promo_id, mercado_id) VALUES (11, 1), (12, 1);");
        runScript("INSERT INTO promo_incentivo (promo_id, incentivo_id) VALUES (11, 5), (12, 6);");
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id) VALUES (11, 1), (12, 2);");
        runScript("INSERT INTO promo_concepto (promo_id, concepto_id) VALUES (11, 5), (12, 5);");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) VALUES (11, 1, 10, 11), (12, 1, 10, 0);");

        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 100), (12, 112, 1, 200);");
        runScript("INSERT INTO promo_medio_sms (promo_id, batch_id) VALUES (12, '01F8392Q6C3VNM0B54E87T107P');");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        String bodyResp = doHtmlGetWithTk(getLocalHttpStr(exp_statistics_path.urlBracesPathParam()))
                .routeParam(exp_statistics_path.pathParamName(), valueOf(1L))
                .asString()
                .getBody();
        assertThat(bodyResp).contains("exp_1");
        assertThat(bodyResp).contains("id=\"fechaInicio\">2020-01-05");
        assertThat(bodyResp).contains("id=\"fechaFin\">2020-01-06");
        assertThat(bodyResp).contains("promo11");
        assertThat(bodyResp).contains("promo12");

        // CASO 1:  medios: ninguna y sms. No muestra recibidos, ni aperturas.
        assertThat(bodyResp).doesNotContain("Aperturas(%)");

        assertThat(bodyResp).contains("<strong>" + "4,76");   //media
        assertThat(bodyResp).contains("<strong>" + "9,09");   //media
        assertThat(bodyResp).contains("<td>1</td>");   // participantes
        // Menu
        checkMenuInHtml(bodyResp);
        // Enlace a clusters del experimento.
        assertThat(bodyResp).contains(exp_pg1_clusters_path.actualPath(1L, 1));

        // CASO 2: añadimos 1 registro a promo_participante_medio: muestra recibidos y aperturas.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg) " +
                "VALUES (12, 112, 2, TRUE, FALSE);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");
        bodyResp = doHtmlGetWithTk(getLocalHttpStr(exp_statistics_path.urlBracesPathParam()))
                .routeParam(exp_statistics_path.pathParamName(), valueOf(1L))
                .asString()
                .getBody();
        assertThat(bodyResp).containsOnlyOnce("Recibidos(%)").contains("100");
        assertThat(bodyResp).containsOnlyOnce("Aperturas(%)").contains("0");
    }

    @Test
    public void test_handleStatisticsExperiment_2() throws JoseException, InvalidJwtException
    {
        // Caso: dos participantes en un experimento, diferentes promociones.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  experimento (experimento_id, nombre) VALUES (1, 'exp_1');");
        runScript(" INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_mercado (promo_id, mercado_id) VALUES (11, 1), (12, 1);");
        runScript("INSERT INTO promo_incentivo (promo_id, incentivo_id) VALUES (11, 5), (12, 6);");

        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id, promo_medio_text) VALUES (11, 3, 'email_text_1'), (12, 1, 'NA');");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg) " +
                "VALUES (11, 111, 3, TRUE, TRUE), (11, 112, 1, FALSE, FALSE);");

        runScript("INSERT INTO promo_concepto (promo_id, concepto_id) VALUES (11, 5), (12, 5);");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) VALUES (11, 1, 10, 11), (12, 1, 10, 0);");
        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 100), (12, 112, 1, 200);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        String bodyResp = doHtmlGetWithTk(getLocalHttpStr(exp_statistics_path.urlBracesPathParam()))
                .routeParam(exp_statistics_path.pathParamName(), valueOf(1L))
                .asString()
                .getBody();

        // Medios: email y ninguna.
        assertThat(bodyResp).containsOnlyOnce("Recibidos(%)").contains("100");
        assertThat(bodyResp).containsOnlyOnce("Aperturas(%)").contains("100");
        assertThat(bodyResp).contains("email").contains("email_text_1").contains("ninguna").contains("NA");
    }

    @Test
    public void test_handleExpProdClusters() throws JoseException, InvalidJwtException
    {
        insideTestHandleExpProdClusters();
    }

    // ......... Carga del fichero de aperturas .........

    @Test
    public void test_handleServeFormFileAperturas() throws JoseException, InvalidJwtException
    {
        final var bodyResp = doHtmlGetWithTk(getLocalHttpStr(aperturas_file.actualPath()))
                .asString().getBody();
        assertThat(bodyResp).contains("Carga de fichero de aperturas");
        checkMenuInHtml(bodyResp);
    }

    @Test
    public void test_handleFileAperturas() throws InvalidJwtException, JoseException, IOException
    {
        AperturasFileDaoTest.dataTestMedCom();

        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));

        HttpResponse<String> response = post(getLocalHttpStr(aperturas_file.actualPath()))
                .header(tokenHeader[0], tokenHeader[1])
                .field(multipart_form_file_param.toString(),
                        newInputStream(FileTestUtil.csvAperturas_2),
                        create(csv_mimetype.toString(), UTF_8),
                        FileTestUtil.csvAperturas_2.toString())
                .asString();
        assertThat(response.getBody())
                .contains("Resultado de la carga del fichero de aperturas")
                .contains("Registros cargados correctamente</h2>");
        checkMenuInHtml(response.getBody());
    }

    // ......... Carga del fichero de comunicaciones .........

    @Test
    public void test_serveFormFileCommunications() throws JoseException, InvalidJwtException
    {
        final var bodyResp = doHtmlGetWithTk(getLocalHttpStr(communications_file.actualPath()))
                .asString().getBody();
        assertThat(bodyResp).contains("Carga de fichero de comunicaciones").contains(communications_file.actualPath());
        checkMenuInHtml(bodyResp);
    }

    @Test
    public void test_handleFileCommunications() throws InvalidJwtException, IOException, JoseException
    {
        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));

        HttpResponse<String> response = post(getLocalHttpStr(communications_file.actualPath()))
                .header(tokenHeader[0], tokenHeader[1])
                .field(multipart_form_file_param.toString(),
                        newInputStream(FileTestUtil.csvComunicaciones_6),
                        create(csv_mimetype.toString(), UTF_8),
                        FileTestUtil.csvComunicaciones_6.toString())
                .asString();
        assertThat(response.getBody())
                .contains("Resultado de la carga de comunicaciones")
                .contains("<p id=\"numeroRegistrosCargados\">" + 6 + "</p>");
        checkMenuInHtml(response.getBody());
    }

    // ......... Carga del fichero de ventas .........

    @Test
    public void test_handleServeFormFileVentas() throws JoseException, InvalidJwtException
    {
        final var bodyResp = doHtmlGetWithTk(getLocalHttpStr(ventas_file.actualPath()))
                .asString().getBody();
        assertThat(bodyResp).contains("Carga de fichero de ventas");
        checkMenuInHtml(bodyResp);
    }

    @Test
    public void test_handleFileVentas_1() throws InvalidJwtException, JoseException, IOException
    {
        insideHandleFileVentas(getLocalHttpStr(ventas_file.actualPath()), csvVentas_2, 2);
    }

    @Test
    public void test_handleFileVentas_2() throws InvalidJwtException, JoseException, IOException
    {
        insideHandleFileVentas(getLocalHttpStr(ventas_file.actualPath()), upLoadDir.resolve("ventas_1000_1.csv"), 5000);
    }

    // ......... Apertura SMS .........

    @Test
    public void test_serveLandingPgSms()
    {
        dataTestSmsPageFlow();
        String bodyResp = doHtmlGetWithoutTk(getLocalHttpStr(landingPgPath.urlBracesPathParam()))
                .routeParam(landingPgPath.pathParamName(), "11_111")
                .asString()
                .getBody();
        assertThat(bodyResp).contains(sms_personal_message);
    }

    // .............. Utilities .................

    static MultipartBody minimumParams(String httpUrl) throws InvalidJwtException, JoseException
    {
        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));
        return post(httpUrl)
                .header(tokenHeader[0], tokenHeader[1])
                .field(exp_nombre.name(), "experimento1")
                .field(cod_promo.name(), "codPromo1")
                .field(fecha_inicio.name(), "23-04-2020")
                .field(fecha_fin.name(), "22-05-2020")
                .field(incentivo_id.name(), "7")
                .field(cod_variante.name(), "codVariante1")
                .field(pg1s_variante.name(), "11")
                .field(incentivo_id_variante.name(), "4")
                .field(pg1s.name(), asList("5", "11"));
    }

    @NotNull
    public static MultipartBody mapParams(String httpUrl) throws InvalidJwtException, JoseException
    {
        return minimumParams(httpUrl)
                .field(mercados.name(), asList("1", "2"))
                .field(conceptos.name(), asList("1", "2", "7"))
                .field(medio_id.name(), "3")
                .field(promo_medio_text.name(), "emailMsg_text")
                .field(medio_id_variante.name(), "2")
                .field(promo_medio_text_variante.name(), "SMSMsg_text");
    }

    public static MultipartBody mapMedios(String httpUrl, List<MedioComunicacion> medios) throws InvalidJwtException, JoseException
    {
        assertThat(medios.size()).isEqualTo(2);
        return minimumParams(httpUrl)
                .field(mercados.name(), asList("1", "2"))
                .field(conceptos.name(), asList("1", "2", "7"))
                .field(medio_id.name(), valueOf(medios.get(0).id))
                .field(promo_medio_text.name(), msgForMedio(medios.get(0)))
                .field(medio_id_variante.name(), valueOf(medios.get(1).id))
                .field(promo_medio_text_variante.name(), msgForMedio(medios.get(1)));
    }

    public static MultipartBody mapMercadosConceptos(String httpUrl, List<Mercado> mercadosIn, List<ConceptoTaller> conceptosIn) throws InvalidJwtException, JoseException
    {
        return minimumParams(httpUrl)
                .field(mercados.name(), mercadosIn.stream().map(mercado -> valueOf(mercado.id)).collect(toList()))
                .field(conceptos.name(), conceptosIn.stream().map(concepto -> concepto.conceptoId).collect(toList()))
                .field(medio_id.name(), "3")
                .field(promo_medio_text.name(), "emailMsg_text")
                .field(medio_id_variante.name(), "2")
                .field(promo_medio_text_variante.name(), "SMSMsg_text");
    }

    public static MultipartBody responseHandleNewExp(Path uploadFilePath, MultipartBody bodyParams)
            throws IOException
    {
        return bodyParams
                .field(multipart_form_file_param.toString(),
                        newInputStream(uploadFilePath),
                        create(csv_mimetype.toString(), UTF_8),
                        uploadFilePath.toString());
    }

    static List<ZipEntry> checkZipFiles(HttpResponse<File> samplesFileResp, ZipFile zipFile) throws IOException
    {
        assertThat(samplesFileResp.getStatus()).isEqualTo(200);
        assertThat(list(downloadDir).count()).isEqualTo(1);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        List<ZipEntry> entryList = new ArrayList<>(2);
        while (entries.hasMoreElements()) {
            entryList.add(entries.nextElement());
        }
        return entryList;
    }

    static void checkZipEmptyFiles(HttpResponse<File> samplesFileResp, int numZipFiles) throws IOException
    {
        try (ZipFile zipFile = new ZipFile(downloadDir.resolve(experimento1.nombre + point_zip).toString(), UTF_8)) {
            final var zipEntries = checkZipFiles(samplesFileResp, zipFile);
            assertThat(zipEntries.size()).isEqualTo(numZipFiles);
            for (ZipEntry entry : zipEntries) {
                assertThat(new String(zipFile.getInputStream(entry).readAllBytes(), UTF_8)).isEqualTo(empty_inzip_file);
            }
        }
    }

    static void checkZipNonEmptyFiles(HttpResponse<File> samplesFileResp, int numZipFiles) throws IOException
    {
        try (ZipFile zipFile = new ZipFile(downloadDir.resolve(experimento1.nombre + point_zip).toString(), UTF_8)) {
            final var zipEntries = checkZipFiles(samplesFileResp, zipFile);
            assertThat(zipEntries.size()).isEqualTo(numZipFiles);
            for (ZipEntry entry : zipEntries) {
                assertThat(new String(zipFile.getInputStream(entry).readAllBytes(), UTF_8).length()).isGreaterThan(empty_inzip_file.length() * 3);
            }
        }
    }

    static void checkZipShortNonEmptyFiles(HttpResponse<File> samplesFileResp, int numZipFiles) throws IOException
    {
        try (ZipFile zipFile = new ZipFile(downloadDir.resolve(experimento1.nombre + point_zip).toString(), UTF_8)) {
            final var zipEntries = checkZipFiles(samplesFileResp, zipFile);
            assertThat(zipEntries.size()).isEqualTo(numZipFiles);
            final Predicate<String> isOk = s -> s.equals(downSampleParticipCsvEven) || s.equals(downSampleParticipCsvOdd);
            assertThat(isOk.test(new String(zipFile.getInputStream(zipEntries.get(0)).readAllBytes(), UTF_8))).isTrue();
            if (numZipFiles == 2) {
                assertThat(isOk.test(new String(zipFile.getInputStream(zipEntries.get(1)).readAllBytes(), UTF_8))).isTrue();
            }
        }
    }

    static void insideHandleFileVentas(String fullHttpPath, Path uploadFilePath, int records) throws
            JoseException, InvalidJwtException, IOException
    {
        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));

        HttpResponse<String> response = post(fullHttpPath)
                .header(tokenHeader[0], tokenHeader[1])
                .field(multipart_form_file_param.toString(),
                        newInputStream(uploadFilePath),
                        create(csv_mimetype.toString(), UTF_8),
                        uploadFilePath.toString())
                .asString();
        assertThat(response.getBody())
                .contains("Resultado de la carga del fichero de ventas")
                .contains("<p id=\"numeroRegistrosCargados\">" + records + "</p>");
        checkMenuInHtml(response.getBody());
    }

    static void insideTestHandleExpProdClusters() throws InvalidJwtException, JoseException
    {
        // Insertamos experimento
        final var promos = new ArrayList<>(txPromosExperiment(experimento1));
        promos.sort(comparingLong(Promocion::getIdPromo));
        final var promo1 = promos.get(0);
        final var promo2 = promos.get(1);
        final var pg1Id = experimento1.pg1sToExperiment().get(0);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        insertPromoParticipPg1(500, promo1.idPromo, promo2.idPromo, pg1Id);
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        final var bodyResp =
                doHtmlGetWithTk(getLocalHttpStr(exp_pg1_clusters_path.actualPath(promo1.experimentoId, pg1Id))).asString().getBody();
        assertThat(bodyResp).contains(scatter_plot_title + promo1.codPromo)
                .contains(scatter_plot_title + promo2.codPromo)
                .contains(scatter_clusters_div + promo1.idPromo)
                .contains(scatter_clusters_div + promo2.idPromo);
        // Menu
        checkMenuInHtml(bodyResp);
    }
}
