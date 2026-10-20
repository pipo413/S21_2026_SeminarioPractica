-- ============================================================
-- 02 - Semillas (datos minimos para que el prototipo funcione)
--
-- Idempotente: usa INSERT ... ON DUPLICATE KEY UPDATE por PK y UNIQUE.
-- Si la base ya tiene datos, los actualiza sin romper nada.
--
-- Orden de ejecucion: inmediatamente despues de
--   SHIGS/sql/01-creacion-de-tablas.sql.
--
-- Contenido (consolida lo que vivia como 01..07- aparte):
--   - 5 roles del sistema
--   - 5 usuarios demo (tecnico1, tecnico2, resp.hys, admin, demo)
--     todos con la misma contrasena 123456 (mismo hash SHA256
--     con sal unica) para facilidad de pruebas
--   - Catalogo de tipos de riesgo con familias y subtipos
--   - 3 empresas, 3 establecimientos, 3 sectores, 6 puestos,
--     6 tareas y 9 visitas (3 por empresa)
--
-- Convencion de ejecucion:
--   docker cp SHIGS/sql/02-semillas.sql mysql-sighs:/tmp/02.sql
--   docker exec mysql-sighs sh -c "mysql -uroot -pPASS --default-character-set=utf8mb4 < /tmp/02.sql"
-- ============================================================

USE sighs_db;

-- ============================================================
-- Roles
-- ============================================================
INSERT INTO rol (id_rol, nombre) VALUES
    (1, 'Tecnico en HyS'),
    (2, 'Responsable del Servicio'),
    (3, 'Administrador'),
    (4, 'demo')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- ============================================================
