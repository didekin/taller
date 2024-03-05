package com.lebenlab.core.mediocom;

/**
 * User: pedro@didekin
 * Date: 02/02/2021
 * Time: 17:09
 */
public enum SqlComunicacion {

    // @formatter:off
    del_all_promo_mediocomunicacion("DELETE FROM promo_mediocomunicacion"),
    del_all_promo_medio_sms("DELETE FROM promo_medio_sms"),
    del_all_promo_participante_medio("DELETE FROM promo_participante_medio"),
    del_all_promo_particip_medio_hist("DELETE FROM promo_participante_medio_hist"),
    del_all_textcomunicacion("DELETE FROM textcomunicacion_class"),
    del_all_word_class_prob("DELETE FROM word_class_prob"),
    insert_promo_mediocomunicacion("" +
            " INSERT INTO promo_mediocomunicacion(promo_id, medio_id, msg_classification, promo_medio_text)" +
            " VALUES (:promoId, :medioId, :codTextClass, :textMsg)"),
    insert_promo_medio_aperturas("" +
            " INSERT INTO promo_medio_aperturas(promo_id, medio_id, aperturas, percent_aperturas) " +
            " VALUES (:promoId, :medioId, 0, 0)"),
    insert_promo_medio_sms("" +
            "INSERT INTO promo_medio_sms (promo_id, batch_id) VALUES (:promo_id, :batch_id); "),
    insert_promo_participante_medio("" +
            " INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)  " +
            " SELECT promo_id,  " +
            "        participante_id,  " +
            "        :medio_id AS medio_id  " +
            " FROM promo_participante  " +
            " WHERE promo_id = :promo_id  " +
            " ORDER BY promo_id, participante_id, medio_id;"),
    /* El ratio de aperturas se calcula para comunicaciones previas en el mismo medio. No tenemos en cuenta el producto. */
    insert_promo_participante_medio_hist("" +
            " INSERT INTO promo_participante_medio_hist (promo_id, participante_id, medio_id, ratio_aperturas) " +
            " SELECT vw1.promo_id, " +
            "       vw1.participante_id, " +
            "       vw1.medio_id, " +
            "       IF(COUNT(IF(vw2.recibido_msg > 0, 1, NULL)) > 0, SUM(vw2.apertura_msg) / COUNT(IF(vw2.recibido_msg > 0, 1, NULL)), 0) AS ratio_aperturas " +
            " FROM pro_par_medio_vw AS vw1 " +
            "         INNER JOIN pro_par_medio_vw AS vw2 ON vw1.medio_id = vw2.medio_id " +
            "    AND vw1.participante_id = vw2.participante_id " +
            "    AND vw2.fecha_fin < vw1.fecha_inicio " +
            " WHERE vw1.promo_id = :promo_id " +
            " GROUP BY vw1.promo_id, vw1.participante_id, vw1.medio_id;"),
    insert_text_to_class("INSERT INTO textcomunicacion_class(text, text_class) VALUES (:text, :text_class);"),
    // Since promo_participante has a record for each participante, we need a join with promo_mediocomunicacion to sort out promos without mediocomunicacion.
    promo_aperturas_resultado("" +
            " SELECT SUM(if(apertura_msg, 1, 0)) / COUNT(promo_id) AS ratioOpen" +
            " FROM promo_participante_medio " +
            " WHERE medio_id != 1 AND promo_id = :promo_id;"),
    promo_recibidos_resultado("" +
            " SELECT SUM(if(recibido_msg, 1, 0)) / COUNT(promo_id) AS ratio_delivered " +
            " FROM promo_participante_medio " +
            " WHERE medio_id != 1 AND promo_id = :promo_id;"),
    promo_batchid_sms("SELECT batch_id FROM promo_medio_sms WHERE promo_id = :promo_id;"),
    promo_medio_sms("SELECT * FROM promo_medio_sms ORDER BY promo_id"),
    promo_participante_medio("SELECT * FROM promo_participante_medio ORDER BY promo_id"),
    promo_participante_tfno("SELECT participante_id, tfno FROM participante WHERE participante_id = :participante_id;"),
    promo_participantes_tfnos("" +
            "SELECT part.participante_id, " +
            "       part.tfno " +
            " FROM promo_participante AS propart" +
            "         INNER JOIN participante part ON propart.participante_id = part.participante_id " +
            " WHERE propart.promo_id = :promo_id AND part.tfno IS NOT NULL;"),
    // Promos with fecha_inicio older than today and fecha_fin more recent than 30 days before today.
    promos_to_update_aperturas("" +
            " SELECT pr.promo_id " +
            " FROM promo AS pr" +
            "         INNER JOIN promo_mediocomunicacion AS pm ON pr.promo_id = pm.promo_id " +
            " WHERE pr.fecha_inicio < CONVERT(NOW(), DATE) " +
            "  AND pr.fecha_fin >  CONVERT(DATE_SUB(NOW(), INTERVAL 30 DAY), DATE) " +
            "  AND pm.medio_id IN (2,3);"),
    text_classes("SELECT * FROM textclass ORDER BY class_id;"),
    update_promo_particip_apertura("UPDATE promo_participante_medio " +
            "SET apertura_msg = TRUE ," +
            "recibido_msg = TRUE " +    // evita la posibilidad de inconsistencia entre apertura y recepción.
            "WHERE promo_id = :promo_id " +
            "AND participante_id = :participante_id"),
    update_promo_particip_recibido("" +
            " UPDATE promo_participante_medio " +
            " SET recibido_msg = TRUE " +
            " WHERE promo_id = :promo_id " +
            "  AND participante_id = :participante_id"),
    wordDictionary("SELECT * FROM word_class_prob ORDER BY word;"),
    ;
    // @formatter:on

    public final String statement;

    SqlComunicacion(String statementIn)
    {
        statement = statementIn;
    }
}
