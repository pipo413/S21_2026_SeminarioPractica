package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Visita;
import ar.com.zyasoc.sighs.modelo.Visita.Estado;
import ar.com.zyasoc.sighs.modelo.Visita.Tipo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Visita. Por ahora expone solo la consulta minima que necesita
 * el selector de visita en VistaARPT y VistaControlEPP: las ultimas N
 * visitas de una empresa ordenadas por fecha descendente. El alta y la
 * baja se hacen via SQL directo (ver TODO - ABM Visitas diferido).
 */
public class VisitaDAO {

    /**
     * Lista las ultimas {@code limite} visitas de la empresa, ordenadas
     * por fecha descendente y luego por id descendente como
     * desempate. Devuelve lista vacia si la empresa no tiene visitas.
     */
    public List<Visita> listarUltimasPorEmpresa(int idEmpresa, int limite) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_visita, id_empresa, id_usuario, fecha, tipo, estado, observaciones "
                            + "FROM visita WHERE id_empresa = ? "
                            + "ORDER BY fecha DESC, id_visita DESC "
                            + "LIMIT ?")) {
                ps.setInt(1, idEmpresa);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Visita> out = new ArrayList<>();
                    while (rs.next()) {
                        Visita v = new Visita();
                        v.setIdVisita(rs.getInt("id_visita"));
                        v.setIdEmpresa(rs.getInt("id_empresa"));
                        v.setIdUsuario(rs.getInt("id_usuario"));
                        Date f = rs.getDate("fecha");
                        if (f != null) v.setFecha(f.toLocalDate());
                        String t = rs.getString("tipo");
                        if (t != null) v.setTipo(Tipo.valueOf(t));
                        String e = rs.getString("estado");
                        if (e != null) v.setEstado(Estado.valueOf(e));
                        v.setObservaciones(rs.getString("observaciones"));
                        out.add(v);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }
}