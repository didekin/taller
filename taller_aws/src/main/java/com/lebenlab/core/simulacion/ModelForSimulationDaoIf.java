package com.lebenlab.core.simulacion;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs;

import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.core.Promocion.FieldLabel.duracion_promo;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.pg1_id;
import static com.lebenlab.core.Promocion.FieldLabel.pg1_id_with_1;
import static com.lebenlab.core.Promocion.FieldLabel.pg1_id_with_2;
import static com.lebenlab.core.Promocion.FieldLabel.promo_id;
import static com.lebenlab.core.Promocion.FieldLabel.quarter_promo;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.msg_classification;
import static com.lebenlab.core.simulacion.SqlQuery.df_model_row;
import static com.lebenlab.core.simulacion.SqlQuery.df_prediction_row;
import static com.lebenlab.core.simulacion.SqlQuery.promoId_from_promosimulacion;
import static com.lebenlab.core.simulacion.SqlQuery.promo_simulacion;
import static com.lebenlab.gson.GsonUtil.objectFromJsonStr;
import static com.lebenlab.gson.GsonUtil.objectToJsonStr;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.lang.Integer.valueOf;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 29/08/2020
 * Time: 18:02
 */
public interface ModelForSimulationDaoIf {

    Logger logger = getLogger(ModelForSimulationDaoIf.class);

    default long insertPromoSimulacion(Promocion promoIn)
    {
        logger.info("insertPromoSimulacion()");
        try {
            return jdbiFactory.getJdbi().withHandle(
                    h -> h.createUpdate(SqlUpdate.insert_promo_simulation.statement)
                            .bind("promo_json", objectToJsonStr(promoIn))
                            .executeAndReturnGeneratedKeys()
                            .mapTo(Long.class)
                            .one()
            );
        } catch (RuntimeException e) {
            logger.warn("insertPromoSimulacion(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * Mainly for tests.
     */
    default long promoSimulacionId()
    {
        return jdbiFactory.getJdbi().withHandle(
                handle -> handle.select(promoId_from_promosimulacion.query)
                        .mapTo(Long.class)
                        .one()
        );
    }

    default Promocion promoSimulation(long promoId)
    {
        logger.info("promoSimulation()");
        try {
            ModelForSimulationDao.PromoSimulationMapper promoMapper = jdbiFactory.getJdbi().withHandle(
                    h -> h.select(promo_simulacion.query, promoId)
                            .map(
                                    (rs, ctx) -> new ModelForSimulationDao.PromoSimulationMapper(
                                            rs.getLong(promo_id.name()), rs.getString("promo_json")
                                    )
                            )
                            .one());
            return new Promocion.PromoBuilder()
                    .copyPromo(objectFromJsonStr(promoMapper.promoJson, Promocion.class))
                    .idPromo(promoMapper.promoId)
                    .build();
        } catch (Exception e) {
            logger.warn("promoSimulation(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    default List<ModelRowDf> rowsForModelDf(int pg1IdResult)
    {
        logger.info("rowsForModelDf()");
        return jdbiFactory.getJdbi().withHandle(
                h -> h.select(df_model_row.query, pg1IdResult)
                        .map(ModelRowDf.mapper)
                        .list()
        );
    }

    /**
     * @param pg1Promocion: instance of Promocion which encapsulates all the predictors,
     *                      including the PG1s, with PG1 to predict average day sale for.
     * @return a list of instances of PredictorRowDf, each of them with a particular set of values for the predictors of the model.
     */
    default List<PredictorRowDf> rowsForPredictionDf(Pg1Promocion pg1Promocion)
    {
        logger.info("rowsForPredictionDf()");
        Promocion promoIn = pg1Promocion.promo;
        final var pg1IdResult = valueOf(pg1Promocion.pg1);
        List<Integer> pg1sPromo = new ArrayList<>(promoIn.getPg1IdsZeroPadded());
        pg1sPromo.remove(pg1IdResult);

        return jdbiFactory.getJdbi().withHandle(
                h -> h.select(df_prediction_row.query)
                        .bind(duracion_promo.name(), promoIn.getDuracionDias())
                        .bind(quarter_promo.name(), promoIn.getQuarter())
                        .bind(incentivo_id.name(), promoIn.incentivo)
                        .bind(medio_id.name(), promoIn.promoMedioComunica.medioId)
                        .bind(msg_classification.name(), promoIn.promoMedioComunica.codTextClass)
                        .bind(pg1_id_with_1.name(), pg1sPromo.get(0))
                        .bind(pg1_id_with_2.name(), pg1sPromo.get(1))
                        .bindList(ParticipanteAbs.FieldLabel.mercadoIds.name(), promoIn.mercados)
                        .bindList(ParticipanteAbs.FieldLabel.conceptoIds.name(), promoIn.conceptos)
                        .bind(pg1_id.name(), pg1IdResult)
                        .map(PredictorRowDf.mapper)
                        .list()
        );
    }

    // ................. Static helpers ...................

    class PromoSimulationMapper {

        final long promoId;
        final String promoJson;

        public PromoSimulationMapper(long promo_id, String promo_json)
        {
            promoId = promo_id;
            promoJson = promo_json;
        }
    }
}