-- Usuarios demo (5 usuarios, contrasena unificada a 123456).
-- El hash es SHA256(CONCAT('123456', salt_unico)); cada usuario
-- tiene su propio salt. Ver ServicioAutenticacion.java (TP3) para
-- el algoritmo de verificacion.
-- ============================================================
INSERT INTO usuario (id_usuario, nombre_usuario, password_hash, salt, nombre_completo, estado, id_created_by, id_updated_by)
VALUES
    (1, 'tecnico1', SHA2(CONCAT('123456', 'salt_tec1'), 256), 'salt_tec1', 'Tecnico 1',          'activo', 1, 1),
    (2, 'tecnico2', SHA2(CONCAT('123456', 'salt_tec2'), 256), 'salt_tec2', 'Tecnico 2',          'activo', 1, 1),
    (3, 'resp.hys', SHA2(CONCAT('123456', 'salt_resp'), 256), 'salt_resp', 'Responsable HyS',     'activo', 1, 1),
    (4, 'admin',    SHA2(CONCAT('123456', 'salt_adm'),  256), 'salt_adm',  'Administrador SIGHS', 'activo', 1, 1),
    (5, 'demo',     SHA2(CONCAT('123456', 'salt_demo'), 256), 'salt_demo', 'Usuario Demo Tecnico', 'activo', 1, 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    salt          = VALUES(salt),
    nombre_completo = VALUES(nombre_completo),
    estado        = 'activo',
    intentos_fallidos = 0,
    updated_at    = CURRENT_TIMESTAMP,
    id_updated_by = 1;

INSERT INTO usuario_rol (id_usuario, id_rol) VALUES
    (1, 1),
    (2, 1),
    (3, 2),
    (4, 3),
    (5, 1)
ON DUPLICATE KEY UPDATE id_rol = VALUES(id_rol);

-- ============================================================
-- Catalogo de tipos de riesgo (familias + subtipos).
--   Las 5 familias (ids 1..5) son el nivel 0 (id_padre NULL).
--   Los subtipos llevan id_padre apuntando a su familia.
--   Acordes con el TP3 §13.1: Fisicos, Fisicos-Mecanicos, Quimicos,
--   Ergonomicos, Biologicos. (Origen histoivo: scripts/01-, 02-, 04-, 06-)
-- ============================================================
INSERT INTO tipo_riesgo (id_tipo_riesgo, nombre, id_padre) VALUES
    -- Familias (nivel 0)
    (1, 'Fisicos',          NULL),
    (2, 'Fisicos-Mecanicos', NULL),
    (3, 'Quimicos',         NULL),
    (4, 'Ergonomicos',      NULL),
    (5, 'Biologicos',       NULL),
    -- Subtipos de Fisicos (id_padre = 1)
    (6,  'Electrico',                1),
    (7,  'Incendio',                 1),
    (8,  'Quemadura por contacto',   1),
    (9,  'Explosion',                1),
    (10, 'Ruido',                    1),
    (11, 'Iluminacion',              1),
    (12, 'Radiacion',                1),
    -- Subtipos de Fisicos-Mecanicos (id_padre = 2)
    (13, 'Proyeccion',               2),
    (14, 'Corte',                    2),
    (15, 'Caida a dif. nivel',       2),
    (16, 'Caida a mismo nivel',      2),
    (17, 'Atropellamiento',          2),
    (18, 'Golpe por caida de objetos', 2),
    (19, 'Choque contra objetos',    2),
    (20, 'Atrapamiento por objetos',  2),
    (21, 'Atrapamiento por maquinas', 2),
    -- Subtipos de Ergonomicos (id_padre = 4)
    (22, 'Postural',                 4),
    (23, 'Mov. repetitivos',         4),
    (24, 'Sobre esfuerzo',           4),
    -- Subtipos de Quimicos (id_padre = 3)
    (25, 'Inhalacion / ingesta sust. nociva', 3),
    (26, 'Contacto sust. corrosivas',         3)
ON DUPLICATE KEY UPDATE
    nombre   = VALUES(nombre),
    id_padre = VALUES(id_padre);

-- ============================================================
-- Categorias de riesgo (necesarias por la FK de riesgo.id_categoria).
-- Coinciden con las familias del bloque anterior de tipo_riesgo.
-- ============================================================
INSERT INTO categoria_riesgo (id_categoria, nombre) VALUES
    (1, 'Fisicos'),
    (2, 'Fisicos-Mecanicos'),
    (3, 'Quimicos'),
    (4, 'Ergonomicos'),
    (5, 'Biologicos')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- ============================================================
-- Catalogo de riesgos (id_categoria NOT NULL, id_tipo_riesgo opcional).
-- Codigos estables: prefijo de la categoria + numero. Pensados para
-- el dominio metalmecanico/pintura/alimentos del TP1.
-- ============================================================
INSERT INTO riesgo (id_riesgo, id_categoria, id_tipo_riesgo, codigo, nombre, descripcion, vigente)
VALUES
    -- Fisicos
    (1, 1, 10, 'FIS-RUI', 'Ruido continuo por encima de 85 dBA',         'Exposicion prolongada a ruido en el puesto de trabajo',                       TRUE),
    (2, 1, 11, 'FIS-ILU', 'Iluminacion insuficiente',                     'Niveles de iluminancia por debajo del minimo reglamentario',                   TRUE),
    (3, 1, 6,  'FIS-ELE', 'Contacto con energia electrica',              'Riesgo de electrocucion por contacto directo o indirecto',                      TRUE),
    -- Fisicos-Mecanicos
    (4, 2, 21, 'MEC-ATR', 'Contacto con partes moviles de una maquina', 'Atrapamiento por partes en movimiento de maquinaria',                          TRUE),
    (5, 2, 13, 'MEC-PRY', 'Proyeccion de fragmentos',                     'Proyeccion de particulas durante mecanizado',                                   TRUE),
    (6, 2, 14, 'MEC-COR', 'Corte con herramientas manuales',             'Cortes durante uso de herramientas de filo',                                    TRUE),
    (7, 2, 15, 'MEC-CAI', 'Caida a distinto nivel',                      'Caida desde altura durante tareas en plataformas o escaleras',                  TRUE),
    -- Ergonomicos
    (8, 4, 22, 'ERG-POS', 'Postura forzada de pie',                      'Bipedestacion prolongada sin alternancia de postura',                            TRUE),
    (9, 4, 23, 'ERG-MOV', 'Movimientos repetitivos',                     'Movimientos ciclicos de alta frecuencia en miembros superiores',                  TRUE),
    -- Quimicos
    (10, 3, 25, 'QUI-INH', 'Inhalacion de solventes',                    'Exposicion a vapores organicos durante tareas de pintura',                       TRUE),
    (11, 3, 26, 'QUI-CON', 'Contacto con sustancias corrosivas',         'Salpicaduras de acidos o bases durante limpieza de superficies',                TRUE)
ON DUPLICATE KEY UPDATE
    nombre      = VALUES(nombre),
    descripcion = VALUES(descripcion),
    vigente     = TRUE;

-- ============================================================
-- Medidas de control (catalogo para asociar a evaluacion_riesgo).
-- Coinciden con las que sugiere la metodologia GP del TP3.
-- ============================================================
INSERT INTO medida_control (id_medida, descripcion, tipo) VALUES
    (1, 'Ventilacion localizada en cabina de pintura',                    'Ingenieria'),
    (2, 'Proteccion auditiva tipo copa o tapones',                        'EPP'),
    (3, 'Gafas de seguridad con filtro UV',                               'EPP'),
    (4, 'Guantes de nitrilo para manipulacion de solventes',              'EPP'),
    (5, 'Respirador con filtro para vapores organicos',                   'EPP'),
    (6, 'Capacitacion sobre riesgos mecanicos',                            'Administrativa'),
    (7, 'Rotacion de puestos para limitar exposicion a ruido',             'Administrativa'),
    (8, 'Mantenimiento preventivo de protecciones de maquinaria',         'Ingenieria')
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    tipo        = VALUES(tipo);

-- ============================================================
-- Catalogo de EPP.
-- ============================================================
INSERT INTO epp (id_epp, nombre, descripcion) VALUES
    (1, 'Proteccion auditiva',            'Copa o tapones para ambiente con ruido > 85 dBA'),
    (2, 'Gafas de seguridad',              'Proteccion ocular contra proyecciones y polvo'),
    (3, 'Guantes de nitrilo',              'Manipulacion de solventes y sustancias quimicas'),
    (4, 'Casco de seguridad',              'Proteccion craneal en planta'),
    (5, 'Mascarilla con filtro para solventes', 'Respirador descartable para pintura y lijado'),
    (6, 'Calzado de seguridad con puntera', 'Calzado de seguridad con puntera de acero')
ON DUPLICATE KEY UPDATE
    nombre      = VALUES(nombre),
    descripcion = VALUES(descripcion);

-- ============================================================
-- Empresas (3, del dominio declarado en el TP1)
-- ============================================================
INSERT INTO empresa (id_empresa, razon_social, cuit, domicilio, actividad,
                     cantidad_trabajadores, email_referente,
                     nombre_referente, cargo_referente, activo)
VALUES
    (1, 'MetalCordoba SA',        '30-12345678-9', 'Av. Sabattini 1234, Cordoba',    'Metalmecanica',     45, 'rrhh@metalcordoba.com.ar',     'Juan Perez',       'Jefe RRHH',          TRUE),
    (2, 'Pinturas del Centro SA', '30-23456789-0', 'Bv. San Juan 567, Cordoba',      'Pintura industrial', 22, 'info@pinturascentro.com.ar',  'Maria Lopez',      'Jefe Produccion',    TRUE),
    (3, 'Alimentos del Sur SA',   '30-34567890-1', 'Ruta 36 km 5, Buenos Aires',     'Alimentos',          60, 'admin@alimentosdelsur.com.ar', 'Carlos Rodriguez', 'Gerente Operaciones', TRUE)
ON DUPLICATE KEY UPDATE
    razon_social    = VALUES(razon_social),
    domicilio       = VALUES(domicilio),
    actividad       = VALUES(actividad),
    email_referente = VALUES(email_referente),
    nombre_referente = VALUES(nombre_referente),
    cargo_referente  = VALUES(cargo_referente),
    activo          = TRUE;

-- ============================================================
-- Establecimientos (uno por empresa)
-- ============================================================
INSERT INTO establecimiento (id_establecimiento, id_empresa, nombre, tipo, domicilio)
VALUES
    (1, 1, 'Planta Cordoba',      'FABRICA', 'Av. Sabattini 1234, Cordoba'),
    (2, 2, 'Planta San Juan',     'FABRICA', 'Bv. San Juan 567, Cordoba'),
    (3, 3, 'Planta Buenos Aires', 'FABRICA', 'Ruta 36 km 5, Buenos Aires')
ON DUPLICATE KEY UPDATE
    nombre    = VALUES(nombre),
    tipo      = VALUES(tipo),
    domicilio = VALUES(domicilio);

-- ============================================================
-- Sectores (uno por establecimiento)
-- ============================================================
INSERT INTO sector (id_sector, id_establecimiento, nombre, descripcion, responsable)
VALUES
    (1, 1, 'Torno',    'Mecanizado y produccion de piezas metalicas',   'Juan Perez'),
    (2, 2, 'Pintura',  'Aplicacion de recubrimientos y acabados',       'Maria Lopez'),
    (3, 3, 'Envasado', 'Envasado y almacenamiento en frio de productos', 'Carlos Rodriguez')
ON DUPLICATE KEY UPDATE
    nombre       = VALUES(nombre),
    descripcion  = VALUES(descripcion),
    responsable  = VALUES(responsable);

-- ============================================================
-- Puestos (dos por sector)
-- ============================================================
INSERT INTO puesto (id_puesto, id_sector, nombre, cantidad_trabajadores)
VALUES
    (1, 1, 'Operario CNC',          6),
    (2, 1, 'Soldador',              4),
    (3, 2, 'Operario pintura',      5),
    (4, 2, 'Prep. superficies',     3),
    (5, 3, 'Operario linea',        8),
    (6, 3, 'Operario camara frio',  3)
ON DUPLICATE KEY UPDATE
    nombre                = VALUES(nombre),
    cantidad_trabajadores = VALUES(cantidad_trabajadores);

-- ============================================================
-- Tareas (2-3 por puesto, suficientes para probar el cascadeo
-- Sector -> Puesto -> Tarea con variedad real en cada combo).
-- ============================================================
INSERT INTO tarea (id_tarea, id_puesto, nombre, descripcion)
VALUES
    -- Sector Torno: Operario CNC (puesto 1) - 3 tareas
    (1, 1, 'Fresado de piezas',         'Fresado CNC de piezas metalicas segun plano'),
    (7, 1, 'Torneado de ejes',          'Torneado manual de ejes y cilindros sobre plano'),
    (8, 1, 'Mantenimiento de maquinas', 'Cambio de refrigerante y revision de husillo'),
    -- Sector Torno: Soldador (puesto 2) - 2 tareas
    (2, 2, 'Soldadura MIG',             'Union de piezas por soldadura MIG'),
    (9, 2, 'Soldadura por puntos',      'Soldadura de chapas finas por resistencia'),
    -- Sector Pintura: Operario pintura (puesto 3) - 2 tareas
    (3, 3, 'Pintura con soplete',       'Aplicacion de pintura con soplete'),
    (10, 3, 'Pintura electrostatica',   'Aplicacion de pintura en polvo por carga electrostatica'),
    -- Sector Pintura: Prep. superficies (puesto 4) - 2 tareas
    (4, 4, 'Lijado de piezas',          'Preparacion de superficies por lijado'),
    (11, 4, 'Masillado y nivelacion',   'Aplicacion de masilla y lijado fino de acabado'),
    -- Sector Envasado: Operario linea (puesto 5) - 2 tareas
    (5, 5, 'Envasado manual',           'Envasado manual de productos en linea'),
    (12, 5, 'Etiquetado y codificado',  'Colocacion de etiquetas y verificacion de codigos de barra'),
    -- Sector Envasado: Operario camara frio (puesto 6) - 2 tareas
    (6, 6, 'Carga camara de frio',      'Carga y descarga de camara de frio'),
    (13, 6, 'Control de temperatura',   'Monitoreo y registro de temperatura de la camara')
ON DUPLICATE KEY UPDATE
    nombre      = VALUES(nombre),
    descripcion = VALUES(descripcion);

-- ============================================================
-- Visitas (3 por empresa, fechas escalonadas y estados variados
-- para que el combo 'Visita' de VistaARPT/VistaControlEPP muestre
-- variedad real). Las referencias a usuarios son por subselect
-- de nombre_usuario para no asumir el id_usuario numerico.
-- ============================================================
INSERT INTO visita (id_visita, id_empresa, id_usuario, fecha, tipo, estado, observaciones)
VALUES
    -- MetalCordoba: 3 visitas (cerrada, cerrada, en curso)
    (1, 1, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico1'),
        '2026-08-15', 'SEGUIMIENTO',    'CERRADA',
        'Visita de seguimiento: revision de EPP y avance del plan de accion del ARPT de fresado.'),
    (2, 1, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico1'),
        '2026-09-20', 'PROGRAMADA',     'CERRADA',
        'Relevamiento previo a la auditoria de ART.'),
    (3, 1, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'resp.hys'),
        '2026-10-10', 'EXTRAORDINARIA', 'EN_CURSO',
        'Visita extraordinaria por reclamo de un trabajador.'),

    -- Pinturas del Centro: 3 visitas con un mes de diferencia
    (4, 2, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico2'),
        '2026-07-12', 'SEGUIMIENTO',    'CERRADA',
        'Control de EPP y verificacion de ventilacion en cabina de pintura.'),
    (5, 2, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico2'),
        '2026-08-22', 'PROGRAMADA',     'CERRADA',
        'Medicion de ruido y actualizacion del ARPT de lijado.'),
    (6, 2, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico2'),
        '2026-09-30', 'SEGUIMIENTO',    'PLANIFICADA',
        'Cierre del plan de capacitacion sobre riesgos quimicos.'),

    -- Alimentos del Sur: 3 visitas con foco en camara de frio
    (7, 3, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico1'),
        '2026-06-05', 'PROGRAMADA',     'CERRADA',
        'Primera visita: relevamiento inicial de la planta.'),
    (8, 3, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'resp.hys'),
        '2026-08-18', 'SEGUIMIENTO',    'CERRADA',
        'Verificacion de cadena de frio y EPP de operarios.'),
    (9, 3, (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'tecnico1'),
        '2026-10-02', 'EXTRAORDINARIA', 'CERRADA',
        'Investigacion de incidente menor en linea de envasado.')
