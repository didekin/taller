package com.lebenlab.core.simulacion;

/**
 * User: pedro@didekin
 * Date: 21/03/2020
 * Time: 18:09
 */
public enum SqlQuery {

    // @formatter:off
    df_model_row(
            " SELECT vta_media_diaria_pg1," +
                    "       mercado_id," +
                    "       concepto_id," +
                    "       dias_registro," +
                    "       vta_media_diaria_pg1_exp, " +
                    "       duracion_promo," +
                    "       quarter_promo," +
                    "       incentivo_id," +
                    "       medio_id," +
                    "       msg_classification," +
                    "       ratio_aperturas, " +
                    "       pg1_id_with_1," +
                    "       pg1_id_with_2" +
                    " FROM result_for_model_vw" +
                    " WHERE pg1_id = ?;"),
    // Para la estimación de ventas se utilizan todos los datos disponibles hasta la fecha de estimación.
    // Igualmente sucede con los días de registro y el ratio de aperturas. Hacerlo así evita tener que hacer un join sobre la misma tabla o vista.
    df_prediction_row(
            " SELECT mercado_id, " +
                    "       concepto_id, " +
                    "       dias_registro, " +
                    "       IF(total_days > 0 AND promos_prev > 0, " +
                    "          (tot_vtas_prev / total_days) * (promos_prev_act / promos_prev), " +
                    "          0)               AS vta_media_diaria_pg1_exp, " +
                    "       :duracion_promo     AS duracion_promo, " +
                    "       :quarter_promo      AS quarter_promo, " +
                    "       :incentivo_id       AS incentivo_id, " +
                    "       :medio_id           AS medio_id, " +
                    "       :msg_classification AS msg_classification, " +
                    "       IF(promos_medio_prev > 0, aperturas_medio_prev/promos_medio_prev, 0) AS ratio_aperturas, " +
                    "       :pg1_id_with_1      AS pg1_id_with_1, " +
                    "       :pg1_id_with_2      AS pg1_id_with_2 " +
                    " FROM (SELECT pa.participante_id, " +
                    "             prov.mercado_id, " +
                    "             pa.concepto_id, " +
                    "             DATEDIFF(NOW(), pa.fecha_registro) AS dias_registro " +
                    "      FROM participante as pa " +
                    "               INNER JOIN provincia AS prov " +
                    "                          ON pa.provincia_id = prov.provincia_id " +
                    "      WHERE mercado_id IN (<mercadoIds>) " +
                    "        AND concepto_id IN (<conceptoIds>) " +
                    "     ) AS vw_1 " +
                    "         LEFT JOIN " +
                    "     (SELECT participante_id, " +
                    "             COALESCE(SUM(dias_con_resultados), 0)          AS total_days, " +
                    "             COALESCE(SUM(vtas_promo_pg1), 0)               AS tot_vtas_prev, " +
                    "             COALESCE(COUNT(promo_id), 0)                   AS promos_prev, " +
                    "             COALESCE(SUM(if(vtas_promo_pg1 > 0, 1, 0)), 0) AS promos_prev_act " +
                    "      FROM pro_part_pg1_vw " +
                    "      WHERE pg1_id = :pg1_id " +
                    "      GROUP BY participante_id " +
                    "     ) AS vw_2 " +
                    "     ON vw_1.participante_id = vw_2.participante_id " +
                    "         LEFT JOIN " +
                    "     (SELECT participante_id, " +
                    "             medio_id, " +
                    "             COALESCE(SUM(apertura_msg), 0) AS aperturas_medio_prev, " +
                    "             COALESCE(COUNT(promo_id), 0) AS promos_medio_prev " +
                    "      FROM pro_par_medio_short_vw " +
                    "      WHERE medio_id = :medio_id " +
                    "      GROUP BY participante_id " +
                    "             , medio_id " +
                    "     ) AS vw_3 " +
                    "     ON vw_2.participante_id = vw_3.participante_id;"),
    promoId_from_promosimulacion("SELECT promo_id FROM promo_simulacion LIMIT 1;"),
    promo_simulacion("SELECT * FROM promo_simulacion WHERE promo_id = ?"),
    // @formatter:on
    ;

    public final String query;

    SqlQuery(String queryIn)
    {
        query = queryIn;
    }
}
