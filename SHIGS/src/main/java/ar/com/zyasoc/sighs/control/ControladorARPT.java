package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.Arpt;
import ar.com.zyasoc.sighs.modelo.Arpt.Estado;
import ar.com.zyasoc.sighs.modelo.Bitacora;
import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo;
import ar.com.zyasoc.sighs.modelo.MedidaControl;
import ar.com.zyasoc.sighs.modelo.Puesto;
import ar.com.zyasoc.sighs.modelo.Riesgo;
import ar.com.zyasoc.sighs.modelo.Sector;
import ar.com.zyasoc.sighs.modelo.Tarea;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.modelo.UsuarioAdministrador;
import ar.com.zyasoc.sighs.modelo.UsuarioResponsable;
import ar.com.zyasoc.sighs.modelo.Establecimiento;
import ar.com.zyasoc.sighs.modelo.Visita;
import ar.com.zyasoc.sighs.persistencia.ArptDAO;
import ar.com.zyasoc.sighs.persistencia.BitacoraDAO;
import ar.com.zyasoc.sighs.persistencia.CatalogoDAO;
import ar.com.zyasoc.sighs.persistencia.EstablecimientoDAO;
import ar.com.zyasoc.sighs.persistencia.EvaluacionEppDAO;
import ar.com.zyasoc.sighs.persistencia.EvaluacionRiesgoDAO;
import ar.com.zyasoc.sighs.persistencia.PuestoDAO;
import ar.com.zyasoc.sighs.persistencia.SectorDAO;
import ar.com.zyasoc.sighs.persistencia.TareaDAO;
import ar.com.zyasoc.sighs.persistencia.VisitaDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador del CU-ARPT-01 (Cargar la planilla del ARPT de una tarea).
 *
 * Sigue el diagrama de secuencia del TP2 §10:
 *   1. cargar combos (categorias, riesgos, medidas)
 *   2. buscar el ARPT previo (en edicion o el ultimo) o crearlo en EDICION
 *   3. cargar las evaluaciones existentes
 *   4. por cada riesgo nuevo: validar PEC, calcular GP, clasificar, guardar
 *   5. al confirmar la planilla: pasar el ARPT a VIGENTE o dejarlo en EDICION
 *
 * Toda la logica de calculo del GP y la clasificacion la hace
 * {@link MotorEvaluacionRiesgo}, que es el unico que sabe la formula
 * (RD-04 y RD-05).
 */
public class ControladorARPT {

    private final ArptDAO arptDAO;
    private final EvaluacionRiesgoDAO evaluacionDAO;
    private final EvaluacionEppDAO evaluacionEppDAO;
    private final CatalogoDAO catalogoDAO;
    private final BitacoraDAO bitacoraDAO;
    private final SectorDAO sectorDAO;
    private final PuestoDAO puestoDAO;
    private final TareaDAO tareaDAO;
    private final VisitaDAO visitaDAO;
    private final EstablecimientoDAO establecimientoDAO;
    private final MotorEvaluacionRiesgo motor;

    public ControladorARPT() {
        this(new ArptDAO(), new EvaluacionRiesgoDAO(), new EvaluacionEppDAO(), new CatalogoDAO(),
                new BitacoraDAO(), new SectorDAO(), new PuestoDAO(), new TareaDAO(),
                new VisitaDAO(), new EstablecimientoDAO(), new MotorEvaluacionRiesgo());
    }

    public ControladorARPT(ArptDAO arptDAO, EvaluacionRiesgoDAO evaluacionDAO,
                            EvaluacionEppDAO evaluacionEppDAO, CatalogoDAO catalogoDAO,
                            BitacoraDAO bitacoraDAO, MotorEvaluacionRiesgo motor) {
        this(arptDAO, evaluacionDAO, evaluacionEppDAO, catalogoDAO, bitacoraDAO,
                new SectorDAO(), new PuestoDAO(), new TareaDAO(), new VisitaDAO(),
                new EstablecimientoDAO(), motor);
    }

    public ControladorARPT(ArptDAO arptDAO, EvaluacionRiesgoDAO evaluacionDAO,
                            EvaluacionEppDAO evaluacionEppDAO, CatalogoDAO catalogoDAO,
                            BitacoraDAO bitacoraDAO, SectorDAO sectorDAO, PuestoDAO puestoDAO,
                            TareaDAO tareaDAO, VisitaDAO visitaDAO,
                            EstablecimientoDAO establecimientoDAO, MotorEvaluacionRiesgo motor) {
        this.arptDAO = arptDAO;
        this.evaluacionDAO = evaluacionDAO;
        this.evaluacionEppDAO = evaluacionEppDAO;
        this.catalogoDAO = catalogoDAO;
        this.bitacoraDAO = bitacoraDAO;
        this.sectorDAO = sectorDAO;
        this.puestoDAO = puestoDAO;
        this.tareaDAO = tareaDAO;
        this.visitaDAO = visitaDAO;
        this.establecimientoDAO = establecimientoDAO;
        this.motor = motor;
    }

