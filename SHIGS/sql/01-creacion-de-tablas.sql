-- ============================================================
-- 01 - Creacion de tablas (modelo relacional FINAL del TP3)
--
-- Este archivo consolida el DDL del TP2 original
-- (tp2-tablas-zaragoza.sql) con todas las migraciones intermedias
-- que vivian como scripts aparte en SHIGS/scripts/ (borrados):
--   02-reorganizar-categorias.sql: fusion categoria_riesgo + tipo_riesgo
--   03-una-evaluacion-por-riesgo.sql: UNIQUE (id_arpt, id_riesgo)
--   05-versionado-arpt.sql: columna generada uq_vigente_tarea
--   06-familias-tipo-riesgo.sql: id_padre en tipo_riesgo
--
-- Esta es la base desde cero, ya en el estado que espera el codigo
-- Java actual. Para una base ya migrada, los ALTER daran error y
-- hay que ignorar.
--
-- Convencion de ejecucion:
--   docker cp SHIGS/sql/01-creacion-de-tablas.sql mysql-sighs:/tmp/01.sql
--   docker exec mysql-sighs sh -c "mysql -uroot -pPASS --default-character-set=utf8mb4 < /tmp/01.sql"
-- ============================================================

CREATE DATABASE IF NOT EXISTS sighs_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sighs_db;

