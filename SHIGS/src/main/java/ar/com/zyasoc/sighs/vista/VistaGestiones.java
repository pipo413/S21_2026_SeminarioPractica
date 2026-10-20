package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.control.ControladorARPT;
import ar.com.zyasoc.sighs.control.ControladorAccion;
import ar.com.zyasoc.sighs.control.ControladorEPP;
import ar.com.zyasoc.sighs.modelo.Empresa;
import ar.com.zyasoc.sighs.modelo.Permiso;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.persistencia.EmpresaDAO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pantalla intermedia "Gestiones": pide la empresa y muestra los CU
 * disponibles para esa empresa segun los permisos del usuario.
 *
 * Cuando el usuario elige una opcion, se abre la vista del CU
 * correspondiente con idEmpresa ya seteado como contexto.
 */
public class VistaGestiones extends JFrame {

    private final Usuario usuario;
    private final JComboBox<Empresa> cmbEmpresa = new JComboBox<>();
    private final JPanel panelCu = new JPanel(new GridLayout(0, 2, 15, 15));
    private final JButton btnArpt = crearBoton("Cargar ARPT", "Elaborar/actualizar la planilla de evaluacion de riesgos", Permiso.CARGAR_ARPT);
    private final JButton btnEpp = crearBoton("Control de EPP", "Generar la planilla de control de EPP en una visita", Permiso.CONTROLAR_EPP);
    private final JButton btnPlanes = crearBoton("Planes de accion", "Gestionar planes de accion de la empresa", Permiso.GESTIONAR_PLANES_ACCION);
    private final JButton btnVisitas = crearBoton("Visitas", "Ver/listar las visitas de la empresa (ABM via SQL)", Permiso.GESTIONAR_VISITAS);
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    public VistaGestiones(Usuario usuario) {
        super("SIGHS - Gestiones");
        this.usuario = usuario;

        construirUi();
        wireEventos();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(700, 480));
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);

        cargarEmpresas();
    }

    private void construirUi() {
        JLabel lblTitulo = new JLabel("Gestiones (elija empresa y operacion)", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel panelEmpresa = new JPanel();
        panelEmpresa.setBorder(BorderFactory.createTitledBorder("Contexto"));
        panelEmpresa.add(new JLabel("Empresa:"));
        panelEmpresa.add(cmbEmpresa);

        JLabel lblCu = new JLabel("Operaciones disponibles (segun sus permisos):", SwingConstants.LEFT);
        lblCu.setFont(new Font("SansSerif", Font.PLAIN, 12));

        panelCu.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        for (JButton b : new JButton[]{btnArpt, btnEpp, btnPlanes, btnVisitas}) {
            panelCu.add(b);
        }

        lblMensaje.setForeground(Color.RED);
        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel panelEncabezado = new JPanel();
        panelEncabezado.setLayout(new javax.swing.BoxLayout(panelEncabezado, javax.swing.BoxLayout.Y_AXIS));
        panelEncabezado.add(panelEmpresa);
        panelEncabezado.add(lblCu);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblTitulo, BorderLayout.NORTH);
        root.add(panelEncabezado, BorderLayout.PAGE_START);
        root.add(panelCu, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(lblMensaje, BorderLayout.CENTER);
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        JPanel pb = new JPanel();
        pb.add(btnCerrar);
        panelSur.add(pb, BorderLayout.NORTH);
        root.add(panelSur, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void wireEventos() {
        cmbEmpresa.addActionListener(e -> onEmpresaSeleccionada());
        btnArpt.addActionListener(e -> abrirCu(() -> {
            Empresa emp = empresaSeleccionada();
            if (emp == null) return;
            VistaARPT vista = new VistaARPT(usuario, emp.getIdEmpresa(), new ControladorARPT());
            vista.setVisible(true);
        }, "ARPT"));
        btnEpp.addActionListener(e -> abrirCu(() -> {
            Empresa emp = empresaSeleccionada();
            if (emp == null) return;
            VistaControlEPP vista = new VistaControlEPP(usuario, emp.getIdEmpresa(), new ControladorEPP(), new ControladorAccion());
            vista.setVisible(true);
        }, "Control de EPP"));
        btnPlanes.addActionListener(e -> abrirCu(() -> {
            Empresa emp = empresaSeleccionada();
            if (emp == null) return;
            VistaPlanAccion vista = new VistaPlanAccion(usuario, emp.getIdEmpresa(), new ControladorAccion());
            vista.setVisible(true);
        }, "Planes de accion"));
        btnVisitas.addActionListener(e -> abrirCu(() -> {
            Empresa emp = empresaSeleccionada();
            if (emp == null) return;
            VistaVisitas vista = new VistaVisitas(usuario, emp.getIdEmpresa());
            vista.setVisible(true);
        }, "Visitas"));
    }

    private Empresa empresaSeleccionada() {
        return (Empresa) cmbEmpresa.getSelectedItem();
    }

    private void abrirCu(Runnable abrirVista, String nombre) {
        Empresa emp = empresaSeleccionada();
        if (emp == null) {
            mostrarError("Elija una empresa antes de abrir " + nombre + ".");
            return;
        }
        abrirVista.run();
    }

    private JButton crearBoton(String titulo, String subtitulo, Permiso permiso) {
        JButton b = new JButton("<html><center><b>" + titulo + "</b><br><span style='font-size:9px'>" + subtitulo + "</span></center></html>");
        b.putClientProperty("permiso", permiso);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return b;
    }

    private void cargarEmpresas() {
        new SwingWorker<List<Empresa>, Void>() {
            @Override
            protected List<Empresa> doInBackground() {
                try { return new EmpresaDAO().listarTodas(); }
                catch (Exception ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<Empresa> lista = get();
                    cmbEmpresa.removeAllItems();
                    cmbEmpresa.addItem(null);
                    if (lista != null) {
                        for (Empresa e : lista) cmbEmpresa.addItem(e);
                    }
                    actualizarBotonesPorPermisos();
                } catch (Exception ex) {
                    mostrarError("Error cargando empresas: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onEmpresaSeleccionada() {
        actualizarBotonesPorPermisos();
    }

    private void actualizarBotonesPorPermisos() {
        List<Permiso> permisos = usuario.calcularPermisos();
        btnArpt.setEnabled(permisos.contains(Permiso.CARGAR_ARPT));
        btnEpp.setEnabled(permisos.contains(Permiso.CONTROLAR_EPP));
        btnPlanes.setEnabled(permisos.contains(Permiso.GESTIONAR_PLANES_ACCION));
        btnVisitas.setEnabled(permisos.contains(Permiso.GESTIONAR_VISITAS));
        Empresa emp = empresaSeleccionada();
        boolean hayEmpresa = emp != null;
        if (!hayEmpresa) {
            for (JButton b : new JButton[]{btnArpt, btnEpp, btnPlanes, btnVisitas}) b.setEnabled(false);
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(Color.RED);
    }
}