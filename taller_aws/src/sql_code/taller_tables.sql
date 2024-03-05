\W  /* Enable warnings*/

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS concepto;
DROP TABLE IF EXISTS experimento;
DROP TABLE IF EXISTS incentivo;
DROP TABLE IF EXISTS mediocomunicacion;
DROP TABLE IF EXISTS mercado;
DROP TABLE IF EXISTS participante;
DROP TABLE IF EXISTS pg1;
DROP TABLE IF EXISTS promo;
DROP TABLE IF EXISTS promo_concepto;
DROP TABLE IF EXISTS promo_incentivo;
DROP TABLE IF EXISTS promo_mediocomunicacion;
DROP TABLE IF EXISTS promo_mercado;
DROP TABLE IF EXISTS promo_participante;
DROP TABLE IF EXISTS promo_participante_medio;
DROP TABLE IF EXISTS promo_participante_medio_hist;
DROP TABLE IF EXISTS promo_medio_sms;
DROP TABLE IF EXISTS promo_participante_pg1;
DROP TABLE IF EXISTS promo_pg1;
DROP TABLE IF EXISTS promo_simulacion;
DROP TABLE IF EXISTS provincia;
DROP TABLE IF EXISTS resultado_pg1;
DROP TABLE IF EXISTS textclass;
DROP TABLE IF EXISTS textcomunicacion_class;
DROP TABLE IF EXISTS word_class_prob;

CREATE TABLE concepto
(
    concepto_id SMALLINT UNSIGNED NOT NULL,
    nombre      VARCHAR(100)      NOT NULL,
    descripcion VARCHAR(250)      NOT NULL,
    PRIMARY KEY (concepto_id)
);

CREATE TABLE experimento
(
    experimento_id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100),
    PRIMARY KEY (experimento_id)
);

CREATE TABLE incentivo
(
    incentivo_id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(250)      NOT NULL,
    PRIMARY KEY (incentivo_id)
);

CREATE TABLE mediocomunicacion
(
    medio_id SMALLINT UNSIGNED NOT NULL, --  1: ninguno, 2: sms, 3: email.
    nombre   VARCHAR(150)      NOT NULL,
    PRIMARY KEY (medio_id)
);

CREATE TABLE mercado
(
    mercado_id SMALLINT UNSIGNED NOT NULL,
    sigla      CHAR(2)           NOT NULL,
    nombre     VARCHAR(150)      NOT NULL,
    PRIMARY KEY (mercado_id),
    INDEX (sigla)
);

