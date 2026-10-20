package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.control.ControladorAccion;
import ar.com.zyasoc.sighs.control.ControladorEPP;
import ar.com.zyasoc.sighs.control.ControladorEPP.EppControlado;
import ar.com.zyasoc.sighs.control.ControladorEPP.FilaControl;
import ar.com.zyasoc.sighs.modelo.Arpt;
import ar.com.zyasoc.sighs.modelo.PlanAccion;
import ar.com.zyasoc.sighs.modelo.PlanAccion.TipoOrigen;
import ar.com.zyasoc.sighs.modelo.Puesto;
import ar.com.zyasoc.sighs.modelo.Sector;
import ar.com.zyasoc.sighs.modelo.Tarea;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.modelo.Visita;
import ar.com.zyasoc.sighs.modelo.Establecimiento;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
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
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla del CU-EPP-01: Generar control de EPP en una visita.
 *
 * Flujo:
 *   1. Combo de sector -> carga puestos.
 *   2. Combo de puesto -> carga tareas.
 *   3. Combo de tarea -> busca ARPT vigente y carga planilla.
 *   4. El usuario completa valorUso/valorEstado por cada EPP.
 *   5. Boton Guardar -> transaccion ACID (RNF-01).
 */
public class VistaControlEPP extends JFrame {

    private final ControladorEPP controlador;
    private final ControladorAccion controladorAccion;
    private final Usuario usuario;
    private final int idEmpresa;

    private final JComboBox<Visita> cmbVisita = new JComboBox<>();
    private final JComboBox<Establecimiento> cmbEstablecimiento = new JComboBox<>();
    private final JComboBox<Sector> cmbSector = new JComboBox<>();
    private final JComboBox<Puesto> cmbPuesto = new JComboBox<>();
    private final JComboBox<Tarea> cmbTarea = new JComboBox<>();
    private final JLabel lblArpt = new JLabel("Seleccione sector / puesto / tarea", SwingConstants.LEFT);
    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"EPP", "valorUso (1..5)", "valorEstado (1..5)", "Observaciones", "Recomendaciones"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // solo las 4 columnas editables (a partir de la 2da)
            return column >= 1;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnCrearPlan = new JButton("Crear plan de accion");
    private final JButton btnCancelar = new JButton("Cancelar");
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    private final Map<Integer, FilaControl> filasPorIdEvaluacionEpp = new HashMap<>();
    private Arpt arptActual;
    /** id del control EPP guardado por la ultima operacion Guardar exitosa. 0 si nunca se guardo. */
    private int idControlGuardado = 0;

    public VistaControlEPP(Usuario usuario, int idEmpresa, ControladorEPP controlador) {
        this(usuario, idEmpresa, controlador, new ControladorAccion());
    }

    public VistaControlEPP(Usuario usuario, int idEmpresa,
                           ControladorEPP controlador,
                           ControladorAccion controladorAccion) {
        super("SIGHS - Control de EPP (CU-EPP-01)");
        this.usuario = usuario;
        this.idEmpresa = idEmpresa;
        this.controlador = controlador;
        this.controladorAccion = controladorAccion;

        construirUi();
        wireEventos();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(820, 760));
        setMinimumSize(new Dimension(820, 760));
        setLocationRelativeTo(null);

