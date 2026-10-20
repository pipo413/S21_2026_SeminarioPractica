package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Establecimiento;
import ar.com.zyasoc.sighs.modelo.Establecimiento.Tipo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Establecimiento. Lista los establecimientos de una empresa
 * para alimentar el combo previo a Sector en CU-ARPT-01 y CU-EPP-01.
 */
public class EstablecimientoDAO {

    public List<Establecimiento> listarPorEmpresa(int idEmpresa) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_establecimiento, id_empresa, nombre, tipo, domicilio "
                            + "FROM establecimiento WHERE id_empresa = ? ORDER BY nombre")) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Establecimiento> out = new ArrayList<>();
                    while (rs.next()) {
                        Establecimiento es = new Establecimiento();
                        es.setIdEstablecimiento(rs.getInt("id_establecimiento"));
                        es.setIdEmpresa(rs.getInt("id_empresa"));
                        es.setNombre(rs.getString("nombre"));
                        String t = rs.getString("tipo");
                        if (t != null) es.setTipo(Tipo.valueOf(t));
                        es.setDomicilio(rs.getString("domicilio"));
                        out.add(es);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}