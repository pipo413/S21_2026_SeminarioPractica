package ar.com.zyasoc.sighs.modelo;

import java.util.List;

/**
 * Tecnico en Higiene y Seguridad. Puede cargar ARPTs, registrar
 * controles de EPP, gestionar planes de accion y consultar la lista
 * de visitas de la empresa (el ABM se hace via SQL directo en esta
 * version). No administra el sistema.
 */
public class UsuarioTecnico extends Usuario {

    @Override
    public List<Permiso> calcularPermisos() {
        return List.of(
                Permiso.CARGAR_ARPT,
                Permiso.CONTROLAR_EPP,
                Permiso.GESTIONAR_PLANES_ACCION,
                Permiso.GESTIONAR_VISITAS
        );
    }
}