ON DUPLICATE KEY UPDATE
    fecha         = VALUES(fecha),
    tipo          = VALUES(tipo),
    estado        = VALUES(estado),
    observaciones = VALUES(observaciones);

-- ============================================================
-- ARPT de muestra (1, en estado VIGENTE para que la defensa pueda
-- ver una planilla ya cargada con todos los rangos del GP).
--   Tarea 1 = Fresado de piezas, visita 3 (MetalCordoba, 2026-10-10).
--   11 evaluaciones: una por cada riesgo del catalogo, con valores
--   P/E/C distribuidos para cubrir TOLERABLE (1-7), PELIGROSO
--   (8-12), URGENTE (13-15) y CRITICO (16+).
--   EPPs asociados a las 4 criticas/urgentes.
--   Medida de control asociada a las criticas (id_medida_control).
-- ============================================================
INSERT INTO arpt (id_arpt, id_tarea, id_visita, fecha, estado,
                  observaciones_generales, conclusion_general,
                  id_created_by, id_updated_by)
VALUES
    (1, 1, 3, '2026-10-10', 'vigente',
     'ARPT vigente del fresado de piezas. La plantilla quedo validada por el Responsable del Servicio tras la visita del 10/10.',
     'Se relevan 4 riesgos en zona critica/urgente y se asocian EPPs a las filas con lleva EPP = Si. Los riesgos TOLERABLE quedan registrados pero sin EPP.',
     (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'resp.hys'),
     (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'resp.hys'))
