
CREATE DATABASE IF NOT EXISTS sighs_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;




USE sighs_db;

-- ---------------------------------------------------------------------
-- Tabla usuario
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    hash_password  VARCHAR(255) NOT NULL,
    salt           VARCHAR(64)  NOT NULL,
    rol            ENUM('TECNICO','RESPONSABLE','ADMINISTRACION') NOT NULL,
    matricula      VARCHAR(50),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_usuario_email UNIQUE (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla empresa
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS empresa (
    id_empresa            INT AUTO_INCREMENT PRIMARY KEY,
    razon_social          VARCHAR(150) NOT NULL,
    cuit                  VARCHAR(13)  NOT NULL,
    domicilio             VARCHAR(200),
    actividad             VARCHAR(100),
    cantidad_trabajadores INT,
    email_referente       VARCHAR(150),
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_empresa_cuit UNIQUE (cuit)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla establecimiento
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS establecimiento (
    id_establecimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa          INT NOT NULL,
    nombre              VARCHAR(100) NOT NULL,
    tipo                ENUM('FABRICA','DEPOSITO','OFICINA','OTRO') NOT NULL,
    domicilio           VARCHAR(200),
    CONSTRAINT fk_establecimiento_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla sector
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sector (
    id_sector         INT AUTO_INCREMENT PRIMARY KEY,
    id_establecimiento INT NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       VARCHAR(255),
    responsable       VARCHAR(100),
    CONSTRAINT fk_sector_establecimiento
        FOREIGN KEY (id_establecimiento) REFERENCES establecimiento(id_establecimiento)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla puesto
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS puesto (
    id_puesto             INT AUTO_INCREMENT PRIMARY KEY,
    id_sector             INT NOT NULL,
    nombre                VARCHAR(100) NOT NULL,
    cantidad_trabajadores INT,
    CONSTRAINT fk_puesto_sector
        FOREIGN KEY (id_sector) REFERENCES sector(id_sector)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla tarea
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tarea (
    id_tarea    INT AUTO_INCREMENT PRIMARY KEY,
    id_puesto   INT NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    CONSTRAINT fk_tarea_puesto
        FOREIGN KEY (id_puesto) REFERENCES puesto(id_puesto)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla paso_tarea
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paso_tarea (
    id_paso     INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea    INT NOT NULL,
    orden       INT NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT uq_paso_orden UNIQUE (id_tarea, orden),
    CONSTRAINT fk_paso_tarea
        FOREIGN KEY (id_tarea) REFERENCES tarea(id_tarea)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla visita
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS visita (
    id_visita     INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa    INT NOT NULL,
    id_usuario    INT NOT NULL,
    fecha         DATE NOT NULL,
    tipo          ENUM('PROGRAMADA','SEGUIMIENTO','EXTRAORDINARIA') NOT NULL,
    estado        ENUM('PLANIFICADA','EN_CURSO','CERRADA','CANCELADA') NOT NULL,
    observaciones TEXT,
    CONSTRAINT fk_visita_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa),
    CONSTRAINT fk_visita_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla categoria_riesgo
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categoria_riesgo (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla riesgo
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS riesgo (
    id_riesgo    INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria INT NOT NULL,
    codigo       VARCHAR(20) NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(255),
    vigente      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_riesgo_codigo UNIQUE (codigo),
    CONSTRAINT fk_riesgo_categoria
        FOREIGN KEY (id_categoria) REFERENCES categoria_riesgo(id_categoria)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla arpt
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS arpt (
    id_arpt            INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea           INT NOT NULL,
    id_visita_origen   INT,
    fecha              DATE NOT NULL,
    estado             ENUM('BORRADOR','VIGENTE','HISTORICO') NOT NULL,
    decision_profesional TEXT,
    CONSTRAINT fk_arpt_tarea
        FOREIGN KEY (id_tarea) REFERENCES tarea(id_tarea),
    CONSTRAINT fk_arpt_visita
        FOREIGN KEY (id_visita_origen) REFERENCES visita(id_visita),
    CONSTRAINT chk_arpt_estado CHECK (estado IN ('BORRADOR','VIGENTE','HISTORICO'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla evaluacion_riesgo
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluacion_riesgo (
    id_evaluacion      INT AUTO_INCREMENT PRIMARY KEY,
    id_arpt            INT NOT NULL,
    id_riesgo          INT NOT NULL,
    probabilidad       TINYINT NOT NULL,
    exposicion         TINYINT NOT NULL,
    consecuencia       TINYINT NOT NULL,
    grado_peligrosidad SMALLINT NOT NULL,
    clasificacion      ENUM('TOLERABLE','PELIGROSO','URGENTE','CRITICO') NOT NULL,
    CONSTRAINT fk_evaluacion_arpt
        FOREIGN KEY (id_arpt) REFERENCES arpt(id_arpt),
    CONSTRAINT fk_evaluacion_riesgo
        FOREIGN KEY (id_riesgo) REFERENCES riesgo(id_riesgo),
    CONSTRAINT chk_probabilidad CHECK (probabilidad BETWEEN 1 AND 5),
    CONSTRAINT chk_exposicion   CHECK (exposicion   BETWEEN 1 AND 5),
    CONSTRAINT chk_consecuencia CHECK (consecuencia BETWEEN 1 AND 5),
    CONSTRAINT chk_clasificacion CHECK (clasificacion IN ('TOLERABLE','PELIGROSO','URGENTE','CRITICO'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla recomendacion
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recomendacion (
    id_recomendacion INT AUTO_INCREMENT PRIMARY KEY,
    descripcion      VARCHAR(255) NOT NULL,
    parametrizada    BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla epp
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS epp (
    id_epp      INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla asociativa evaluacion_recomendacion
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluacion_recomendacion (
    id_evaluacion    INT NOT NULL,
    id_recomendacion INT NOT NULL,
    PRIMARY KEY (id_evaluacion, id_recomendacion),
    CONSTRAINT fk_evrec_evaluacion
        FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_riesgo(id_evaluacion),
    CONSTRAINT fk_evrec_recomendacion
        FOREIGN KEY (id_recomendacion) REFERENCES recomendacion(id_recomendacion)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla asociativa evaluacion_epp
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluacion_epp (
    id_evaluacion INT NOT NULL,
    id_epp        INT NOT NULL,
    PRIMARY KEY (id_evaluacion, id_epp),
    CONSTRAINT fk_evepp_evaluacion
        FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_riesgo(id_evaluacion),
    CONSTRAINT fk_evepp_epp
        FOREIGN KEY (id_epp) REFERENCES epp(id_epp)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla control_epp
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS control_epp (
    id_control     INT AUTO_INCREMENT PRIMARY KEY,
    id_visita      INT NOT NULL,
    id_tarea       INT NOT NULL,
    fecha          DATE NOT NULL,
    resultado      ENUM('DISPONIBLE','EN_USO','AUSENTE','DETERIORADO') NOT NULL,
    observaciones  TEXT,
    CONSTRAINT fk_control_visita
        FOREIGN KEY (id_visita) REFERENCES visita(id_visita),
    CONSTRAINT fk_control_tarea
        FOREIGN KEY (id_tarea) REFERENCES tarea(id_tarea)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla asociativa control_epp_items (qué EPP se verificaron)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS control_epp_item (
    id_control INT NOT NULL,
    id_epp     INT NOT NULL,
    presente   BOOLEAN NOT NULL,
    en_uso     BOOLEAN NOT NULL,
    PRIMARY KEY (id_control, id_epp),
    CONSTRAINT fk_item_control
        FOREIGN KEY (id_control) REFERENCES control_epp(id_control),
    CONSTRAINT fk_item_epp
        FOREIGN KEY (id_epp) REFERENCES epp(id_epp)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla plan_accion
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS plan_accion (
    id_accion                 INT AUTO_INCREMENT PRIMARY KEY,
    id_evaluacion             INT NOT NULL,
    descripcion               VARCHAR(255) NOT NULL,
    responsable               VARCHAR(100) NOT NULL,
    fecha_compromiso          DATE NOT NULL,
    fecha_cierre              DATE,
    estado                    ENUM('EN_TRATAMIENTO','SUBSANADO','VENCIDO','VERIFICADO') NOT NULL,
    justificacion_apartamiento TEXT,
    CONSTRAINT fk_accion_evaluacion
        FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_riesgo(id_evaluacion)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla trabajador
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trabajador (
    id_trabajador   INT AUTO_INCREMENT PRIMARY KEY,
    id_sector       INT,
    id_empresa      INT NOT NULL,
    legajo          VARCHAR(50) NOT NULL,
    nombre          VARCHAR(150) NOT NULL,
    fecha_ingreso   DATE,
    CONSTRAINT uq_trabajador_legajo UNIQUE (legajo),
    CONSTRAINT fk_trabajador_sector
        FOREIGN KEY (id_sector) REFERENCES sector(id_sector),
    CONSTRAINT fk_trabajador_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla capacitacion
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS capacitacion (
    id_capacitacion  INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa       INT NOT NULL,
    id_accion        INT,
    tema             VARCHAR(150) NOT NULL,
    fecha            DATE NOT NULL,
    duracion_horas   DECIMAL(5,2),
    modalidad        ENUM('PRESENCIAL','VIRTUAL','MIXTA') NOT NULL,
    CONSTRAINT fk_capacitacion_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa),
    CONSTRAINT fk_capacitacion_accion
        FOREIGN KEY (id_accion) REFERENCES plan_accion(id_accion)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla asociativa capacitacion_trabajador
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS capacitacion_trabajador (
    id_capacitacion  INT NOT NULL,
    id_trabajador    INT NOT NULL,
    asistio          BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_capacitacion, id_trabajador),
    CONSTRAINT fk_cap_trab_cap
        FOREIGN KEY (id_capacitacion) REFERENCES capacitacion(id_capacitacion),
    CONSTRAINT fk_cap_trab_trab
        FOREIGN KEY (id_trabajador) REFERENCES trabajador(id_trabajador)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla bitacora
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bitacora (
    id_bitacora  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario   INT NOT NULL,
    entidad      VARCHAR(50) NOT NULL,
    id_entidad   INT NOT NULL,
    operacion    ENUM('ALTA','MODIFICACION','BAJA','LOGIN','CONSULTA','CONFIRMAR_ARPT','ENVIAR_INFORME') NOT NULL,
    fecha_hora   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalle      TEXT,
    CONSTRAINT fk_bitacora_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla informe_mensual
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS informe_mensual (
    id_informe   INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa   INT NOT NULL,
    periodo      CHAR(7) NOT NULL, -- formato AAAA-MM
    ruta_pdf     VARCHAR(255) NOT NULL,
    fecha_envio  DATETIME,
    estado_envio ENUM('PENDIENTE','ENVIADO','ERROR') NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT fk_informe_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa)
) ENGINE=InnoDB;




select * from informe_mensual