-- ---------------------------------------------------------------------
-- Tabla usuario (modelo TP3: nombre_usuario, password_hash, salt,
-- estado, intentos_fallidos, auditoria)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario        INT AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario    VARCHAR(50) NOT NULL,
    password_hash     CHAR(64) NOT NULL,
    salt              VARCHAR(50) NOT NULL,
    nombre_completo   VARCHAR(150),
    estado            ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    intentos_fallidos INT NOT NULL DEFAULT 0,
    id_created_by     INT,
    id_updated_by     INT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_nombre UNIQUE (nombre_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla rol (N:M con usuario via usuario_rol)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rol (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuario_rol (
    id_usuario INT NOT NULL,
    id_rol     INT NOT NULL,
    PRIMARY KEY (id_usuario, id_rol),
    CONSTRAINT fk_ur_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT fk_ur_rol     FOREIGN KEY (id_rol)     REFERENCES rol(id_rol)
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
    nombre_referente      VARCHAR(150),
    cargo_referente       VARCHAR(100),
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_empresa_cuit UNIQUE (cuit)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla establecimiento (jerarquia Empresa -> Establecimiento -> ...)
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
    id_sector          INT AUTO_INCREMENT PRIMARY KEY,
    id_establecimiento INT NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    descripcion        VARCHAR(255),
    responsable        VARCHAR(100),
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
-- Tabla tipo_riesgo (modelo vigente TP3: con id_padre para familias).
--   La tabla categoria_riesgo del TP2 se conserva por compatibilidad
--   pero el modelo real usa tipo_riesgo como jerarquia unica.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tipo_riesgo (
    id_tipo_riesgo INT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    id_padre       INT NULL,
    CONSTRAINT uq_tipo_riesgo_nombre UNIQUE (nombre),
    KEY idx_tipo_riesgo_padre (id_padre),
    CONSTRAINT fk_tipo_riesgo_padre FOREIGN KEY (id_padre)
        REFERENCES tipo_riesgo (id_tipo_riesgo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categoria_riesgo (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla riesgo (modelo TP3: id_tipo_riesgo en lugar de id_categoria)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS riesgo (
    id_riesgo      INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria   INT NOT NULL,
    id_tipo_riesgo INT NULL,
    codigo         VARCHAR(20) NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    descripcion    VARCHAR(255),
    vigente        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_riesgo_codigo UNIQUE (codigo),
    CONSTRAINT fk_riesgo_categoria FOREIGN KEY (id_categoria)   REFERENCES categoria_riesgo(id_categoria),
    CONSTRAINT fk_riesgo_tipo      FOREIGN KEY (id_tipo_riesgo) REFERENCES tipo_riesgo(id_tipo_riesgo)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla medida_control
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medida_control (
    id_medida   INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(200) NOT NULL,
    tipo        VARCHAR(50)  NOT NULL
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
-- Tabla bitacora (RNF-04: registrar toda operacion sobre entidades
-- criticas). Modelo TP3: operacion libre (VARCHAR) + resultado libre,
-- detalle libre.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bitacora (
    id_bitacora BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario  INT NOT NULL,
    operacion   VARCHAR(80) NOT NULL,
    id_empresa  INT,
    detalle     TEXT,
    resultado   VARCHAR(20),
    fecha_hora  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bitacora_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla arpt (modelo TP3: id_visita en vez de id_visita_origen;
-- estado lowercase; con columna generada para versionado).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS arpt (
    id_arpt                 INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea                INT NOT NULL,
    id_visita               INT NULL,
    fecha                   DATE NOT NULL,
    estado                  ENUM('edicion','vigente','historico') NOT NULL DEFAULT 'edicion',
    observaciones_generales TEXT,
    conclusion_general      TEXT,
    id_created_by           INT,
    id_updated_by           INT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Cardinalidad tarea -> ARPT (RD-07):
    --   - A lo sumo un ARPT VIGENTE por tarea. Lo enforce la
    --     columna generada uq_vigente_tarea + el UNIQUE KEY
    --     uq_arpt_vigente_tarea de abajo.
    --   - A lo sumo un ARPT en EDICION por tarea al mismo tiempo
    --     (cuando hay un VIGENTE, no deberia haber uno en edicion;
    --     esto lo controla la aplicacion, no la base).
    --   - N ARPTs HISTORICO permitidos (son las versiones viejas
    --     que se reemplazaron). La tabla los acepta sin tope.
    --   - El UNIQUE KEY de abajo solo cubre el caso VIGENTE
    --     porque la columna generada vale NULL en los otros
    --     estados y MySQL trata varios NULL como distintos en
    --     indices UNIQUE.
    uq_vigente_tarea INT GENERATED ALWAYS AS
        (CASE WHEN estado = 'vigente' THEN id_tarea END) STORED,
    KEY idx_arpt_tarea (id_tarea),
    UNIQUE KEY uq_arpt_vigente_tarea (uq_vigente_tarea),
    CONSTRAINT fk_arpt_tarea  FOREIGN KEY (id_tarea)  REFERENCES tarea(id_tarea),
    CONSTRAINT fk_arpt_visita FOREIGN KEY (id_visita) REFERENCES visita(id_visita)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla evaluacion_riesgo (modelo TP3: id_medida_control + auditoria)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluacion_riesgo (
    id_evaluacion      INT AUTO_INCREMENT PRIMARY KEY,
    id_arpt            INT NOT NULL,
    id_riesgo          INT NOT NULL,
    id_medida_control  INT NULL,
    probabilidad       INT NOT NULL,
    exposicion         INT NOT NULL,
    consecuencia       INT NOT NULL,
    grado_peligrosidad INT NOT NULL,
    clasificacion      ENUM('TOLERABLE','PELIGROSO','URGENTE','CRITICO') NOT NULL,
    id_created_by      INT,
    id_updated_by      INT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 03-una-evaluacion-por-riesgo.sql: una sola evaluacion por
    -- (ARPT, riesgo). La creacion del UNIQUE vive aca, no necesita
    -- ser un script de migracion aparte.
    UNIQUE KEY uq_eval_arpt_riesgo (id_arpt, id_riesgo),
    CONSTRAINT fk_evr_arpt   FOREIGN KEY (id_arpt)           REFERENCES arpt(id_arpt),
    CONSTRAINT fk_evr_riesgo FOREIGN KEY (id_riesgo)         REFERENCES riesgo(id_riesgo),
    CONSTRAINT fk_evr_medida FOREIGN KEY (id_medida_control) REFERENCES medida_control(id_medida)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla evaluacion_epp (modelo TP3: PK propia id_evaluacion_epp)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluacion_epp (
    id_evaluacion_epp INT AUTO_INCREMENT PRIMARY KEY,
    id_evaluacion     INT NOT NULL,
    id_epp            INT NOT NULL,
    UNIQUE KEY uq_evr_epp (id_evaluacion, id_epp),
    CONSTRAINT fk_evepp_evr FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_riesgo(id_evaluacion),
    CONSTRAINT fk_evepp_epp FOREIGN KEY (id_epp)         REFERENCES epp(id_epp)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla control_epp (modelo TP3: con FK a id_puesto e id_arpt)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS control_epp (
    id_control_epp INT AUTO_INCREMENT PRIMARY KEY,
    id_visita      INT NOT NULL,
    id_puesto      INT NOT NULL,
    id_tarea       INT NOT NULL,
    id_arpt        INT NOT NULL,
    fecha          DATE NOT NULL,
    estado         ENUM('registrado','cerrado') NOT NULL DEFAULT 'registrado',
    CONSTRAINT fk_control_visita FOREIGN KEY (id_visita) REFERENCES visita(id_visita),
    CONSTRAINT fk_control_puesto FOREIGN KEY (id_puesto) REFERENCES puesto(id_puesto),
    CONSTRAINT fk_control_tarea  FOREIGN KEY (id_tarea)  REFERENCES tarea(id_tarea),
    CONSTRAINT fk_control_arpt   FOREIGN KEY (id_arpt)   REFERENCES arpt(id_arpt)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla control_epp_detalle
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS control_epp_detalle (
    id_detalle        INT AUTO_INCREMENT PRIMARY KEY,
    id_control_epp    INT NOT NULL,
    id_evaluacion_epp INT NOT NULL,
    valor_uso         INT NOT NULL,
    valor_estado      INT NOT NULL,
    observaciones     VARCHAR(500),
    recomendaciones   VARCHAR(500),
    CONSTRAINT fk_ctrdet_control FOREIGN KEY (id_control_epp)    REFERENCES control_epp(id_control_epp),
    CONSTRAINT fk_ctrdet_evepp   FOREIGN KEY (id_evaluacion_epp) REFERENCES evaluacion_epp(id_evaluacion_epp)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla plan_accion (modelo TP3: tipo_origen + id_origen + baja_logica)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS plan_accion (
    id_plan_accion  INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa      INT NOT NULL,
    tipo_origen     ENUM('arpt','control_epp','visita') NOT NULL,
    id_origen       INT NOT NULL,
    descripcion     VARCHAR(500) NOT NULL,
    -- responsable: nombre libre de la persona de la empresa cliente
    -- que se hara cargo del plan (NO es FK a usuario; el staff de
    -- Zaragoza no necesariamente esta atado a un usuario del sistema).
    -- Coincide con el modelo del TP2 (plan_accion.responsable VARCHAR).
    responsable     VARCHAR(150) NOT NULL,
    fecha_plazo     DATE,
    estado          ENUM('entratamiento','cumplido','nocumplido') NOT NULL DEFAULT 'entratamiento',
    fecha_cierre    DATE,
    baja_logica     BOOLEAN NOT NULL DEFAULT FALSE,
    -- id_responsable queda como columna legacy nullable del esquema
    -- viejo (FK a usuario). En el modelo actual no se setea nunca: el
    -- responsable real esta en `responsable`. Si en el futuro se quiere
    -- vincular a un usuario del sistema, se hace por aca.
    id_responsable  INT NULL,
    id_created_by   INT,
    id_updated_by   INT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pa_empresa     FOREIGN KEY (id_empresa)      REFERENCES empresa(id_empresa),
    CONSTRAINT fk_pa_responsable FOREIGN KEY (id_responsable)  REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;