ON DUPLICATE KEY UPDATE
    estado                 = 'vigente',
    observaciones_generales = VALUES(observaciones_generales),
    conclusion_general      = VALUES(conclusion_general),
    updated_at             = CURRENT_TIMESTAMP,
    id_updated_by          = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'resp.hys');

-- 11 evaluaciones: una por cada riesgo del catalogo (id_riesgo 1..11)
-- Los P/E/C cubren los 4 rangos del GP.
--   id 1  FIS-RUI  Ruido                       P=3 E=4 C=2  -> GP=24  CRITICO
--   id 2  FIS-ILU  Iluminacion                 P=2 E=3 C=2  -> GP=12  PELIGROSO
--   id 3  FIS-ELE  Electrico                   P=1 E=2 C=3  -> GP=6   TOLERABLE
--   id 4  MEC-ATR  Atrapamiento                P=3 E=3 C=4  -> GP=36  CRITICO
--   id 5  MEC-PRY  Proyeccion                  P=2 E=4 C=3  -> GP=24  CRITICO
--   id 6  MEC-COR  Corte                       P=2 E=2 C=2  -> GP=8   PELIGROSO
--   id 7  MEC-CAI  Caida                       P=1 E=3 C=3  -> GP=9   PELIGROSO
--   id 8  ERG-POS  Postura                     P=2 E=4 C=2  -> GP=16  CRITICO
--   id 9  ERG-MOV  Mov. repetitivos            P=2 E=2 C=1  -> GP=4   TOLERABLE
--   id 10 QUI-INH  Inhalacion solventes        P=3 E=3 C=3  -> GP=27  CRITICO
--   id 11 QUI-CON  Contacto corrosivos         P=1 E=2 C=2  -> GP=4   TOLERABLE
INSERT INTO evaluacion_riesgo
    (id_evaluacion, id_arpt, id_riesgo, id_medida_control,
     probabilidad, exposicion, consecuencia, grado_peligrosidad, clasificacion,
     id_created_by, id_updated_by)
