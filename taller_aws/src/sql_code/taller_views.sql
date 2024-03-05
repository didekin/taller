\W  /* Enable warnings*/

DROP VIEW IF EXISTS experimento_concepto_vw;
DROP VIEW IF EXISTS experimento_fechas_vw;
DROP VIEW IF EXISTS experimento_mercado_vw;
DROP VIEW IF EXISTS experimento_testpg1_vw;
DROP VIEW IF EXISTS pro_par_medio_vw;
DROP VIEW IF EXISTS pro_par_medio_short_vw;
DROP VIEW IF EXISTS pro_part_pg1_vw;
DROP VIEW IF EXISTS result_promo_part_pg1_vw;
DROP VIEW IF EXISTS result_for_model_vw;

# ================  EXPERIMENTO  ================

CREATE VIEW experimento_concepto_vw AS
SELECT DISTINCT ex.experimento_id,
                pc.concepto_id
FROM experimento AS ex
         INNER JOIN promo AS pr ON pr.experimento_id = ex.experimento_id
         INNER JOIN promo_concepto AS pc on pr.promo_id = pc.promo_id
GROUP BY ex.experimento_id, pc.concepto_id;

CREATE VIEW experimento_mercado_vw AS
SELECT DISTINCT ex.experimento_id,
                pm.mercado_id
FROM experimento AS ex
         INNER JOIN promo AS pr ON pr.experimento_id = ex.experimento_id
         INNER JOIN promo_mercado AS pm on pr.promo_id = pm.promo_id
GROUP BY ex.experimento_id, pm.mercado_id;

CREATE VIEW experimento_fechas_vw AS
SELECT DISTINCT ex.experimento_id,
                pr.fecha_inicio,
                pr.fecha_fin
FROM experimento AS ex
         INNER JOIN promo AS pr ON pr.experimento_id = ex.experimento_id
GROUP BY ex.experimento_id, pr.fecha_inicio, pr.fecha_fin;

CREATE VIEW experimento_testpg1_vw AS
SELECT ex.experimento_id,
       pg.pg1_id
FROM experimento AS ex
         INNER JOIN promo AS pr ON pr.experimento_id = ex.experimento_id
         INNER JOIN promo_pg1 AS pg ON pr.promo_id = pg.promo_id
GROUP BY ex.experimento_id, pg.pg1_id;

# ================  RESULTADO  ================

CREATE VIEW pro_part_pg1_vw AS
SELECT tb1.promo_id,
       tb1.participante_id,
       tb1.pg1_id,
       tb1.vta_media_diaria_pg1_exp,
       tb1.vtas_promo_pg1,
       tb2.fecha_inicio,
       tb2.fecha_fin,
       tb2.dias_con_resultados
FROM promo_participante_pg1 AS tb1
         INNER JOIN promo AS tb2 ON tb1.promo_id = tb2.promo_id;

CREATE VIEW result_promo_part_pg1_vw AS
SELECT pr_pa_pg1.promo_id,
       DATE(pr.fecha_inicio)                                 AS date_inicio,
       DATE(pr.fecha_fin)                                    AS date_fin,
       pr_pa_pg1.participante_id,
       pr_pa_pg1.pg1_id,
       COALESCE(MAX(re.fecha_resultado), CAST(NULL AS DATE)) as last_result_date_pro_par_pg1,
       COALESCE(SUM(re.cantidad), 0)                         AS promo_vtas -- resultado a 0 para campañas sin registros de resultados.
FROM promo AS pr
         INNER JOIN promo_participante_pg1 AS pr_pa_pg1
                    ON pr.promo_id = pr_pa_pg1.promo_id
         LEFT JOIN resultado_pg1 AS re
                   ON pr_pa_pg1.participante_id = re.participante_id
                       AND pr_pa_pg1.pg1_id = re.pg1_id
                       AND re.fecha_resultado BETWEEN pr.fecha_inicio AND pr.fecha_fin
GROUP BY pr_pa_pg1.promo_id, pr_pa_pg1.participante_id, pr_pa_pg1.pg1_id
ORDER BY pr_pa_pg1.promo_id, pr_pa_pg1.participante_id, pr_pa_pg1.pg1_id;

# ================  SIMULACIÓN  ================

CREATE VIEW result_for_model_vw AS
SELECT IF(pro.dias_con_resultados > 0,
          pro_pa_pg1.vtas_promo_pg1 / pro.dias_con_resultados,
          0)                                         AS vta_media_diaria_pg1,
       pro_pa.participante_id,
       prov.mercado_id,
       pro_pa.provincia_id,
       pro_pa.concepto_id,
       pro_pa.dias_registro,
       pro_pa_pg1.vta_media_diaria_pg1_exp,
       pro_pa.promo_id,
       DATEDIFF(pro.fecha_fin, pro.fecha_inicio) + 1 AS duracion_promo,
       QUARTER(pro.fecha_inicio)                     AS quarter_promo,
       pro_in.incentivo_id,
       pro_med.medio_id,
       pro_med.msg_classification,
       # valor 0 para participantes sin registros en promo_participante_medio_hist.
       # Este valor no los diferencia de quien tienen 0 porque no han abierto ninguno de los mensajes recibidos.
       COALESCE(pro_pa_me.ratio_aperturas, 0)        AS ratio_aperturas,
       pro_pg1.pg1_id,
       pro_pg1.pg1_id_with_1,
       pro_pg1.pg1_id_with_2
FROM promo AS pro
         INNER JOIN promo_incentivo pro_in ON pro.promo_id = pro_in.promo_id
         INNER JOIN promo_mediocomunicacion pro_med ON pro.promo_id = pro_med.promo_id
         INNER JOIN promo_participante AS pro_pa ON pro_pa.promo_id = pro.promo_id
         INNER JOIN provincia AS prov ON pro_pa.provincia_id = prov.provincia_id
         LEFT JOIN promo_participante_medio_hist AS pro_pa_me
                   ON pro_pa_me.promo_id = pro_med.promo_id
                       AND pro_pa_me.medio_id = pro_med.medio_id
                       AND pro_pa_me.participante_id = pro_pa.participante_id
         INNER JOIN promo_participante_pg1 AS pro_pa_pg1
                    ON pro_pa_pg1.promo_id = pro_pa.promo_id
                        AND pro_pa_pg1.participante_id = pro_pa.participante_id
         INNER JOIN promo_pg1 AS pro_pg1
                    ON pro_pg1.promo_id = pro_pa_pg1.promo_id
                        AND pro_pg1.pg1_id = pro_pa_pg1.pg1_id;

# ================  AUXILIARY VIEWS FOR TABLE promo_participante_medio  ================

CREATE VIEW pro_par_medio_vw AS
SELECT t1.promo_id,
       t1.participante_id,
       t1.medio_id,
       t1.recibido_msg,
       t1.apertura_msg,
       t2.fecha_inicio,
       t2.fecha_fin
FROM promo_participante_medio AS t1
         INNER JOIN promo AS t2 ON t1.promo_id = t2.promo_id
WHERE t1.medio_id IN (2, 3);

CREATE VIEW pro_par_medio_short_vw AS
SELECT pro_pa.promo_id,
       participante_id,
       medio_id,
       recibido_msg,
       apertura_msg
FROM promo_participante_medio AS pro_pa
WHERE medio_id IN (2, 3);


\w  /* Disable warnings*/
