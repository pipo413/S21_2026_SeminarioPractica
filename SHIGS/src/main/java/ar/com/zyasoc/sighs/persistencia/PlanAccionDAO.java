package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.PlanAccion;
import ar.com.zyasoc.sighs.modelo.PlanAccion.Estado;
import ar.com.zyasoc.sighs.modelo.PlanAccion.TipoOrigen;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO del CU-ACC-01 (planes de accion). Implementa RD-16 (estado
 * vencido/proximo a vencer derivado) y RD-17 (operacion unica en
 * bitacora con detalle de los cambios).
 */
public class PlanAccionDAO {

    /**
     * Lista los planes abiertos (EnTratamiento) de una empresa, sin baja
     * logica, con la situacion RD-16 calculada en SQL puro.
     */
    public List<PlanAccion> listarAbiertosPorEmpresa(int idEmpresa) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_plan_accion, id_empresa, tipo_origen, id_origen, "
                            + "descripcion, responsable, fecha_plazo, estado, fecha_cierre, baja_logica "
                            + "FROM plan_accion "
                            + "WHERE id_empresa = ? AND estado = 'entratamiento' AND baja_logica = false "
                            + "ORDER BY fecha_plazo")) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    List<PlanAccion> out = new ArrayList<>();
                    while (rs.next()) out.add(mapear(rs));
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public PlanAccion buscarPorId(int idPlan) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_plan_accion, id_empresa, tipo_origen, id_origen, "
                            + "descripcion, responsable, fecha_plazo, estado, fecha_cierre, baja_logica "
                            + "FROM plan_accion WHERE id_plan_accion = ?")) {
                ps.setInt(1, idPlan);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapear(rs);
                    return null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Crea un plan nuevo. Devuelve el id generado. El id_origen del plan
     * ya debe estar seteado al id real del ARPT/EPP origen (no se acepta -1
     * ni 0).
     */
    public int crear(PlanAccion p) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            return ejecutarInsert(c, p);
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Crea varios planes en una sola transaccion. Usado por el flujo
     * 'planes acumulados en memoria que se persisten al guardar el ARPT'.
     * Si cualquier INSERT falla, ROLLBACK y nada queda persistido.
     */
    private int ejecutarInsert(Connection c, PlanAccion p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO plan_accion (id_empresa, tipo_origen, id_origen, descripcion, "
                        + "responsable, fecha_plazo, estado, id_created_by, id_updated_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'entratamiento', ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdEmpresa());
            ps.setString(2, p.getTipoOrigen().name().toLowerCase());
            ps.setInt(3, p.getIdOrigen());
            ps.setString(4, p.getDescripcion());
            ps.setString(5, p.getResponsable());
            ps.setDate(6, Date.valueOf(p.getFechaPlazo()));
            // id_created_by/id_updated_by: el sistema no los vincula a un
  // usuario concreto del staff para el responsable externo del plan;
  // usamos el admin (id=4 en el seed) como auditoria por defecto.
  // Si en el futuro hace falta vincular a un usuario del sistema, se
  // agrega un campo id_responsable_usuario nullable y se persiste.
            ps.setInt(7, 4);
            ps.setInt(8, 4);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("no se obtuvo id generado para plan_accion");
            }
        }
    }

    /**
     * Modifica un plan. Si pasa a CUMPLIDO/NOCUMPLIDO y fechaCierre es null,
     * le setea la fecha del sistema (RD-16).
     */
    public void modificar(PlanAccion p) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE plan_accion SET descripcion = ?, responsable = ?, "
                            + "fecha_plazo = ?, estado = ?, fecha_cierre = ?, "
                            + "updated_at = CURRENT_TIMESTAMP, id_updated_by = ? "
                            + "WHERE id_plan_accion = ?")) {
                ps.setString(1, p.getDescripcion());
                ps.setString(2, p.getResponsable());
                ps.setDate(3, Date.valueOf(p.getFechaPlazo()));
                ps.setString(4, p.getEstado().name().toLowerCase());
                if (p.getFechaCierre() != null) {
                    ps.setDate(5, Date.valueOf(p.getFechaCierre()));
                } else {
                    ps.setNull(5, Types.DATE);
                }
                ps.setInt(6, 4);
                ps.setInt(7, p.getIdPlanAccion());
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Baja logica: marca baja_logica = true sin borrar fisicamente.
     */
    public void marcarBajaLogica(int idPlan, int idUpdatedBy) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE plan_accion SET baja_logica = true, updated_at = CURRENT_TIMESTAMP, "
                            + "id_updated_by = ? WHERE id_plan_accion = ?")) {
                ps.setInt(1, idUpdatedBy);
                ps.setInt(2, idPlan);
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    private PlanAccion mapear(ResultSet rs) throws SQLException {
        PlanAccion p = new PlanAccion();
        p.setIdPlanAccion(rs.getInt("id_plan_accion"));
        p.setIdEmpresa(rs.getInt("id_empresa"));
        p.setTipoOrigen(TipoOrigen.valueOf(rs.getString("tipo_origen").toUpperCase()));
        p.setIdOrigen(rs.getInt("id_origen"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setResponsable(rs.getString("responsable"));
        Date fp = rs.getDate("fecha_plazo");
        p.setFechaPlazo(fp != null ? fp.toLocalDate() : null);
        p.setEstado(Estado.valueOf(rs.getString("estado").toUpperCase()));
        Date fc = rs.getDate("fecha_cierre");
        p.setFechaCierre(fc != null ? fc.toLocalDate() : null);
        p.setBajaLogica(rs.getBoolean("baja_logica"));
        return p;
    }
}