CREATE TABLE participante
(
    participante_id    INTEGER UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_fiscal          VARCHAR(12)       NOT NULL,
    concepto_id        SMALLINT UNSIGNED NOT NULL,
    provincia_id       SMALLINT UNSIGNED NOT NULL,
    email              VARCHAR(200)      NULL,
    tfno               CHAR(11)          NULL,     -- incluye obligatoriamente prefijo de país (34 para ES), sin 00, ni +.
    fecha_registro     DATE              NOT NULL, -- Podría servir para segmentar por antigüedad como registrado.
    fecha_modificacion DATETIME          NOT NULL,
    PRIMARY KEY (participante_id),
    INDEX (provincia_id),
    INDEX (concepto_id),
    UNIQUE (id_fiscal),
    FOREIGN KEY (concepto_id) REFERENCES concepto (concepto_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (provincia_id) REFERENCES provincia (provincia_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE pg1
(
    pg1_id      SMALLINT UNSIGNED NOT NULL,
    cod_pg1     VARCHAR(6)        NOT NULL,
    descripcion VARCHAR(250)      NOT NULL,
    PRIMARY KEY (pg1_id),
    UNIQUE (cod_pg1)
);

--  dias_con_resultados puede sobreestimar la media diaria de ventas que utilizamos como predictor, si los resultados se concentran en los primeros
--  días de la campaña y no hay resultados en los últimos. Esto solo afecta a las campañas en curso.
CREATE TABLE promo
(
    promo_id            INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
    cod_promo           VARCHAR(100)     NOT NULL,
    fecha_inicio        DATE             NOT NULL,
    fecha_fin           DATE             NOT NULL,
    dias_con_resultados INTEGER UNSIGNED NULL DEFAULT 0, -- nº de días entre la fecha del último resultado actualizado para esta promo y la fecha de inicio.
    experimento_id      INTEGER UNSIGNED NOT NULL,
    PRIMARY KEY (promo_id),
    UNIQUE (cod_promo),
    INDEX (experimento_id),
    FOREIGN KEY (experimento_id) REFERENCES experimento (experimento_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE promo_concepto
(
    promo_id    INTEGER UNSIGNED  NOT NULL,
    concepto_id SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (promo_id, concepto_id),
    INDEX (promo_id),
    INDEX (concepto_id),
    FOREIGN KEY (promo_id) REFERENCES promo (promo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (concepto_id) REFERENCES concepto (concepto_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE promo_incentivo
(
    promo_id     INTEGER UNSIGNED  NOT NULL,
    incentivo_id SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (promo_id, incentivo_id),
    INDEX (promo_id),
    INDEX (incentivo_id),
    FOREIGN KEY (promo_id) REFERENCES promo (promo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (incentivo_id) REFERENCES incentivo (incentivo_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE promo_mediocomunicacion
(
    promo_id           INTEGER UNSIGNED  NOT NULL,
    medio_id           SMALLINT UNSIGNED NOT NULL,
    msg_classification SMALLINT UNSIGNED NULL DEFAULT 0,
    promo_medio_text   VARCHAR(500)      NULL DEFAULT 'NA',
    PRIMARY KEY (promo_id, medio_id),
    INDEX (promo_id),
    INDEX (medio_id),
    FOREIGN KEY (promo_id) REFERENCES promo (promo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (medio_id) REFERENCES mediocomunicacion (medio_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE promo_medio_sms
(
    promo_id  INTEGER UNSIGNED  NOT NULL,
    medio_id  SMALLINT UNSIGNED NOT NULL DEFAULT 2,
    batch_id  VARCHAR(250)      NOT NULL,
    send_date TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (promo_id, medio_id),
    FOREIGN KEY (promo_id, medio_id) REFERENCES promo_mediocomunicacion (promo_id, medio_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE promo_mercado
(
    promo_id   INTEGER UNSIGNED  NOT NULL,
    mercado_id SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (promo_id, mercado_id),
    INDEX (promo_id),
    INDEX (mercado_id),
    FOREIGN KEY (promo_id) REFERENCES promo (promo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (mercado_id) REFERENCES mercado (mercado_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

--  Tabla para cargar los participantes obtenidos en las muestras.
-- No incluyo restricciones de integridad con la tabla de participantes.
-- No impongo restricciones a la fecha de apertura del mensaje en el fichero CSV: actualizo tal cual.
--  Permite que se pueda actualizar la tabla de participantes sin que esta tabla quede afectada: es autosuficiente (recoge concepto y provincia).
--  TODO: Requiere borrado periódico por fechas.
CREATE TABLE promo_participante
(
    promo_id        INTEGER UNSIGNED  NOT NULL,
    participante_id INTEGER UNSIGNED  NOT NULL,
    concepto_id     SMALLINT UNSIGNED NULL     DEFAULT 0,
    provincia_id    SMALLINT UNSIGNED NULL     DEFAULT 0,
    dias_registro   INTEGER SIGNED    NULL     DEFAULT 0, -- antigüedad como registrado del participante en la fecha de inicio promo.
    fecha_update    TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (promo_id, participante_id),
    INDEX (promo_id),
    FOREIGN KEY (promo_id) REFERENCES promo (promo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

--  TODO: Requiere borrado periódico por fechas en sintonía con promo_participante.
CREATE TABLE promo_participante_medio
(
    promo_id        INTEGER UNSIGNED  NOT NULL,               -- ésta es la promoción para la que se calcula el ratio PREVIO de aperturas.
    participante_id INTEGER UNSIGNED  NOT NULL,
    medio_id        SMALLINT UNSIGNED NOT NULL,               -- la relación con medio_id es 1 ---> 1.
    recibido_msg    BOOLEAN           NOT NULL DEFAULT FALSE, -- ¿ha recibido el mensaje promocional?
    apertura_msg    BOOLEAN           NOT NULL DEFAULT FALSE, -- ¿ha abierto el mensaje promocional?
    fecha_update    TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (promo_id, participante_id),
    UNIQUE (promo_id, participante_id, medio_id),
    INDEX (medio_id)
);

--  TODO: Requiere borrado periódico por fechas.
--  Esta tabla se utiliza en simulaciones, por ello los datos se refieren a campañas previas a la campaña en curso (promo_id).
CREATE TABLE promo_participante_medio_hist
(
    promo_id        INTEGER UNSIGNED  NOT NULL,           -- ésta es la promoción para la que se calcula el ratio PREVIO de aperturas.
    participante_id INTEGER UNSIGNED  NOT NULL,
    medio_id        SMALLINT UNSIGNED NOT NULL,
    ratio_aperturas DOUBLE            NULL     DEFAULT 0, -- ratio de aperturas en promociones anteriores a ésta.
    fecha_update    TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (promo_id, participante_id, medio_id),
    INDEX (medio_id)
);

--  TODO: Requiere borrado periódico por fechas en sintonía con promo_participante.
CREATE TABLE promo_participante_pg1
(
    promo_id                 INTEGER UNSIGNED  NOT NULL,
    participante_id          INTEGER UNSIGNED  NOT NULL,
    pg1_id                   SMALLINT UNSIGNED NOT NULL,
    vta_media_diaria_pg1_exp DOUBLE            NULL     DEFAULT 0, -- venta media esperada de un pg1 de un participante, basada en promos anteriores.
    vtas_promo_pg1           DOUBLE            NULL     DEFAULT 0, -- ventas hechas en 'esta' campaña en un pg1 por un participante.
    fecha_update             TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (promo_id, participante_id, pg1_id),
    INDEX (promo_id, participante_id)
);

CREATE TABLE promo_pg1
(
    promo_id      INTEGER UNSIGNED  NOT NULL,
    pg1_id        SMALLINT UNSIGNED NOT NULL,
    pg1_id_with_1 SMALLINT UNSIGNED NULL DEFAULT 0,
    pg1_id_with_2 SMALLINT UNSIGNED NULL DEFAULT 0,
    PRIMARY KEY (promo_id, pg1_id),
    INDEX (promo_id),
    INDEX (pg1_id),
    FOREIGN KEY (promo_id) REFERENCES promo (promo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (pg1_id) REFERENCES pg1 (pg1_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

--  TODO: Requiere proceso periódico de borrado o uno asociado a la inserción que borre las promos con fecha anterior a ayer/hoy.
CREATE TABLE promo_simulacion
(
    promo_id        INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
    promo_json      JSON,
    fecha_insercion TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (promo_id)
);

--  Incluye los distritos portugueses
CREATE TABLE provincia
(
    provincia_id SMALLINT UNSIGNED NOT NULL, -- código INE de la provincia para España.
    mercado_id   SMALLINT UNSIGNED NOT NULL,
    nombre       VARCHAR(100)      NOT NULL,
    PRIMARY KEY (provincia_id),
    INDEX (mercado_id),
    FOREIGN KEY (mercado_id) REFERENCES mercado (mercado_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

--  Tabla para la inserción del fichero de resultados.
--  fecha_insercion : permite identificar resultados con fecha_insercion > última fecha_insertion en respuesta TB.
CREATE TABLE resultado_pg1
(
    resultado_id    INTEGER UNSIGNED  NOT NULL AUTO_INCREMENT,
    participante_id INTEGER UNSIGNED  NOT NULL,
    pg1_id          SMALLINT UNSIGNED NOT NULL,
    cantidad        DOUBLE            NOT NULL,
    fecha_resultado DATE              NOT NULL,
    fecha_insercion TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (resultado_id),
    INDEX (participante_id),
    INDEX (pg1_id),
    INDEX (fecha_resultado)
);

CREATE TABLE textclass
(
    class_id   SMALLINT UNSIGNED NOT NULL,
    nombre     VARCHAR(25)       NOT NULL,
    prob_prior DOUBLE            NOT NULL DEFAULT 0,
    PRIMARY KEY (class_id)
);

CREATE TABLE textcomunicacion_class
(
    text_id         INTEGER UNSIGNED  NOT NULL AUTO_INCREMENT,
    text            VARCHAR(500),
    text_class      SMALLINT UNSIGNED NOT NULL,
    fecha_insercion TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (text_id),
    INDEX (text_class),
    FOREIGN KEY (text_class) REFERENCES textclass (class_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE word_class_prob
(
    word       VARCHAR(100)      NOT NULL,
    text_class SMALLINT UNSIGNED NOT NULL,
    prior_prob DOUBLE            NOT NULL DEFAULT 0,
    UNIQUE (word, text_class),
    INDEX (text_class),
    FOREIGN KEY (text_class) REFERENCES textclass (class_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

SET FOREIGN_KEY_CHECKS = 1;

\w   /* Disable warnings*/