VALUES
    (1,  1, 1,  2,  3, 4, 2, 24,  'CRITICO',    1, 1),
    (2,  1, 2,  8,  2, 3, 2, 12,  'PELIGROSO',  1, 1),
    (3,  1, 3,  NULL, 1, 2, 3, 6,  'TOLERABLE', 1, 1),
    (4,  1, 4,  8,  3, 3, 4, 36,  'CRITICO',    1, 1),
    (5,  1, 5,  3,  2, 4, 3, 24,  'CRITICO',    1, 1),
    (6,  1, 6,  3,  2, 2, 2, 8,   'PELIGROSO',  1, 1),
    (7,  1, 7,  NULL, 1, 3, 3, 9,  'PELIGROSO',  1, 1),
    (8,  1, 8,  7,  2, 4, 2, 16,  'CRITICO',    1, 1),
    (9,  1, 9,  7,  2, 2, 1, 4,   'TOLERABLE',  1, 1),
    (10, 1, 10, 5,  3, 3, 3, 27,  'CRITICO',    1, 1),
    (11, 1, 11, 4,  1, 2, 2, 4,   'TOLERABLE',  1, 1)
ON DUPLICATE KEY UPDATE
    id_medida_control  = VALUES(id_medida_control),
    probabilidad       = VALUES(probabilidad),
    exposicion         = VALUES(exposicion),
    consecuencia       = VALUES(consecuencia),
    grado_peligrosidad = VALUES(grado_peligrosidad),
    clasificacion      = VALUES(clasificacion),
    updated_at         = CURRENT_TIMESTAMP,
    id_updated_by      = VALUES(id_updated_by);

