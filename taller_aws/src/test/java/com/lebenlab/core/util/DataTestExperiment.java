package com.lebenlab.core.util;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv;
import com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1;
import com.lebenlab.core.experimento.Experimento;
import com.lebenlab.core.experimento.PromoVariante;
import com.lebenlab.core.experimento.PromoVariante.VarianteBuilder;
import com.lebenlab.core.experimento.ResultadoPg1Dao;
import com.lebenlab.core.experimento.SqlUpdate;
import com.lebenlab.core.mediocom.DataTestMedioCom;
import com.lebenlab.core.mediocom.PromoMedioComunica;

import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import smile.math.MathEx;
import smile.stat.hypothesis.TTest;

import static com.lebenlab.core.UrlPath.login;
import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.getHeaderStr;
import static com.lebenlab.core.experimento.ParticipanteDao.insertParticipantes;
import static com.lebenlab.core.mediocom.DataTestMedioCom.medioOne;
import static com.lebenlab.core.mediocom.SqlComunicacion.del_all_promo_medio_sms;
import static com.lebenlab.core.mediocom.SqlComunicacion.del_all_promo_mediocomunicacion;
import static com.lebenlab.core.mediocom.SqlComunicacion.del_all_promo_particip_medio_hist;
import static com.lebenlab.core.mediocom.SqlComunicacion.del_all_promo_participante_medio;
import static com.lebenlab.core.tbmaster.ConceptoTaller.AD_Talleres;
import static com.lebenlab.core.tbmaster.ConceptoTaller.EuroTaller;
import static com.lebenlab.core.tbmaster.Incentivo.otros;
import static com.lebenlab.core.tbmaster.Incentivo.tarjeta_regalo;
import static com.lebenlab.core.tbmaster.Mercado.ES;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_17;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.tbmaster.PG1.PG1_3;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.tbmaster.PG1.PG1_5;
import static com.lebenlab.csv.CsvConstant.newLine;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 19:36
 */
public final class DataTestExperiment {

    private static final Logger logger = getLogger(DataTestExperiment.class);

    private DataTestExperiment()
    {
    }

    public static final String tbMasterPathParam = "tabla";

    // =============== Tablas maestras ===============

    //@formatter:off
    public static final String tbMastersUrl = login + "{" + tbMasterPathParam + "}";
    public static final String conceptoTb = "conceptos";
    public static final String conceptosJson =
            "[" +
                    "{\"conceptoId\":1,\"nombre\":\"AD_Talleres\"}," +
                    "{\"conceptoId\":2,\"nombre\":\"AllTrucks\"}," +
                    "{\"conceptoId\":3,\"nombre\":\"AutoCrew\"}," +
                    "{\"conceptoId\":4,\"nombre\":\"Autotaller\"}," +
                    "{\"conceptoId\":5,\"nombre\":\"BDC_BDS\"}," +
                    "{\"conceptoId\":6,\"nombre\":\"Bosch_Car_Service\"}," +
                    "{\"conceptoId\":7,\"nombre\":\"CGA_Car_Service\"}," +
                    "{\"conceptoId\":8,\"nombre\":\"Confortauto\"}," +
                    "{\"conceptoId\":9,\"nombre\":\"Euro_Repar\"}," +
                    "{\"conceptoId\":10,\"nombre\":\"Euromaster\"}," +
                    "{\"conceptoId\":11,\"nombre\":\"EuroTaller\"}," +
                    "{\"conceptoId\":12,\"nombre\":\"Otros\"}," +
                    "{\"conceptoId\":13,\"nombre\":\"Cecauto\"}," +
                    "{\"conceptoId\":14,\"nombre\":\"Profesional_Plus\"}" +
                    "]";
    public static final String incentivoTb = "incentivos";

    public static final String incentivosJson =
            "[" +
                    "{\"incentivoId\":1,\"nombre\":\"Descuento en metálico\"}," +
                    "{\"incentivoId\":2,\"nombre\":\"Tarjetas regalo\"}," +
                    "{\"incentivoId\":3,\"nombre\":\"Dispositivos electrónicos\"}," +
                    "{\"incentivoId\":4,\"nombre\":\"Bicicletas o patinetes eléctricos\"}," +
                    "{\"incentivoId\":5,\"nombre\":\"Viajes\"}," +
                    "{\"incentivoId\":6,\"nombre\":\"Entradas eventos deportivos\"}," +
                    "{\"incentivoId\":7,\"nombre\":\"Entradas eventos musicales\"}," +
                    "{\"incentivoId\":8,\"nombre\":\"Otros tipos de entradas\"}," +
                    "{\"incentivoId\":9,\"nombre\":\"Puntos de programas de fidelización externos\"}," +
                    "{\"incentivoId\":10,\"nombre\":\"Otro tipo de incentivos\"}" +
                    "]";

