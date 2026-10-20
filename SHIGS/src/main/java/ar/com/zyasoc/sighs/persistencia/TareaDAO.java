package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Tarea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Tarea - consulta la jerarquia organizacional para CU-EPP-01.
 */
public class TareaDAO {

    public List<Tarea> listarPorPuesto(int idPuesto) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_tarea, id_puesto, nombre, descripcion FROM tarea WHERE id_puesto = ? ORDER BY nombre")) {
                ps.setInt(1, idPuesto);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Tarea> out = new ArrayList<>();
                    while (rs.next()) {
                        Tarea t = new Tarea();
                        t.setIdTarea(rs.getInt("id_tarea"));
                        t.setIdPuesto(rs.getInt("id_puesto"));
                        t.setNombre(rs.getString("nombre"));
                        t.setDescripcion(rs.getString("descripcion"));
                        out.add(t);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}