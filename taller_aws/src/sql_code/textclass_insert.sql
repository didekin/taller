INSERT INTO bosch.textclass (class_id, nombre, prob_prior)
VALUES (0, 'NA', 0),
       (1, 'vacacion', ROUND(1/3, 4)),
       (2, 'negocio', ROUND(1/3, 4)),
       (3, 'celebracion', ROUND(1/3, 4))
;
# TODO: actualizar probabilidades en función del proceso de carga de documentos.