    public static final String mercadoTb = "mercados";

    public static final String mercadosJson =
            "[" +
                    "{\"mercadoId\":1,\"sigla\":\"ES\",\"nombre\":\"España\"}," +
                    "{\"mercadoId\":2,\"sigla\":\"PT\",\"nombre\":\"Portugal\"}," +
                    "{\"mercadoId\":3,\"sigla\":\"AN\",\"nombre\":\"Andorra\"}" +
                    "]";
    public static final String pg1Tb = "pg1s";

    public static final String pg1sJson =
            "[" +
                    "{\"idPg1\":1,\"descripcion\":\"Sistemas de frenado\"}," +
                    "{\"idPg1\":2,\"descripcion\":\"Iluminación\"}," +
                    "{\"idPg1\":3,\"descripcion\":\"Encendido\"}," +
                    "{\"idPg1\":4,\"descripcion\":\"Sondas Lambda\"}," +
                    "{\"idPg1\":5,\"descripcion\":\"Caudalímetros\"}," +
                    "{\"idPg1\":6,\"descripcion\":\"Sistem. de gasolina y gest. del motor\"}," +
                    "{\"idPg1\":7,\"descripcion\":\"Bujías y calentadores\"}," +
                    "{\"idPg1\":8,\"descripcion\":\"Filtros\"}," +
                    "{\"idPg1\":10,\"descripcion\":\"Escobillas\"}," +
                    "{\"idPg1\":11,\"descripcion\":\"Diésel\"}," +
                    "{\"idPg1\":12,\"descripcion\":\"Máquinas rotativas\"}," +
                    "{\"idPg1\":13,\"descripcion\":\"Correas\"}," +
                    "{\"idPg1\":14,\"descripcion\":\"Baterías\"}," +
                    "{\"idPg1\":15,\"descripcion\":\"Equipos de taller\"}," +
                    "{\"idPg1\":17,\"descripcion\":\"Sistemas de dirección\"}" +
                    "]";
    public static final String provinciaTb = "provincias";

    public static final String provinciaJson =
            "[" +
                    "{\"provinciaId\":1,\"mercadoId\":1,\"nombre\":\"Álava/Araba\"}," +
                    "{\"provinciaId\":2,\"mercadoId\":1,\"nombre\":\"Albacete\"}," +
                    "{\"provinciaId\":3,\"mercadoId\":1,\"nombre\":\"Alicante/Alacant\"}," +
                    "{\"provinciaId\":4,\"mercadoId\":1,\"nombre\":\"Almería\"},";


    // =============== Datos para experimentos ===============

    public static final String promocion1Str =
            "{" +
                    "\"codPromo\":\"promo_test01\"," +
                    "\"fechaInicio\":{\"year\":2019,\"month\":9,\"day\":29}," +
                    "\"fechaFin\":{\"year\":2019,\"month\":10,\"day\":30}," +
                    "\"mercados\":[1]," +
                    "\"conceptos\":[1,11]," +
                    "\"pg1s\":[1,2,3]," +
                    "\"incentivo\":2," +
                    "\"promoMedioComunica\":{\"promoId\":0,\"medioId\":1,\"codTextClass\":0,\"textMsg\":\"NA\"}" +
                    "}";

    public static final String promocion2Str =
            "{" +
                    "\"idPromo\":0," +
                    "\"codPromo\":\"promo_test01\"," +
                    "\"fechaInicio\":{\"year\":2019,\"month\":9,\"day\":29}," +
                    "\"fechaFin\":{\"year\":2019,\"month\":10,\"day\":30}," +
                    "\"mercados\":[1]," +
                    "\"conceptos\":[1,11]," +
                    "\"pg1s\":[6,10,4]," +
                    "\"incentivo\":4," +
                    "\"promoMedioComunica\":{\"promoId\":0,\"medioId\":3,\"codTextClass\":0,\"textMsg\":\"NA\"}," +
                    "\"experimentoId\":0" +
                    "}";

