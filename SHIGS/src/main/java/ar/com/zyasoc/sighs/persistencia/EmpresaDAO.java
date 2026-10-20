package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Empresa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Empresa. Lista las empresas asesoradas para que el menu
 * "Gestiones" pueda ofrecer un dropdown.
 */
public class EmpresaDAO {

    public List<Empresa> listarTodas() throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_empresa, razon_social, cuit, domicilio, "
                            + "nombre_referente, cargo_referente, email_referente "
                            + "FROM empresa ORDER BY razon_social")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<Empresa> out = new ArrayList<>();
                    while (rs.next()) {
                        Empresa e = new Empresa();
                        e.setIdEmpresa(rs.getInt("id_empresa"));
                        e.setRazonSocial(rs.getString("razon_social"));
                        e.setCuit(rs.getString("cuit"));
                        e.setDomicilio(rs.getString("domicilio"));
                        e.setNombreReferente(rs.getString("nombre_referente"));
                        e.setCargoReferente(rs.getString("cargo_referente"));
                        e.setEmailReferente(rs.getString("email_referente"));
                        out.add(e);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}