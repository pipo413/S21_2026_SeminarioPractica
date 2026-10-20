package ar.com.zyasoc.sighs.modelo;

/**
 * Entity EvaluacionRiesgo - cada fila del ARPT: riesgo + P, E, C + GP
 * calculado + clasificacion. Alineado con §11 del TP2.
 *
 * Regla BCE: el calculo del GP y la clasificacion NO viven aca, los hace
 * {@link MotorEvaluacionRiesgo} en el paquete control.
 */
public class EvaluacionRiesgo {

    public enum Clasificacion { TOLERABLE, PELIGROSO, URGENTE, CRITICO }

    private int idEvaluacion;
    private int idArpt;
    private int idRiesgo;
    private Integer idMedidaControl;
    private int probabilidad;
    private int exposicion;
    private int consecuencia;
    private int gradoPeligrosidad;
    private Clasificacion clasificacion;

    public EvaluacionRiesgo() {}

    public int getIdEvaluacion() { return idEvaluacion; }
    public void setIdEvaluacion(int idEvaluacion) { this.idEvaluacion = idEvaluacion; }

    public int getIdArpt() { return idArpt; }
    public void setIdArpt(int idArpt) { this.idArpt = idArpt; }

    public int getIdRiesgo() { return idRiesgo; }
    public void setIdRiesgo(int idRiesgo) { this.idRiesgo = idRiesgo; }

    public Integer getIdMedidaControl() { return idMedidaControl; }
    public void setIdMedidaControl(Integer idMedidaControl) { this.idMedidaControl = idMedidaControl; }

    public int getProbabilidad() { return probabilidad; }
    public void setProbabilidad(int probabilidad) { this.probabilidad = probabilidad; }

    public int getExposicion() { return exposicion; }
    public void setExposicion(int exposicion) { this.exposicion = exposicion; }

    public int getConsecuencia() { return consecuencia; }
    public void setConsecuencia(int consecuencia) { this.consecuencia = consecuencia; }

    public int getGradoPeligrosidad() { return gradoPeligrosidad; }
    public void setGradoPeligrosidad(int gradoPeligrosidad) { this.gradoPeligrosidad = gradoPeligrosidad; }

    public Clasificacion getClasificacion() { return clasificacion; }
    public void setClasificacion(Clasificacion clasificacion) { this.clasificacion = clasificacion; }
}