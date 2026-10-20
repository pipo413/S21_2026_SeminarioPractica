package ar.com.zyasoc.sighs.persistencia;

import ar.com.zyasoc.sighs.modelo.EstadoUsuario;
import ar.com.zyasoc.sighs.modelo.Rol;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.modelo.UsuarioAdministrador;
import ar.com.zyasoc.sighs.modelo.UsuarioResponsable;
import ar.com.zyasoc.sighs.modelo.UsuarioTecnico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la entity Usuario. Encapsula el SQL del TP2 (seccion 13.4 del TP2 -
 * consultas de CU-USR-01) y devuelve el subtipo concreto segun el rol
 * principal del usuario (polimorfismo en la capa de persistencia).
 */
public class UsuarioDAO {

    private static final String SQL_BUSCAR_POR_NOMBRE = """
            SELECT u.id_usuario, u.nombre_usuario, u.password_hash, u.salt,
                   u.nombre_completo, u.estado, u.intentos_fallidos,
                   u.id_created_by, u.id_updated_by,
                   r.id_rol, r.nombre AS rol_nombre
            FROM usuario u
            LEFT JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
            LEFT JOIN rol        r  ON r.id_rol     = ur.id_rol
            WHERE u.nombre_usuario = ?
            ORDER BY r.id_rol
            """;

    private static final String SQL_INCREMENTAR_INTENTOS = """
            UPDATE usuario SET intentos_fallidos = intentos_fallidos + 1,
                                updated_at = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
            """;

    private static final String SQL_MARCAR_INACTIVO = """
            UPDATE usuario SET estado = 'inactivo',
                                updated_at = CURRENT_TIMESTAMP
            WHERE id_usuario = ? AND intentos_fallidos >= 3
            """;

    private static final String SQL_RESETEAR_INTENTOS = """
            UPDATE usuario SET intentos_fallidos = 0,
                                updated_at = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
            """;

    public Usuario buscarPorNombre(String nombreUsuario) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(SQL_BUSCAR_POR_NOMBRE)) {
                ps.setString(1, nombreUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return mapearUno(rs);
                }
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public void incrementarIntentosFallidos(int idUsuario) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(SQL_INCREMENTAR_INTENTOS)) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public boolean marcarInactivoSiSuperaUmbral(int idUsuario) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(SQL_MARCAR_INACTIVO)) {
                ps.setInt(1, idUsuario);
                return ps.executeUpdate() > 0;
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    public void resetearIntentosFallidos(int idUsuario) throws SQLException {
        Connection c = null;
        try {
            c = ConexionBD.abrir();
            try (PreparedStatement ps = c.prepareStatement(SQL_RESETEAR_INTENTOS)) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }
        } finally {
            ConexionBD.cerrar(c);
        }
    }

    /**
     * Mapea el ResultSet a un Usuario concreto segun el rol principal.
     * Si el primer rol es "administrador" -> UsuarioAdministrador;
     * si es "responsable del servicio" -> UsuarioResponsable;
     * caso contrario -> UsuarioTecnico. Esto demuestra polimorfismo:
     * el DAO devuelve la subclase correcta.
     */
    private Usuario mapearUno(ResultSet rs) throws SQLException {
        List<Usuario> encontrados = mapearTodos(rs);
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    /**
     * Mapea todas las filas del ResultSet a UNA sola instancia de Usuario
     * agregando los roles a su lista interna. El JOIN produce N filas
     * por usuario (una por cada rol), por eso se acumula.
     */
    private List<Usuario> mapearTodos(ResultSet rs) throws SQLException {
        Usuario u = null;
        while (rs.next()) {
            if (u == null) {
                u = instanciarSubclase(rs);
            }
            int idRol = rs.getInt("id_rol");
            if (!rs.wasNull()) {
                u.getRoles().add(new Rol(idRol, rs.getString("rol_nombre")));
            }
        }
        List<Usuario> out = new ArrayList<>();
        if (u != null) out.add(u);
        return out;
    }

    private Usuario instanciarSubclase(ResultSet rs) throws SQLException {
        int idUsuario = rs.getInt("id_usuario");
        String nombreUsuario = rs.getString("nombre_usuario");
        String passwordHash = rs.getString("password_hash");
        String salt = rs.getString("salt");
        String nombreCompleto = rs.getString("nombre_completo");
        EstadoUsuario estado = EstadoUsuario.valueOf(rs.getString("estado").toUpperCase());
        int intentos = rs.getInt("intentos_fallidos");

        // discriminamos por el primer rol que venga asociado; si no hay rol
        // todavia, asumimos Tecnico (defensa). El polimorfismo se aplica
        // cuando se asignen roles luego via el DAO de administracion.
        String rolNombre = rs.getString("rol_nombre");
        Usuario u;
        if (rolNombre != null && rolNombre.toLowerCase().contains("admin")) {
            u = new UsuarioAdministrador();
        } else if (rolNombre != null && rolNombre.toLowerCase().contains("responsable")) {
            u = new UsuarioResponsable();
        } else {
            u = new UsuarioTecnico();
        }
        u.setIdUsuario(idUsuario);
        u.setNombreUsuario(nombreUsuario);
        u.setPasswordHash(passwordHash);
        u.setSalt(salt);
        u.setNombreCompleto(nombreCompleto);
        u.setEstado(estado);
        u.setIntentosFallidos(intentos);
        return u;
    }
}