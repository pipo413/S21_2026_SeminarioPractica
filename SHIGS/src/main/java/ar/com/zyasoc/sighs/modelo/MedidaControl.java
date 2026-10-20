package ar.com.zyasoc.sighs.modelo;

/**
 * Entity MedidaControl - catalogo de medidas de control aplicables a un riesgo.
 * Alineado con §11 del TP2.
 */
public class MedidaControl {

    private int idMedida;
    private String descripcion;
    private String tipo;

    public MedidaControl() {}

    public int getIdMedida() { return idMedida; }
    public void setIdMedida(int idMedida) { this.idMedida = idMedida; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() { return descripcion + " (" + tipo + ")"; }
}