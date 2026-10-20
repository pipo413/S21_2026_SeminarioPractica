package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.modelo.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Placeholder para el ABM de Visitas (CU-VIS-ABM). Se accede desde
 * VistaGestiones porque necesita idEmpresa como contexto (la visita
 * se asocia siempre a UNA empresa).
 *
 * Por ahora esta pantalla solo informa al tecnico que las visitas se
 * cargan via SQL directo sobre {@code sighs_db.visita} (ver
 * tp2-tablas-zaragoza.sql, tabla visita). Cuando se implemente el
 * ABM real, esta clase se reemplaza por la pantalla de alta/listado
 * con fechas, tipo y estado, manteniendo el mismo constructor.
 */
public class VistaVisitas extends JFrame {

    public VistaVisitas(Usuario usuario, int idEmpresa) {
        super("SIGHS - Visitas (empresa #" + idEmpresa + ")");
        construirUi(usuario, idEmpresa);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(560, 300));
        setMinimumSize(new Dimension(560, 300));
        setLocationRelativeTo(null);
    }

    private void construirUi(Usuario usuario, int idEmpresa) {
        JLabel lblTitulo = new JLabel("Visitas (empresa #" + idEmpresa + ")", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel lblPendiente = new JLabel(
                "<html><center>El ABM de visitas se hace en esta version via SQL directo<br>"
                        + "(INSERT en la tabla 'visita' de sighs_db, ver tp2-tablas-zaragoza.sql).<br>"
                        + "La UI solo lista las ultimas 15 al abrir un ARPT o un Control de EPP.<br><br>"
                        + "Cuando se sume el ABM visual, esta pantalla pasa a tener alta y listado.</center></html>",
                SwingConstants.CENTER);
        lblPendiente.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        JPanel pb = new JPanel();
        pb.add(btnCerrar);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblTitulo, BorderLayout.NORTH);
        root.add(lblPendiente, BorderLayout.CENTER);
        root.add(pb, BorderLayout.SOUTH);
        setContentPane(root);
    }
}