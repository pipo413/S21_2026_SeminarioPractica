package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Bitacora;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * DAO de Bitacora. RNF-04: registra toda operacion critica.
 */
public class BitacoraDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO bitacora (id_usuario, operacion, id_empresa, detalle, resultado)
            VALUES (?, ?, ?, ?, ?)
            """;

    public void registrar(Bitacora b) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(SQL_INSERTAR)) {
                ps.setInt(1, b.getIdUsuario());
                ps.setString(2, b.getOperacion());
                if (b.getIdEmpresa() != null) {
                    ps.setInt(3, b.getIdEmpresa());
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
                ps.setString(4, b.getDetalle());
                ps.setString(5, b.getResultado());
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}