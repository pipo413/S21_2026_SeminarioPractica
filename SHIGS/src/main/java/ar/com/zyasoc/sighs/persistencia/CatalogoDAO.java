package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.Epp;
import ar.com.zyasoc.sighs.modelo.MedidaControl;
import ar.com.zyasoc.sighs.modelo.Riesgo;
import ar.com.zyasoc.sighs.modelo.TipoRiesgo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO que carga los catalogos que se usan en los combos de la planilla
 * del ARPT: tipos de riesgo, riesgos vigentes y medidas de control.
 */
public class CatalogoDAO {

    public List<Riesgo> cargarRiesgos() throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            // Listado completo (vigentes y dados de baja) del catalogo de
            // riesgos, agrupado por familia y, dentro de cada familia, por subtipo y codigo.
            // Como el catalogo tiene tipos con jerarquia (familia -> subtipo, via tipo_riesgo.id_padre),
            // hacemos un JOIN al tipo del riesgo y un LEFT JOIN a su familia para
            // poder ordenar primero por familia.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.id_riesgo, r.id_tipo_riesgo, r.codigo, r.nombre, "
                            + "r.descripcion, r.vigente, "
                            + "t.nombre AS tipo_nombre, "
                            + "COALESCE(f.nombre, t.nombre) AS familia_nombre "
                            + "FROM riesgo r "
                            + "JOIN tipo_riesgo t ON t.id_tipo_riesgo = r.id_tipo_riesgo "
                            + "LEFT JOIN tipo_riesgo f ON f.id_tipo_riesgo = t.id_padre "
                            + "ORDER BY COALESCE(f.nombre, t.nombre), t.nombre, r.codigo")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<Riesgo> out = new ArrayList<>();
                    while (rs.next()) {
                        Riesgo r = new Riesgo();
                        r.setIdRiesgo(rs.getInt("id_riesgo"));
                        r.setIdTipoRiesgo(rs.getInt("id_tipo_riesgo"));
                        r.setCodigo(rs.getString("codigo"));
                        r.setNombre(rs.getString("nombre"));
                        r.setDescripcion(rs.getString("descripcion"));
                        r.setVigente(rs.getBoolean("vigente"));
                        out.add(r);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public List<TipoRiesgo> cargarTiposRiesgo() throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_tipo_riesgo, nombre, id_padre FROM tipo_riesgo ORDER BY nombre")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<TipoRiesgo> out = new ArrayList<>();
                    while (rs.next()) {
                        TipoRiesgo t = new TipoRiesgo();
                        t.setIdTipoRiesgo(rs.getInt("id_tipo_riesgo"));
                        t.setNombre(rs.getString("nombre"));
                        int padre = rs.getInt("id_padre");
                        t.setIdPadre(rs.wasNull() ? null : padre);
                        out.add(t);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public List<MedidaControl> cargarMedidasControl() throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_medida, descripcion, tipo FROM medida_control ORDER BY tipo, descripcion")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<MedidaControl> out = new ArrayList<>();
                    while (rs.next()) {
                        MedidaControl m = new MedidaControl();
                        m.setIdMedida(rs.getInt("id_medida"));
                        m.setDescripcion(rs.getString("descripcion"));
                        m.setTipo(rs.getString("tipo"));
                        out.add(m);
                    }
                    return out;
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public List<Epp> cargarEpp() throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_epp, nombre, descripcion FROM epp ORDER BY nombre")) {
                try (ResultSet rs = ps.executeQuery()) {
                    List<Epp> out = new ArrayList<>();
                    while (rs.next()) {
                        Epp e = new Epp();
                        e.setIdEpp(rs.getInt("id_epp"));
                        e.setNombre(rs.getString("nombre"));
                        e.setDescripcion(rs.getString("descripcion"));
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