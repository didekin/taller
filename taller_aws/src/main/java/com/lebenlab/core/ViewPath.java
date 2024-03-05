package com.lebenlab.core;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion.FieldLabel;
import com.lebenlab.core.experimento.ExpPlotter;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.mediocom.PromoMedioComunica.PromoMedComFacade;
import com.lebenlab.core.mediocom.TextClassifier;
import com.lebenlab.core.simulacion.ModelForSimulationDao;
import com.lebenlab.core.simulacion.RndForestSmile;
import com.lebenlab.core.simulacion.SimPlotter;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.PG1;
import com.lebenlab.core.tbmaster.TbMaestraAction;

import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lebenlab.ProcessArgException.error_pg1s;
import static com.lebenlab.ProcessArgException.experimento_wrongly_initialized;
import static com.lebenlab.core.UrlPath.aperturas_file;
import static com.lebenlab.core.UrlPath.communications_file;
import static com.lebenlab.core.UrlPath.exp_list_path;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.experimentoPath;
import static com.lebenlab.core.UrlPath.sim_pg1_cluster_path;
import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.ViewPath.ViewLabels.check_aperturas;
import static com.lebenlab.core.ViewPath.ViewLabels.count_participantes;
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
import static com.lebenlab.core.experimento.ExpPlotter.doClustersScatter;
import static com.lebenlab.core.experimento.ExpStatisticsFlow.statisticsFlow;
import static com.lebenlab.core.experimento.ExperimentoDao.getExperimentos;
import static com.lebenlab.core.experimento.ExperimentoDao.promosByExperiment;
import static com.lebenlab.core.experimento.ParticipanteDao.countParticipantes;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.resultPg1Dao;
import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static com.lebenlab.core.mediocom.AperturasFileDao.aperturasFileDao;
import static com.lebenlab.core.mediocom.LandingPageFlow.smsPageFlow;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_promo;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medios_comunicacion;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.sms_custom_page_txt;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingLong;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 16:48
 */
public enum ViewPath {

