package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.PromoParticipante;
import com.lebenlab.core.mediocom.MedioComunicacion.PromoParticipanteMedio;

import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

import java.util.ArrayList;
import java.util.List;

import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.ProcessArgException.error_promos_in_exp;
import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static com.lebenlab.core.mediocom.SqlComunicacion.insert_promo_participante_medio;
import static com.lebenlab.core.mediocom.SqlComunicacion.insert_promo_participante_medio_hist;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_aperturas_resultado;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_participante_medio;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_recibidos_resultado;
import static com.lebenlab.core.mediocom.SqlComunicacion.update_promo_particip_apertura;
import static com.lebenlab.core.mediocom.SqlComunicacion.update_promo_particip_recibido;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.util.Arrays.stream;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 16/05/2021
 * Time: 16:32
 */
public interface MedioComunicaDaoIf {

    Logger logger = getLogger(MedioComunicaDaoIf.class);
    Jdbi jdbiInstance = jdbiFactory.getJdbi();

    //............................ Inserts/updates ..............................

    static int insertPromoParticipMedio(PromoMedioComunica promoMedioComunica)
    {
        try {
            final var inserted = jdbiInstance.withHandle(
                    h -> h.createUpdate(insert_promo_participante_medio.statement)
                            .bind("promo_id", promoMedioComunica.promoId)
                            .bind("medio_id", promoMedioComunica.medioId)
                            .execute()
            );
            logger.info("insertPromoParticipMedio(); inserted = {}", inserted);
            return inserted;
        } catch (JdbiException e) {
            logger.error("insertPromoParticipMedio(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static int updateEventsComunicaMedio(List<PromoParticipante> promoParticipantes, SqlComunicacion updateStmt)
    {
        try {
            return jdbiInstance.withHandle(
                    h -> {
                        final var batch = h.prepareBatch(updateStmt.statement);
                        for (PromoParticipante promoParticipante : promoParticipantes) {
                            batch
                                    .bind("promo_id", promoParticipante.promoId)
                                    .bind("participante_id", promoParticipante.participanteId)
                                    .add();
                        }
                        final var updated = stream(batch.execute()).sum();
                        logger.info("updateEventsComunicaMedio(); updated = {}", updated);
                        return updated;
                    }
            );
        } catch (JdbiException e) {
            logger.error("updateEventsComunicaMedio(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static int updateApertura(long promoId, long participanteId)
    {
        try {
            return jdbiInstance.withHandle(
                    h -> h.createUpdate(update_promo_particip_apertura.statement)
                            .bind("promo_id", promoId)
                            .bind("participante_id", participanteId)
                            .execute()
            );
        } catch (JdbiException e) {
            logger.error("updateApertura(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * Updates messages opened (with TRUE) by participants in a promotion.
     */
    static int updateAperturas(List<PromoParticipante> promoParticipantes)
    {
        logger.debug("updateAperturas() entering ...");
        return updateEventsComunicaMedio(promoParticipantes, update_promo_particip_apertura);
    }

    /**
     * Updates messages opened (with TRUE) by participants in a promotion.
     */
    static int updateRecibidos(List<PromoParticipante> promoParticipantes)
    {
        logger.debug("updateAperturas() entering ...");
        return updateEventsComunicaMedio(promoParticipantes, update_promo_particip_recibido);
    }

    //............................ Consultas ..............................

    /**
     * @return ratio of aperturas or NO_MEDIO_IN_PROMO when no medio is associated to a promo (medio == ninguno).
     */
    static double eventsComunicaInPromo(long idPromo, SqlComunicacion sql)
    {
        logger.info("eventsComunicaInPromo()");
        try {
            return jdbiInstance.withHandle(handle -> handle.select(sql.statement)
                    .bind("promo_id", idPromo)
                    .mapTo(Double.class)
                    .findOne().orElse(NO_MEDIO_IN_PROMO));
        } catch (RuntimeException e) {
            logger.error("eventsComunicaInPromo; JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static List<Double> eventsComunicaInExp(List<Promocion> promosIn, SqlComunicacion sql)
    {
        logger.info("eventsComunicaInExp()");

        if (promosIn == null || promosIn.size() != 2) {
            throw new ProcessArgException(error_promos_in_exp);
        }

        final var listResults = new ArrayList<Double>(2);
        for (Promocion promo : promosIn) {
            listResults.add(eventsComunicaInPromo(promo.idPromo, sql));
        }
        return listResults;
    }

    static List<Double> aperturasInExperimento(List<Promocion> promosIn)
    {
        logger.info("aperturasInExperimento()");
        return eventsComunicaInExp(promosIn, promo_aperturas_resultado);
    }

    static List<Double> recibidosInExperimento(List<Promocion> promosIn)
    {
        logger.info("recibidosInExperimento()");
        return eventsComunicaInExp(promosIn, promo_recibidos_resultado);
    }

    // ======================== Aperturas históricas para predicciones ========================

    /**
     * @param idPromo is the promo whose previous open communication ratios are to be computed.
     */
    static int insertPromoParticipMedioHist(long idPromo)
    {
        try {
            final var inserted = jdbiInstance.withHandle(
                    h -> h.createUpdate(insert_promo_participante_medio_hist.statement)
                            .bind("promo_id", idPromo)
                            .execute()
            );
            logger.info("insertPromoParticipMedioHist(); inserted = {}", inserted);
            return inserted;
        } catch (JdbiException e) {
            logger.error("insertPromoParticipMedioHist; JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // ======================== Utilidades ========================

    /**
     * Utility mainly for tests.
     */
    static List<PromoParticipanteMedio> promoParticipanteMedio()
    {
        return jdbiInstance.withHandle(
                h -> h.select(promo_participante_medio.statement)
                        .map((r, ctx) -> new PromoParticipanteMedio(
                                        r.getLong("promo_id"),
                                        r.getLong("participante_id"),
                                        r.getShort("medio_id"),
                                        r.getBoolean("recibido_msg"),
                                        r.getBoolean("apertura_msg"),
                                        r.getDate("fecha_update").toLocalDate()
                                )
                        )
                        .list());
    }
}