    public static final String expForJson = "" +
            "{" +
            "\"promocion\":" +
            "{" +
            "\"codPromo\":\"promo_test01\"," +
            "\"fechaInicio\":{\"year\":2019,\"month\":9,\"day\":29}," +
            "\"fechaFin\":{\"year\":2019,\"month\":10,\"day\":30}," +
            "\"mercados\":[1]," +
            "\"conceptos\":[1,11]," +
            "\"pg1s\":[1,2,3]," +
            "\"incentivo\":2" +
            "}," +
            "\"variante\":" +
            "{" +
            "\"codPromo\":\"cod_12\"," +
            "\"pg1s\":[2,4]," +
            "\"incentivo\":10," +
            "\"medioComunicacion\":2" +
            "}," +
            "\"nombre\":\"experim_AB\"" +
            "}";

    //@formatter:on

    public static final Promocion promocion1 = new Promocion.PromoBuilder()
            .conceptos(asList(AD_Talleres.conceptoId, EuroTaller.conceptoId))
            .fechaInicio(parse("2019-09-29"))
            .fechaFin(parse("2019-10-30"))
            .mercados(singletonList(ES.id))
            .codPromo("promo_test01")
            .pg1s(asList(PG1_1.idPg1, PG1_2.idPg1, PG1_3.idPg1))
            .incentivo(tarjeta_regalo.incentivoId)
            .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(medioOne).build())
            .experimentoId(0)
            .build();

    public static final Promocion promo_1A =
            new Promocion.PromoBuilder().copyPromo(promocion1).conceptos(asList(1, 2, 7)).mercados(asList(1, 2)).build();

    public static final PromoVariante variante_1A =
            new VarianteBuilder(
                    "cod_12",
                    asList(PG1_2.idPg1, PG1_4.idPg1),
                    otros.incentivoId,
                    new PromoMedioComunica.PromoMedComBuilder().medioId(DataTestMedioCom.medioTwo).build()
            ).build();

    public static final Promocion promo_2A = new Promocion.PromoBuilder().copyPromo(promocion1)
            .codPromo("codPromo_2A").pg1s(singletonList(PG1_5.idPg1)).build();
    public static final PromoVariante variante_2B =
            new VarianteBuilder(
                    "codPromo_2B",
                    asList(PG1_5.idPg1, PG1_17.idPg1),
                    tarjeta_regalo.incentivoId,
                    new PromoMedioComunica.PromoMedComBuilder().medioId(DataTestMedioCom.medioThree).build()
            ).build();

    public static final Experimento experimento1 =
            new Experimento.ExperimentoBuilder().promocion(promo_1A).variante(variante_1A).nombre("experim_AB").build();

    public static final Experimento experimento2 =
            new Experimento.ExperimentoBuilder().nombre("exp2_AB").promocion(promo_2A).variante(variante_2B).build();

    // =============================== Scripts BD ==============================

    public static final String insert_particip_test =
            "INSERT INTO participante(id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) VALUES (?, ?, ?, ?, ?)";
    public static final String alter_seq_particip = "ALTER TABLE participante AUTO_INCREMENT = 1";
    public static final String alter_seq_promo = "ALTER TABLE promo AUTO_INCREMENT = 1";

    public static void runScript(String scriptIn)
    {
        jdbiFactory.getJdbi().useHandle(handle -> handle.createScript(scriptIn).execute());
    }

    public static void runScript(String scriptIn, Jdbi jdbiIn)
    {
        jdbiIn.useHandle(handle -> handle.createScript(scriptIn).execute());
    }

    public static void insert2Participantes()
    {
        runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11');");
    }

    public static void insertExp1Promos2()
    {
        runScript("INSERT INTO  experimento (experimento_id, nombre)" +
                " VALUES (1, 'experimento1');" +
                " INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 1)," +
                "       (12, 'promo12', '2020-01-05', '2020-01-06', 1);");
    }

    public static void insertPromoParticipPg1(int rounds, long idPromo1, long idPromo2, int pg1Id)
    {
        logger.debug("insertPromoParticipPg1()");
        jdbiFactory.getJdbi().withHandle(
                h -> {
                    PreparedBatch batch = h.prepareBatch(SqlUpdate.getInsert_promo_particip_pg1_test.statement);
                    Random rnd = new Random(113);
                    for (int i = 0; i < rounds; ++i) {
                        logger.debug("insertPromoParticipPg1(); round: {}", i);
                        batch
                                .bind("promo_id", i % 2 == 0 ? idPromo1 : idPromo2)
                                .bind("participante_id", i + 1)
                                .bind("pg1_id", pg1Id)
                                .bind("vta_media_diaria_pg1_exp", rnd.nextDouble() * 100)
                                .bind("vtas_promo_pg1", rnd.nextDouble() * 100)
                                .add();
                    }
                    return assertThat(stream(batch.execute()).sum()).isEqualTo(rounds);
                }
        );
    }

    public static void cleanExpTables()
    {
        final var jdbi = jdbiFactory.getJdbi();
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_experiment.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.particip_delete_all.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promos.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promo_concepto.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promo_incentivo.statement));
        jdbi.withHandle(handle -> handle.execute(del_all_promo_mediocomunicacion.statement));
        jdbi.withHandle(handle -> handle.execute(del_all_promo_medio_sms.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promo_mercado.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promos_particip.statement));
        jdbi.withHandle(h -> h.execute(del_all_promo_participante_medio.statement));
        jdbi.withHandle(h -> h.execute(del_all_promo_particip_medio_hist.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promo_particpante_pg1.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_all_promo_pg1.statement));
        jdbi.withHandle(handle -> handle.execute(SqlUpdate.del_results.statement));
        runScript("SET FOREIGN_KEY_CHECKS = 1;", jdbi);
    }

    // =============================== Ficheros CSV ==============================

    public static final String empty_inzip_file =
            "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine + "";

    public static final String upCsvParticip = getHeaderStr() + newLine +
            "B12345X;12;7;;;2004-12-31" + newLine +
            "C98345Z;101;2;;;2019-01-02";

    public static final String upCsvParticipFull = getHeaderStr() + newLine +
            "B12345X;12;7;mail1@lebenlab.com;34600000100;2004-12-31" + newLine +
            "C98345Z;101;2;mail2@lebenlab.com;34600000200;2019-01-02";

    public static final String upCsvApertura = HeaderAperturaCsv.getHeaderStr() + newLine +
            "1111;11;2020-12-31" + newLine +
            "2222;12;2020-12-29";

    public static final String upCsvResult = HeaderResultPg1.getHeaderStr() + newLine +
            "1;11;27;2019-12-31" + newLine +
            "5;8;3;2019-01-02";
    public static final String upCsvResultWrong = HeaderResultPg1.getHeaderStr() + newLine +
            "11;16;2;2019-12-31" + newLine +
            "22;9;3;2019-01-02";
    public static final String downSampleParticipCsvEven =
            "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine +
                    "2;H98895J;2;101;1";
    public static final String downSampleParticipCsvOdd =
            "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine +
                    "1;B12345X;1;12;7" + newLine
                    + "3;C98345Z;2;101;2";

    public static void insertCsvParticip150() throws IOException
    {
        String fileString = readAllLines(FileTestUtil.csvParticipantes_150, UTF_8).stream().collect(joining(newLine.toString()));
        InputStream inStream = new ByteArrayInputStream(fileString.getBytes(UTF_8));
        assertThat(insertParticipantes(inStream)).isEqualTo(150);
    }

    public static void insertCsvResults150() throws IOException
    {
        String fileString = readAllLines(WebConnTestUtils.upLoadDir.resolve("ventas_150.csv"), UTF_8).stream().collect(joining(newLine.toString()));
        InputStream inStream = new ByteArrayInputStream(fileString.getBytes(UTF_8));
        Assertions.assertThat(ResultadoPg1Dao.resultPg1Dao.insertResults(inStream)).isEqualTo(750); // 150 * 5.
    }

    // =============================== Smile ==============================

    public static TTest getTest()
    {
        double[] x = {44.4, 45.9, 41.9, 53.3, 44.7, 44.1, 50.7, 45.2, 60.1};
        double[] y = {2.6, 3.1, 2.5, 5.0, 3.6, 4.0, 5.2, 2.8, 3.8};
        return TTest.test(MathEx.cor(x, y), x.length - 2);
    }

    public static TTest getTest(double[] seq1, double[] seq2)
    {
        return TTest.test(MathEx.cor(seq1, seq2), seq1.length - 2);
    }

    @NotNull
    public static double[][] getXDataCluster()
    {
        double[][] x = new double[6000][];
        Random rnd = new Random(113L);

        for (int i = 0; i < 2000; i++) {
            x[i] = rnd.doubles(4, 0, 1).toArray();
        }

        for (int i = 0; i < 2000; i++) {
            x[2000 + i] = x[i] = rnd.doubles(4, 0, 1).toArray();
        }

        for (int i = 0; i < 2000; i++) {
            x[4000 + i] = x[i] = rnd.doubles(4, 0, 1).toArray();
        }
        return x;
    }
}