    error("/velocity/error_page_fake.vm"),
    experimento_form("/velocity/experiment_new_form.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("experimento_form: getModelView()");
            var model = super.getModelView(inObject);
            model.put(count_participantes.name(), countParticipantes());
            combosPromo(model);
            model.put(form_action.name(), experimentoPath.actualPath());
            return model;
        }
    },
    experimento_list("/velocity/experiments_list.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("experimento_list: getModelView()");
            var model = super.getModelView(inObject);
            model.put(experimentos.name(), getExperimentos());
            model.put(resultado_exp_path.name(), exp_statistics_path);
            return model;
        }
    },
    experimento_statistics("/velocity/experiment_statistics.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("experimento_statistics: getModelView()");
            var model = super.getModelView(inObject);
            final var resultExp = statisticsFlow.apply((String) inObject[0]);
            model.put(resultado_exp.name(), resultExp);
            model.put(medio_promo.name(), new PromoMedComFacade(resultExp.promocion.promoMedioComunica));
            model.put(medio_variante.name(), new PromoMedComFacade(resultExp.variante.promoMedioComunica));
            model.put(check_aperturas.name(),
                    resultExp.aperturasPercent.get(0) != NO_MEDIO_IN_PROMO || resultExp.aperturasPercent.get(1) != NO_MEDIO_IN_PROMO);
            model.put(FieldLabel.mercados.name(), resultExp.promocion.mercados.stream()
                    .map(Mercado::fromIntToMercado)
                    .collect(toList()));
            model.put(FieldLabel.conceptos.name(), resultExp.promocion.conceptos.stream()
                    .map(ConceptoTaller::fromIntToConcepto)
                    .collect(toList()));
            model.put(ViewLabels.exp_pg1_clusters_path.name(), UrlPath.exp_pg1_clusters_path);
            return model;
        }
    },
    exp_pg1_clusters("/velocity/exp_pg1_clusters.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("exp_pg1_clusters: getModelView()");
            var model = super.getModelView(inObject);
            int pg1Id = (int) inObject[0];
            long experimentoId = (long) inObject[1];

            if (experimentoId <= 0 || !PG1.checkPg1sIn(singletonList(pg1Id))) {
                logger.error("exp_pg1_clusters: getModelView(); error in experimentoId or pgId");
                throw new ProcessArgException(experimento_wrongly_initialized + " or " + error_pg1s);
            }
            final var promos = promosByExperiment(experimentoId);
            promos.sort(comparingLong(pr -> pr.idPromo));

            final var promo_1 = promos.get(0);
            final var promo_2 = promos.get(1);
            model.put(pg1_clusters_plots_div_1.name(), ExpPlotter.scatter_clusters_div + promo_1.idPromo);
            model.put(pg1_clusters_plots_div_2.name(), ExpPlotter.scatter_clusters_div + promo_2.idPromo);
            model.put(pg1_clusters_plots_script_1.name(), doClustersScatter(promo_1, pg1Id));
            model.put(pg1_clusters_plots_script_2.name(), doClustersScatter(promo_2, pg1Id));
            return model;
        }
    },
    login("/velocity/login_fake.vm"),
    simulacion_form("/velocity/simulacion_form.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("simulacion_form: getModelView()");
            var model = super.getModelView(inObject);
            combosPromo(model);
            model.put(form_action.name(), simulacionPath.actualPath());
            return model;
        }
    },
    /**
     * It inserts a simulated promo in promo_simulacion. This promo is futher used to present the cluster diagrams with simulated data.
     */
    simulacion_result("/velocity/simulacion_result.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("simulacion_result: getModelView()");
            var model = super.getModelView(inObject);
            Promocion promoIn = (Promocion) inObject[0];
            // Insertion in table promo_simulacion.
            final var idPromoIn = ModelForSimulationDao.modelSimulateDao.insertPromoSimulacion(promoIn);
            promoIn = new Promocion.PromoBuilder().copyPromo(promoIn)
                    .idPromo(idPromoIn)
                    .medio(new PromoMedioComunica.PromoMedComBuilder().copy(promoIn.promoMedioComunica).promoId(idPromoIn).build())
                    .build();
            model.put(medio_promo.name(), new PromoMedComFacade(promoIn.promoMedioComunica));
            model.put(promo_simulacion.name(), promoIn);
            model.put(result_aggr_simulation.name(), RndForestSmile.rndForestSmile(ModelForSimulationDao.modelSimulateDao).aggregateResultSimulation(promoIn));
            model.put(sim_pg1_clusters_path.name(), sim_pg1_cluster_path);
            return model;
        }
    },
    /**
     * It retrieves a simulated promo and composes a cluster diagram.
     */
    sim_pg1_clusters("/velocity/sim_pg1_clusters.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("sim_pg1_clusters: getModelView()");
            var model = super.getModelView(inObject);

            final long promoId = (long) inObject[0];
            final int pg1Id = (int) inObject[1];
            // Retrieval of the data of the simulated promotion.
            Pg1Promocion pg1Promocion = new Pg1Promocion.Pg1PromoBuilder()
                    .promo(ModelForSimulationDao.modelSimulateDao.promoSimulation(promoId))
                    .pg1(pg1Id)
                    .build();

            model.put(pg1_clusters_plots_div.name(), SimPlotter.scatter_clusters_div);
            model.put(pg1_clusters_plots_script.name(), SimPlotter.simPlotter(ModelForSimulationDao.modelSimulateDao).doClustersScatter(pg1Promocion, 1));
            return model;
        }
    },
    aperturas_file_form("/velocity/aperturas_file_form.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("aperturas_file_form: getModelView()");
            var model = super.getModelView(inObject);
            model.put(form_action.name(), aperturas_file.actualPath());
            return model;
        }
    },
    aperturas_file_result("/velocity/aperturas_file_result.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("aperturas_file_result: getModelView()");
            var model = super.getModelView(inObject);
            InputStream uploadedInput = (InputStream) inObject[0];
            final var insertedResults = aperturasFileDao.handleFileAperturas(uploadedInput);
            model.put(records_in.name(), insertedResults);
            return model;
        }
    },
    communications_file_form("/velocity/comunicaciones_file_form.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("communications_file_form: getModelView()");
            var model = super.getModelView(inObject);
            model.put(form_action.name(), communications_file.actualPath());
            return model;
        }
    },
    communications_file_result("/velocity/comunicaciones_file_result.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("communications_file_result: getModelView()");
            var model = super.getModelView(inObject);
            InputStream uploadedInput = (InputStream) inObject[0];
            final var insertedResults = TextClassifier.textClassDao.insertTextToClasify(TextClassifier.readTextToClasify.apply(uploadedInput));
            model.put(records_in.name(), insertedResults);
            return model;
        }
    },
    ventas_file_form("/velocity/ventas_file_form.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("ventas_file_form: getModelView()");
            var model = super.getModelView(inObject);
            model.put(form_action.name(), ventas_file.actualPath());
            return model;
        }
    },
    ventas_file_result("/velocity/ventas_file_result.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("ventas_file_result: getModelView()");
            var model = super.getModelView(inObject);
            InputStream uploadedInput = (InputStream) inObject[0];
            final var insertedResults = resultPg1Dao.handleVentasFile(uploadedInput);
            model.put(records_in.name(), insertedResults);
            return model;
        }
    },
    landing_pg_sms("/velocity/landing_pg_sms.vm") {
        @Override
        public Map<String, Object> getModelView(Object... inObject)
        {
            logger.info("landing_pg_sms: getModelView()");
            var model = super.getModelView(inObject);
            final var customTxt = smsPageFlow((String) inObject[0]);
            model.put(sms_custom_page_txt.name(), customTxt);
            return model;
        }
    };

    public final String path_vm;

    ViewPath(String viewPathStr)
    {
        path_vm = viewPathStr;
    }

    public Map<String, Object> getModelView(Object... inObject)
    {
        Map<String, Object> model = new HashMap<>(1);
        model.put(menu_items.name(), menuPathsList);
        return model;
    }

    // ============= STATIC MEMBERS ================

    private static final Logger logger = getLogger(ViewPath.class);

    public enum ViewLabels {
        check_aperturas,
        count_participantes,
        experimentos,
        exp_pg1_clusters_path,
        form_action,
        menu_items,
        pg1_clusters_plots_div,
        pg1_clusters_plots_div_1,
        pg1_clusters_plots_div_2,
        pg1_clusters_plots_script,
        pg1_clusters_plots_script_1,
        pg1_clusters_plots_script_2,
        promo_simulacion,
        records_in,
        resultado_exp,
        resultado_exp_path,
        result_aggr_simulation,
        sim_pg1_clusters_path
    }

    static final List<String> menuPathsList = of(
            experimentoPath.actualPath(),
            exp_list_path.actualPath(),
            simulacionPath.actualPath(),
            ventas_file.actualPath(),
            aperturas_file.actualPath(),
            communications_file.actualPath()
    );

    static void combosPromo(Map<String, Object> model)
    {
        model.put(FieldLabel.conceptos.name(), TbMaestraAction.conceptos.getTableValues());
        model.put(FieldLabel.incentivo.name(), TbMaestraAction.incentivos.getTableValues());
        model.put(medios_comunicacion.name(), TbMaestraAction.medios_comunicacion.getTableValues());
        model.put(FieldLabel.mercados.name(), TbMaestraAction.mercados.getTableValues());
        model.put(FieldLabel.pg1s.name(), TbMaestraAction.pg1s.getTableValues());
    }
}