-- EPPs asociados a las 4 filas criticas (id_evaluacion 1, 4, 5, 10).
-- Cada evaluacion puede tener varios EPPs, modelados como una fila
-- por par (evaluacion, epp).
INSERT INTO evaluacion_epp (id_evaluacion_epp, id_evaluacion, id_epp)
VALUES
    (1, 1,  1),  -- Ruido (id 1)       -> Proteccion auditiva (id 1)
    (2, 1,  2),  -- Ruido (id 1)       -> Gafas de seguridad (id 2)
    (3, 4,  4),  -- Atrapamiento (id 4) -> Casco (id 4)
    (4, 4,  6),  -- Atrapamiento (id 4) -> Calzado con puntera (id 6)
    (5, 5,  2),  -- Proyeccion (id 5)  -> Gafas de seguridad (id 2)
    (6, 10, 5),  -- Inhalacion (id 10)  -> Mascarilla c/filtro solventes (id 5)
    (7, 10, 3)   -- Inhalacion (id 10)  -> Guantes de nitrilo (id 3)
ON DUPLICATE KEY UPDATE
    id_epp = VALUES(id_epp);

-- ============================================================
-- Bitacora: dejar constancia de la operacion del ARPT semilla
-- (RNF-04: toda operacion sobre ARPT se registra).
-- ============================================================
INSERT INTO bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
VALUES
    ((SELECT id_usuario FROM usuario WHERE nombre_usuario = 'resp.hys'),
     'llevar_vigente', 1,
     'ARPT 1 llevado a vigente por resp.hys (seed)', 'ok');

