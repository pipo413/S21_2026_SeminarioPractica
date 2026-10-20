use sighs_db;

-- ============================================================
-- consultas y operaciones de prueba
-- cada bloque sigue el flujo de un caso de uso critico del tp2
-- basadas en los diagramas de secuencia de la seccion 10
-- ============================================================

-- ============================================================
-- cu-usr-01: iniciar sesion
-- flujo del sequence diagram: usuario -> vista -> ctrl ->
-- servicioautenticacion -> usuario -> bitacora
-- ============================================================

-- buscar usuario por nombre de usuario (paso 7a del sequence)
select id_usuario, nombre_usuario, password_hash, salt, estado, intentos_fallidos
from usuario
where nombre_usuario = 'tecnico1';

-- comparar hash (lo hace la app en memoria con messagedigest, no en sql)

-- registrar intento fallido (paso 12a)
update usuario
set intentos_fallidos = intentos_fallidos + 1,
    updated_at = current_timestamp
where id_usuario = 1;

-- al tercer intento fallido: bloquear (paso 14a1)
update usuario
set estado = 'inactivo',
    updated_at = current_timestamp
where id_usuario = 1
  and intentos_fallidos >= 3;

-- resetear intentos tras login exitoso (paso 13b2)
update usuario
set intentos_fallidos = 0,
    updated_at = current_timestamp
where id_usuario = 1;

-- obtener roles del usuario (paso 15b2)
select r.id_rol, r.nombre
from usuario_rol ur
join rol r on r.id_rol = ur.id_rol
where ur.id_usuario = 1;

-- registrar en bitacora login ok (paso 17b2)
insert into bitacora (id_usuario, operacion, detalle, resultado)
values (1, 'login_ok', 'Inicio de sesion', 'ok');

-- registrar en bitacora login fallido (paso 14a2)
insert into bitacora (id_usuario, operacion, detalle, resultado)
values (1, 'login_fallido', 'credenciales invalidas', 'fallo');

-- registrar en bitacora usuario bloqueado (paso 16a1)
insert into bitacora (id_usuario, operacion, detalle, resultado)
values (1, 'usuario_bloqueado', 'cuenta bloqueada tras 3 intentos', 'bloqueado');

-- ============================================================
-- cu-arpt-01: cargar la planilla del arpt de una tarea
-- flujo del sequence diagram: ctrl -> cat/riesgo/medida/epp
-- -> arpt -> evaluacion_riesgo -> motor -> propuestas -> bitacora
-- ============================================================

-- cargar combos para la planilla (pasos 3 a 9)
select id_categoria, nombre from categoria_riesgo order by nombre;
select id_riesgo, id_categoria, codigo, nombre from riesgo where vigente = true order by codigo;
select id_medida, descripcion, tipo from medida_control order by descripcion;
select id_epp, nombre, descripcion from epp order by nombre;

-- cargar epps requeridos por la tarea a partir del arpt vigente (paso equivalente al 9 del sequence)
select e.id_epp, e.nombre, e.descripcion
from arpt a
join evaluacion_riesgo ev on ev.id_arpt = a.id_arpt
join evaluacion_epp ee on ee.id_evaluacion = ev.id_evaluacion
join epp e on e.id_epp = ee.id_epp
where a.estado = 'vigente'
  and a.id_tarea = 1;

-- buscar ultimo arpt de la tarea (paso 11)
select id_arpt, id_tarea, id_visita, fecha, estado
from arpt
where id_tarea = 1
order by fecha desc
limit 1;

-- cargar evaluaciones del arpt (paso 13a)
select ev.id_evaluacion, ev.id_arpt, ev.id_riesgo, ev.id_medida_control,
       ev.probabilidad, ev.exposicion, ev.consecuencia,
       ev.grado_peligrosidad, ev.clasificacion,
       r.nombre as riesgo_nombre
from evaluacion_riesgo ev
join riesgo r on r.id_riesgo = ev.id_riesgo
where ev.id_arpt = 1;

-- crear o actualizar una evaluacion de riesgo (paso 18b)
-- para gp=36 (3,4,3) -> critico
insert into evaluacion_riesgo (id_arpt, id_riesgo, id_medida_control, probabilidad, exposicion, consecuencia, grado_peligrosidad, clasificacion, id_created_by, id_updated_by)
values (1, 1, 1, 3, 4, 3, 36, 'critico', 1, 1);

