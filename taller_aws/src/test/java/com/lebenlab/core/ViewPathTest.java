package com.lebenlab.core;

import com.lebenlab.core.experimento.Experimento;
import com.lebenlab.core.experimento.ResultadoExp;
import com.lebenlab.core.mediocom.AperturasFileDaoTest;
import com.lebenlab.core.mediocom.DataTestMedioCom;
import com.lebenlab.core.mediocom.MedioComunicacion;
import com.lebenlab.core.mediocom.MedioComunicacion.MedioComunicaJson;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.mediocom.PromoMedioComunica.PromoMedComFacade;
import com.lebenlab.core.simulacion.ResultAggregSimulation;
import com.lebenlab.core.simulacion.SimPlotter;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.ConceptoTaller.ConceptoForJson;
import com.lebenlab.core.tbmaster.Incentivo;
import com.lebenlab.core.tbmaster.Incentivo.IncentivoJson;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.Mercado.MercadoForJson;
import com.lebenlab.core.tbmaster.PG1;
import com.lebenlab.core.tbmaster.PG1.Pg1ForJson;
import com.lebenlab.core.util.DataTestExperiment;
import com.lebenlab.core.util.DataTestSimulation;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo;
import static com.lebenlab.core.UrlPath.aperturas_file;
import static com.lebenlab.core.UrlPath.communications_file;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.sim_pg1_cluster_path;
import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.ViewPath.ViewLabels.check_aperturas;
import static com.lebenlab.core.ViewPath.ViewLabels.count_participantes;
import static com.lebenlab.core.ViewPath.ViewLabels.exp_pg1_clusters_path;
import static com.lebenlab.core.ViewPath.ViewLabels.experimentos;
import static com.lebenlab.core.ViewPath.ViewLabels.form_action;
import static com.lebenlab.core.ViewPath.ViewLabels.menu_items;
import static com.lebenlab.core.ViewPath.ViewLabels.pg1_clusters_plots_div;
import static com.lebenlab.core.ViewPath.ViewLabels.pg1_clusters_plots_div_1;
import static com.lebenlab.core.ViewPath.ViewLabels.pg1_clusters_plots_div_2;
import static com.lebenlab.core.ViewPath.ViewLabels.pg1_clusters_plots_script;
import static com.lebenlab.core.ViewPath.ViewLabels.pg1_clusters_plots_script_1;
import static com.lebenlab.core.ViewPath.ViewLabels.pg1_clusters_plots_script_2;
import static com.lebenlab.core.ViewPath.ViewLabels.promo_simulacion;
import static com.lebenlab.core.ViewPath.ViewLabels.records_in;
import static com.lebenlab.core.ViewPath.ViewLabels.result_aggr_simulation;
import static com.lebenlab.core.ViewPath.ViewLabels.resultado_exp;
import static com.lebenlab.core.ViewPath.ViewLabels.resultado_exp_path;
import static com.lebenlab.core.ViewPath.ViewLabels.sim_pg1_clusters_path;
import static com.lebenlab.core.ViewPath.aperturas_file_form;
import static com.lebenlab.core.ViewPath.aperturas_file_result;
import static com.lebenlab.core.ViewPath.communications_file_form;
import static com.lebenlab.core.ViewPath.communications_file_result;
import static com.lebenlab.core.ViewPath.exp_pg1_clusters;
import static com.lebenlab.core.ViewPath.experimento_form;
import static com.lebenlab.core.ViewPath.experimento_list;
import static com.lebenlab.core.ViewPath.experimento_statistics;
import static com.lebenlab.core.ViewPath.landing_pg_sms;
import static com.lebenlab.core.ViewPath.menuPathsList;
import static com.lebenlab.core.ViewPath.sim_pg1_clusters;
import static com.lebenlab.core.ViewPath.simulacion_form;
import static com.lebenlab.core.ViewPath.simulacion_result;
import static com.lebenlab.core.ViewPath.ventas_file_form;
import static com.lebenlab.core.ViewPath.ventas_file_result;
import static com.lebenlab.core.experimento.ExpPlotter.scatter_clusters_div;
import static com.lebenlab.core.experimento.ExpPlotter.scatter_plot_title;
import static com.lebenlab.core.experimento.ExpStatisticsFlowTest.checkExperimentResult;
import static com.lebenlab.core.experimento.ExperimentoDao.txPromosExperiment;
import static com.lebenlab.core.mediocom.LandingPageFlowTest.dataTestSmsPageFlow;
import static com.lebenlab.core.mediocom.LandingPageFlowTest.sms_personal_message;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_promo;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medios_comunicacion;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.sms_custom_page_txt;
import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingLong;
import static java.util.EnumSet.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 12/06/2020
 * Time: 18:20
 */
