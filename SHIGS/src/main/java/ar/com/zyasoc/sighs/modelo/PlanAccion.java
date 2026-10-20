package ar.com.zyasoc.sighs.modelo;

import java.time.LocalDate;

/**
 * Entity PlanAccion - alineado con §11 del TP2.
 * Modelo puro (sin logica de negocio, BCE). El estado "vencido" /
 * "proximo a vencer" es derivado (RD-16) y se calcula en la vista o en
 * la consulta SQL, no se persiste.
 */
public class PlanAccion {

    public enum Estado { ENTRATAMIENTO, CUMPLIDO, NOCUMPLIDO }
    public enum TipoOrigen { ARPT, CONTROL_EPP, VISITA }

    private int idPlanAccion;
    private int idEmpresa;
    private TipoOrigen tipoOrigen;
    private int idOrigen;
    private String descripcion;
    /**
     * Nombre del responsable del plan: persona de la empresa cliente
     * (no necesariamente usuario del sistema). En el modelo del TP2
     * este campo era 'responsable' como VARCHAR libre, sin FK a
     * usuario. El prototipo respeta eso.
     */
    private String responsable;
    private LocalDate fechaPlazo;
    private Estado estado;
    private LocalDate fechaCierre;
    private boolean bajaLogica;

    public PlanAccion() {}

    public int getIdPlanAccion() { return idPlanAccion; }
    public void setIdPlanAccion(int idPlanAccion) { this.idPlanAccion = idPlanAccion; }

    public int getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(int idEmpresa) { this.idEmpresa = idEmpresa; }

    public TipoOrigen getTipoOrigen() { return tipoOrigen; }
    public void setTipoOrigen(TipoOrigen tipoOrigen) { this.tipoOrigen = tipoOrigen; }

    public int getIdOrigen() { return idOrigen; }
    public void setIdOrigen(int idOrigen) { this.idOrigen = idOrigen; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public LocalDate getFechaPlazo() { return fechaPlazo; }
    public void setFechaPlazo(LocalDate fechaPlazo) { this.fechaPlazo = fechaPlazo; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }

    public boolean isBajaLogica() { return bajaLogica; }
    public void setBajaLogica(boolean bajaLogica) { this.bajaLogica = bajaLogica; }

    /**
     * Situacion derivada segun RD-16:
     *  - VENCIDO si EnTratamiento y fechaPlazo < hoy.
     *  - PROXIMO_A_VENCER si EnTratamiento y fechaPlazo <= hoy + 15 dias.
     *  - VIGENTE en cualquier otro caso.
     */
    public String getSituacion() {
        if (estado == Estado.CUMPLIDO || estado == Estado.NOCUMPLIDO) {
            return "CERRADO";
        }
        LocalDate hoy = LocalDate.now();
        if (fechaPlazo.isBefore(hoy)) return "VENCIDO";
        if (!fechaPlazo.isAfter(hoy.plusDays(15))) return "PROXIMO_A_VENCER";
        return "VIGENTE";
    }
}