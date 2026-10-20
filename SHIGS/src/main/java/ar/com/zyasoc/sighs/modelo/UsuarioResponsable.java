package ar.com.zyasoc.sighs.modelo;

import java.util.List;

/**
 * Responsable del Servicio. Tiene todos los permisos del Tecnico mas
 * la administracion de catalogos.
 */
public class UsuarioResponsable extends Usuario {

    @Override
    public List<Permiso> calcularPermisos() {
        return List.of(
                Permiso.CARGAR_ARPT,
                Permiso.CONTROLAR_EPP,
                Permiso.GESTIONAR_PLANES_ACCION,
                Permiso.GESTIONAR_VISITAS,
                Permiso.ADMINISTRAR_CATALOGOS
        );
    }
}