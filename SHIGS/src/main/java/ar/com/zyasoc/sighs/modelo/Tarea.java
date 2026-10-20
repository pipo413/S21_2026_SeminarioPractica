package ar.com.zyasoc.sighs.modelo;

/**
 * Entity Tarea - alineado con §11 del TP2.
 */
public class Tarea {

    private int idTarea;
    private int idPuesto;
    private String nombre;
    private String descripcion;

    public Tarea() {}

    public int getIdTarea() { return idTarea; }
    public void setIdTarea(int idTarea) { this.idTarea = idTarea; }

    public int getIdPuesto() { return idPuesto; }
    public void setIdPuesto(int idPuesto) { this.idPuesto = idPuesto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() { return nombre; }
}