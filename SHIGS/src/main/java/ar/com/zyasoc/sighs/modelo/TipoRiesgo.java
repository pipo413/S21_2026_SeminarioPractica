package ar.com.zyasoc.sighs.modelo;

/**
 * Entity TipoRiesgo - catalogo de tipos de riesgo del ARPT.
 * Agrupa tanto las familias (Fisicos, Quimicos, Ergonomicos, ...) como
 * los subtipos concretos (Electrico, Incendio, Proyeccion, Corte).
 * Alineado con la tabla tipo_riesgo del modelo ampliado del TP2.
 */
public class TipoRiesgo {

    private int idTipoRiesgo;
    private String nombre;
    /** Familia a la que pertenece el subtipo; null si esta fila es una familia. */
    private Integer idPadre;

    public TipoRiesgo() {}

    public int getIdTipoRiesgo() { return idTipoRiesgo; }
    public void setIdTipoRiesgo(int idTipoRiesgo) { this.idTipoRiesgo = idTipoRiesgo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getIdPadre() { return idPadre; }
    public void setIdPadre(Integer idPadre) { this.idPadre = idPadre; }

    @Override
    public String toString() { return nombre; }
}