package com.lebenlab.core.experimento;

/**
 * User: pedro@didekin.es
 * Date: 27/01/2020
 * Time: 18:01
 */
public enum SqlUpdate {

    // @formatter:off
    del_all_experiment("DELETE FROM experimento"),
    del_experiment("DELETE FROM experimento WHERE experimento_id = ? "),
    del_results("DELETE FROM resultado_pg1"),
    del_all_promo_pg1("DELETE FROM promo_pg1"),
    del_all_promos("DELETE FROM promo"),
    del_all_promo_concepto("DELETE FROM promo_concepto"),
    del_all_promo_incentivo("DELETE FROM promo_incentivo"),
    del_all_promo_mercado("DELETE FROM promo_mercado"),
    del_all_promos_particip("DELETE FROM promo_participante"),
    del_all_promo_particpante_pg1("DELETE FROM promo_participante_pg1"),
    insert_experimento("INSERT INTO experimento(nombre) VALUES(:nombre)"),
    insert_participante("INSERT INTO participante (participante_id, id_fiscal, provincia_id, concepto_id, " +
            " email, tfno, fecha_registro, fecha_modificacion) " +
            " VALUES (:participId, :idFiscal, :provinciaId, :conceptoId, :email, :tfno, :fechaRegistro, :fechaModificacion) " +
            " ON DUPLICATE KEY UPDATE participante_id = participante_id, " +
            "                         concepto_id = VALUES(concepto_id), " +
            "                         provincia_id = VALUES(provincia_id), " +
            "                         fecha_modificacion = VALUES(fecha_modificacion) "),
    insert_promo("INSERT INTO promo(cod_promo, fecha_inicio, fecha_fin, experimento_id) " +
            " VALUES (:codPromo,:fechaInicio,:fechaFin, :experimentoId)"),
    insert_promo_conceptos("INSERT INTO promo_concepto(promo_id, concepto_id) VALUES(:idPromo, :cod) "),
    insert_promo_incentivo("INSERT INTO promo_incentivo(promo_id, incentivo_id) VALUES (:idPromo, :incentivo)"),
    insert_promo_mercados("INSERT INTO promo_mercado(promo_id, mercado_id) VALUES (:idPromo, :idMercado)"),
    insert_promo_participante("INSERT INTO promo_participante(promo_id, participante_id) VALUES (:promoId, :participId)"),
    insert_promo_particip_pg1("INSERT INTO promo_participante_pg1 (promo_id, participante_id, pg1_id)" +
            "                             SELECT pro_pa.promo_id, " +
            "                                    pro_pa.participante_id, " +
            "                                    pro_pg1.pg1_id " +
            "                             FROM promo_participante AS pro_pa " +
            "                                       INNER JOIN promo_pg1 AS pro_pg1 " +
            "                                           ON pro_pg1.promo_id = pro_pa.promo_id " +
            "                             WHERE pro_pa.promo_id = :promo_id" +
            "                             ORDER BY pro_pa.promo_id, pro_pa.participante_id, pro_pg1.pg1_id;"),
    getInsert_promo_particip_pg1_test(
            "INSERT INTO promo_participante_pg1 " +
                    "        (promo_id, participante_id, pg1_id, vta_media_diaria_pg1_exp, vtas_promo_pg1)" +
                    " VALUES (:promo_id, :participante_id, :pg1_id, :vta_media_diaria_pg1_exp, :vtas_promo_pg1);"),
    insert_promo_pg1s("INSERT INTO promo_pg1(promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2)" +
            " VALUES (:idPromo, :idPg1, :idPg1_with_1, :idPg1_with_2)"),
    insert_resultado_pg1("INSERT INTO resultado_pg1(participante_id, pg1_id, cantidad, fecha_resultado) " +
            " VALUES (:participanteId, :pg1Id, :udsPg1, :fechaResultado)"),
    participante_delete_by_time("DELETE FROM participante WHERE fecha_modificacion < :fecha_modificacion "),
    particip_delete_all("DELETE FROM participante"),
    update_promo_days_on("" +
            " UPDATE promo AS pro," +
            "    (SELECT pr.promo_id," +
            "            pr.fecha_inicio," +
            "            pr.fecha_fin," +
            "            MAX(vw_in.last_result_date_pro_par_pg1) as date_last_result" +
            "     FROM promo AS pr" +
            "              INNER JOIN result_promo_part_pg1_vw AS vw_in" +
            "                         ON pr.promo_id = vw_in.promo_id" +
            "     GROUP BY pr.promo_id" +
            "     ORDER BY pr.promo_id" +
            "    ) AS vw_out" +
            " SET pro.dias_con_resultados = IF(vw_out.date_last_result IS NOT NULL," +
            "                                 DATEDIFF(vw_out.date_last_result, pro.fecha_inicio) + 1," +
            "                                 pro.dias_con_resultados)" +
            " WHERE pro.promo_id = vw_out.promo_id;"),
    update_promo_participante("" +
            " UPDATE" +
            "    promo_participante AS pro_pa," +
            "    (SELECT pr_pa.participante_id," +
            "            pr_pa.promo_id," +
            "            pro.fecha_inicio," +
            "            pa.fecha_registro," +
            "            pa.concepto_id," +
            "            prov.provincia_id" +
            "     FROM promo_participante as pr_pa" +
            "              INNER JOIN participante AS pa" +
            "                         ON pr_pa.participante_id = pa.participante_id" +
            "              INNER JOIN provincia AS prov" +
            "                         ON pa.provincia_id = prov.provincia_id" +
            "              INNER JOIN promo AS pro" +
            "                         ON pr_pa.promo_id = pro.promo_id" +
            "    ) AS pr_pa_join" +
            " SET pro_pa.concepto_id = pr_pa_join.concepto_id," +
            "     pro_pa.provincia_id = pr_pa_join.provincia_id," +
            "     dias_registro = DATEDIFF(pr_pa_join.fecha_inicio, pr_pa_join.fecha_registro)" +
            " WHERE pro_pa.promo_id = pr_pa_join.promo_id" +
            "    AND pro_pa.participante_id = pr_pa_join.participante_id" +
            "    AND pr_pa_join.promo_id = :promo_id;"),
    // No hay actualización de campañas definidas posteriormente a la carga del fichero.
    // Las ventas se aplican a promos en vigor en el momento de la carga.
    update_promo_particip_pg1_act("" +
            " UPDATE promo_participante_pg1 AS tb," +
            "    result_promo_part_pg1_vw AS vw" +
            " SET tb.vtas_promo_pg1 = tb.vtas_promo_pg1 + vw.promo_vtas" +
            " WHERE tb.promo_id = vw.promo_id" +
            "  AND tb.participante_id = vw.participante_id" +
            "  AND tb.pg1_id = vw.pg1_id;"),
    update_promo_particip_pg1_prev("" +
            " UPDATE promo_participante_pg1 AS tb1," +
            "    (SELECT vw1.promo_id," +
            "            vw1.participante_id," +
            "            vw1.pg1_id," +
            "            SUM(vw2.dias_con_resultados)          AS total_days," +
            "            SUM(vw2.vtas_promo_pg1)               AS tot_vtas_prev," +
            "            COUNT(vw2.promo_id)                   AS promos_prev," +
            "            SUM(if(vw2.vtas_promo_pg1 > 0, 1, 0)) AS promos_prev_act" +
            "     FROM pro_part_pg1_vw AS vw1" +
            "              INNER JOIN pro_part_pg1_vw AS vw2" +
            "                         ON vw1.participante_id = vw2.participante_id" +
            "                             AND vw1.pg1_id = vw2.pg1_id" +
            "                             AND vw2.fecha_fin < vw1.fecha_inicio" +
            "     WHERE vw1.promo_id = :promo_id" +
            "     GROUP BY vw1.promo_id, vw1.participante_id, vw1.pg1_id" +
            "    ) AS vw1_2" +
            " SET tb1.vta_media_diaria_pg1_exp = IF(" +
            "                                       vw1_2.total_days > 0 AND vw1_2.promos_prev > 0," +
            "                                       (vw1_2.tot_vtas_prev / vw1_2.total_days) * (vw1_2.promos_prev_act / vw1_2.promos_prev)," +
            "                                       0" +
            "                                    )" +
            " WHERE tb1.promo_id = vw1_2.promo_id" +
            "  AND tb1.participante_id = vw1_2.participante_id" +
            "  AND tb1.pg1_id = vw1_2.pg1_id;"),
    ;
    // @formatter:on

    public final String statement;

    SqlUpdate(String statementIn)
    {
        statement = statementIn;
    }
}
