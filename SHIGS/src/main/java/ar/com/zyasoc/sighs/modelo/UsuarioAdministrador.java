package ar.com.zyasoc.sighs.modelo;

import java.util.List;

/**
 * Administrador del sistema. Tiene todos los permisos operativos mas la
 * administracion de usuarios y catalogos.
 */
public class UsuarioAdministrador extends Usuario {

    @Override
    public List<Permiso> calcularPermisos() {
        return List.of(
                Permiso.CARGAR_ARPT,
                Permiso.CONTROLAR_EPP,
                Permiso.GESTIONAR_PLANES_ACCION,
                Permiso.GESTIONAR_VISITAS,
                Permiso.ADMINISTRAR_USUARIOS,
                Permiso.ADMINISTRAR_CATALOGOS
        );
    }
}