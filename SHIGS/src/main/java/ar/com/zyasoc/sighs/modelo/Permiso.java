package ar.com.zyasoc.sighs.modelo;

/**
 * Permisos discretos del sistema. La lista de permisos por usuario se calcula
 * con polimorfismo: cada subclase de {@link Usuario} redefine
 * {@code calcularPermisos()} segun su rol.
 */
public enum Permiso {
    CARGAR_ARPT,
    CONTROLAR_EPP,
    GESTIONAR_PLANES_ACCION,
    GESTIONAR_VISITAS,
    ADMINISTRAR_USUARIOS,
    ADMINISTRAR_CATALOGOS
}