-- ============================================================
-- Verificaciones
-- ============================================================

-- 11 evaluaciones del ARPT semilla con su GP y clasificacion
SELECT r.codigo, ev.probabilidad, ev.exposicion, ev.consecuencia,
       ev.grado_peligrosidad AS gp, ev.clasificacion,
       m.descripcion AS medida
FROM evaluacion_riesgo ev
JOIN riesgo r            ON r.id_riesgo = ev.id_riesgo
LEFT JOIN medida_control m ON m.id_medida = ev.id_medida_control
WHERE ev.id_arpt = 1
ORDER BY ev.grado_peligrosidad DESC;

-- EPPs asociados al ARPT semilla
SELECT r.codigo, e.nombre AS epp
FROM evaluacion_epp ee
JOIN evaluacion_riesgo ev ON ev.id_evaluacion = ee.id_evaluacion
JOIN riesgo r             ON r.id_riesgo = ev.id_riesgo
JOIN epp e                ON e.id_epp = ee.id_epp
WHERE ev.id_arpt = 1
ORDER BY r.codigo;

-- Visitas por empresa: cardinalidad del seed.
SELECT e.id_empresa, e.razon_social, COUNT(v.id_visita) AS cantidad_visitas
FROM empresa e
LEFT JOIN visita v ON v.id_empresa = e.id_empresa
GROUP BY e.id_empresa, e.razon_social
ORDER BY e.id_empresa;

SELECT u.id_usuario, u.nombre_usuario, u.nombre_completo, u.estado, r.nombre AS rol
FROM usuario u
LEFT JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
LEFT JOIN rol r          ON r.id_rol = ur.id_rol
ORDER BY u.id_usuario;

-- Conteo por empresa recorriendo la jerarquia completa:
-- Empresa -> Establecimiento -> Sector -> Puesto -> Tarea.
-- Asi la verificacion refleja el modelo real (cada sector vive
-- adentro de un establecimiento que vive adentro de UNA empresa).
SELECT e.razon_social                                      AS empresa,
       es.nombre                                           AS establecimiento,
       s.nombre                                            AS sector,
       COUNT(DISTINCT p.id_puesto)                          AS puestos,
       COUNT(t.id_tarea)                                   AS tareas
FROM empresa e
LEFT JOIN establecimiento es ON es.id_empresa = e.id_empresa
LEFT JOIN sector s           ON s.id_establecimiento = es.id_establecimiento
LEFT JOIN puesto p           ON p.id_sector = s.id_sector
LEFT JOIN tarea  t           ON t.id_puesto = p.id_puesto
GROUP BY e.id_empresa, e.razon_social, es.id_establecimiento, es.nombre, s.id_sector, s.nombre
ORDER BY e.id_empresa, es.id_establecimiento, s.id_sector;