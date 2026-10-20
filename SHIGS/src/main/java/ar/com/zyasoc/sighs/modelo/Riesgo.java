package ar.com.zyasoc.sighs.modelo;

/**
 * Entity Riesgo - catalogo de riesgos especificos.
 * Alineado con §11 del TP2.
 */
public class Riesgo {

    private int idRiesgo;
    private int idTipoRiesgo;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean vigente;

    public Riesgo() {}

    public int getIdRiesgo() { return idRiesgo; }
    public void setIdRiesgo(int idRiesgo) { this.idRiesgo = idRiesgo; }

    public int getIdTipoRiesgo() { return idTipoRiesgo; }
    public void setIdTipoRiesgo(int idTipoRiesgo) { this.idTipoRiesgo = idTipoRiesgo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isVigente() { return vigente; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }

    @Override
    public String toString() { return codigo + " - " + nombre; }
}