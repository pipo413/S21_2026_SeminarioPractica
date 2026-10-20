package ar.com.zyasoc.sighs.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase abstracta que representa a cualquier usuario del sistema.
 *
 * POO - Pilares justificados:
 * - Abstraccion: Usuario no se instancia; existen solo las subclases
 *   {@link UsuarioTecnico}, {@link UsuarioResponsable}, {@link UsuarioAdministrador}.
 * - Encapsulamiento: todos los atributos son private y se accede por getters/setters.
 * - Herencia: las 3 subclases extienden Usuario.
 * - Polimorfismo: el metodo abstracto {@link #calcularPermisos()} se redefine
 *   en cada subclase y devuelve una lista distinta de {@link Permiso}.
 *
 * Regla BCE (§11 del TP2, N29): las entities no concentran logica de negocio.
 * El unico metodo que vive aca es el que define el contrato polimorfico
 * (calcularPermisos). Todo el resto (validar password, etc.) vive en
 * ServicioAutenticacion del paquete control.
 */
public abstract class Usuario {

    private int idUsuario;
    private String nombreUsuario;
    private String passwordHash;
    private String salt;
    private String nombreCompleto;
    private EstadoUsuario estado;
    private int intentosFallidos;
    private int idCreatedBy;
    private int idUpdatedBy;
    private List<Rol> roles;

    public Usuario() {
        this.estado = EstadoUsuario.ACTIVO;
        this.intentosFallidos = 0;
        this.roles = new ArrayList<>();
    }

    /**
     * Metodo abstracto: cada subclase devuelve la lista de permisos
     * concretos segun su rol. Este es el ejemplo de polimorfismo.
     */
    public abstract List<Permiso> calcularPermisos();

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public int getIdCreatedBy() {
        return idCreatedBy;
    }

    public void setIdCreatedBy(int idCreatedBy) {
        this.idCreatedBy = idCreatedBy;
    }

    public int getIdUpdatedBy() {
        return idUpdatedBy;
    }

    public void setIdUpdatedBy(int idUpdatedBy) {
        this.idUpdatedBy = idUpdatedBy;
    }

    public List<Rol> getRoles() {
        return roles;
    }

    public void setRoles(List<Rol> roles) {
        this.roles = roles;
    }

    /** Conveniencia: devuelve el primer rol o null si no tiene. */
    public Rol getRolPrincipal() {
        return (roles == null || roles.isEmpty()) ? null : roles.get(0);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{id=" + idUsuario
                + ", nombreUsuario='" + nombreUsuario + '\''
                + ", nombreCompleto='" + nombreCompleto + '\''
                + ", estado=" + estado
                + ", intentosFallidos=" + intentosFallidos
                + ", roles=" + roles
                + '}';
    }
}