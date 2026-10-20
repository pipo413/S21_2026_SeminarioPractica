package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Sector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Sector - consulta la jerarquia organizacional para
 * CU-ARPT-01 y CU-EPP-01. El filtro canonico es por
 * {@code id_establecimiento} (un sector vive adentro de un
 * establecimiento que vive adentro de una empresa).
 */
public class SectorDAO {

    /**
     * Lista los sectores del establecimiento. Es el metodo que
     * usa el cascadeo de VistaARPT/VistaControlEPP: una vez
     * elegido el establecimiento, este filtro da los sectores
     * concretos sin pasar por la empresa.
     */
    public List<Sector> listarPorEstablecimiento(int idEstablecimiento) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_sector, id_establecimiento, nombre "
                            + "FROM sector WHERE id_establecimiento = ? ORDER BY nombre")) {
                ps.setInt(1, idEstablecimiento);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Sector> out = new ArrayList<>();
                    while (rs.next()) {
                        Sector s = new Sector();
                        s.setIdSector(rs.getInt("id_sector"));
                        s.setIdEstablecimiento(rs.getInt("id_establecimiento"));
                        s.setNombre(rs.getString("nombre"));
                        out.add(s);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}