    /** Establecimientos de la empresa, para el primer combo del panel. */
    public List<Establecimiento> listarEstablecimientos(int idEmpresa) throws SQLException {
        return establecimientoDAO.listarPorEmpresa(idEmpresa);
    }

    /** Sectores del establecimiento, filtrados por id_establecimiento. */
    public List<Sector> listarSectoresPorEstablecimiento(int idEstablecimiento) throws SQLException {
        return sectorDAO.listarPorEstablecimiento(idEstablecimiento);
    }

    /** Puestos de un sector, para el segundo combo. */
    public List<Puesto> listarPuestos(int idSector) throws SQLException {
        return puestoDAO.listarPorSector(idSector);
    }

    /** Tareas de un puesto, para el tercer combo. */
    public List<Tarea> listarTareas(int idPuesto) throws SQLException {
        return tareaDAO.listarPorPuesto(idPuesto);
    }

    /** Ultimas N visitas de la empresa, para el combo "Visita" de la pantalla. */
    public List<Visita> listarVisitasRecientes(int idEmpresa, int limite) throws SQLException {
        return visitaDAO.listarUltimasPorEmpresa(idEmpresa, limite);
    }

    public List<ar.com.zyasoc.sighs.modelo.TipoRiesgo> cargarTiposRiesgo() throws SQLException {
        return catalogoDAO.cargarTiposRiesgo();
    }

    public List<Riesgo> cargarRiesgos() throws SQLException {
        return catalogoDAO.cargarRiesgos();
    }

    public List<MedidaControl> cargarMedidasControl() throws SQLException {
        return catalogoDAO.cargarMedidasControl();
    }

    public List<ar.com.zyasoc.sighs.modelo.Epp> cargarEpp() throws SQLException {
        return catalogoDAO.cargarEpp();
    }

    public java.util.List<Integer> cargarEppPorEvaluacion(int idEvaluacion) throws SQLException {
        return evaluacionEppDAO.cargarPorEvaluacion(idEvaluacion);
    }

    public void guardarEppPorEvaluacion(int idEvaluacion, java.util.List<Integer> idsEpp) throws SQLException {
        evaluacionEppDAO.guardar(idEvaluacion, idsEpp);
    }

    /**
     * Sincroniza las asociaciones evaluacion_epp de una evaluacion.
     * Devuelve los idEpp que no se pudieron quitar porque ya tienen
     * controles de EPP registrados (no se borra trazabilidad).
     */
    public java.util.List<Integer> reemplazarEppPorEvaluacion(int idEvaluacion, java.util.List<Integer> idsEpp) throws SQLException {
        if (idEvaluacion <= 0) {
            throw new SQLException("No se puede asociar EPPs a una evaluacion sin id.");
        }
        return evaluacionEppDAO.reemplazar(idEvaluacion, idsEpp);
    }

    /**
     * Devuelve el ARPT que se debe editar. Si no hay ninguno previo,
     * crea uno nuevo en estado EDICION.
     */
    public Arpt obtenerOCrearParaTarea(int idTarea, int idVisita, int idTecnico) throws SQLException {
        Arpt previo = arptDAO.buscarEnEdicionPorTarea(idTarea);
        if (previo != null) return previo;
        // si ya hay vigente, devolvemos null para no romper la regla
        // "un solo ARPT vigente por tarea" (uq_arpt_vigente_tarea)
        if (arptDAO.buscarVigentePorTarea(idTarea) != null) return null;
        int idArpt = arptDAO.crearEnEdicion(idTarea, idVisita, LocalDate.now(), idTecnico, "", "");
        return arptDAO.buscarVigentePorTarea(idTarea) == null
                ? new Arpt() {{ setIdArpt(idArpt); setEstado(Estado.EDICION); setIdTarea(idTarea); setIdVisita(idVisita); setFecha(LocalDate.now()); }}
                : null;
    }

    public List<EvaluacionRiesgo> cargarEvaluaciones(int idArpt) throws SQLException {
        return evaluacionDAO.cargarPorArpt(idArpt);
    }

    /**
     * RF-ARPT-04: valida que P, E, C esten en el rango 1..5 y calcula GP.
     * Devuelve la misma instancia con GP y clasificacion asignados, o lanza
     * IllegalArgumentException si algun valor esta fuera de rango.
     */
    public EvaluacionRiesgo evaluar(EvaluacionRiesgo ev) {
        return motor.evaluar(ev);
    }

    /**
     * Persiste la evaluacion sin poder duplicarla: si ya existe una fila para
     * ese (ARPT, riesgo) la actualiza; si no, la inserta. Es lo que usan
     * "Calcular GP" y "Guardar borrador".
     */
    public int guardarOActualizarEvaluacion(EvaluacionRiesgo ev) throws SQLException {
        // OJO: no usar un ternario int/Integer aca. JLS §15.25 unifica el
        // tipo a int y desempaca la rama Integer; si la consulta no
        // encuentra la fila, eso devuelve null y revienta con NPE.
        int idExistente = ev.getIdEvaluacion();
        if (idExistente <= 0) {
            Integer found = evaluacionDAO.buscarIdPorArptYRiesgo(
                    ev.getIdArpt(), ev.getIdRiesgo());
            if (found != null) idExistente = found;
        }
        if (idExistente > 0) {
            ev.setIdEvaluacion(idExistente);
            evaluacionDAO.actualizar(ev);
            return idExistente;
        }
        evaluacionDAO.guardar(ev);
        return ev.getIdEvaluacion();
    }

