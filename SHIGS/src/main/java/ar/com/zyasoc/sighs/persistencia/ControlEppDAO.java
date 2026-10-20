package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.ControlEpp;
import ar.com.zyasoc.sighs.modelo.ControlEpp.Estado;
import ar.com.zyasoc.sighs.modelo.ControlEppDetalle;
import ar.com.zyasoc.sighs.modelo.Epp;
import ar.com.zyasoc.sighs.modelo.EvaluacionEpp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO del CU-EPP-01: cabecera y detalles del control de EPP.
 * Implementa RNF-01 (transacciones ACID): crear cabecera, insertar
 * detalles y asociar a la visita se hace en una sola transaccion
 * (autoCommit=false + commit/rollback).
 */
public class ControlEppDAO {

    /**
     * Devuelve los EPPs requeridos por el ARPT vigente de una tarea,
     * excluyendo los riesgos no vigentes.
     */
    public List<EvaluacionEpp> buscarEppRequeridos(int idArpt) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT ee.id_evaluacion_epp, ev.id_evaluacion, ev.id_riesgo, e.id_epp, e.nombre, e.descripcion "
                            + "FROM arpt a "
                            + "JOIN evaluacion_riesgo ev ON ev.id_arpt = a.id_arpt "
                            + "JOIN evaluacion_epp ee ON ee.id_evaluacion = ev.id_evaluacion "
                            + "JOIN epp e ON e.id_epp = ee.id_epp "
                            + "JOIN riesgo r ON r.id_riesgo = ev.id_riesgo "
                            + "WHERE a.estado = 'vigente' AND a.id_arpt = ? AND r.vigente = TRUE")) {
                ps.setInt(1, idArpt);
                try (ResultSet rs = ps.executeQuery()) {
                    List<EvaluacionEpp> out = new ArrayList<>();
                    while (rs.next()) {
                        EvaluacionEpp ee = new EvaluacionEpp();
                        ee.setIdEvaluacionEpp(rs.getInt("id_evaluacion_epp"));
                        ee.setIdEvaluacion(rs.getInt("id_evaluacion"));
                        ee.setIdEpp(rs.getInt("id_epp"));
                        out.add(ee);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Carga el catalogo de EPPs (id + nombre + descripcion) a partir
     * de una lista de EvaluacionEpp.
     */
    public List<Epp> cargarCatalogoEpp(List<Integer> idsEpp) throws SQLException {
        if (idsEpp == null || idsEpp.isEmpty()) return new ArrayList<>();
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            StringBuilder sql = new StringBuilder(
                    "SELECT id_epp, nombre, descripcion FROM epp WHERE id_epp IN (");
            for (int i = 0; i < idsEpp.size(); i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
            }
            sql.append(") ORDER BY nombre");
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < idsEpp.size(); i++) {
                    ps.setInt(i + 1, idsEpp.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Epp> out = new ArrayList<>();
                    while (rs.next()) {
                        Epp e = new Epp();
                        e.setIdEpp(rs.getInt("id_epp"));
                        e.setNombre(rs.getString("nombre"));
                        e.setDescripcion(rs.getString("descripcion"));
                        out.add(e);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Guarda cabecera + detalles en una sola transaccion (RNF-01 ACID).
     * Devuelve el idControlEpp generado.
     */
    public int guardarControlConDetalles(ControlEpp cabecera, List<ControlEppDetalle> detalles) throws SQLException {
        Connection c = null;
        boolean autoCommitOriginal;
        try {
            c = ConexionBD.abrir();
            autoCommitOriginal = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                int idControlEpp = insertarCabecera(c, cabecera);
                cabecera.setIdControlEpp(idControlEpp);
                for (ControlEppDetalle d : detalles) {
                    d.setIdControlEpp(idControlEpp);
                    insertarDetalle(c, d);
                }
                c.commit();
                return idControlEpp;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(autoCommitOriginal);
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    private int insertarCabecera(Connection c, ControlEpp cab) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO control_epp (id_visita, id_puesto, id_tarea, id_arpt, fecha, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cab.getIdVisita());
            ps.setInt(2, cab.getIdPuesto());
            ps.setInt(3, cab.getIdTarea());
            ps.setInt(4, cab.getIdArpt());
            ps.setDate(5, Date.valueOf(cab.getFecha()));
            ps.setString(6, cab.getEstado().name().toLowerCase());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("no se obtuvo id generado para control_epp");
            }
        }
    }

    private void insertarDetalle(Connection c, ControlEppDetalle d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO control_epp_detalle (id_control_epp, id_evaluacion_epp, valor_uso, valor_estado, observaciones, recomendaciones) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, d.getIdControlEpp());
            ps.setInt(2, d.getIdEvaluacionEpp());
            ps.setInt(3, d.getValorUso());
            ps.setInt(4, d.getValorEstado());
            ps.setString(5, d.getObservaciones());
            ps.setString(6, d.getRecomendaciones());
            ps.executeUpdate();
        }
    }

    public ControlEpp buscarPorId(int idControlEpp) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_control_epp, id_visita, id_puesto, id_tarea, id_arpt, fecha, estado "
                            + "FROM control_epp WHERE id_control_epp = ?")) {
                ps.setInt(1, idControlEpp);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ControlEpp c2 = new ControlEpp();
                        c2.setIdControlEpp(rs.getInt("id_control_epp"));
                        c2.setIdVisita(rs.getInt("id_visita"));
                        c2.setIdPuesto(rs.getInt("id_puesto"));
                        c2.setIdTarea(rs.getInt("id_tarea"));
                        c2.setIdArpt(rs.getInt("id_arpt"));
                        Date f = rs.getDate("fecha");
                        c2.setFecha(f != null ? f.toLocalDate() : null);
                        c2.setEstado(Estado.valueOf(rs.getString("estado").toUpperCase()));
                        return c2;
                    }
                    return null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Busca el control de EPP mas reciente (no anulado) para una tarea
     * en una visita concreta. Sirve para cargar el historial en la vista.
     */
    public ControlEpp buscarRecientePorTarea(int idTarea, int idVisita) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_control_epp, id_visita, id_puesto, id_tarea, id_arpt, fecha, estado "
                            + "FROM control_epp "
                            + "WHERE id_tarea = ? AND id_visita = ? AND estado = 'registrado' "
                            + "ORDER BY fecha DESC, id_control_epp DESC LIMIT 1")) {
                ps.setInt(1, idTarea);
                ps.setInt(2, idVisita);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return buscarPorId(rs.getInt("id_control_epp"));
                    }
                    return null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Devuelve los detalles (filas por EPP) de un control.
     */
    public List<ControlEppDetalle> cargarDetalles(int idControlEpp) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_detalle, id_control_epp, id_evaluacion_epp, valor_uso, valor_estado, "
                            + "observaciones, recomendaciones "
                            + "FROM control_epp_detalle WHERE id_control_epp = ?")) {
                ps.setInt(1, idControlEpp);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ControlEppDetalle> out = new ArrayList<>();
                    while (rs.next()) {
                        ControlEppDetalle d = new ControlEppDetalle();
                        d.setIdDetalle(rs.getInt("id_detalle"));
                        d.setIdControlEpp(rs.getInt("id_control_epp"));
                        d.setIdEvaluacionEpp(rs.getInt("id_evaluacion_epp"));
                        d.setValorUso(rs.getInt("valor_uso"));
                        d.setValorEstado(rs.getInt("valor_estado"));
                        d.setObservaciones(rs.getString("observaciones"));
                        d.setRecomendaciones(rs.getString("recomendaciones"));
                        out.add(d);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}