package ar.com.zyasoc.sighs.modelo;

/**
 * Entity EPP (Elemento de Proteccion Personal) - alineado con §11 del TP2.
 */
public class Epp {

    private int idEpp;
    private String nombre;
    private String descripcion;

    public Epp() {}

    public int getIdEpp() { return idEpp; }
    public void setIdEpp(int idEpp) { this.idEpp = idEpp; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() { return nombre; }
}