-- asociar medida de control a la evaluacion (paso 27b)
update evaluacion_riesgo
set id_medida_control = 1,
    updated_at = current_timestamp,
    id_updated_by = 1
where id_evaluacion = 1;

-- asociar epps a la evaluacion (paso 28b)
insert into evaluacion_epp (id_evaluacion, id_epp) values (1, 1);
insert into evaluacion_epp (id_evaluacion, id_epp) values (1, 2);
-- ignora duplicados si ya existe la pareja (uq_evr_epp)

-- guardar el arpt y dejarlo vigente (paso 42b3 del responsable validando)
update arpt
set estado = 'vigente',
    updated_at = current_timestamp,
    id_updated_by = 3
where id_arpt = 1;

-- consultar el arpt vigente de una tarea (para cu-arpt-02 y para el informe de cu-rep-03)
select id_arpt, id_tarea, fecha, estado
from arpt
where id_tarea = 1
  and estado = 'vigente';

-- dejar arpt en edicion (paso 42a del tecnico)
update arpt
set estado = 'edicion',
    updated_at = current_timestamp,
    id_updated_by = 1
where id_arpt = 1;

-- registrar la operacion en bitacora (paso 44a / 44b3)
insert into bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
values (3, 'dejar_edicion',  1, 'Arpt 1 guardado en edicion por tecnico1', 'ok');

insert into bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
values (3, 'llevar_vigente', 1, 'Arpt 1 llevado a vigente por responsable', 'ok');

-- ============================================================
-- cu-epp-01: generar control de epp en una visita
-- flujo del sequence: ctrl -> sector/puesto/tarea -> arpt
-- -> evaluacion_epp -> epp -> control_epp -> bitacora
-- ============================================================

-- listar sectores por empresa (paso 3)
select id_sector, nombre from sector where id_empresa = 1 order by nombre;

-- listar puestos por sector (paso 8)
select id_puesto, nombre from puesto where id_sector = 1 order by nombre;

-- listar tareas por puesto (paso 13)
select id_tarea, nombre from tarea where id_puesto = 1 order by nombre;

-- buscar arpt vigente de la tarea (paso 18)
select id_arpt, fecha, estado
from arpt
where id_tarea = 1
  and estado = 'vigente';

-- buscar epps requeridos del arpt vigente (paso 20b)
select ee.id_evaluacion_epp, ev.id_evaluacion, ev.id_riesgo,
       e.id_epp, e.nombre, e.descripcion
from arpt a
join evaluacion_riesgo ev on ev.id_arpt = a.id_arpt
join evaluacion_epp ee on ee.id_evaluacion = ev.id_evaluacion
join epp e on e.id_epp = ee.id_epp
where a.estado = 'vigente'
  and a.id_tarea = 1
  and ev.id_riesgo in (select id_riesgo from riesgo where vigente = true);

-- crear cabecera del control (paso 35b)
insert into control_epp (id_visita, id_puesto, id_tarea, id_arpt, fecha, estado)
values (1, 1, 1, 1, current_date, 'registrado');
-- asumimos que devuelve id_control_epp = 4 (los inserts de prueba cargaron id=1)

-- crear detalle por cada epp controlado (paso 37b)
insert into control_epp_detalle (id_control_epp, id_evaluacion_epp, valor_uso, valor_estado, observaciones, recomendaciones)
values (1, 1, 4, 4, 'Operario usa proteccion auditiva toda la jornada', 'Mantener');

insert into control_epp_detalle (id_control_epp, id_evaluacion_epp, valor_uso, valor_estado, observaciones, recomendaciones)
values (1, 2, 5, 5, 'Guantes en buen estado', 'Mantener');

insert into control_epp_detalle (id_control_epp, id_evaluacion_epp, valor_uso, valor_estado, observaciones, recomendaciones)
values (1, 3, 3, 4, 'Gafas con pequenas ralladuras', 'Reemplazar en proxima reposicion');

-- consultas utiles de lectura del control generado
-- resumen de puntajes por epp
select e.nombre as epp,
       d.valor_uso,
       d.valor_estado,
       d.observaciones,
       d.recomendaciones
