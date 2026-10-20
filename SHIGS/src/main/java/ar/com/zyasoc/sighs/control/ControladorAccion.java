package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.Bitacora;
import ar.com.zyasoc.sighs.modelo.PlanAccion;
import ar.com.zyasoc.sighs.modelo.PlanAccion.Estado;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.persistencia.BitacoraDAO;
import ar.com.zyasoc.sighs.persistencia.PlanAccionDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador del CU-ACC-01 (Gestionar planes de accion). Implementa:
 *  - RD-16: el estado "vencido / proximo a vencer" es derivado en la entidad.
 *  - RD-17: una sola operacion en bitacora por gestion, con detalle.
 */
public class ControladorAccion {

    private final PlanAccionDAO planAccionDAO;
    private final BitacoraDAO bitacoraDAO;

    public ControladorAccion() {
        this(new PlanAccionDAO(), new BitacoraDAO());
    }

    public ControladorAccion(PlanAccionDAO planAccionDAO, BitacoraDAO bitacoraDAO) {
        this.planAccionDAO = planAccionDAO;
        this.bitacoraDAO = bitacoraDAO;
    }

    public List<PlanAccion> listarAbiertos(int idEmpresa) throws SQLException {
        return planAccionDAO.listarAbiertosPorEmpresa(idEmpresa);
    }

    /**
     * Valida una fila de la grilla: descripcion no vacia, responsable
     * (nombre de persona, no FK) no vacio, fechaPlazo >= hoy.
     */
    public boolean validarFila(String descripcion, String responsable, LocalDate fechaPlazo) {
        if (descripcion == null || descripcion.trim().isEmpty()) return false;
        if (responsable == null || responsable.trim().isEmpty()) return false;
        if (fechaPlazo == null) return false;
        if (fechaPlazo.isBefore(LocalDate.now())) return false;
        return true;
    }

    public int crearPlan(Usuario usuario, PlanAccion p) throws SQLException {
        int idPlan = planAccionDAO.crear(p);
        registrarBitacora(usuario, "alta de plan " + idPlan);
        return idPlan;
    }

    /**
     * Crea varios planes en lote (caso ARPT/EPP con planes acumulados
     * en memoria). Aplica el id_origen a cada plan antes de persistir y
     * registra una sola entrada de bitacora con el resumen de los planes
     * creados (RD-17: una sola operacion agrupada).
     */
    public void modificarPlan(Usuario usuario, PlanAccion p) throws SQLException {
        // RD-16: si pasa a CUMPLIDO o NOCUMPLIDO y no hay fechaCierre, la
        // ponemos en hoy.
        if ((p.getEstado() == Estado.CUMPLIDO || p.getEstado() == Estado.NOCUMPLIDO)
                && p.getFechaCierre() == null) {
            p.setFechaCierre(LocalDate.now());
        }
        planAccionDAO.modificar(p);
        registrarBitacora(usuario, "modificacion de plan " + p.getIdPlanAccion() + " (estado=" + p.getEstado() + ")");
    }

    public void bajaLogicaPlan(Usuario usuario, int idPlan) throws SQLException {
        planAccionDAO.marcarBajaLogica(idPlan, usuario.getIdUsuario());
        registrarBitacora(usuario, "baja logica de plan " + idPlan);
    }

    private void registrarBitacora(Usuario usuario, String detalle) {
        try {
            bitacoraDAO.registrar(new Bitacora(
                    LocalDateTime.now(),
                    usuario.getIdUsuario(),
                    "gestionar_plan_accion",
                    usuario.getIdUsuario(),  // idEmpresa no aplica, usamos el id del usuario como dato de contexto
                    detalle,
                    "ok"));
        } catch (SQLException ex) {
            System.err.println("[WARN] no se pudo escribir en bitacora: " + ex.getMessage());
        }
    }
}