@SuppressWarnings("unchecked")
public class ViewPathTest {

    @After
    public void clean()
    {
        DataTestSimulation.cleanSimTables();
        DataTestMedioCom.cleanMedCommTables();
    }

    @Test
    public void test_experimento_form()
    {
        runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, 1, '2019-01-04', '2020-06-01:12:32:11');");

        final Map<String, ?> model = experimento_form.getModelView();
        assertThat(model.get(count_participantes.name())).isEqualTo(3);
        assertThat(model.get(form_action.name())).isEqualTo(UrlPath.experimentoPath.actualPath());

        checkCombos(model);
        // Menu
        checkMenu(model);
    }

    @Test
    public void test_experimento_list()
    {
        txPromosExperiment(DataTestExperiment.experimento1);

        final var model = experimento_list.getModelView();
        Experimento experimento = ((List<Experimento>) model.get(experimentos.name())).get(0);
        assertThat(experimento.promocion).extracting("codPromo", "fechaInicio", "fechaFin", "incentivo").containsExactlyInAnyOrder(
                DataTestExperiment.experimento1.promocion.codPromo,
                DataTestExperiment.experimento1.promocion.fechaInicio,
                DataTestExperiment.experimento1.promocion.fechaFin,
                DataTestExperiment.experimento1.promocion.incentivo);
        assertThat(experimento.variante).extracting("codPromo", "incentivo").containsExactlyInAnyOrder(
                DataTestExperiment.experimento1.variante.codPromo,
                DataTestExperiment.experimento1.variante.incentivo);
        assertThat(experimento.nombre).isEqualTo(DataTestExperiment.experimento1.nombre);
        assertThat(model.get(resultado_exp_path.name())).isEqualTo(exp_statistics_path);
        // Menu
        checkMenu(model);
    }

    @Test
    public void test_experimento_statistics()
    {
        // Caso: dos participantes en un experimento, diferentes promociones.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO experimento (experimento_id, nombre) VALUES (1, 'exp_1');");
        runScript(" INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, dias_con_resultados, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-06', 21, 1)," +
                "        (12, 'promo12', '2020-01-08', '2020-01-09', 22, 1);");
        runScript("INSERT INTO promo_mercado (promo_id, mercado_id) VALUES (11, 1), (12, 1);");
        runScript("INSERT INTO promo_incentivo (promo_id, incentivo_id) VALUES (11, 5), (12, 6);");

        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id, promo_medio_text)" +
                "VALUES (11, 3, 'email_text_1'), (12, 1, 'NA');");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg) " +
                "VALUES (11, 111, 3, TRUE, FALSE), (11, 112, 1, FALSE, FALSE);");

        runScript("INSERT INTO promo_concepto (promo_id, concepto_id) VALUES (11, 5), (12, 5);");
        runScript("INSERT INTO promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2) VALUES (11, 1, 10, 11), (12, 1, 10, 0);");

        runScript("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id, vtas_promo_pg1)" +
                " VALUES (11, 111, 1, 100), (12, 112, 1, 200);");
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        final var model = experimento_statistics.getModelView(valueOf(1L));
        // ======= Checks =========
        // Menu
        checkMenu(model);
        // Paths
        assertThat(model.get(exp_pg1_clusters_path.name())).isEqualTo(UrlPath.exp_pg1_clusters_path);
        // Resultado
        final var resultExp = (ResultadoExp) model.get(resultado_exp.name());
        checkExperimentResult(resultExp);
        assertThat(((Boolean) model.get(check_aperturas.name())).booleanValue()).isTrue();
        // MedioPromo
        assertThat(((PromoMedComFacade) model.get(medio_promo.name()))).usingRecursiveComparison()
                .isEqualTo(new PromoMedComFacade(11L, "email_text_1", email.name()));
        assertThat(((PromoMedComFacade) model.get(medio_variante.name()))).usingRecursiveComparison()
                .isEqualTo(new PromoMedComFacade(12L, "NA", ninguna.name()));
    }

    @Test
    public void test_exp_pg1_clusters()
    {
        // Insertamos experimento
        final var promos = new ArrayList<>(txPromosExperiment(DataTestExperiment.experimento1));
        promos.sort(comparingLong(Promocion::getIdPromo));
        final var promo1 = promos.get(0);
        final var promo2 = promos.get(1);
        final var pg1Id = DataTestExperiment.experimento1.pg1sToExperiment().get(0);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        DataTestExperiment.insertPromoParticipPg1(2000, promo1.idPromo, promo2.idPromo, pg1Id);
        runScript("SET FOREIGN_KEY_CHECKS = 1;");

        final var mapModel = exp_pg1_clusters.getModelView(pg1Id, promos.get(0).experimentoId);
        assertThat(mapModel.get(pg1_clusters_plots_script_1.name()).toString()).containsOnlyOnce(scatter_plot_title + promo1.codPromo);
        assertThat(mapModel.get(pg1_clusters_plots_script_2.name()).toString()).containsOnlyOnce(scatter_plot_title + promo2.codPromo);
        assertThat(mapModel.get(pg1_clusters_plots_div_1.name()).toString()).isEqualTo(scatter_clusters_div + promo1.idPromo);
        assertThat(mapModel.get(pg1_clusters_plots_div_2.name()).toString()).isEqualTo(scatter_clusters_div + promo2.idPromo);
        // Menu
        checkMenu(mapModel);
    }

    @Test
    public void test_simulacion_form()
    {
        final var model = simulacion_form.getModelView(DataTestExperiment.promocion1);
        assertThat(model.get(form_action.name())).isEqualTo(simulacionPath.actualPath());
        checkCombos(model);
        // Menu
        checkMenu(model);
    }

    @Test
    public void test_simulacion_result()
    {
        final var promoIn = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(DataTestMedioCom.medioThree).textMsg("email_sim_1").build())
                .build();
        final var model = simulacion_result.getModelView(promoIn);

        // Result model.
        ResultAggregSimulation result = (ResultAggregSimulation) model.get(result_aggr_simulation.name());
        assertThat(result.resultAvgPG1s).isNotNull();
        // Promoción
        final Promocion promoOut = (Promocion) model.get(promo_simulacion.name());
        assertThat(promoOut).usingRecursiveComparison().ignoringFields("idPromo", "promoMedioComunica").isEqualTo(promoIn);
        assertThat(promoOut.idPromo).isNotEqualTo(DataTestExperiment.promocion1.idPromo).isGreaterThan(0L);
        // Mediocomunicacion.
        final PromoMedComFacade promoMedComOut = (PromoMedComFacade) model.get(medio_promo.name());
        assertThat(promoMedComOut)
                .matches(facade -> facade.medioName.equals(MedioComunicacion.fromIdToInstance(DataTestMedioCom.medioThree).name())
                        && facade.getTextMsg().equals("email_sim_1"));
        assertThat(promoMedComOut.promoId).isGreaterThan(0L).isEqualTo(promoOut.idPromo);
        // Enlaces
        assertThat(model.get(sim_pg1_clusters_path.name())).isEqualTo(sim_pg1_cluster_path);
        // Menu
        checkMenu(model);
    }

    @Test
    public void test_sim_pg1_clusters()
    {
        long promoIdIn = modelSimulateDao.insertPromoSimulacion(DataTestExperiment.promocion1);
        final var model = sim_pg1_clusters.getModelView(promoIdIn, DataTestExperiment.promocion1.pg1s.get(0));

        // Sin datos ni modelo.
        assertThat(model.get(pg1_clusters_plots_script.name()).toString()).containsOnlyOnce(no_data_for_prediction);
        assertThat(model.get(pg1_clusters_plots_div.name()).toString()).isEqualTo(SimPlotter.scatter_clusters_div);
        // Menu
        checkMenu(model);
    }

    @Test
    public void test_aperturas_file_form()
    {
        final var modelView = aperturas_file_form.getModelView(23);
        // Menu
        checkMenu(modelView);
        assertThat(modelView.get(form_action.name())).isEqualTo(aperturas_file.actualPath());
    }

    @Test
    public void test_aperturas_file_result()
    {
        AperturasFileDaoTest.dataTestMedCom();
        final var modelView = aperturas_file_result.getModelView(new ByteArrayInputStream(DataTestMedioCom.upCsvAperturas().getBytes(UTF_8)));
        // Menu
        checkMenu(modelView);
        assertThat(modelView.get(records_in.name())).isEqualTo(2);
    }

    @Test
    public void test_communications_file_form()
    {
        final var modelView = communications_file_form.getModelView();
        // Menu
        checkMenu(modelView);
        assertThat(modelView.get(form_action.name())).isEqualTo(communications_file.actualPath());
    }

    @Test
    public void test_communications_file_result()
    {
        final var modelView = communications_file_result
                .getModelView(new ByteArrayInputStream(DataTestMedioCom.upCsvCommunications.getBytes(UTF_8)));
        // Menu
        checkMenu(modelView);
        assertThat(modelView.get(records_in.name())).isEqualTo(2);
    }

    @Test
    public void test_ventas_file_form()
    {
        final var modelView = ventas_file_form.getModelView(23);
        // Menu
        checkMenu(modelView);
        assertThat(modelView.get(form_action.name())).isEqualTo(ventas_file.actualPath());
    }

    @Test
    public void test_ventas_file_result()
    {
        InputStream inStream = new ByteArrayInputStream(DataTestExperiment.upCsvResult.getBytes(UTF_8));
        final var modelView = ventas_file_result.getModelView(inStream);
        assertThat(modelView.get(records_in.name())).isEqualTo(2);
        // Menu
        checkMenu(modelView);
    }

    @Test
    public void test_landing_pg_sms()
    {
        dataTestSmsPageFlow();
        //noinspection ResultOfMethodCallIgnored
        assertThat(landing_pg_sms.getModelView("11_111").get(sms_custom_page_txt.name()))
                .satisfies(o -> o.toString().contains(sms_personal_message));
    }

    // ============ Utilities =============

    static void checkMenu(Map<String, ?> model)
    {
        assertThat((List<String>) model.get(menu_items.name())).containsExactly(menuPathsList.toArray(String[]::new));
    }

    public static void checkMenuInHtml(String htmlToCheck)
    {
        assertThat(htmlToCheck)
                .contains(">" + "Nuevo")
                .contains(">" + "Lista")
                .contains(">" + "Simulaciones")
                .contains(">" + "Ventas")
                .contains(">" + "Aperturas")
                .contains(">" + "Comunicaciones");
    }

    static void checkCombos(Map<String, ?> model)
    {
        final var conceptos = ((List<ConceptoForJson>) model.get(Promocion.FieldLabel.conceptos.name()));
        assertThat(conceptos.stream().mapToInt(v -> v.conceptoId).toArray()).containsExactlyInAnyOrder(stream(ConceptoTaller.values()).mapToInt(c -> c.conceptoId).toArray());
        assertThat(conceptos.stream().map(v -> v.nombre).toArray()).containsExactlyInAnyOrder(stream(ConceptoTaller.values()).map(Enum::name).toArray());

        final var incentivos = ((List<IncentivoJson>) model.get(incentivo.name()));
        assertThat(incentivos.stream().mapToInt(v -> v.incentivoId).toArray()).containsExactlyInAnyOrder(stream(Incentivo.values()).mapToInt(i -> i.incentivoId).toArray());
        assertThat(incentivos.stream().map(v -> v.nombre).toArray()).containsExactlyInAnyOrder(stream(Incentivo.values()).map(i -> i.nombre).toArray());

        final var medios = (List<MedioComunicaJson>) model.get(medios_comunicacion.name());
        assertThat(medios.stream().mapToInt(medio -> medio.medioId).toArray()).containsExactly(1, 2, 3);

        final var mercados = ((List<MercadoForJson>) model.get(Promocion.FieldLabel.mercados.name()));
        assertThat(mercados.stream().mapToInt(v -> v.mercadoId).toArray()).containsExactlyInAnyOrder(stream(Mercado.values()).mapToInt(m -> m.id).toArray());
        assertThat(mercados.stream().map(v -> v.sigla).toArray()).containsExactlyInAnyOrder(stream(Mercado.values()).map(Enum::name).toArray());

        final var pg1s = ((List<Pg1ForJson>) model.get(Promocion.FieldLabel.pg1s.name()));
        assertThat(pg1s.stream().mapToInt(v -> v.idPg1).toArray()).containsExactlyInAnyOrder(range(PG1.PG1_1, PG1.PG1_17).stream().mapToInt(p -> p.idPg1).toArray());
    }
}