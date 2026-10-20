package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.Usuario;

/**
 * Encapsula el resultado del flujo de autenticacion del CU-USR-01.
 * Es un DTO: no tiene logica, solo datos.
 */
public class ResultadoAutenticacion {

    public enum Estado {
        OK,
        CREDENCIALES_INVALIDAS,
        USUARIO_INACTIVO,
        ERROR_CONEXION
    }

    private final Estado estado;
    private final Usuario usuarioObj;
    private final String mensaje;

    private ResultadoAutenticacion(Estado estado, Usuario usuarioObj, String mensaje) {
        this.estado = estado;
        this.usuarioObj = usuarioObj;
        this.mensaje = mensaje;
    }

    public static ResultadoAutenticacion ok(Usuario u) {
        return new ResultadoAutenticacion(Estado.OK, u, "Bienvenido, " + u.getNombreCompleto());
    }

    public static ResultadoAutenticacion credencialesInvalidas() {
        return new ResultadoAutenticacion(Estado.CREDENCIALES_INVALIDAS, null,
                "Usuario o contrasena invalidos");
    }

    public static ResultadoAutenticacion usuarioInactivo() {
        return new ResultadoAutenticacion(Estado.USUARIO_INACTIVO, null,
                "Usuario inactivo, contacte al administrador");
    }

    public static ResultadoAutenticacion bloqueado(String mensaje) {
        return new ResultadoAutenticacion(Estado.CREDENCIALES_INVALIDAS, null, mensaje);
    }

    public static ResultadoAutenticacion errorConexion(String mensaje) {
        return new ResultadoAutenticacion(Estado.ERROR_CONEXION, null, mensaje);
    }

    public Estado getEstado() {
        return estado;
    }

    public Usuario getUsuario() {
        return usuarioObj;
    }

    public String getMensaje() {
        return mensaje;
    }
}