from control_epp_detalle d
join control_epp c on c.id_control_epp = d.id_control_epp
join evaluacion_epp ee on ee.id_evaluacion_epp = d.id_evaluacion_epp
join epp e on e.id_epp = ee.id_epp
where c.id_control_epp = 1;

-- promedio de valor_uso y valor_estado por control (indicador de cumplimiento)
select c.id_control_epp,
       avg(d.valor_uso)     as promedio_uso,
       avg(d.valor_estado)  as promedio_estado,
       count(*)             as cantidad_epp_controlados
from control_epp c
join control_epp_detalle d on d.id_control_epp = c.id_control_epp
where c.id_control_epp = 1
group by c.id_control_epp;

-- registrar la operacion en bitacora (paso 43b)
insert into bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
values (1, 'registrar_control_epp', 1, 'Control de epp 1 sobre tarea 1', 'ok');

-- ============================================================
-- cu-acc-01: gestionar planes de accion
-- flujo del sequence: ctrl -> plan_accion (y bitacora)
-- ============================================================

-- listar planes abiertos de una empresa (paso 3)
-- incluye los que estan EnTratamiento y cuya fecha_plazo >= hoy
-- (los vencidos aparecen pero la app los marca visualmente)
select pa.id_plan_accion, pa.descripcion, pa.tipo_origen, pa.id_origen,
       pa.fecha_plazo, pa.estado,
       u.nombre_completo as responsable,
       case
           when pa.estado = 'entratamiento' and pa.fecha_plazo < current_date
               then 'vencido'
           when pa.estado = 'entratamiento' and pa.fecha_plazo <= date_add(current_date, interval 15 day)
               then 'proximo a vencer'
           else 'vigente'
       end as situacion
from plan_accion pa
join usuario u on u.id_usuario = pa.id_responsable
where pa.id_empresa = 1
  and pa.estado = 'entratamiento'
  and pa.baja_logica = false
order by pa.fecha_plazo;

-- modificar un plan (paso 10b): cambiar descripcion, responsable, plazo, estado
update plan_accion
set descripcion = 'Instalar proteccion fija y verificar sujecion',
    id_responsable = 1,
    fecha_plazo = '2026-11-30',
    estado = 'entratamiento',
    updated_at = current_timestamp,
    id_updated_by = 1
where id_plan_accion = 1;

-- al pasar a cumplido o no cumplido se registra fecha_cierre (rd-16)
update plan_accion
set estado = 'cumplido',
    fecha_cierre = current_date,
    updated_at = current_timestamp,
    id_updated_by = 1
where id_plan_accion = 1;

-- crear un plan nuevo (paso 12b): la fila nueva hereda tipoOrigen e idOrigen del contexto
insert into plan_accion (id_empresa, tipo_origen, id_origen, descripcion, id_responsable, fecha_plazo, estado, id_created_by, id_updated_by)
values (1, 'control_epp', 1, 'Reemplazar lote completo de gafas', 1, '2026-12-15', 'entratamiento', 1, 1);

-- baja logica (paso 14b): el plan sigue visible pero no aparece en la grilla principal
update plan_accion
set baja_logica = true,
    updated_at = current_timestamp,
    id_updated_by = 1
where id_plan_accion = 3;

-- consulta para el informe: planes del periodo abiertos, cerrados, vencidos y proximos a vencer (rd-16)
select id_plan_accion, tipo_origen, descripcion, fecha_plazo, estado, fecha_cierre,
       case
           when estado = 'entratamiento' and fecha_plazo < current_date then 'vencido'
           when estado = 'entratamiento' and fecha_plazo <= date_add(current_date, interval 15 day) then 'proximo a vencer'
           else estado
       end as situacion
from plan_accion
where id_empresa = 1
  and baja_logica = false
order by fecha_plazo;

-- registrar la operacion (rd-17: una sola operacion con el detalle de los campos modificados)
insert into bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
values (1, 'gestionar_plan_accion', 1, 'plan 1 modificado (estado->cumplido), plan 4 creado, plan 3 baja logica', 'ok');

-- ============================================================
-- cu-rep-03: enviar informe mensual por correo
-- flujo del sequence: ctrl -> empresa/arpt/control_epp/plan_accion
-- -> serviciopdf -> serviciocorreo -> bitacora
-- ============================================================

