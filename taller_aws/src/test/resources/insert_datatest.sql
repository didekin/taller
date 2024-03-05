INSERT INTO bosch.experimento (experimento_id, nombre)
VALUES (1, 'exp_1'),
       (2, 'exp_2');
INSERT INTO bosch.promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)
VALUES (11, 'cod_pr_1_A', '2019-01-01', '2019-02-28', 1),
       (12, 'cod_pr_1_B', '2019-02-01', '2019-03-30', 1),
       (21, 'cod_pr_2_A', '2019-03-01', '2019-03-15', 2),
       (22, 'cod_pr_2_B', '2019-03-01', '2019-03-15', 2);

INSERT INTO promo_incentivo (promo_id, incentivo_id)
VALUES (11, 2),
       (12, 5),
       (21, 6),
       (22, 7);

INSERT INTO bosch.promo_mercado (promo_id, mercado_id)
VALUES (11, 1),
       (12, 1),
       (21, 2),
       (22, 2);

INSERT INTO bosch.promo_concepto (promo_id, concepto_id)
VALUES (11, 1),
       (12, 1),
       (11, 2),
       (12, 2),
       (11, 7),
       (12, 7),
       (21, 4),
       (22, 4);

INSERT INTO bosch.promo_pg1 (promo_id, pg1_id, pg1_id_with_1, pg1_id_with_2)
VALUES (11, 5, 11, 17),
       (11, 11, 5, 17),
       (11, 17, 5, 11),
       (12, 5, 0, 0),
       (21, 13, 0, 0),
       (22, 13, 0, 0);

INSERT INTO participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion)
VALUES (1, 'B12345X', 12, 7, '2004-12-31', '2005-12-31'),
       (4, 'H98895J', 101, 1, '2018-04-01', '2019-04-01'),
       (5, 'C98345Z', 101, 2, '2019-01-01', '2019-01-01');

INSERT INTO bosch.promo_participante (promo_id, participante_id)
VALUES (11, 1),
       (12, 12),
       (11, 13),
       (12, 4),
       (11, 5),
       (12, 16),
       (11, 17),
       (12, 18),
       (11, 19),
       (12, 20),
       (11, 21),
       (12, 22),
       (11, 23),
       (12, 24),
       (11, 25),
       (12, 26),
       (11, 27),
       (12, 28),
       (11, 29),
       (12, 30);

INSERT INTO bosch.resultado_pg1 (participante_id, pg1_id, cantidad, fecha_resultado)
VALUES (1, 5, 1111, '2019-01-01'),
       (12, 5, 1112, '2019-02-28'),
       (13, 5, 1113, '2019-01-02'),
       (4, 5, 1114, '2019-02-27'),
       (5, 5, 1115, '2019-01-03'),
       (16, 5, 0, '2019-02-26'),
       (17, 5, 1117, '2019-01-04'),
       (18, 5, 1118, '2019-02-25'),
       (19, 5, 1119, '2019-01-05'),
       (20, 5, 0, '2019-02-26'),
       (21, 5, 2001, '2019-01-06'),
       (22, 5, 1002, '2019-02-23'),
       (23, 5, 1003, '2019-01-08'),
       (24, 5, 0, '2019-02-21'),
       (25, 5, 1005, '2019-01-09'),
       (26, 5, 1006, '2019-02-20'),
       (27, 5, 1007, '2019-01-02'),
       (28, 5, 0, '2019-02-27'),
       (29, 5, 1009, '2019-01-11'),
       (30, 5, 1010, '2019-02-28');
