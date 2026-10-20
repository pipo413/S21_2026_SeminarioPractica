package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Puesto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Puesto - consulta la jerarquia organizacional para CU-EPP-01.
 */
public class PuestoDAO {

    public List<Puesto> listarPorSector(int idSector) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_puesto, id_sector, nombre, cantidad_trabajadores "
                            + "FROM puesto WHERE id_sector = ? ORDER BY nombre")) {
                ps.setInt(1, idSector);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Puesto> out = new ArrayList<>();
                    while (rs.next()) {
                        Puesto p = new Puesto();
                        p.setIdPuesto(rs.getInt("id_puesto"));
                        p.setIdSector(rs.getInt("id_sector"));
                        p.setNombre(rs.getString("nombre"));
                        // cantidad_trabajadores no se usa en la UI del prototipo
                        out.add(p);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}