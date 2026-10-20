package ar.com.zyasoc.sighs.modelo;

/**
 * Entity ControlEPPDetalle - una fila por cada EPP controlado.
 * Alineado con §11 del TP2 (entity ControlEPPDetalle).
 */
public class ControlEppDetalle {

    private int idDetalle;
    private int idControlEpp;
    private int idEvaluacionEpp;
    private int valorUso;
    private int valorEstado;
    private String observaciones;
    private String recomendaciones;

    public ControlEppDetalle() {}

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    public int getIdControlEpp() { return idControlEpp; }
    public void setIdControlEpp(int idControlEpp) { this.idControlEpp = idControlEpp; }

    public int getIdEvaluacionEpp() { return idEvaluacionEpp; }
    public void setIdEvaluacionEpp(int idEvaluacionEpp) { this.idEvaluacionEpp = idEvaluacionEpp; }

    public int getValorUso() { return valorUso; }
    public void setValorUso(int valorUso) { this.valorUso = valorUso; }

    public int getValorEstado() { return valorEstado; }
    public void setValorEstado(int valorEstado) { this.valorEstado = valorEstado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(String recomendaciones) { this.recomendaciones = recomendaciones; }
}