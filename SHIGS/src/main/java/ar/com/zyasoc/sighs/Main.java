package ar.com.zyasoc.sighs;

import ar.com.zyasoc.sighs.control.ControladorUsuario;
import ar.com.zyasoc.sighs.control.ResultadoAutenticacion;
import ar.com.zyasoc.sighs.persistencia.ConexionBD;
import ar.com.zyasoc.sighs.vista.CallbackLogin;
import ar.com.zyasoc.sighs.vista.VistaLogin;
import ar.com.zyasoc.sighs.vista.VistaMenuPrincipal;

/**
 * Punto de entrada. Arranca la vista de login y, tras autenticarse,
 * muestra el menu principal. Maneja el caso "el usuario cierra la ventana
 * de login" saliendo de la JVM limpio.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("[SIGHS] Conectando a " + ConexionBD.infoConexion());

        ControladorUsuario controlador = new ControladorUsuario();

        CallbackLogin callback = new CallbackLogin() {


            @Override
            public void onLoginOk(ResultadoAutenticacion resultado) {
                VistaMenuPrincipal menu = new VistaMenuPrincipal(resultado.getUsuario());
                menu.setVisible(true);
            }

            @Override
            public void onLoginCancelado() {
                System.out.println("[SIGHS] Login cancelado. Saliendo.");
                System.exit(0);
            }
        };

        VistaLogin login = new VistaLogin(controlador, callback);
        login.setVisible(true);
    }
}