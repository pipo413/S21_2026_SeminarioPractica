package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo;
import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo.Clasificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de EvaluacionRiesgo: inserta y consulta las filas del ARPT.
 */
public class EvaluacionRiesgoDAO {

    public void guardar(EvaluacionRiesgo ev) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO evaluacion_riesgo (id_arpt, id_riesgo, id_medida_control, "
                            + "probabilidad, exposicion, consecuencia, grado_peligrosidad, clasificacion, "
                            + "id_created_by, id_updated_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ev.getIdArpt());
                ps.setInt(2, ev.getIdRiesgo());
                if (ev.getIdMedidaControl() != null) ps.setInt(3, ev.getIdMedidaControl());
                else ps.setNull(3, Types.INTEGER);
                ps.setInt(4, ev.getProbabilidad());
                ps.setInt(5, ev.getExposicion());
                ps.setInt(6, ev.getConsecuencia());
                ps.setInt(7, ev.getGradoPeligrosidad());
                ps.setString(8, ev.getClasificacion().name().toLowerCase());
                ps.setInt(9, 1); // id_created_by: simplificado para test
                ps.setInt(10, 1); // id_updated_by: simplificado para test
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) ev.setIdEvaluacion(rs.getInt(1));
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Devuelve el id de la evaluacion de un riesgo dentro de un ARPT, o null
     * si todavia no existe. Sirve para no duplicar filas cuando el usuario
     * vuelve a calcular la planilla.
     */
    public Integer buscarIdPorArptYRiesgo(int idArpt, int idRiesgo) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_evaluacion FROM evaluacion_riesgo "
                            + "WHERE id_arpt = ? AND id_riesgo = ? ORDER BY id_evaluacion DESC LIMIT 1")) {
                ps.setInt(1, idArpt);
                ps.setInt(2, idRiesgo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : null;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public void actualizar(EvaluacionRiesgo ev) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE evaluacion_riesgo SET id_medida_control = ?, "
                            + "probabilidad = ?, exposicion = ?, consecuencia = ?, "
                            + "grado_peligrosidad = ?, clasificacion = ?, id_updated_by = ?, "
                            + "updated_at = CURRENT_TIMESTAMP "
                            + "WHERE id_evaluacion = ?")) {
                if (ev.getIdMedidaControl() != null) ps.setInt(1, ev.getIdMedidaControl());
                else ps.setNull(1, Types.INTEGER);
                ps.setInt(2, ev.getProbabilidad());
                ps.setInt(3, ev.getExposicion());
                ps.setInt(4, ev.getConsecuencia());
                ps.setInt(5, ev.getGradoPeligrosidad());
                ps.setString(6, ev.getClasificacion().name().toLowerCase());
                ps.setInt(7, 1);
                ps.setInt(8, ev.getIdEvaluacion());
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public List<EvaluacionRiesgo> cargarPorArpt(int idArpt) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_evaluacion, id_arpt, id_riesgo, id_medida_control, "
                            + "probabilidad, exposicion, consecuencia, grado_peligrosidad, clasificacion "
                            + "FROM evaluacion_riesgo WHERE id_arpt = ? ORDER BY id_evaluacion")) {
                ps.setInt(1, idArpt);
                try (ResultSet rs = ps.executeQuery()) {
                    List<EvaluacionRiesgo> out = new ArrayList<>();
                    while (rs.next()) {
                        EvaluacionRiesgo ev = new EvaluacionRiesgo();
                        ev.setIdEvaluacion(rs.getInt("id_evaluacion"));
                        ev.setIdArpt(rs.getInt("id_arpt"));
                        ev.setIdRiesgo(rs.getInt("id_riesgo"));
                        int mc = rs.getInt("id_medida_control");
                        ev.setIdMedidaControl(rs.wasNull() ? null : mc);
                        ev.setProbabilidad(rs.getInt("probabilidad"));
                        ev.setExposicion(rs.getInt("exposicion"));
                        ev.setConsecuencia(rs.getInt("consecuencia"));
                        ev.setGradoPeligrosidad(rs.getInt("grado_peligrosidad"));
                        ev.setClasificacion(Clasificacion.valueOf(rs.getString("clasificacion").toUpperCase()));
                        out.add(ev);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}