-- listar empresas para que el responsable elija (paso 3)
select id_empresa, razon_social, cuit, email_referente
from empresa
order by razon_social;

-- obtener datos del referente (paso 8)
select nombre_referente, cargo_referente, email_referente
from empresa
where id_empresa = 1;

-- listar arpts vigentes de la empresa en el periodo (paso 9)
-- en esta version "periodo" se modela por mes-anio de la fecha del arpt
select a.id_arpt, a.id_tarea, t.nombre as tarea, p.nombre as puesto,
       s.nombre as sector, a.fecha
from arpt a
join tarea t on t.id_tarea = a.id_tarea
join puesto p on p.id_puesto = t.id_puesto
join sector s on s.id_sector = p.id_sector
where a.estado = 'vigente'
  and s.id_empresa = 1
  and date_format(a.fecha, '%Y-%m') = '2026-08'
order by s.nombre, p.nombre, t.nombre;

-- listar controles de epp del periodo (paso 11)
select c.id_control_epp, c.id_visita, c.fecha, p.nombre as puesto, t.nombre as tarea,
       count(d.id_detalle) as cantidad_epp_controlados,
       avg(d.valor_uso)    as promedio_uso,
       avg(d.valor_estado) as promedio_estado
from control_epp c
join visita v on v.id_visita = c.id_visita
join puesto p on p.id_puesto = c.id_puesto
join tarea t on t.id_tarea = c.id_tarea
left join control_epp_detalle d on d.id_control_epp = c.id_control_epp
where v.id_empresa = 1
  and date_format(c.fecha, '%Y-%m') = '2026-10'
group by c.id_control_epp, c.id_visita, c.fecha, p.nombre, t.nombre
order by c.fecha;

-- listar planes de accion del periodo (paso 13): abiertos, cerrados, vencidos y proximos a vencer (rd-16)
select pa.id_plan_accion, pa.tipo_origen, pa.descripcion, pa.fecha_plazo, pa.estado, pa.fecha_cierre,
       u.nombre_completo as responsable,
       case
           when pa.estado = 'entratamiento' and pa.fecha_plazo < current_date then 'vencido'
           when pa.estado = 'entratamiento' and pa.fecha_plazo <= date_add(current_date, interval 15 day) then 'proximo a vencer'
           else pa.estado
       end as situacion
from plan_accion pa
join usuario u on u.id_usuario = pa.id_responsable
where pa.id_empresa = 1
  and pa.baja_logica = false
order by pa.estado, pa.fecha_plazo;

-- caso borde: empresa sin actividad (rd-a3 del informe: "se emite igual indicando que no hubo actividad")
-- la consulta anterior devuelve 0 filas; el informe se genera igual con la leyenda correspondiente.

-- registrar constancia del envio en bitacora (paso 20b5)
insert into bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
values (3, 'enviar_informe_mensual', 1, 'destinatario=rrhh@metalcordoba.com.ar, hash_pdf=abc123, periodo=2026-10', 'ok');

-- registrar cancelacion (paso 21a)
insert into bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
values (3, 'cancelar_envio_informe', 1, 'periodo=2026-10', 'cancelado');

-- ============================================================
-- consultas de auditoria y trazabilidad
-- rnf-04: registrar en bitacora toda operacion sobre entidades criticas
-- ============================================================

-- historial de operaciones de un usuario
select fecha_hora, operacion, detalle, resultado
from bitacora
where id_usuario = 1
order by fecha_hora desc;

-- historial de cambios sobre un arpt especifico
select fecha_hora, operacion, u.nombre_completo as usuario, detalle
from bitacora b
join usuario u on u.id_usuario = b.id_usuario
where b.operacion in ('dejar_edicion', 'llevar_vigente')
  and b.detalle like '%arpt 1%'
order by fecha_hora;

-- ultimas acciones por empresa
select fecha_hora, operacion, u.nombre_completo as usuario, detalle
from bitacora b
join usuario u on u.id_usuario = b.id_usuario
where b.id_empresa = 1
order by fecha_hora desc
limit 20;

-- ============================================================
-- consultas de mantenimiento para tp3
-- estas son utiles para las pantallas de abmc y consulta
-- ============================================================

