package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.Bitacora;
import ar.com.zyasoc.sighs.modelo.EstadoUsuario;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.persistencia.BitacoraDAO;
import ar.com.zyasoc.sighs.persistencia.UsuarioDAO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Servicio de autenticacion (CU-USR-01).
 *
 * Implementa RNF-02 (hash con sal por usuario): el password nunca se guarda
 * ni se compara en claro. El algoritmo es SHA-256(password || salt) en formato
 * hexadecimal, que es el mismo que usa el script de actualizacion de hashes
 * de prueba (SHA2(CONCAT(password, salt), 256)).
 *
 * Implementa el bloqueo al tercer intento fallido consecutivo (RF-USR-01).
 *
 * Politica de bitacora (RNF-04): solo se escribe una entrada cuando hay
 * un usuario identificado. Si el nombre de usuario no existe en la base,
 * no se registra nada (no tiene sentido auditar un id inexistente).
 */
public class ServicioAutenticacion {

    private final UsuarioDAO usuarioDAO;
    private final BitacoraDAO bitacoraDAO;

    public ServicioAutenticacion(UsuarioDAO usuarioDAO, BitacoraDAO bitacoraDAO) {
        this.usuarioDAO = usuarioDAO;
        this.bitacoraDAO = bitacoraDAO;
    }

    public ResultadoAutenticacion autenticar(String nombreUsuario, String passwordIngresado) {
        Usuario u;
        try {
            u = usuarioDAO.buscarPorNombre(nombreUsuario);
        } catch (SQLException ex) {
            return ResultadoAutenticacion.errorConexion(
                    "No se pudo conectar al servidor: " + ex.getMessage());
        }

        if (u == null) {
            // usuario inexistente: no se escribe en bitacora
            return ResultadoAutenticacion.credencialesInvalidas();
        }

        if (u.getEstado() == EstadoUsuario.INACTIVO) {
            registrarBitacora(u.getIdUsuario(), "login_fallido", null,
                    "usuario inactivo", "bloqueado");
            return ResultadoAutenticacion.usuarioInactivo();
        }

        boolean coincide = compararHash(passwordIngresado, u.getSalt(), u.getPasswordHash());

        if (!coincide) {
            try {
                usuarioDAO.incrementarIntentosFallidos(u.getIdUsuario());
                boolean llegoAlUmbral = usuarioDAO.marcarInactivoSiSuperaUmbral(u.getIdUsuario());
                if (llegoAlUmbral) {
                    registrarBitacora(u.getIdUsuario(), "usuario_bloqueado", null,
                            "cuenta bloqueada tras 3 intentos fallidos", "bloqueado");
                    return ResultadoAutenticacion.bloqueado(
                            "Cuenta bloqueada, contacte al Responsable del Servicio");
                }
            } catch (SQLException ex) {
                return ResultadoAutenticacion.errorConexion(
                        "No se pudo registrar el intento: " + ex.getMessage());
            }
            registrarBitacora(u.getIdUsuario(), "login_fallido", null,
                    "credenciales invalidas", "fallo");
            return ResultadoAutenticacion.credencialesInvalidas();
        }

        // login correcto: resetear intentos y registrar ok
        try {
            usuarioDAO.resetearIntentosFallidos(u.getIdUsuario());
        } catch (SQLException ex) {
            // no es fatal: el usuario ya fue autenticado
        }
        registrarBitacora(u.getIdUsuario(), "login_ok", null,
                "Inicio de sesion de " + u.getNombreUsuario(), "ok");
        return ResultadoAutenticacion.ok(u);
    }

    private boolean compararHash(String password, String salt, String hashEsperadoHex) {
        String calculado = calcularHashHex(password, salt);
        if (calculado == null) return false;
        // comparacion de tiempo constante para evitar timing attacks
        return MessageDigest.isEqual(
                calculado.getBytes(StandardCharsets.UTF_8),
                hashEsperadoHex.getBytes(StandardCharsets.UTF_8));
    }

    public static String calcularHashHex(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // concatenamos password || salt en un solo array de bytes
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[passwordBytes.length + saltBytes.length];
            System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
            System.arraycopy(saltBytes, 0, combined, passwordBytes.length, saltBytes.length);
            byte[] digest = md.digest(combined);
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void registrarBitacora(int idUsuario, String operacion, Integer idEmpresa,
                                    String detalle, String resultado) {
        try {
            bitacoraDAO.registrar(new Bitacora(
                    LocalDateTime.now(), idUsuario, operacion, idEmpresa, detalle, resultado));
        } catch (SQLException ex) {
            // la falla de bitacora no debe impedir el flujo principal de la CU
            System.err.println("[WARN] no se pudo escribir en bitacora: " + ex.getMessage());
        }
    }
}