package ar.com.zyasoc.sighs.modelo;

import java.time.LocalDate;

/**
 * Entity ARPT (Analisis de Riesgos del Puesto de Trabajo) - alineado con §11.
 */
public class Arpt {

    public enum Estado { EDICION, VIGENTE, HISTORICO }

    private int idArpt;
    private int idTarea;
    private int idVisita;
    private LocalDate fecha;
    private Estado estado;
    private String observacionesGenerales;
    private String conclusionGeneral;

    public Arpt() {}

    public int getIdArpt() { return idArpt; }
    public void setIdArpt(int idArpt) { this.idArpt = idArpt; }

    public int getIdTarea() { return idTarea; }
    public void setIdTarea(int idTarea) { this.idTarea = idTarea; }

    public int getIdVisita() { return idVisita; }
    public void setIdVisita(int idVisita) { this.idVisita = idVisita; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getObservacionesGenerales() { return observacionesGenerales; }
    public void setObservacionesGenerales(String observacionesGenerales) {
        this.observacionesGenerales = observacionesGenerales;
    }

    public String getConclusionGeneral() { return conclusionGeneral; }
    public void setConclusionGeneral(String conclusionGeneral) {
        this.conclusionGeneral = conclusionGeneral;
    }

    @Override
    public String toString() {
        return "ARPT#" + idArpt + " (" + estado + ")";
    }
}