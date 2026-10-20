package ar.com.zyasoc.sighs.modelo;

import java.time.LocalDate;

/**
 * Entity Visita - alineado con la tabla {@code visita} del TP2 (DDL en
 * tp2-tablas-zaragoza.sql, seccion 'Tabla visita').
 *
 * Una visita pertence a UNA empresa (FK {@code id_empresa}) y a UN
 * usuario responsable (FK {@code id_usuario}). El prototipo trabaja con
 * el {@code tipo} y el {@code estado} como enums Java para mantener
 * consistencia con los valores validos en la base.
 */
public class Visita {

    /** Tipos de visita definidos en el DDL. */
    public enum Tipo { PROGRAMADA, SEGUIMIENTO, EXTRAORDINARIA }

    /** Estados posibles del ciclo de vida de una visita. */
    public enum Estado { PLANIFICADA, EN_CURSO, CERRADA, CANCELADA }

    private int idVisita;
    private int idEmpresa;
    private int idUsuario;
    private LocalDate fecha;
    private Tipo tipo;
    private Estado estado;
    private String observaciones;

    public Visita() {}

    public int getIdVisita() { return idVisita; }
    public void setIdVisita(int idVisita) { this.idVisita = idVisita; }

    public int getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(int idEmpresa) { this.idEmpresa = idEmpresa; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    /**
     * Etiqueta corta para combos en la UI: "YYYY-MM-DD - TIPO".
     * El id se expone aparte via getIdVisita().
     */
    @Override
    public String toString() {
        return fecha + " - " + tipo;
    }
}