package ar.com.zyasoc.sighs.control;

import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo;
import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo.Clasificacion;

/**
 * Motor de evaluacion de riesgos (CU-ARPT-01). Implementa:
 *  - RD-04: GP = P * E * C con P, E, C en rango 1..5.
 *  - RD-05: clasificacion segun rangos de GP.
 *
 * Regla BCE: este motor es la unica clase que sabe calcular GP y
 * clasificar. Las entities EvaluacionRiesgo y EvaluacionRiesgo.Clasificacion
 * son puro dato.
 *
 * Rangos (alineados con el TP2 base, §13.1):
 *   GP <=  7  -> TOLERABLE
 *    8 <=  GP <= 12  -> PELIGROSO
 *   13 <=  GP <= 15  -> URGENTE
 *   GP >= 16  -> CRITICO
 */
public class MotorEvaluacionRiesgo {

    /**
     * Evalua una evaluacion rellenando GP y clasificacion a partir de P, E, C.
     * Devuelve la misma instancia con los campos calculados. Los rangos de
     * P/E/C (1..5) y la clasificacion por rangos de GP viven en este metodo
     * y solo aca (BCE: el motor es el unico que sabe la formula).
     */
    public EvaluacionRiesgo evaluar(EvaluacionRiesgo ev) {
        validarRango(ev.getProbabilidad(), "Probabilidad");
        validarRango(ev.getExposicion(), "Exposicion");
        validarRango(ev.getConsecuencia(), "Consecuencia");
        int gp = ev.getProbabilidad() * ev.getExposicion() * ev.getConsecuencia();
        ev.setGradoPeligrosidad(gp);
        ev.setClasificacion(clasificar(gp));
        return ev;
    }

    /**
     * Clasifica un GP segun los rangos declarados. Privado: vive solo para
     * que {@link #evaluar(EvaluacionRiesgo)} sea legible.
     */
    private Clasificacion clasificar(int gp) {
        if (gp <= 7) return Clasificacion.TOLERABLE;
        if (gp <= 12) return Clasificacion.PELIGROSO;
        if (gp <= 15) return Clasificacion.URGENTE;
        return Clasificacion.CRITICO;
    }

    private void validarRango(int v, String campo) {
        if (v < 1 || v > 5) {
            throw new IllegalArgumentException(campo + " fuera de rango (1..5).");
        }
    }
}