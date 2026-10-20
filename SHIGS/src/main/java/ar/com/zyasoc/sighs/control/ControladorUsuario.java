package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.persistencia.BitacoraDAO;
import ar.com.zyasoc.sighs.persistencia.UsuarioDAO;

/**
 * Controlador del CU-USR-01 (Iniciar sesion).
 *
 * Sigue el diagrama de secuencia del TP2:
 *   1. VISTA -> CTRL: autenticar(usuario, password)
 *   2. CTRL  -> SVC : verificarCredenciales(usuario, password)
 *   3. SVC   -> DAO : buscarPorNombre(usuario)
 *   4. DAO   --> SVC : usuario (o null)
 *   5. SVC   -> DAO : compararHash(password, salt, hash)
 *   6. (segun resultado) bitacora / incrementar intentos / resetear
 *
 * Regla BCE (N29 del TP2): este es el unico punto que conoce el flujo
 * completo de la CU; las entidades no saben nada de hashing ni de intentos.
 */
public class ControladorUsuario {

    private final ServicioAutenticacion servicioAutenticacion;

    public ControladorUsuario() {
        this(new UsuarioDAO(), new BitacoraDAO());
    }

    public ControladorUsuario(UsuarioDAO usuarioDAO, BitacoraDAO bitacoraDAO) {
        this.servicioAutenticacion = new ServicioAutenticacion(usuarioDAO, bitacoraDAO);
    }

    public ResultadoAutenticacion autenticar(String nombreUsuario, String password) {
        return servicioAutenticacion.autenticar(nombreUsuario, password);
    }

    /** Conveniencia para tests: expone el hashing puro. */
    public static String calcularHashHex(String password, String salt) {
        return ServicioAutenticacion.calcularHashHex(password, salt);
    }

    /** Conveniencia para la vista: devuelve los permisos del usuario logueado. */
    public java.util.List<ar.com.zyasoc.sighs.modelo.Permiso> permisosDe(Usuario u) {
        return u == null ? java.util.Collections.emptyList() : u.calcularPermisos();
    }
}