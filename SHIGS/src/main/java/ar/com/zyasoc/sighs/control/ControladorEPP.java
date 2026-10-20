package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.Arpt;
import ar.com.zyasoc.sighs.modelo.Bitacora;
import ar.com.zyasoc.sighs.modelo.ControlEpp;
import ar.com.zyasoc.sighs.modelo.ControlEppDetalle;
import ar.com.zyasoc.sighs.modelo.Epp;
import ar.com.zyasoc.sighs.modelo.Establecimiento;
import ar.com.zyasoc.sighs.modelo.EvaluacionEpp;
import ar.com.zyasoc.sighs.modelo.Puesto;
import ar.com.zyasoc.sighs.modelo.Sector;
import ar.com.zyasoc.sighs.modelo.Tarea;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.modelo.Visita;
import ar.com.zyasoc.sighs.persistencia.ArptDAO;
import ar.com.zyasoc.sighs.persistencia.BitacoraDAO;
import ar.com.zyasoc.sighs.persistencia.ControlEppDAO;
import ar.com.zyasoc.sighs.persistencia.EstablecimientoDAO;
import ar.com.zyasoc.sighs.persistencia.PuestoDAO;
import ar.com.zyasoc.sighs.persistencia.SectorDAO;
import ar.com.zyasoc.sighs.persistencia.TareaDAO;
import ar.com.zyasoc.sighs.persistencia.VisitaDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador del CU-EPP-01 (Generar control de EPP en una visita).
 *
 * Sigue el diagrama de secuencia del TP2 §10. Orquesta la consulta de la
 * jerarquia organizacional, la busqueda del ARPT vigente, la carga de los
 * EPPs requeridos, la validacion de puntajes y el guardado transaccional
 * (RNF-01 ACID).
 */
public class ControladorEPP {

    private final SectorDAO sectorDAO;
    private final PuestoDAO puestoDAO;
    private final TareaDAO tareaDAO;
    private final ArptDAO arptDAO;
    private final ControlEppDAO controlEppDAO;
    private final BitacoraDAO bitacoraDAO;
    private final VisitaDAO visitaDAO;
    private final EstablecimientoDAO establecimientoDAO;

    public ControladorEPP() {
        this(new SectorDAO(), new PuestoDAO(), new TareaDAO(),
                new ArptDAO(), new ControlEppDAO(), new BitacoraDAO(), new VisitaDAO(),
                new EstablecimientoDAO());
    }

    public ControladorEPP(SectorDAO sectorDAO, PuestoDAO puestoDAO, TareaDAO tareaDAO,
                          ArptDAO arptDAO, ControlEppDAO controlEppDAO, BitacoraDAO bitacoraDAO,
                          VisitaDAO visitaDAO, EstablecimientoDAO establecimientoDAO) {
        this.sectorDAO = sectorDAO;
        this.puestoDAO = puestoDAO;
        this.tareaDAO = tareaDAO;
        this.arptDAO = arptDAO;
        this.controlEppDAO = controlEppDAO;
        this.bitacoraDAO = bitacoraDAO;
        this.visitaDAO = visitaDAO;
        this.establecimientoDAO = establecimientoDAO;
    }

    /** Establecimientos de la empresa, para el primer combo del panel. */
    public List<Establecimiento> listarEstablecimientos(int idEmpresa) throws SQLException {
        return establecimientoDAO.listarPorEmpresa(idEmpresa);
    }

    /** Sectores del establecimiento, filtrados por id_establecimiento. */
    public List<Sector> listarSectoresPorEstablecimiento(int idEstablecimiento) throws SQLException {
        return sectorDAO.listarPorEstablecimiento(idEstablecimiento);
    }

    public List<Puesto> listarPuestos(int idSector) throws SQLException {
        return puestoDAO.listarPorSector(idSector);
    }

    public List<Tarea> listarTareas(int idPuesto) throws SQLException {
        return tareaDAO.listarPorPuesto(idPuesto);
    }

    /** Ultimas N visitas de la empresa, para el combo "Visita" de la pantalla. */
    public List<Visita> listarVisitasRecientes(int idEmpresa, int limite) throws SQLException {
        return visitaDAO.listarUltimasPorEmpresa(idEmpresa, limite);
    }

    public Arpt buscarArptVigente(int idTarea) throws SQLException {
        return arptDAO.buscarVigentePorTarea(idTarea);
    }

