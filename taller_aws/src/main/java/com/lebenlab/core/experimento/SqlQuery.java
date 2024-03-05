package com.lebenlab.core.experimento;

/**
 * User: pedro@didekin.es
 * Date: 27/01/2020
 * Time: 19:01
 */
public enum SqlQuery {

    //@formatter:off
    conceptos("SELECT concepto_id, nombre FROM concepto"),
    experiment_byId("SELECT experimento_id, nombre FROM experimento WHERE experimento_id = ?"),
    experiments_same_dates("SELECT experimento_id FROM experimento_fechas_vw " +
            " WHERE NOT (fecha_fin < :fechaInicio OR fecha_inicio > :fechaFin ) "),
    experiments_same_conceptos("SELECT experimento_id FROM experimento_concepto_vw " +
            " WHERE concepto_id IN (<conceptoIds>) "),
    experiments_same_mercados("SELECT experimento_id FROM experimento_mercado_vw " +
            " WHERE mercado_id IN (<mercadoIds>) "),
    experiments_same_pg1s("SELECT experimento_id FROM experimento_testpg1_vw " +
            " WHERE pg1_id IN (<pg1Ids>) "),
    experimentos("SELECT ex.experimento_id, " +
            " ex.nombre, " +
            " pr.promo_id, " +
            " pr.cod_promo, " +
            " pr.fecha_inicio, " +
            " pr.fecha_fin, " +
            " inc.incentivo_id " +
            " FROM experimento AS ex " +
            " INNER JOIN promo AS pr " +
            " INNER JOIN promo_incentivo AS inc " +
            " ON ex.experimento_id = pr.experimento_id " +
            " AND pr.promo_id = inc.promo_id"),
    experimento_samples("SELECT * " +
            " FROM (" +
            "   SELECT " +
            "    participante_id," +
            "    id_fiscal," +
            "    email, " +
            "    tfno, " +
            "    provincia.mercado_id," +
            "    participante.provincia_id," +
            "    concepto_id," +
            "    ROW_NUMBER() OVER (ORDER BY mercado_id, concepto_id, RAND()) AS row_num " +
            "   FROM participante INNER JOIN provincia USING (provincia_id) " +
            " ) r " +
            " WHERE mercado_id IN (<mercadoIds>) " +
            " AND concepto_id IN (<conceptoIds>) "
    ),
    incentivos("SELECT incentivo_id, nombre FROM incentivo"),
    medioscomunicacion("SELECT medio_id, nombre FROM mediocomunicacion"),
    medioNombresTextos("" +
            " SELECT  pm.promo_id, pm.promo_medio_text, me.nombre " +
            " FROM  promo_mediocomunicacion AS pm INNER JOIN mediocomunicacion AS me ON pm.medio_id = me.medio_id " +
            " WHERE pm.promo_id IN (<promosId>) " +
            " ORDER BY pm.promo_id;"),
    mercados("SELECT mercado_id, sigla, nombre FROM mercado"),
    participante_all_count("SELECT COUNT(*) FROM participante "),
    participante_count_by_promo("SELECT COUNT(*)" +
            " FROM participante as pa" +
            " INNER JOIN provincia AS pro ON pa.provincia_id = pro.provincia_id" +
            " WHERE mercado_id IN (<mercadoIds>)" +
            " AND concepto_id IN (<conceptoIds>)"),
    participantes("SELECT participante_id, id_fiscal, provincia_id, concepto_id," +
            " email, tfno, fecha_registro, fecha_modificacion " +
            " FROM participante ORDER BY participante_id ASC"),
    participantes_old_time("SELECT max(fecha_modificacion) from participante"),
    participantes_pk("SELECT participante_id FROM participante ORDER BY participante_id ASC"),
    participante_by_promo("SELECT promo_id, participante_id, concepto_id, provincia_id, dias_registro " +
            " from promo_participante" +
            " WHERE promo_id = ?;"),
    particip_by_promo_pg1("SELECT tb1.promo_id," +
            "       tb1.participante_id," +
            "       tb1.pg1_id," +
            "       tb1.vta_media_diaria_pg1_exp," +
            "       IF(tb2.dias_con_resultados > 0, tb1.vtas_promo_pg1 / tb2.dias_con_resultados, 0) AS vta_media_diaria_pg1" +
            " FROM promo_participante_pg1 AS tb1" +
            "         INNER JOIN promo AS tb2" +
            "                    ON tb1.promo_id = tb2.promo_id" +
            " WHERE tb1.promo_id IN (<promoIds>)" +
            "   AND pg1_id IN (<pg1Ids>)" +
            " GROUP BY promo_id, pg1_id, participante_id" +
            " ORDER BY pg1_id, promo_id, participante_id;"),
    participantes_pg1_by_promo("SELECT tb1.promo_id," +
            "                   tb1.participante_id," +
            "                   tb1.pg1_id," +
            "                   tb1.vta_media_diaria_pg1_exp," +
            "                   IF(tb2.dias_con_resultados > 0, tb1.vtas_promo_pg1 / tb2.dias_con_resultados, 0) AS vta_media_diaria_pg1" +
            "             FROM promo_participante_pg1 AS tb1" +
            "                     INNER JOIN promo AS tb2" +
            "                                ON tb1.promo_id = tb2.promo_id" +
            "             WHERE tb1.promo_id = ? " +
            "             GROUP BY promo_id, pg1_id, participante_id " +
            "             ORDER BY pg1_id, promo_id, participante_id;"),
    pg1s("SELECT pg1_id, descripcion FROM pg1"),
    promo_byId("SELECT * FROM promo where promo_id = ? "),
    promos_by_experimentoId("SELECT pr.experimento_id," +
            "       pr.promo_id," +
            "       pr.cod_promo," +
            "       pr.fecha_inicio," +
            "       pr.fecha_fin," +
            "       me.mercado_id," +
            "       co.concepto_id," +
            "       i.incentivo_id," +
            "       md.medio_id," +
            "       md.promo_medio_text," +
            "       pg.pg1_id " +
            " FROM promo AS pr " +
            "         INNER JOIN promo_mercado AS me " +
            "         INNER JOIN promo_concepto AS co " +
            "         INNER JOIN promo_incentivo AS i " +
            "         INNER JOIN promo_mediocomunicacion AS md " +
            "         INNER JOIN promo_pg1 AS pg " +
            "                    ON pr.promo_id = me.promo_id " +
            "                        AND me.promo_id = co.promo_id " +
            "                        AND co.promo_id = pg.promo_id " +
            "                        AND pg.promo_id = i.promo_id " +
            "                        AND i.promo_id = md.promo_id " +
            " WHERE pr.experimento_id = ? " +
            " ORDER BY pr.experimento_id, pr.promo_id"),
    promos_days_on("SELECT promo_id, dias_con_resultados FROM promo ORDER BY promo_id "),
    provincias("SELECT * FROM provincia"),
    ;
    // @formatter:on

    public final String query;

    SqlQuery(String queryIn)
    {
        query = queryIn;
    }
}