-- listado paginado para la pantalla de planes de una empresa
select pa.id_plan_accion, pa.descripcion, pa.tipo_origen, pa.fecha_plazo, pa.estado,
       u.nombre_completo as responsable
from plan_accion pa
join usuario u on u.id_usuario = pa.id_responsable
where pa.id_empresa = 1
  and pa.baja_logica = false
order by pa.fecha_plazo
limit 20 offset 0;

-- consulta del catalogo de riesgos vigentes (para combos)
select id_riesgo, codigo, nombre, id_categoria
from riesgo
where vigente = true
order by codigo;

-- consulta del catalogo de medidas de control
select id_medida, descripcion, tipo
from medida_control
order by tipo, descripcion;

-- consulta del catalogo de epp
select id_epp, nombre, descripcion
from epp
order by nombre;

-- ============================================================
-- ANEXO TP3: SELECT de solo lectura alineados con los DAO Java actuales
-- ============================================================
-- Los bloques anteriores son consultas historicas/ilustrativas y pueden
-- mencionar columnas o flujos que no coinciden con el prototipo actual.
-- Aqui se sustituyen los ? de PreparedStatement por valores de ejemplo
-- de 02-semillas.sql. Los resultados pueden variar segun la base cargada.
-- No se ejecutaron estas consultas contra una base de datos.

-- UsuarioDAO.buscarPorNombre: se omiten hash y salt de esta muestra.
-- LEFT JOIN conserva al usuario aunque no tenga roles asociados.
SELECT u.id_usuario, u.nombre_usuario, u.nombre_completo, u.estado,
       u.intentos_fallidos, r.id_rol, r.nombre AS rol_nombre
FROM usuario u
LEFT JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
LEFT JOIN rol r ON r.id_rol = ur.id_rol
WHERE u.nombre_usuario = 'tecnico1'
ORDER BY r.id_rol;

-- CatalogoDAO.cargarRiesgos: incluye riesgos no vigentes; la familia
-- se obtiene mediante el tipo padre cuando existe.
SELECT r.id_riesgo, r.id_tipo_riesgo, r.codigo, r.nombre,
       r.descripcion, r.vigente, t.nombre AS tipo_nombre,
       COALESCE(f.nombre, t.nombre) AS familia_nombre
FROM riesgo r
JOIN tipo_riesgo t ON t.id_tipo_riesgo = r.id_tipo_riesgo
LEFT JOIN tipo_riesgo f ON f.id_tipo_riesgo = t.id_padre
ORDER BY COALESCE(f.nombre, t.nombre), t.nombre, r.codigo;

-- SectorDAO.listarPorEstablecimiento: sector pertenece al establecimiento.
SELECT id_sector, id_establecimiento, nombre
FROM sector WHERE id_establecimiento = 1 ORDER BY nombre;

-- ArptDAO.buscarVigentePorTarea: solo la version vigente de la tarea.
SELECT id_arpt, id_tarea, id_visita, fecha, estado,
       observaciones_generales, conclusion_general
FROM arpt WHERE id_tarea = 1 AND estado = 'vigente'
ORDER BY fecha DESC LIMIT 1;

-- EvaluacionRiesgoDAO.cargarPorArpt: evaluaciones de la planilla elegida.
SELECT id_evaluacion, id_arpt, id_riesgo, id_medida_control,
       probabilidad, exposicion, consecuencia, grado_peligrosidad, clasificacion
FROM evaluacion_riesgo WHERE id_arpt = 1 ORDER BY id_evaluacion;

-- ControlEppDAO.buscarEppRequeridos: cinco tablas; solo ARPT vigente
-- y riesgos vigentes, sin agregar ni deduplicar los EPP asociados.
SELECT ee.id_evaluacion_epp, ev.id_evaluacion, ev.id_riesgo,
       e.id_epp, e.nombre, e.descripcion
FROM arpt a
JOIN evaluacion_riesgo ev ON ev.id_arpt = a.id_arpt
JOIN evaluacion_epp ee ON ee.id_evaluacion = ev.id_evaluacion
JOIN epp e ON e.id_epp = ee.id_epp
JOIN riesgo r ON r.id_riesgo = ev.id_riesgo
WHERE a.estado = 'vigente' AND a.id_arpt = 1 AND r.vigente = TRUE;