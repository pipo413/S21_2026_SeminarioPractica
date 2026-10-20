package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.control.ControladorAccion;
import ar.com.zyasoc.sighs.modelo.PlanAccion;
import ar.com.zyasoc.sighs.modelo.PlanAccion.Estado;
import ar.com.zyasoc.sighs.modelo.PlanAccion.TipoOrigen;
import ar.com.zyasoc.sighs.modelo.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del CU-ACC-01 (Gestionar planes de accion). Permite:
 *  - Listar los planes abiertos con su situacion RD-16.
 *  - Dar de alta un plan nuevo.
 *  - Modificar uno existente (incluido pasarlo a CUMPLIDO con fechaCierre).
 *  - Dar de baja logica un plan.
 */
public class VistaPlanAccion extends JFrame {

    private final ControladorAccion controlador;
    private final Usuario usuario;
    private final int idEmpresa;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Origen", "Descripcion", "Plazo", "Estado", "Situacion (RD-16)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);

    // panel superior: solo-lectura con info del plan seleccionado
    private final JLabel lblOrigenValor = new JLabel("-", SwingConstants.LEFT);
    private final JTextArea txtDescripcionValor = new JTextArea(2, 40);
    private final JLabel lblResponsableValor = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblIdValor = new JLabel("-", SwingConstants.LEFT);

    // panel de edicion: solo lo que el usuario puede cambiar
    private final JTextField txtFechaPlazo = new JTextField(LocalDate.now().plusDays(30).toString(), 10);
    private final JComboBox<Estado> cmbEstado = new JComboBox<>(Estado.values());

    private final JButton btnGuardar = new JButton("Guardar cambios");
    private final JButton btnBajaLogica = new JButton("Baja logica");
    private final JButton btnRefrescar = new JButton("Refrescar");
    private final JButton btnCerrar = new JButton("Cerrar");
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    private final List<PlanAccion> planesActuales = new ArrayList<>();
    private PlanAccion planSeleccionado = null;

    public VistaPlanAccion(Usuario usuario, int idEmpresa, ControladorAccion controlador) {
        super("SIGHS - Planes de accion (CU-ACC-01)");
        this.usuario = usuario;
        this.idEmpresa = idEmpresa;
        this.controlador = controlador;

        construirUi();
        wireEventos();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(900, 600));
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        // al inicio no hay plan seleccionado -> los botones de accion
        // quedan deshabilitados hasta que se seleccione uno de la grilla.
        btnGuardar.setEnabled(false);
        btnBajaLogica.setEnabled(false);

        cargarPlanes();
    }

    private void construirUi() {
        JLabel lblTitulo = new JLabel("Gestionar planes de accion (empresa #" + idEmpresa + ")",
                SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));

        // panel superior: info del plan (solo-lectura) + edicion de plazo/estado
        JPanel panelInfo = new JPanel(new GridLayout(2, 4, 5, 5));
        panelInfo.setBorder(BorderFactory.createTitledBorder("Detalle del plan (solo lectura)"));
        panelInfo.add(new JLabel("ID:"));
        panelInfo.add(lblIdValor);
        panelInfo.add(new JLabel("Origen:"));
        panelInfo.add(lblOrigenValor);
        panelInfo.add(new JLabel("Responsable:"));
        panelInfo.add(lblResponsableValor);
        panelInfo.add(new JLabel("Creado:"));
        panelInfo.add(new JLabel("(no editable)"));

        // descripcion completa en su propio panel (puede ser larga)
        JPanel panelDescripcion = new JPanel(new BorderLayout());
        panelDescripcion.setBorder(BorderFactory.createTitledBorder("Descripcion"));
        txtDescripcionValor.setLineWrap(true);
        txtDescripcionValor.setWrapStyleWord(true);
        txtDescripcionValor.setEditable(false);
        txtDescripcionValor.setBackground(new Color(245, 245, 245));
        panelDescripcion.add(new JScrollPane(txtDescripcionValor), BorderLayout.CENTER);

        // panel de edicion: lo unico que el usuario puede cambiar
        JPanel panelEdit = new JPanel(new GridLayout(2, 4, 5, 5));
        panelEdit.setBorder(BorderFactory.createTitledBorder("Edicion (modifique y guarde)"));
        panelEdit.add(new JLabel("Plazo (AAAA-MM-DD):"));
        panelEdit.add(txtFechaPlazo);
        panelEdit.add(new JLabel("Estado:"));
        panelEdit.add(cmbEstado);

        JPanel panelEncabezado = new JPanel();
        panelEncabezado.setLayout(new javax.swing.BoxLayout(panelEncabezado, javax.swing.BoxLayout.Y_AXIS));
        panelEncabezado.add(panelInfo);
        panelEncabezado.add(panelDescripcion);
        panelEncabezado.add(panelEdit);

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnGuardar);
        panelBotones.add(btnBajaLogica);
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnCerrar);

        lblMensaje.setForeground(Color.RED);
        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("Planes abiertos (RD-16)"));
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(280);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(130);
        // resaltar la celda de situacion segun RD-16
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object value,
                                                                   boolean isSelected, boolean hasFocus,
                                                                   int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (column == 5 && value != null) {
                    String s = value.toString();
                    if (s.contains("VENCIDO")) c.setForeground(Color.RED);
                    else if (s.contains("PROXIMO")) c.setForeground(new Color(200, 120, 0));
                    else c.setForeground(new Color(0, 100, 0));
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        panelTabla.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblTitulo, BorderLayout.NORTH);
        root.add(panelEncabezado, BorderLayout.PAGE_START);
        root.add(panelTabla, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelBotones, BorderLayout.NORTH);
        panelSur.add(lblMensaje, BorderLayout.CENTER);
        root.add(panelSur, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void wireEventos() {
        tabla.getSelectionModel().addListSelectionListener(e -> onTablaSeleccion());
        btnGuardar.addActionListener(e -> onGuardar());
        btnBajaLogica.addActionListener(e -> onBajaLogica());
        btnRefrescar.addActionListener(e -> cargarPlanes());
        btnCerrar.addActionListener(e -> dispose());
    }

    private void cargarPlanes() {
        new SwingWorker<List<PlanAccion>, Void>() {
            @Override
            protected List<PlanAccion> doInBackground() {
                try { return controlador.listarAbiertos(idEmpresa); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<PlanAccion> lista = get();
                    planesActuales.clear();
                    modelo.setRowCount(0);
                    if (lista != null) {
                        planesActuales.addAll(lista);
                        for (PlanAccion p : lista) {
                            modelo.addRow(new Object[]{
                                    p.getIdPlanAccion(),
                                    p.getTipoOrigen(),
                                    p.getDescripcion(),
                                    p.getFechaPlazo(),
                                    p.getEstado(),
                                    p.getSituacion()
                            });
                        }
                    }
                } catch (Exception ex) {
                    mostrarError("Error cargando planes: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onTablaSeleccion() {
        int row = tabla.getSelectedRow();
        if (row < 0) return;
        planSeleccionado = planesActuales.get(row);
        lblIdValor.setText("#" + planSeleccionado.getIdPlanAccion());
        lblOrigenValor.setText(planSeleccionado.getTipoOrigen() + " #" + planSeleccionado.getIdOrigen());
        lblResponsableValor.setText(planSeleccionado.getResponsable());
        txtDescripcionValor.setText(planSeleccionado.getDescripcion());
        txtFechaPlazo.setText(planSeleccionado.getFechaPlazo().toString());
        cmbEstado.setSelectedItem(planSeleccionado.getEstado());
        btnGuardar.setEnabled(true);
        btnBajaLogica.setEnabled(true);
    }

    private void limpiarFormulario() {
        planSeleccionado = null;
        tabla.clearSelection();
        lblIdValor.setText("-");
        lblOrigenValor.setText("-");
        lblResponsableValor.setText("-");
        txtDescripcionValor.setText("");
        txtFechaPlazo.setText(LocalDate.now().plusDays(30).toString());
        cmbEstado.setSelectedIndex(0);
        btnGuardar.setEnabled(false);
        btnBajaLogica.setEnabled(false);
    }

    private void onGuardar() {
        if (planSeleccionado == null) {
            mostrarError("Selecciona un plan de la tabla primero.");
            return;
        }
        LocalDate plazo;
        try { plazo = LocalDate.parse(txtFechaPlazo.getText().trim()); }
        catch (Exception ex) {
            mostrarError("Fecha invalida. Formato esperado AAAA-MM-DD.");
            return;
        }
        Estado estado = (Estado) cmbEstado.getSelectedItem();

        if (!controlador.validarFila(planSeleccionado.getDescripcion(), planSeleccionado.getResponsable(), plazo)) {
            mostrarError("Datos invalidos: descripcion vacia, responsable faltante o fecha en el pasado.");
            return;
        }

        planSeleccionado.setFechaPlazo(plazo);
        planSeleccionado.setEstado(estado);

        try {
            controlador.modificarPlan(usuario, planSeleccionado);
            mostrarMensaje("Plan #" + planSeleccionado.getIdPlanAccion() + " modificado.", new Color(0, 100, 0));
        } catch (SQLException ex) {
            mostrarError("Error al modificar: " + ex.getMessage());
            return;
        }
        cargarPlanes();
        limpiarFormulario();
    }

    private void onBajaLogica() {
        if (planSeleccionado == null) {
            mostrarError("Selecciona un plan de la tabla primero.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Dar de baja logica el plan #" + planSeleccionado.getIdPlanAccion() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            controlador.bajaLogicaPlan(usuario, planSeleccionado.getIdPlanAccion());
            mostrarMensaje("Plan #" + planSeleccionado.getIdPlanAccion() + " dado de baja logica.",
                    new Color(0, 100, 0));
            cargarPlanes();
            limpiarFormulario();
        } catch (SQLException ex) {
            mostrarError("Error: " + ex.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(Color.RED);
    }

    private void mostrarMensaje(String mensaje, Color color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(color);
    }
}