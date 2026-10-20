package ar.com.zyasoc.sighs.modelo;

/**
 * Entity Sector - alineado con §11 del TP2.
 *
 * La jerarquia real es Empresa -> Establecimiento -> Sector, asi
 * que el FK relevante es id_establecimiento, no id_empresa.
 */
public class Sector {

    private int idSector;
    private int idEstablecimiento;
    private String nombre;

    public Sector() {}

    public int getIdSector() { return idSector; }
    public void setIdSector(int idSector) { this.idSector = idSector; }

    public int getIdEstablecimiento() { return idEstablecimiento; }
    public void setIdEstablecimiento(int idEstablecimiento) { this.idEstablecimiento = idEstablecimiento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() { return nombre; }
}