        cargarVisitas();
        cargarEstablecimientos();
    }

    private void construirUi() {
        JLabel lblTitulo = new JLabel("Generar control de EPP (elija visita, sector, puesto y tarea)",
                SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel panelContexto = new JPanel();
        panelContexto.setBorder(BorderFactory.createTitledBorder("Contexto"));
        // ancho preferido mayor para que entre el texto completo de las opciones
        cmbVisita.setPreferredSize(new Dimension(220, 26));
        cmbSector.setPreferredSize(new Dimension(220, 26));
        cmbPuesto.setPreferredSize(new Dimension(220, 26));
        cmbTarea.setPreferredSize(new Dimension(320, 26));
        panelContexto.add(new JLabel("Visita:"));
        panelContexto.add(cmbVisita);
        panelContexto.add(new JLabel("Establecimiento:"));
        panelContexto.add(cmbEstablecimiento);
        panelContexto.add(new JLabel("Sector:"));
        panelContexto.add(cmbSector);
        panelContexto.add(new JLabel("Puesto:"));
        panelContexto.add(cmbPuesto);
        panelContexto.add(new JLabel("Tarea:"));
        panelContexto.add(cmbTarea);

        // panel de referencia de la escala cualitativa 1..5
        JPanel panelEscala = new JPanel(new java.awt.GridLayout(1, 6, 4, 0));
        panelEscala.setBorder(BorderFactory.createTitledBorder("Escala de puntaje (1 a 5)"));
        panelEscala.add(celdaEscala("1", "Nunca / Deteriorado"));
        panelEscala.add(celdaEscala("2", "Ocasional / Muy usado"));
        panelEscala.add(celdaEscala("3", "A veces / Aceptable"));
        panelEscala.add(celdaEscala("4", "Casi siempre / Bueno"));
        panelEscala.add(celdaEscala("5", "Siempre / Excelente"));
        // la sexta columna es para una celda vacia de cierre, ver celdaEscala
        panelEscala.add(new JLabel(""));

        JPanel panelArpt = new JPanel(new BorderLayout());
        panelArpt.setBorder(BorderFactory.createTitledBorder("ARPT"));
        panelArpt.add(lblArpt, BorderLayout.CENTER);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("EPPs a controlar (del ARPT vigente)"));
        tabla.setRowHeight(28);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(180);
        // cell editor: JTextField con NumberFormatter que restringe a 1..5
        // (DefaultCellEditor SI acepta JTextField)
        tabla.getColumnModel().getColumn(1).setCellEditor(
                new javax.swing.DefaultCellEditor(crearCampoNumerico1a5()));
        tabla.getColumnModel().getColumn(2).setCellEditor(
                new javax.swing.DefaultCellEditor(crearCampoNumerico1a5()));
        panelTabla.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCrearPlan);
        panelBotones.add(btnCancelar);

        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblMensaje.setForeground(java.awt.Color.RED);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelEncabezado = new JPanel();
        panelEncabezado.setLayout(new javax.swing.BoxLayout(panelEncabezado, javax.swing.BoxLayout.Y_AXIS));
        panelEncabezado.add(panelContexto);
        panelEncabezado.add(panelEscala);
        panelEncabezado.add(panelArpt);
        root.add(panelEncabezado, BorderLayout.PAGE_START);

        root.add(panelTabla, BorderLayout.CENTER);
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelBotones, BorderLayout.NORTH);
        panelSur.add(lblMensaje, BorderLayout.CENTER);
        root.add(panelSur, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void wireEventos() {
        cmbEstablecimiento.addActionListener(e -> onEstablecimientoSeleccionado());
        cmbSector.addActionListener(e -> onSectorSeleccionado());
        cmbPuesto.addActionListener(e -> onPuestoSeleccionado());
        cmbTarea.addActionListener(e -> onTareaSeleccionada());
        btnGuardar.addActionListener(e -> onGuardar());
        btnCrearPlan.addActionListener(e -> onCrearPlan());
        btnCancelar.addActionListener(e -> dispose());

        // btnCrearPlan arranca deshabilitado. Se habilita recien cuando hay
        // un control guardado (idControlGuardado > 0), porque el plan
        // necesita id_origen = id_control_epp y ese id solo existe despues
        // del Guardar exitoso.
        btnCrearPlan.setEnabled(false);
    }

    private void cargarVisitas() {
        new SwingWorker<List<Visita>, Void>() {
            @Override
            protected List<Visita> doInBackground() {
                try { return controlador.listarVisitasRecientes(idEmpresa, 15); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<Visita> lista = get();
                    cmbVisita.removeAllItems();
                    cmbVisita.addItem(null);
                    if (lista != null) for (Visita v : lista) cmbVisita.addItem(v);
                } catch (Exception ex) {
                    mostrarError("Error cargando visitas: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Visita visitaSeleccionada() {
        return (Visita) cmbVisita.getSelectedItem();
    }

    private void cargarEstablecimientos() {
        new SwingWorker<List<Establecimiento>, Void>() {
            @Override
            protected List<Establecimiento> doInBackground() {
                try { return controlador.listarEstablecimientos(idEmpresa); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<Establecimiento> lista = get();
                    Establecimiento autoSeleccion = null;
                    cmbEstablecimiento.removeAllItems();
                    cmbEstablecimiento.addItem(null);
                    if (lista != null) for (Establecimiento es : lista) cmbEstablecimiento.addItem(es);
                    // Guardamos la auto-seleccion para hacerla explicita abajo,
                    // despues de que el combo termine de poblarse. Asi el listener
                    // de cmbEstablecimiento (que llama a cargarSectores) corre
                    // cuando el repoblamiento ya termino.
                    if (lista != null && lista.size() == 1) {
                        autoSeleccion = lista.get(0);
                    }
                    if (autoSeleccion != null) {
                        cmbEstablecimiento.setSelectedItem(autoSeleccion);
                    }
                } catch (Exception ex) {
                    mostrarError("Error cargando establecimientos: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Establecimiento establecimientoSeleccionado() {
        return (Establecimiento) cmbEstablecimiento.getSelectedItem();
    }

    private void onEstablecimientoSeleccionado() {
        cargarSectores();
    }

    private void cargarSectores() {
        final Establecimiento est = establecimientoSeleccionado();
        if (est == null) {
            cmbSector.removeAllItems();
            cmbSector.addItem(null);
            cmbPuesto.removeAllItems();
            cmbPuesto.addItem(null);
            cmbTarea.removeAllItems();
            cmbTarea.addItem(null);
            return;
        }
        final int idEst = est.getIdEstablecimiento();
        new SwingWorker<List<Sector>, Void>() {
            @Override
            protected List<Sector> doInBackground() {
                try {
                    return controlador.listarSectoresPorEstablecimiento(idEst);
                } catch (SQLException ex) {
                    return null;
                }
            }
            @Override
            protected void done() {
                try {
                    List<Sector> lista = get();
                    cmbSector.removeAllItems();
                    cmbSector.addItem(null);
                    if (lista != null) for (Sector s : lista) cmbSector.addItem(s);
                } catch (Exception ex) {
                    mostrarError("Error cargando sectores: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onSectorSeleccionado() {
        Sector s = (Sector) cmbSector.getSelectedItem();
        if (s == null) return;
        // reset en cascada: al cambiar sector, limpio puesto y tarea
        cmbPuesto.removeAllItems();
        cmbPuesto.addItem(null);
        cmbTarea.removeAllItems();
        cmbTarea.addItem(null);
        modeloTabla.setRowCount(0);
        lblArpt.setText("Seleccione sector / puesto / tarea");
        btnGuardar.setEnabled(false);
        arptActual = null;

        new SwingWorker<List<Puesto>, Void>() {
            @Override
            protected List<Puesto> doInBackground() {
                try { return controlador.listarPuestos(s.getIdSector()); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<Puesto> lista = get();
                    if (lista != null) for (Puesto p : lista) cmbPuesto.addItem(p);
                } catch (Exception ex) {
                    mostrarError("Error cargando puestos: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onPuestoSeleccionado() {
        Puesto p = (Puesto) cmbPuesto.getSelectedItem();
        if (p == null) return;
        // reset en cascada: al cambiar puesto, limpio tarea y tabla
        cmbTarea.removeAllItems();
        cmbTarea.addItem(null);
        modeloTabla.setRowCount(0);
        lblArpt.setText("Seleccione sector / puesto / tarea");
        btnGuardar.setEnabled(false);
        arptActual = null;

        new SwingWorker<List<Tarea>, Void>() {
            @Override
            protected List<Tarea> doInBackground() {
                try { return controlador.listarTareas(p.getIdPuesto()); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<Tarea> lista = get();
                    if (lista != null) for (Tarea t : lista) cmbTarea.addItem(t);
                } catch (Exception ex) {
                    mostrarError("Error cargando tareas: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onTareaSeleccionada() {
        Tarea t = (Tarea) cmbTarea.getSelectedItem();
        if (t == null) return;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    arptActual = controlador.buscarArptVigente(t.getIdTarea());
                    if (arptActual == null) return null;
                    List<EppControlado> planilla = controlador.cargarPlanilla(arptActual.getIdArpt());
                    filasPorIdEvaluacionEpp.clear();
                    modeloTabla.setRowCount(0);
                    // Las celdas arrancan vacias: el tecnico carga los valores
                    // reales de la inspeccion. No pre-poblamos con datos
                    // anteriores para no confundir (los valores previos van en
                    // una vista de historial aparte, no en esta).
                    for (EppControlado ec : planilla) {
                        FilaControl f = new FilaControl(ec.idEvaluacionEpp);
                        filasPorIdEvaluacionEpp.put(ec.idEvaluacionEpp, f);
                        modeloTabla.addRow(new Object[]{
                                ec.epp.getNombre() + " (" + ec.epp.getDescripcion() + ")",
                                null, null, "", ""
                        });
                    }
                    return null;
                } catch (SQLException ex) {
                    return null;
                }
            }
            @Override
            protected void done() {
                if (arptActual == null) {
                    lblArpt.setText("No hay ARPT VIGENTE para esta tarea. Actualice el ARPT primero.");
                    modeloTabla.setRowCount(0);
                    btnGuardar.setEnabled(false);
                } else {
                    lblArpt.setText("ARPT #" + arptActual.getIdArpt() + " (" + arptActual.getEstado() + ") - " + arptActual.getFecha());
                    btnGuardar.setEnabled(modeloTabla.getRowCount() > 0);
                }
            }
        }.execute();
    }

    private void onGuardar() {
        if (arptActual == null) {
            mostrarError("No hay ARPT cargado. Seleccione una tarea con ARPT vigente.");
            return;
        }
        if (filasPorIdEvaluacionEpp.isEmpty()) {
            mostrarError("No hay EPPs para guardar.");
            return;
        }

        List<FilaControl> filas = new ArrayList<>(filasPorIdEvaluacionEpp.values());
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            int valorUso = toInt(modeloTabla.getValueAt(i, 1));
            int valorEstado = toInt(modeloTabla.getValueAt(i, 2));
            if (!controlador.validarPuntaje(valorUso, valorEstado)) {
                mostrarError("Fila " + (i + 1) + ": puntajes fuera de rango (1..5).");
                return;
            }
            String obs = (String) modeloTabla.getValueAt(i, 3);
            String rec = (String) modeloTabla.getValueAt(i, 4);
            FilaControl f = filas.get(i);
            f.valorUso = valorUso;
            f.valorEstado = valorEstado;
            f.observaciones = obs;
            f.recomendaciones = rec;
        }

        Tarea tarea = (Tarea) cmbTarea.getSelectedItem();
        Puesto puestoSel = (Puesto) cmbPuesto.getSelectedItem();

        btnGuardar.setEnabled(false);
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                try {
                    Visita v = visitaSeleccionada();
                    if (v == null) return null;
                    return controlador.guardarControl(usuario, v.getIdVisita(),
                            puestoSel.getIdPuesto(), tarea.getIdTarea(),
                            arptActual.getIdArpt(), LocalDate.now(), filas);
                } catch (SQLException ex) {
                    return null;
                }
            }
            @Override
            protected void done() {
                btnGuardar.setEnabled(true);
                try {
                    Integer id = get();
                    if (id == null) {
                        mostrarError("Error al guardar.");
                    } else {
                        idControlGuardado = id;
                        mostrarMensaje("Control de EPP #" + id + " guardado correctamente.", new java.awt.Color(0, 100, 0));
                        JOptionPane.showMessageDialog(VistaControlEPP.this,
                                "Control de EPP #" + id + " registrado y trazado.",
                                "OK", JOptionPane.INFORMATION_MESSAGE);
                        // Recien ahora hay id_control_epp valido, asi que
                        // habilitamos el alta de plan de accion vinculada.
                        btnCrearPlan.setEnabled(idControlGuardado > 0);
                    }
                } catch (Exception ex) {
                    mostrarError("Error inesperado: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Abre el modal de alta de plan de accion. Al confirmar, el plan
     * se persiste inmediatamente (INSERT directo) vinculado al id del
     * control EPP que se guardo. Asi no se acumula en memoria y no se
     * pierde si el usuario cierra la vista sin guardar nada nuevo.
     */
    private void onCrearPlan() {
        if (idControlGuardado <= 0) {
            mostrarError("Guarde el control de EPP primero.");
            return;
        }

        JDialog dialog = new JDialog(this, "Crear plan de accion (Control de EPP #" + idControlGuardado + ")", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(new Dimension(520, 320));
        dialog.setLocationRelativeTo(this);

        JPanel panelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        panelForm.add(new JLabel("Descripcion:"), gbc);
        JTextArea txtDesc = new JTextArea(4, 30);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 0.3;
        panelForm.add(new JScrollPane(txtDesc), gbc);

        // responsable: nombre libre de la persona de la empresa cliente
        // que se hara cargo del plan (no es FK a usuario del sistema).
        // El modelo del TP2 ya tenia este campo como VARCHAR libre.
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.weighty = 0;
        panelForm.add(new JLabel("Responsable:"), gbc);
        JTextField txtResponsable = new JTextField(30);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        panelForm.add(txtResponsable, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panelForm.add(new JLabel("Fecha plazo:"), gbc);
        JFormattedTextField txtFecha = new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd"));
        txtFecha.setValue(new java.util.Date());
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        panelForm.add(txtFecha, gbc);

        dialog.add(panelForm, BorderLayout.CENTER);

        JButton btnAceptar = new JButton("Aceptar");
        JButton btnCancelarModal = new JButton("Cancelar");
        btnCancelarModal.addActionListener(e -> dialog.dispose());
        btnAceptar.addActionListener(e -> {
            String descripcion = txtDesc.getText().trim();
            String responsable = txtResponsable.getText().trim();
            java.util.Date fechaUtil = (java.util.Date) txtFecha.getValue();
            LocalDate fecha = fechaUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!controladorAccion.validarFila(descripcion, responsable, fecha)) {
                mostrarError("Datos invalidos: descripcion no vacia, responsable no vacio, fecha >= hoy.");
                return;
            }
            PlanAccion p = new PlanAccion();
            p.setIdEmpresa(idEmpresa);
            p.setTipoOrigen(TipoOrigen.CONTROL_EPP);
            // El control EPP ya tiene id (sino el boton 'Crear plan' estaria
            // deshabilitado). Vinculamos el plan a el en este momento.
            p.setIdOrigen(idControlGuardado);
            p.setDescripcion(descripcion);
            p.setResponsable(responsable);
            p.setFechaPlazo(fecha);
            p.setEstado(PlanAccion.Estado.ENTRATAMIENTO);
            try {
                int idPlan = controladorAccion.crearPlan(usuario, p);
                mostrarMensaje("Plan #" + idPlan + " creado y vinculado al control #"
                        + idControlGuardado + ".", new java.awt.Color(0, 100, 0));
                dialog.dispose();
            } catch (SQLException ex) {
                mostrarError("Error creando el plan: " + ex.getMessage());
            }
        });
        JPanel panelBotonesModal = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelBotonesModal.add(btnCancelarModal);
        panelBotonesModal.add(btnAceptar);
        dialog.add(panelBotonesModal, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(java.awt.Color.RED);
    }

    private void mostrarMensaje(String mensaje, java.awt.Color color) {
        lblMensaje.setText(mensaje);
        lblMensaje.setForeground(color);
    }

    /**
     * Devuelve un JPanel con el numero en negrita arriba y la descripcion
     * cualitativa abajo, usado como celda de la tablita de referencia 1..5.
     */
    private static JPanel celdaEscala(String numero, String descripcion) {
        JPanel p = new JPanel(new java.awt.BorderLayout());
        JLabel lblNumero = new JLabel(numero, SwingConstants.CENTER);
        lblNumero.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblNumero.setForeground(new java.awt.Color(0, 70, 140));
        JLabel lblDesc = new JLabel("<html><center><span style='font-size:9px'>" + descripcion + "</span></center></html>", SwingConstants.CENTER);
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 9));
        lblDesc.setForeground(java.awt.Color.DARK_GRAY);
        p.add(lblNumero, java.awt.BorderLayout.NORTH);
        p.add(lblDesc, java.awt.BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        return p;
    }

    /**
     * Crea un JFormattedTextField restringido a numeros enteros entre 1 y 5.
     * Se usa como cell editor de las columnas valorUso y valorEstado.
     */
    private static JFormattedTextField crearCampoNumerico1a5() {
        DecimalFormat formato = new DecimalFormat("0");
        formato.setMaximumIntegerDigits(1);
        NumberFormatter formatter = new NumberFormatter(formato);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(1);
        formatter.setMaximum(5);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField campo = new JFormattedTextField(formatter);
        campo.setHorizontalAlignment(SwingConstants.CENTER);
        // sin valor inicial: la celda arranca vacia hasta que cargue datos
        return campo;
    }

    /**
     * Convierte un Object de la celda a int. A veces JFormattedTextField
     * devuelve String en lugar de Integer (bug de Swing al no commitear
     * el edit antes de leer el valor). Este helper acepta los dos casos.
     */
    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) return Integer.parseInt(((String) v).trim());
        throw new IllegalArgumentException("no se puede convertir a int: " + v);
    }
}