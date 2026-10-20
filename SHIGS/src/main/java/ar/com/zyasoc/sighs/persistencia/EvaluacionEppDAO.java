package ar.com.zyasoc.sighs.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la tabla puente evaluacion_epp (N:M entre EvaluacionRiesgo y EPP).
 * Alineado con §11 / §15 del TP2.
 */
public class EvaluacionEppDAO {

    public void guardar(int idEvaluacion, List<Integer> idsEpp) throws SQLException {
        if (idsEpp == null || idsEpp.isEmpty()) return;
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            for (Integer idEpp : idsEpp) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO evaluacion_epp (id_evaluacion, id_epp) VALUES (?, ?)")) {
                    ps.setInt(1, idEvaluacion);
                    ps.setInt(2, idEpp);
                    ps.executeUpdate();
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Sincroniza las asociaciones evaluacion_epp de una evaluacion con la
     * lista deseada sin romper la trazabilidad: solo borra las filas que
     * ningun control de EPP haya usado. Las que ya estan referenciadas por
     * control_epp_detalle se conservan y se devuelven como "no quitadas"
     * para que la vista pueda avisar.
     */
    public List<Integer> reemplazar(int idEvaluacion, List<Integer> idsEpp) throws SQLException {
        List<Integer> noQuitados = new ArrayList<>();
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            c.setAutoCommit(false);

            java.util.Map<Integer, Integer> actuales = new java.util.LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_evaluacion_epp, id_epp FROM evaluacion_epp WHERE id_evaluacion = ?")) {
                ps.setInt(1, idEvaluacion);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        actuales.put(rs.getInt("id_epp"), rs.getInt("id_evaluacion_epp"));
                    }
                }
            }
            java.util.Set<Integer> deseados = new java.util.HashSet<>();
            if (idsEpp != null) deseados.addAll(idsEpp);

            // altas de los EPPs que faltan
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO evaluacion_epp (id_evaluacion, id_epp) VALUES (?, ?)")) {
                for (Integer idEpp : deseados) {
                    if (actuales.containsKey(idEpp)) continue;
                    ins.setInt(1, idEvaluacion);
                    ins.setInt(2, idEpp);
                    ins.executeUpdate();
                }
            }

            // bajas: solo las que no tengan controles de EPP asociados
            try (PreparedStatement ref = c.prepareStatement(
                    "SELECT 1 FROM control_epp_detalle WHERE id_evaluacion_epp = ? LIMIT 1");
                 PreparedStatement del = c.prepareStatement(
                    "DELETE FROM evaluacion_epp WHERE id_evaluacion_epp = ?")) {
                for (java.util.Map.Entry<Integer, Integer> e : actuales.entrySet()) {
                    if (deseados.contains(e.getKey())) continue;
                    int idEvaluacionEpp = e.getValue();
                    ref.setInt(1, idEvaluacionEpp);
                    boolean referenciado;
                    try (ResultSet rs = ref.executeQuery()) {
                        referenciado = rs.next();
                    }
                    if (referenciado) {
                        noQuitados.add(e.getKey());
                    } else {
                        del.setInt(1, idEvaluacionEpp);
                        del.executeUpdate();
                    }
                }
            }
            c.commit();
        } catch (SQLException ex) {
            if (c != null) c.rollback();
            throw ex;
        } finally {
            if (c != null) c.setAutoCommit(true);
            ConexionBD.cerrar(c);
        }
        return noQuitados;
    }

    /** Devuelve los idEpp asociados a una evaluacion. */
    public List<Integer> cargarPorEvaluacion(int idEvaluacion) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_epp FROM evaluacion_epp WHERE id_evaluacion = ?")) {
                ps.setInt(1, idEvaluacion);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Integer> out = new ArrayList<>();
                    while (rs.next()) out.add(rs.getInt(1));
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}