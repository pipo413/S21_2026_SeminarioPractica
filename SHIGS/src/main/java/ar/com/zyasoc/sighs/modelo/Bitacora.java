package ar.com.zyasoc.sighs.modelo;

import java.time.LocalDateTime;

/**
 * Entity Bitacora - registra operaciones criticas del sistema.
 * Alineado con §11 del TP2 (entity Bitacora).
 */
public class Bitacora {

    private long idBitacora;
    private LocalDateTime fechaHora;
    private int idUsuario;
    private String operacion;
    private Integer idEmpresa;
    private String detalle;
    private String resultado;

    public Bitacora() {}

    public Bitacora(LocalDateTime fechaHora, int idUsuario, String operacion,
                    Integer idEmpresa, String detalle, String resultado) {
        this.fechaHora = fechaHora;
        this.idUsuario = idUsuario;
        this.operacion = operacion;
        this.idEmpresa = idEmpresa;
        this.detalle = detalle;
        this.resultado = resultado;
    }

    public long getIdBitacora() {
        return idBitacora;
    }

    public void setIdBitacora(long idBitacora) {
        this.idBitacora = idBitacora;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Integer idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    @Override
    public String toString() {
        return "Bitacora{id=" + idBitacora
                + ", fechaHora=" + fechaHora
                + ", idUsuario=" + idUsuario
                + ", operacion='" + operacion + '\''
                + ", resultado='" + resultado + '\''
                + '}';
    }
}