    /**
     * Busca el control de EPP mas reciente para una tarea en una visita.
     * Sirve para cargar el historial cuando el usuario entra a la vista.
     */
    public ControlEpp buscarControlReciente(int idTarea, int idVisita) throws SQLException {
        return controlEppDAO.buscarRecientePorTarea(idTarea, idVisita);
    }

    /**
     * Devuelve los detalles (filas por EPP) de un control ya guardado.
     */
    public List<ControlEppDetalle> cargarDetalles(int idControlEpp) throws SQLException {
        return controlEppDAO.cargarDetalles(idControlEpp);
    }

    /**
     * Devuelve los EPPs requeridos por el ARPT vigente de una tarea, ya
     * cruzados con el catalogo de EPPs (nombre + descripcion).
     */
    public List<EppControlado> cargarPlanilla(int idArpt) throws SQLException {
        List<EvaluacionEpp> reqs = controlEppDAO.buscarEppRequeridos(idArpt);
        if (reqs.isEmpty()) return new ArrayList<>();
        List<Integer> idsEpp = new ArrayList<>();
        for (EvaluacionEpp e : reqs) idsEpp.add(e.getIdEpp());
        List<Epp> catalogo = controlEppDAO.cargarCatalogoEpp(idsEpp);
        Map<Integer, Epp> porId = new HashMap<>();
        for (Epp e : catalogo) porId.put(e.getIdEpp(), e);
        List<EppControlado> planilla = new ArrayList<>();
        for (EvaluacionEpp e : reqs) {
            Epp epp = porId.get(e.getIdEpp());
            if (epp != null) {
                planilla.add(new EppControlado(e.getIdEvaluacionEpp(), epp));
            }
        }
        return planilla;
    }

    /** RF-EP-01 (RN): puntajes en rango 1..5. */
    public boolean validarPuntaje(int valorUso, int valorEstado) {
        return valorUso >= 1 && valorUso <= 5 && valorEstado >= 1 && valorEstado <= 5;
    }

    /**
     * Crea la cabecera, inserta los detalles y asocia a la visita en una
     * sola transaccion (RNF-01 ACID). Devuelve el idControlEpp generado.
     */
    public int guardarControl(Usuario usuario, int idVisita, int idPuesto, int idTarea, int idArpt,
                              LocalDate fecha, List<FilaControl> filas) throws SQLException {
        ControlEpp cab = new ControlEpp();
        cab.setIdVisita(idVisita);
        cab.setIdPuesto(idPuesto);
        cab.setIdTarea(idTarea);
        cab.setIdArpt(idArpt);
        cab.setFecha(fecha);
        cab.setEstado(ControlEpp.Estado.REGISTRADO);

        List<ControlEppDetalle> detalles = new ArrayList<>();
        for (FilaControl f : filas) {
            ControlEppDetalle d = new ControlEppDetalle();
            d.setIdEvaluacionEpp(f.idEvaluacionEpp);
            d.setValorUso(f.valorUso);
            d.setValorEstado(f.valorEstado);
            d.setObservaciones(f.observaciones);
            d.setRecomendaciones(f.recomendaciones);
            detalles.add(d);
        }

        int idControlEpp = controlEppDAO.guardarControlConDetalles(cab, detalles);

        // RNF-04: registrar la operacion en bitacora
        try {
            bitacoraDAO.registrar(new Bitacora(
                    LocalDateTime.now(),
                    usuario.getIdUsuario(),
                    "registrar_control_epp",
                    null,
                    "Control de EPP " + idControlEpp + " sobre tarea " + idTarea,
                    "ok"));
        } catch (SQLException ex) {
            System.err.println("[WARN] no se pudo escribir en bitacora: " + ex.getMessage());
        }
        return idControlEpp;
    }

    /** DTO liviano: une una evaluacion_epp con su EPP del catalogo. */
    public static class EppControlado {
        public final int idEvaluacionEpp;
        public final Epp epp;

        public EppControlado(int idEvaluacionEpp, Epp epp) {
            this.idEvaluacionEpp = idEvaluacionEpp;
            this.epp = epp;
        }
    }

    /** DTO: una fila que el usuario completo en la vista. */
    public static class FilaControl {
        public final int idEvaluacionEpp;
        public int valorUso;
        public int valorEstado;
        public String observaciones;
        public String recomendaciones;

        public FilaControl(int idEvaluacionEpp) {
            this.idEvaluacionEpp = idEvaluacionEpp;
        }
    }
}