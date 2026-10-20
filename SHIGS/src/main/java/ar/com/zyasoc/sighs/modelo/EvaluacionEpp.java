package ar.com.zyasoc.sighs.modelo;

/**
 * Entity EvaluacionEPP - tabla puente entre EvaluacionRiesgo y EPP.
 * Alineado con §11 del TP2 (entity EvaluacionEPP).
 */
public class EvaluacionEpp {

    private int idEvaluacionEpp;
    private int idEvaluacion;
    private int idEpp;

    public EvaluacionEpp() {}

    public int getIdEvaluacionEpp() { return idEvaluacionEpp; }
    public void setIdEvaluacionEpp(int idEvaluacionEpp) { this.idEvaluacionEpp = idEvaluacionEpp; }

    public int getIdEvaluacion() { return idEvaluacion; }
    public void setIdEvaluacion(int idEvaluacion) { this.idEvaluacion = idEvaluacion; }

    public int getIdEpp() { return idEpp; }
    public void setIdEpp(int idEpp) { this.idEpp = idEpp; }
}