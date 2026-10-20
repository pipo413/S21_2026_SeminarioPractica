package ar.com.zyasoc.sighs.modelo;

/**
 * Entity Empresa - alineado con §11 del TP2.
 * Modelo puro (sin logica, BCE).
 */
public class Empresa {

    private int idEmpresa;
    private String razonSocial;
    private String cuit;
    private String domicilio;
    private String nombreReferente;
    private String cargoReferente;
    private String emailReferente;

    public Empresa() {}

    public int getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(int idEmpresa) { this.idEmpresa = idEmpresa; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getNombreReferente() { return nombreReferente; }
    public void setNombreReferente(String nombreReferente) { this.nombreReferente = nombreReferente; }

    public String getCargoReferente() { return cargoReferente; }
    public void setCargoReferente(String cargoReferente) { this.cargoReferente = cargoReferente; }

    public String getEmailReferente() { return emailReferente; }
    public void setEmailReferente(String emailReferente) { this.emailReferente = emailReferente; }

    @Override
    public String toString() {
        return razonSocial + " (" + cuit + ")";
    }
}