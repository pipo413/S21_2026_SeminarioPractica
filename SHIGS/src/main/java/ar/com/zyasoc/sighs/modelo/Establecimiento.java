package ar.com.zyasoc.sighs.modelo;

/**
 * Entity Establecimiento - nivel intermedio entre Empresa y Sector
 * en la jerarquia Empresa -> Establecimiento -> Sector -> Puesto ->
 * Tarea. Un establecimiento tiene un tipo (FABRICA, DEPOSITO,
 * OFICINA, OTRO) y un domicilio, lo que permite representar plantas
 * geograficamente separadas de una misma empresa.
 */
public class Establecimiento {

    /** Tipo declarado en el DDL (columna {@code tipo} de establecimiento). */
    public enum Tipo { FABRICA, DEPOSITO, OFICINA, OTRO }

    private int idEstablecimiento;
    private int idEmpresa;
    private String nombre;
    private Tipo tipo;
    private String domicilio;

    public Establecimiento() {}

    public int getIdEstablecimiento() { return idEstablecimiento; }
    public void setIdEstablecimiento(int idEstablecimiento) { this.idEstablecimiento = idEstablecimiento; }

    public int getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(int idEmpresa) { this.idEmpresa = idEmpresa; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ")";
    }
}