    public void confirmarAVigente(Usuario usuario, int idArpt, String observaciones, String conclusion)
            throws SQLException {
        // RD-07: 'un solo VIGENTE por tarea'. Si ya existe un ARPT vigente
        // para esta tarea (caso normal de reversion: el ARPT vigente
        // anterior y el que estamos confirmando conviven en EDICION),
        // primero lo marcamos como HISTORICO antes de promover el nuevo a
        // VIGENTE. Sin este paso el UNIQUE KEY uq_arpt_vigente_tarea
        // rechaza el UPDATE con 'Duplicate entry N for key
        // uq_arpt_vigente_tarea'.
        Arpt actual = arptDAO.buscarPorId(idArpt);
        if (actual == null) {
            throw new SQLException("ARPT " + idArpt + " no existe");
        }
        Arpt vigenteActual = arptDAO.buscarVigentePorTarea(actual.getIdTarea());
        if (vigenteActual != null && vigenteActual.getIdArpt() != idArpt) {
            arptDAO.marcarHistorico(vigenteActual.getIdArpt());
            registrarBitacora(usuario, vigenteActual.getIdArpt(), "llevar_historico",
                    "Arpt " + vigenteActual.getIdArpt() + " reemplazado por Arpt " + idArpt);
        }
        arptDAO.actualizar(idArpt, Estado.VIGENTE, observaciones, conclusion);
        registrarBitacora(usuario, idArpt, "llevar_vigente", "Arpt " + idArpt + " llevado a VIGENTE");
    }

    public void dejarEnEdicion(Usuario usuario, int idArpt, String observaciones, String conclusion)
            throws SQLException {
        arptDAO.actualizar(idArpt, Estado.EDICION, observaciones, conclusion);
        registrarBitacora(usuario, idArpt, "dejar_edicion", "Arpt " + idArpt + " guardado en EDICION");
    }

    /**
     * RF de autorizacion por roles (RNF-03): solo responsable y administrador
     * pueden llevar un ARPT a VIGENTE.
     */
    public boolean puedeValidar(Usuario usuario) {
        return usuario instanceof UsuarioResponsable || usuario instanceof UsuarioAdministrador;
    }

    /**
     * Crea una version nueva en EDICION de un ARPT existente (normalmente
     * VIGENTE). El original queda intacto: recien pasa a HISTORICO cuando la
     * version se publica con {@link #publicarVersion}.
     */
    public int crearVersionEnEdicion(Usuario usuario, Arpt origen,
                                     String observaciones, String conclusion) throws SQLException {
        int idNuevo = arptDAO.crearEnEdicion(origen.getIdTarea(), origen.getIdVisita(),
                LocalDate.now(), usuario.getIdUsuario(), observaciones, conclusion);
        registrarBitacora(usuario, idNuevo, "crear_version",
                "Arpt " + idNuevo + " creado como version del Arpt " + origen.getIdArpt());
        return idNuevo;
    }

    /**
     * Publica la version nueva y deja la anterior en HISTORICO. El orden
     * importa: primero se libera el VIGENTE anterior y despues se marca el
     * nuevo, porque la base admite un solo VIGENTE por tarea.
     */
    public void publicarVersion(Usuario usuario, int idAnterior, int idNuevo,
                                String observaciones, String conclusion) throws SQLException {
        arptDAO.marcarHistorico(idAnterior);
        arptDAO.actualizar(idNuevo, Estado.VIGENTE, observaciones, conclusion);
        registrarBitacora(usuario, idNuevo, "nueva_version",
                "Arpt " + idNuevo + " pasa a VIGENTE y reemplaza al Arpt " + idAnterior);
    }

    /**
     * Confirma la planilla segun el rol del usuario (RNF-03, autorizacion
     * por roles):
     *  - UsuarioResponsable / UsuarioAdministrador -> lleva a VIGENTE.
     *  - UsuarioTecnico (u otro sin permiso) -> deja en EDICION (borrador).
     * Alineado con el paso 38b del sequence diagram del TP2 (CU-ARPT-01),
     * donde la rama "Confirmar" depende del actor.
     */
    public void confirmarSegunRol(Usuario usuario, int idArpt, String observaciones, String conclusion)
            throws SQLException {
        if (puedeValidar(usuario)) {
            confirmarAVigente(usuario, idArpt, observaciones, conclusion);
        } else {
            dejarEnEdicion(usuario, idArpt, observaciones, conclusion);
        }
    }

    private void registrarBitacora(Usuario usuario, int idArpt, String operacion, String detalle) {
        try {
            bitacoraDAO.registrar(new Bitacora(
                    LocalDateTime.now(),
                    usuario.getIdUsuario(),
                    operacion,
                    null,
                    detalle,
                    "ok"));
        } catch (SQLException ex) {
            System.err.println("[WARN] no se pudo escribir en bitacora: " + ex.getMessage());
        }
    }
}