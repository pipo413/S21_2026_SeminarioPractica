package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.modelo.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Menu principal con 2 botones grandes: Gestiones (los CU que necesitan
 * empresa como contexto) y ABMs (catalogo: empresa, sectores, puestos,
 * tareas, usuarios). "Informes" se movio a Gestiones porque necesita
 * idEmpresa como contexto.
 */
public class VistaMenuPrincipal extends JFrame {

    private final Usuario usuario;

    public VistaMenuPrincipal(Usuario usuario) {
        super("SIGHS - Menu principal");
        this.usuario = usuario;
        construirUi();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(560, 360));
        setMinimumSize(new Dimension(560, 360));
        setLocationRelativeTo(null);
    }

    private void construirUi() {
        JLabel lblBienvenida = new JLabel(
                "Bienvenido, " + usuario.getNombreCompleto() + " (" + usuario.getClass().getSimpleName() + ")",
                SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel lblSubtitulo = new JLabel("Elegi una operacion:", SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton btnGestiones = crearBoton("Gestiones",
                "ARPT, Control de EPP, Planes de accion, Informes\n(elegir empresa y CU)");
        JButton btnAbms = crearBoton("ABMs",
                "Empresa, sectores, puestos, tareas, usuarios\n(en construccion)");

        btnGestiones.addActionListener(e -> abrirGestiones());
        btnAbms.addActionListener(e -> abrirAbms());

        JPanel panelBotones = new JPanel(new GridLayout(2, 1, 15, 15));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));
        panelBotones.add(btnGestiones);
        panelBotones.add(btnAbms);

        JLabel lblPie = new JLabel(
                "Usuario: " + usuario.getNombreUsuario() +
                        "  |  Rol: " + usuario.getClass().getSimpleName() +
                        "  |  Permisos: " + usuario.calcularPermisos().size(),
                SwingConstants.CENTER);
        lblPie.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblPie.setForeground(Color.DARK_GRAY);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblBienvenida, BorderLayout.NORTH);
        root.add(lblSubtitulo, BorderLayout.PAGE_START);
        root.add(panelBotones, BorderLayout.CENTER);
        root.add(lblPie, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JButton crearBoton(String titulo, String subtitulo) {
        JButton b = new JButton();
        b.setLayout(new java.awt.BorderLayout());
        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel lblSubtitulo = new JLabel(subtitulo, SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblSubtitulo.setForeground(Color.DARK_GRAY);
        JPanel panel = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        panel.add(lblTitulo);
        panel.add(lblSubtitulo);
        b.add(panel, java.awt.BorderLayout.CENTER);
        return b;
    }

    private void abrirGestiones() {
        VistaGestiones vista = new VistaGestiones(usuario);
        vista.setVisible(true);
    }

    /**
     * Placeholder: los ABMs (empresa, sectores, puestos, tareas, usuarios)
     * son la siguiente iteracion. Mientras tanto mostramos un cartel para
     * que el boton no parezca "muerto".
     */
    private void abrirAbms() {
        JOptionPane.showMessageDialog(this,
                "Los ABMs del catalogo (empresa, sectores, puestos, tareas, "
                        + "usuarios) se implementan en la proxima iteracion.\n"
                        + "Por ahora los catalogos viven en los scripts SQL "
                        + "(scripts/01-...).",
                "En construccion",
                JOptionPane.INFORMATION_MESSAGE);
    }
}