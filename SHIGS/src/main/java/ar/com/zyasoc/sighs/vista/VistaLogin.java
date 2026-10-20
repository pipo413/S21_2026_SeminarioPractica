package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.control.ControladorUsuario;
import ar.com.zyasoc.sighs.control.ResultadoAutenticacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Pantalla de inicio de sesion (CU-USR-01).
 *
 * Swing puro, sin frameworks web (RNF-08). Lanza el flujo de login
 * llamando al ControladorUsuario y delega el resultado a un CallbackLogin
 * que la Main provee.
 */
public class VistaLogin extends JFrame {

    private final ControladorUsuario controlador;
    private final CallbackLogin callback;

    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);
    private final JButton btnIngresar = new JButton("Iniciar sesion");
    private final JButton btnCancelar = new JButton("Cancelar");

    public VistaLogin(ControladorUsuario controlador, CallbackLogin callback) {
        super("SIGHS - Iniciar sesion");
        this.controlador = controlador;
        this.callback = callback;

        construirUi();
        wireEventos();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(460, 360));
        setMinimumSize(new Dimension(460, 360));
        setLocationRelativeTo(null);
    }

    private void construirUi() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // si no se puede usar el L&F del sistema, sigue con el default
        }

        JLabel lblTitulo = new JLabel("SIGHS - Sistema de Gestion de HyS", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        panelForm.add(new JLabel("Usuario:"), c);
        c.gridx = 1;
        panelForm.add(txtUsuario, c);

        c.gridx = 0; c.gridy = 1;
        panelForm.add(new JLabel("Contrasena:"), c);
        c.gridx = 1;
        panelForm.add(txtPassword, c);

        JPanel panelBotones = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 10));
        btnIngresar.setPreferredSize(new Dimension(130, 30));
        btnCancelar.setPreferredSize(new Dimension(110, 30));
        panelBotones.add(btnIngresar);
        panelBotones.add(btnCancelar);

        lblMensaje.setForeground(Color.RED);
        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel panelSur = new JPanel();
        panelSur.setLayout(new javax.swing.BoxLayout(panelSur, javax.swing.BoxLayout.Y_AXIS));
        panelSur.add(panelBotones);
        panelSur.add(lblMensaje);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblTitulo, BorderLayout.NORTH);
        root.add(panelForm, BorderLayout.CENTER);
        root.add(panelSur, BorderLayout.SOUTH);

        setContentPane(root);

        SwingUtilities.invokeLater(() -> txtUsuario.requestFocusInWindow());
    }

    private void wireEventos() {
        btnIngresar.addActionListener(e -> onIngresar());
        btnCancelar.addActionListener(e -> cerrarYCancelar());
        txtPassword.addActionListener(e -> onIngresar());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cerrarYCancelar();
            }
        });
    }

    private void onIngresar() {
        String usuario = txtUsuario.getText().trim();
        char[] passChars = txtPassword.getPassword();
        String password = new String(passChars).trim();
        java.util.Arrays.fill(passChars, '\0');

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarMensaje("Complete usuario y contrasena", Color.RED);
            return;
        }

        btnIngresar.setEnabled(false);
        lblMensaje.setText("Conectando...");
        lblMensaje.setForeground(Color.DARK_GRAY);

        // ejecutamos el login en un hilo separado para no congelar la UI
        new SwingWorker<ResultadoAutenticacion, Void>() {
            @Override
            protected ResultadoAutenticacion doInBackground() {
                return controlador.autenticar(usuario, password);
            }

            @Override
            protected void done() {
                btnIngresar.setEnabled(true);
                try {
                    ResultadoAutenticacion r = get();
                    switch (r.getEstado()) {
                        case OK:
                            mostrarMensaje(r.getMensaje(), new Color(0, 100, 0));
                            // pequeno delay para que se vea el mensaje verde
                            Timer t = new Timer(500, e -> {
                                dispose();
                                if (callback != null) callback.onLoginOk(r);
                            });
                            t.setRepeats(false);
                            t.start();
                            break;
                        case CREDENCIALES_INVALIDAS:
                        case USUARIO_INACTIVO:
                            mostrarMensaje(r.getMensaje(), Color.RED);
                            txtPassword.setText("");
                            txtPassword.requestFocusInWindow();
                            break;
                        case ERROR_CONEXION:
                            mostrarMensaje(r.getMensaje(), Color.RED);
                            break;
                    }
                } catch (Exception ex) {
                    mostrarMensaje("Error inesperado: " + ex.getMessage(), Color.RED);
                }
            }
        }.execute();
    }

    private void mostrarMensaje(String mensaje, Color color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(color);
    }

    private void cerrarYCancelar() {
        dispose();
        if (callback != null) callback.onLoginCancelado();
    }
}