package ar.com.zyasoc.sighs.modelo;

import java.time.LocalDate;

/**
 * Entity ControlEPP - cabecera del control de EPP en una visita.
 * Alineado con §11 del TP2 (entity ControlEPP).
 */
public class ControlEpp {

    public enum Estado { REGISTRADO, ANULADO }

    private int idControlEpp;
    private int idVisita;
    private int idPuesto;
    private int idTarea;
    private int idArpt;
    private LocalDate fecha;
    private Estado estado;

    public ControlEpp() {}

    public int getIdControlEpp() { return idControlEpp; }
    public void setIdControlEpp(int idControlEpp) { this.idControlEpp = idControlEpp; }

    public int getIdVisita() { return idVisita; }
    public void setIdVisita(int idVisita) { this.idVisita = idVisita; }

    public int getIdPuesto() { return idPuesto; }
    public void setIdPuesto(int idPuesto) { this.idPuesto = idPuesto; }

    public int getIdTarea() { return idTarea; }
    public void setIdTarea(int idTarea) { this.idTarea = idTarea; }

    public int getIdArpt() { return idArpt; }
    public void setIdArpt(int idArpt) { this.idArpt = idArpt; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}