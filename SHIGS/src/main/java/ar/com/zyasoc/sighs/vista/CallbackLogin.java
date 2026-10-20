package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.control.ResultadoAutenticacion;

/**
 * Contrato que la VistaLogin usa para devolver el control a la capa que
 * la lanzo (Main o un futuro menu). Despues de un login exitoso se invoca
 * onLoginOk; si el usuario cierra la ventana, onLoginCancelado.
 */
public interface CallbackLogin {

    void onLoginOk(ResultadoAutenticacion resultado);

    void onLoginCancelado();
}