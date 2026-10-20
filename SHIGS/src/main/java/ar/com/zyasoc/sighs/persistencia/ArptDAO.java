package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Arpt;
import ar.com.zyasoc.sighs.modelo.Arpt.Estado;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * DAO de ARPT - el CU-EPP-01 consulta el ARPT vigente de una tarea,
 * y el CU-ARPT-01 crea/modifica/lleva a vigente el ARPT.
 */
public class ArptDAO {

    public Arpt buscarVigentePorTarea(int idTarea) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_arpt, id_tarea, id_visita, fecha, estado, observaciones_generales, conclusion_general "
                            + "FROM arpt WHERE id_tarea = ? AND estado = 'vigente' ORDER BY fecha DESC LIMIT 1")) {
                ps.setInt(1, idTarea);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapear(rs);
                    }
                    return null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /** Busca un ARPT por su id. */
    public Arpt buscarPorId(int idArpt) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_arpt, id_tarea, id_visita, fecha, estado, observaciones_generales, conclusion_general "
                            + "FROM arpt WHERE id_arpt = ?")) {
                ps.setInt(1, idArpt);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapear(rs);
                    return null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /** Busca el ARPT mas reciente de una tarea (editor o vigente). */
    public Arpt buscarEnEdicionPorTarea(int idTarea) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_arpt, id_tarea, id_visita, fecha, estado, observaciones_generales, conclusion_general "
                            + "FROM arpt WHERE id_tarea = ? ORDER BY fecha DESC, id_arpt DESC LIMIT 1")) {
                ps.setInt(1, idTarea);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapear(rs);
                    return null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /** Crea un ARPT nuevo en estado EDICION. Devuelve el id generado. */
    public int crearEnEdicion(int idTarea, int idVisita, java.time.LocalDate fecha,
                              int idTecnico, String observaciones, String conclusion) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO arpt (id_tarea, id_visita, fecha, estado, "
                            + "observaciones_generales, conclusion_general, id_created_by, id_updated_by) "
                            + "VALUES (?, ?, ?, 'edicion', ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idTarea);
                ps.setInt(2, idVisita);
                ps.setDate(3, Date.valueOf(fecha));
                if (observaciones != null) ps.setString(4, observaciones);
                else ps.setNull(4, Types.VARCHAR);
                if (conclusion != null) ps.setString(5, conclusion);
                else ps.setNull(5, Types.VARCHAR);
                ps.setInt(6, idTecnico);
                ps.setInt(7, idTecnico);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                    throw new SQLException("no se obtuvo id generado para arpt");
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /** Marca un ARPT como historico (quedo reemplazado por una version nueva). */
    public void marcarHistorico(int idArpt) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE arpt SET estado = 'historico', updated_at = CURRENT_TIMESTAMP "
                            + "WHERE id_arpt = ?")) {
                ps.setInt(1, idArpt);
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public void actualizar(int idArpt, Estado estado, String obs, String conc) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE arpt SET estado = ?, observaciones_generales = ?, conclusion_general = ?, "
                            + "updated_at = CURRENT_TIMESTAMP WHERE id_arpt = ?")) {
                ps.setString(1, estado.name().toLowerCase());
                if (obs != null) ps.setString(2, obs);
                else ps.setNull(2, Types.VARCHAR);
                if (conc != null) ps.setString(3, conc);
                else ps.setNull(3, Types.VARCHAR);
                ps.setInt(4, idArpt);
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    private Arpt mapear(ResultSet rs) throws SQLException {
        Arpt a = new Arpt();
        a.setIdArpt(rs.getInt("id_arpt"));
        a.setIdTarea(rs.getInt("id_tarea"));
        a.setIdVisita(rs.getInt("id_visita"));
        Date f = rs.getDate("fecha");
        a.setFecha(f != null ? f.toLocalDate() : null);
        a.setEstado(Estado.valueOf(rs.getString("estado").toUpperCase()));
        a.setObservacionesGenerales(rs.getString("observaciones_generales"));
        a.setConclusionGeneral(rs.getString("conclusion_general"));
        return a;
    }
}