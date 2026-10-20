package ar.com.zyasoc.sighs.vista;

import ar.com.zyasoc.sighs.control.ControladorARPT;
import ar.com.zyasoc.sighs.control.ControladorAccion;
import ar.com.zyasoc.sighs.control.MotorEvaluacionRiesgo;
import ar.com.zyasoc.sighs.modelo.Arpt;
import ar.com.zyasoc.sighs.modelo.Arpt.Estado;
import ar.com.zyasoc.sighs.modelo.PlanAccion;
import ar.com.zyasoc.sighs.modelo.PlanAccion.TipoOrigen;
import ar.com.zyasoc.sighs.modelo.TipoRiesgo;
import ar.com.zyasoc.sighs.modelo.Epp;
import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo;
import ar.com.zyasoc.sighs.modelo.EvaluacionRiesgo.Clasificacion;
import ar.com.zyasoc.sighs.modelo.MedidaControl;
import ar.com.zyasoc.sighs.modelo.Puesto;
import ar.com.zyasoc.sighs.modelo.Riesgo;
import ar.com.zyasoc.sighs.modelo.Sector;
import ar.com.zyasoc.sighs.modelo.Tarea;
import ar.com.zyasoc.sighs.modelo.Usuario;
import ar.com.zyasoc.sighs.modelo.Visita;
import ar.com.zyasoc.sighs.modelo.Establecimiento;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFormattedTextField;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla del CU-ARPT-01 (Cargar la planilla del ARPT de una tarea).
 *
 * Flujo:
 *  1. Selecciona tarea -> el sistema carga/crea ARPT en EDICION.
 *  2. Tabla con los riesgos: el usuario completa P, E, C y elige medida.
 *  3. Al hacer click en "Agregar fila", se calcula GP y clasificacion
 *     en vivo (vía MotorEvaluacionRiesgo) y se guarda la evaluacion.
 *  4. Al confirmar la planilla: el ARPT pasa a VIGENTE o queda en EDICION.
 */
public class VistaARPT extends JFrame {

    private final ControladorARPT controlador;
    private final ControladorAccion controladorAccion;
    private final Usuario usuario;
    private final int idEmpresa;

    private final JComboBox<Visita> cmbVisita = new JComboBox<>();
    private final JComboBox<Establecimiento> cmbEstablecimiento = new JComboBox<>();
    private final JComboBox<Sector> cmbSector = new JComboBox<>();
    private final JComboBox<Puesto> cmbPuesto = new JComboBox<>();
    private final JComboBox<Tarea> cmbTarea = new JComboBox<>();
    private final JLabel lblArpt = new JLabel("Seleccione una tarea", SwingConstants.LEFT);
    private final JTextArea txtObs = new JTextArea(3, 40);
    private final JTextArea txtConc = new JTextArea(2, 40);

    // Indices de columna de la planilla. Se usan constantes para que agregar
    // columnas no deje indices hardcodeados desincronizados.
    private static final int COL_RIESGO = 0;
    private static final int COL_FAMILIA = 1;
    private static final int COL_TIPO = 2;
    private static final int COL_P = 3;
    private static final int COL_E = 4;
    private static final int COL_C = 5;
    private static final int COL_GP = 6;
    private static final int COL_CLASIF = 7;
    private static final int COL_LLEVA_EPP = 8;
    private static final int COL_EPPS = 9;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Riesgo", "Familia", "Tipo de riesgo", "P", "E", "C", "GP",
                    "Clasificacion", "Lleva EPP", "EPPs"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // Editables: P, E, C y Lleva EPP. El riesgo y su tipo son del
            // catalogo; GP, clasificacion y EPPs los calcula el sistema.
            return column == COL_P || column == COL_E || column == COL_C || column == COL_LLEVA_EPP;
        }
    };

    /** indice de banda de color por tipo, para agrupar visualmente. */
    private final Map<String, Integer> bandaPorTipo = new HashMap<>();

    private final JTable tabla = new JTable(modelo) {
        @Override
        public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,
                                                  int row, int column) {
            java.awt.Component comp = super.prepareRenderer(renderer, row, column);
            if (!isRowSelected(row)) {
                comp.setBackground(colorDeGrupo(getValueAt(row, COL_TIPO)));
            }
            return comp;
        }
    };
    private final JButton btnCalcular = new JButton("Calcular GP");
    private final JButton btnConfirmar = new JButton("Confirmar y validar");
    private final JButton btnGuardarBorrador = new JButton("Guardar borrador");
    private final JButton btnCrearPlan = new JButton("Crear plan de accion");
    private final JButton btnCancelar = new JButton("Cancelar");
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    private Arpt arptActual;
    /** true mientras se repueblan los combos, para no disparar cargas en cascada. */
    private boolean cargandoCombos = false;
    /** EPPs que no se pudieron desasociar porque ya tienen controles registrados. */
    private int eppsNoQuitados;
    private List<Riesgo> riesgosCatalogo = new ArrayList<>();
    private List<MedidaControl> medidasCatalogo = new ArrayList<>();
    private List<Epp> eppCatalogo = new ArrayList<>();
    private List<TipoRiesgo> tiposRiesgo = new ArrayList<>();
    /** mapa idRiesgo -> lista de idsEpp ya asociados a su evaluacion. */
    private final java.util.Map<Integer, java.util.List<Integer>> eppPorEvaluacion = new java.util.HashMap<>();

    public VistaARPT(Usuario usuario, int idEmpresa, ControladorARPT controlador) {
        this(usuario, idEmpresa, controlador, new ControladorAccion());
    }

    public VistaARPT(Usuario usuario, int idEmpresa,
                      ControladorARPT controlador,
                      ControladorAccion controladorAccion) {
        super("SIGHS - Cargar ARPT (CU-ARPT-01)");
        this.usuario = usuario;
        this.idEmpresa = idEmpresa;
        this.controlador = controlador;
        this.controladorAccion = controladorAccion;

        construirUi();
        wireEventos();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(1120, 720));
        setMinimumSize(new Dimension(1000, 720));
        setLocationRelativeTo(null);

        cargarVisitas();
        cargarEstablecimientos();
        cargarCatalogos();
    }

    private void construirUi() {
        JLabel lblTitulo = new JLabel("Cargar la planilla de evaluacion de riesgos (ARPT)",
                SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel panelTarea = new JPanel();
        panelTarea.setBorder(BorderFactory.createTitledBorder(
                "Visita / establecimiento / sector / puesto / tarea"));
        panelTarea.add(new JLabel("Visita:"));
        cmbVisita.setPreferredSize(new Dimension(220, 26));
        panelTarea.add(cmbVisita);
        panelTarea.add(new JLabel("Establecimiento:"));
        cmbEstablecimiento.setPreferredSize(new Dimension(220, 26));
        panelTarea.add(cmbEstablecimiento);
        panelTarea.add(new JLabel("Sector:"));
        cmbSector.setPreferredSize(new Dimension(170, 26));
        panelTarea.add(cmbSector);
        panelTarea.add(new JLabel("Puesto:"));
        cmbPuesto.setPreferredSize(new Dimension(170, 26));
        panelTarea.add(cmbPuesto);
        panelTarea.add(new JLabel("Tarea:"));
        cmbTarea.setPreferredSize(new Dimension(240, 26));
        panelTarea.add(cmbTarea);

        JPanel panelArpt = new JPanel(new BorderLayout());
        panelArpt.setBorder(BorderFactory.createTitledBorder("ARPT"));
        panelArpt.add(lblArpt, BorderLayout.CENTER);
        panelArpt.add(new JLabel("Tecnico: " + usuario.getNombreCompleto()), BorderLayout.EAST);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder("Planilla de riesgos (una fila por riesgo, agrupada por tipo)"));
        tabla.setRowHeight(26);
        tabla.getColumnModel().getColumn(COL_RIESGO).setPreferredWidth(215);
        tabla.getColumnModel().getColumn(COL_FAMILIA).setPreferredWidth(135);      // clasificacion del tipo
        tabla.getColumnModel().getColumn(COL_TIPO).setPreferredWidth(135);
        tabla.getColumnModel().getColumn(COL_P).setPreferredWidth(34);
        tabla.getColumnModel().getColumn(COL_E).setPreferredWidth(34);
        tabla.getColumnModel().getColumn(COL_C).setPreferredWidth(34);
        tabla.getColumnModel().getColumn(COL_GP).setPreferredWidth(34);
        tabla.getColumnModel().getColumn(COL_CLASIF).setPreferredWidth(92);
        tabla.getColumnModel().getColumn(COL_LLEVA_EPP).setPreferredWidth(68);    // Si/No
        tabla.getColumnModel().getColumn(COL_EPPS).setPreferredWidth(175);        // resumen EPPs
        // cell editor 1..5 para P, E, C
        JFormattedTextField celdaPEC = crearCampoNumerico1a5();
        tabla.getColumnModel().getColumn(COL_P).setCellEditor(new DefaultCellEditor(celdaPEC));
        tabla.getColumnModel().getColumn(COL_E).setCellEditor(new DefaultCellEditor(crearCampoNumerico1a5()));
        tabla.getColumnModel().getColumn(COL_C).setCellEditor(new DefaultCellEditor(crearCampoNumerico1a5()));
        // el combo Si/No de "Lleva EPP" se instala en cargarCatalogos()
        panelTabla.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnCalcular);
        panelBotones.add(btnGuardarBorrador);
        panelBotones.add(btnConfirmar);
        panelBotones.add(btnCrearPlan);
        panelBotones.add(btnCancelar);

        lblMensaje.setForeground(Color.RED);
        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel panelEncabezado = new JPanel();
        panelEncabezado.setLayout(new javax.swing.BoxLayout(panelEncabezado, javax.swing.BoxLayout.Y_AXIS));
        panelEncabezado.add(panelTarea);
        panelEncabezado.add(panelArpt);

        JPanel panelObsConc = new JPanel(new GridLayout(2, 1, 5, 5));
        panelObsConc.setBorder(BorderFactory.createTitledBorder("Observaciones y conclusion"));
        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JLabel("Observaciones generales:"), BorderLayout.NORTH);
        p1.add(new JScrollPane(txtObs), BorderLayout.CENTER);
        JPanel p2 = new JPanel(new BorderLayout());
        p2.add(new JLabel("Conclusion general:"), BorderLayout.NORTH);
        p2.add(new JScrollPane(txtConc), BorderLayout.CENTER);
        panelObsConc.add(p1);
        panelObsConc.add(p2);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panelTabla, BorderLayout.CENTER);
        panelCentro.add(panelObsConc, BorderLayout.SOUTH);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelBotones, BorderLayout.NORTH);
        panelSur.add(lblMensaje, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(lblTitulo, BorderLayout.NORTH);
        root.add(panelEncabezado, BorderLayout.PAGE_START);
        root.add(panelCentro, BorderLayout.CENTER);
        root.add(panelSur, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void wireEventos() {
        cmbEstablecimiento.addActionListener(e -> onEstablecimientoSeleccionado());
        cmbSector.addActionListener(e -> onSectorSeleccionado());
        cmbPuesto.addActionListener(e -> onPuestoSeleccionado());
        cmbTarea.addActionListener(e -> onTareaSeleccionada());
        btnCalcular.addActionListener(e -> onCalcular());
        btnGuardarBorrador.addActionListener(e -> onGuardarBorrador());
        btnConfirmar.addActionListener(e -> onConfirmar());
        btnCrearPlan.addActionListener(e -> onCrearPlan());
        btnCancelar.addActionListener(e -> dispose());

        // btnCrearPlan arranca deshabilitado. Se habilita cuando el ARPT esta
        // VIGENTE y por tanto permite originar un plan de accion desde el.
        btnCrearPlan.setEnabled(false);
    }

    /** Combo de visita: ultimas 15 visitas de la empresa, desc por fecha. */
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

    /** Visita elegida en el combo (null si no hay seleccionada). */
    private Visita visitaSeleccionada() {
        return (Visita) cmbVisita.getSelectedItem();
    }

    /** Combo de establecimientos: lista los de la empresa, orden por nombre. */
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
                    cargandoCombos = true;
                    try {
                        cmbEstablecimiento.removeAllItems();
                        cmbEstablecimiento.addItem(null);
                        if (lista != null) for (Establecimiento e : lista) cmbEstablecimiento.addItem(e);
                        // si hay un solo establecimiento, lo guardamos para
                        // autoseleccionar DESPUES de bajar el flag. setSelectedIndex
                        // dispara el ActionListener y este metodo es el que
                        // carga el sector, pero el listener hace 'if (cargandoCombos)
                        // return' para no entrar en cascadas durante el repoblamiento,
                        // asi que si lo llamamos dentro del bloque el sector nunca se
                        // carga. Lo disparamos abajo, fuera del try.
                        if (lista != null && lista.size() == 1) {
                            autoSeleccion = lista.get(0);
                        }
                    } finally {
                        cargandoCombos = false;
                    }
                    // Autoseleccion explicita fuera del flag: asi el listener
                    // dispara con cargandoCombos=false y cargarSectores() corre
                    // de verdad. Aplica para ARPT y para EPP (mismo patron).
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

    /** Al elegir un establecimiento, repoblamos el combo Sector. */
    private void onEstablecimientoSeleccionado() {
        if (cargandoCombos) return;
        cargarSectores();
    }

    /** Sectores del establecimiento elegido (null si no hay establecimiento). */
    private void cargarSectores() {
        final Establecimiento est = establecimientoSeleccionado();
        if (est == null) {
            cargandoCombos = true;
            try {
                cmbSector.removeAllItems();
                cmbSector.addItem(null);
            } finally {
                cargandoCombos = false;
            }
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
                try { return controlador.listarSectoresPorEstablecimiento(idEst); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    List<Sector> lista = get();
                    cargandoCombos = true;
                    try {
                        cmbSector.removeAllItems();
                        cmbSector.addItem(null);
                        if (lista != null) for (Sector s : lista) cmbSector.addItem(s);
                    } finally {
                        cargandoCombos = false;
                    }
                } catch (Exception ex) {
                    mostrarError("Error cargando sectores: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onSectorSeleccionado() {
        if (cargandoCombos) return;
        Sector s = (Sector) cmbSector.getSelectedItem();
        limpiarPlanilla("Seleccione sector / puesto / tarea");
        // al cambiar de sector se descartan puesto y tarea
        cargandoCombos = true;
        try {
            cmbPuesto.removeAllItems();
            cmbPuesto.addItem(null);
            cmbTarea.removeAllItems();
            cmbTarea.addItem(null);
        } finally {
            cargandoCombos = false;
        }
        if (s == null) return;

        new SwingWorker<List<Puesto>, Void>() {
            @Override
            protected List<Puesto> doInBackground() {
                try { return controlador.listarPuestos(s.getIdSector()); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                // si ya cambio de sector, esta respuesta quedo vieja
                if (cmbSector.getSelectedItem() != s) return;
                try {
                    List<Puesto> lista = get();
                    cargandoCombos = true;
                    try {
                        cmbPuesto.removeAllItems();
                        cmbPuesto.addItem(null);
                        if (lista != null) for (Puesto p : lista) cmbPuesto.addItem(p);
                    } finally {
                        cargandoCombos = false;
                    }
                } catch (Exception ex) {
                    mostrarError("Error cargando puestos: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onPuestoSeleccionado() {
        if (cargandoCombos) return;
        Puesto p = (Puesto) cmbPuesto.getSelectedItem();
        limpiarPlanilla("Seleccione una tarea");
        cargandoCombos = true;
        try {
            cmbTarea.removeAllItems();
            cmbTarea.addItem(null);
        } finally {
            cargandoCombos = false;
        }
        if (p == null) return;

        new SwingWorker<List<Tarea>, Void>() {
            @Override
            protected List<Tarea> doInBackground() {
                try { return controlador.listarTareas(p.getIdPuesto()); }
                catch (SQLException ex) { return null; }
            }
            @Override
            protected void done() {
                if (cmbPuesto.getSelectedItem() != p) return;
                try {
                    List<Tarea> lista = get();
                    cargandoCombos = true;
                    try {
                        cmbTarea.removeAllItems();
                        cmbTarea.addItem(null);
                        if (lista != null) for (Tarea t : lista) cmbTarea.addItem(t);
                    } finally {
                        cargandoCombos = false;
                    }
                } catch (Exception ex) {
                    mostrarError("Error cargando tareas: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /** Deja la planilla vacia cuando cambia el sector, el puesto o la tarea. */
    private void limpiarPlanilla(String mensaje) {
        arptActual = null;
        idEvaluacionPorRiesgo.clear();
        eppPorEvaluacion.clear();
        modelo.setRowCount(0);
        txtObs.setText("");
        txtConc.setText("");
        lblArpt.setText(mensaje);
    }

    private void cargarCatalogos() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    riesgosCatalogo = controlador.cargarRiesgos();
                    medidasCatalogo = controlador.cargarMedidasControl();
                    eppCatalogo = controlador.cargarEpp();
                    tiposRiesgo = controlador.cargarTiposRiesgo();
                } catch (SQLException ex) {
                }
                return null;
            }
            @Override
            protected void done() {
                // El riesgo no se elige desde la tabla: la planilla ya trae
                // todos los riesgos del catalogo. Solo se editan P, E, C y
                // "Lleva EPP".
                JComboBox<String> comboLlevaEpp = new JComboBox<>(new String[]{"No", "Si"});
                comboLlevaEpp.setFont(new Font("SansSerif", Font.PLAIN, 12));
                tabla.getColumnModel().getColumn(COL_LLEVA_EPP).setCellEditor(new DefaultCellEditor(comboLlevaEpp));
                // La columna EPPs (resumen) NO es editable: se actualiza al
                // cerrar el dialogo de edicion.
            }
        }.execute();
    }

    private void onTareaSeleccionada() {
        if (cargandoCombos) return;
        Tarea t = (Tarea) cmbTarea.getSelectedItem();
        if (t == null) {
            limpiarPlanilla("Seleccione una tarea");
            return;
        }
        new SwingWorker<Arpt, Void>() {
            @Override
            protected Arpt doInBackground() {
                try {
                    Visita v = visitaSeleccionada();
                    if (v == null) return null;
                    return controlador.obtenerOCrearParaTarea(t.getIdTarea(), v.getIdVisita(), usuario.getIdUsuario());
                } catch (SQLException ex) {
                    return null;
                }
            }
            @Override
            protected void done() {
                try {
                    arptActual = get();
                    if (arptActual == null || arptActual.getIdArpt() == 0) {
                        mostrarError("Esta tarea ya tiene un ARPT VIGENTE. Actualice primero el existente.");
                        arptActual = null;
                        lblArpt.setText("ARPT vigente existente");
                        modelo.setRowCount(0);
                        return;
                    }
                    lblArpt.setText("ARPT #" + arptActual.getIdArpt() + " (estado=" + arptActual.getEstado() + ")");
                    // El boton 'Crear plan de accion' se habilita para cualquier ARPT
                    // que ya tenga id (cargado por obtenerOCrearParaTarea). Tiene
                    // sentido en EDICION tambien, porque el tecnico puede estar
                    // trabajando en la planilla y querer originar un hallazgo. El
                    // plan se vincula al id_arpt real, asi que el toggle del estado
                    // del ARPT no afecta la vinculacion.
                    btnCrearPlan.setEnabled(arptActual.getIdArpt() > 0);
                    // hidratar el encabezado: sin esto, "Guardar borrador"
                    // parecia no haber guardado nada al reabrir la pantalla.
                    txtObs.setText(arptActual.getObservacionesGenerales() != null
                            ? arptActual.getObservacionesGenerales() : "");
                    txtConc.setText(arptActual.getConclusionGeneral() != null
                            ? arptActual.getConclusionGeneral() : "");
                    cargarEvaluacionesExistentes();
                } catch (Exception ex) {
                    mostrarError("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Carga la planilla del ARPT siguiendo el sequence diagram del TP2.
     * Siempre muestra TODOS los riesgos vigentes del catalogo como checklist
     * (una fila por riesgo): los que ya tienen evaluacion vienen hidratados y
     * el resto queda vacio para que el tecnico los complete. Un riesgo que ya
     * no esta vigente pero tiene evaluacion tambien se muestra, al final.
     */
    private void cargarEvaluacionesExistentes() {
        new SwingWorker<Void, Void>() {
            private List<EvaluacionRiesgo> evaluacionesPrevias;

            @Override
            protected Void doInBackground() {
                try {
                    tiposRiesgo = controlador.cargarTiposRiesgo();
                    riesgosCatalogo = controlador.cargarRiesgos();
                    medidasCatalogo = controlador.cargarMedidasControl();
                    eppCatalogo = controlador.cargarEpp();
                    idEvaluacionPorRiesgo.clear();
                    eppPorEvaluacion.clear();
                    // Una sola evaluacion por riesgo: si la base todavia tiene
                    // duplicados historicos, nos quedamos con el mas nuevo.
                    java.util.LinkedHashMap<Integer, EvaluacionRiesgo> porRiesgo =
                            new java.util.LinkedHashMap<>();
                    for (EvaluacionRiesgo ev : controlador.cargarEvaluaciones(arptActual.getIdArpt())) {
                        porRiesgo.put(ev.getIdRiesgo(), ev);
                    }
                    evaluacionesPrevias = new java.util.ArrayList<>(porRiesgo.values());
                    for (EvaluacionRiesgo ev : evaluacionesPrevias) {
                        idEvaluacionPorRiesgo.put(ev.getIdRiesgo(), ev.getIdEvaluacion());
                        eppPorEvaluacion.put(ev.getIdRiesgo(),
                                controlador.cargarEppPorEvaluacion(ev.getIdEvaluacion()));
                    }
                    return null;
                } catch (SQLException ex) {
                    return null;
                }
            }
            @Override
            protected void done() {
                try {
                    modelo.setRowCount(0);
                    if (evaluacionesPrevias == null) {
                        mostrarError("No se pudieron cargar las evaluaciones.");
                        return;
                    }
                    java.util.Map<Integer, EvaluacionRiesgo> porRiesgo = new java.util.HashMap<>();
                    for (EvaluacionRiesgo ev : evaluacionesPrevias) {
                        porRiesgo.put(ev.getIdRiesgo(), ev);
                    }
                    // checklist completo: riesgos vigentes con o sin
                    // evaluacion previa; los dados de baja solo aparecen si
                    // tienen una evaluacion registrada en este ARPT (si no,
                    // seria una fila vacia ruidosa).
                    for (Riesgo r : riesgosCatalogo) {
                        EvaluacionRiesgo ev = porRiesgo.remove(r.getIdRiesgo());
                        if (ev != null) {
                            agregarFilaDeEvaluacion(ev, r);
                        } else if (r.isVigente()) {
                            modelo.addRow(filaVacia(r));
                        }
                    }
                    // red de seguridad: evaluaciones cuyo riesgo ya no esta
                    // en el catalogo (no deberia pasar, pero por si acaso).
                    for (EvaluacionRiesgo ev : porRiesgo.values()) {
                        agregarFilaDeEvaluacion(ev, null);
                    }
                } catch (Exception ex) {
                    mostrarError("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Convierte una EvaluacionRiesgo de la base en una fila de la tabla
     * mostrando Riesgo (codigo+nombre), P, E, C, GP, Clasificacion, Lleva EPP, EPPs.
     */
    private void agregarFilaDeEvaluacion(EvaluacionRiesgo ev, Riesgo riesgo) {
        String riesgoTxt = buscarTextoRiesgo(ev.getIdRiesgo());
        if (riesgo == null) riesgo = buscarRiesgo(ev.getIdRiesgo());
        String tipoTxt = riesgo != null ? nombreTipo(riesgo.getIdTipoRiesgo()) : "(sin tipo)";
        String familiaTxt = riesgo != null ? familiaTipo(riesgo.getIdTipoRiesgo()) : "(sin familia)";
        int p = ev.getProbabilidad();
        int e = ev.getExposicion();
        int c_val = ev.getConsecuencia();
        int gp = ev.getGradoPeligrosidad();
        EvaluacionRiesgo.Clasificacion cls = ev.getClasificacion();
        List<Integer> idsEpp = eppPorEvaluacion.get(ev.getIdRiesgo());
        boolean llevaEpp = idsEpp != null && !idsEpp.isEmpty();
        modelo.addRow(new Object[]{
                riesgoTxt, familiaTxt, tipoTxt, p, e, c_val, gp, cls,
                llevaEpp ? "Si" : "No", resumenEpp(idsEpp)
        });
    }

    /** Fila inicial de un riesgo del catalogo, sin valores cargados. */
    private Object[] filaVacia(Riesgo r) {
        return new Object[]{
                r.getCodigo() + " - " + r.getNombre(),
                familiaTipo(r.getIdTipoRiesgo()), nombreTipo(r.getIdTipoRiesgo()),
                0, 0, 0, 0, null, "No", "(sin EPPs)"
        };
    }

    private String buscarTextoRiesgo(int idRiesgo) {
        for (Riesgo r : riesgosCatalogo) {
            if (r.getIdRiesgo() == idRiesgo) return r.getCodigo() + " - " + r.getNombre();
        }
        return "id=" + idRiesgo;
    }

    private Riesgo buscarRiesgo(int idRiesgo) {
        for (Riesgo r : riesgosCatalogo) {
            if (r.getIdRiesgo() == idRiesgo) return r;
        }
        return null;
    }

    /** Nombre del tipo_riesgo (la categoria con la que se agrupa la planilla). */
    private String nombreTipo(int idTipoRiesgo) {
        for (TipoRiesgo t : tiposRiesgo) {
            if (t.getIdTipoRiesgo() == idTipoRiesgo) return t.getNombre();
        }
        return "(sin tipo)";
    }

    /**
     * Clasificacion del tipo de riesgo: la familia a la que pertenece
     * (Fisicos, Fisicos-Mecanicos, Quimicos, Ergonomicos, Biologicos).
     * Si la fila del catalogo es ella misma una familia, devuelve su nombre.
     */
    private String familiaTipo(int idTipoRiesgo) {
        for (TipoRiesgo t : tiposRiesgo) {
            if (t.getIdTipoRiesgo() != idTipoRiesgo) continue;
            Integer idPadre = t.getIdPadre();
            if (idPadre == null) return t.getNombre();
            for (TipoRiesgo f : tiposRiesgo) {
                if (f.getIdTipoRiesgo() == idPadre) return f.getNombre();
            }
            break;
        }
        return "(sin familia)";
    }

    private String resumenEpp(java.util.List<Integer> idsEpp) {
        if (idsEpp == null || idsEpp.isEmpty()) return "(sin EPPs)";
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < idsEpp.size(); k++) {
            if (k > 0) sb.append(", ");
            String nom = buscarNombreEpp(idsEpp.get(k));
            sb.append(nom != null ? nom : "id=" + idsEpp.get(k));
        }
        return sb.toString();
    }

    /**
     * Color de fondo por grupo de tipo de riesgo: dos tonos alternados para
     * que se vea donde termina un tipo y empieza el siguiente.
     */
    private Color colorDeGrupo(Object tipo) {
        String clave = String.valueOf(tipo);
        Integer banda = bandaPorTipo.get(clave);
        if (banda == null) {
            banda = bandaPorTipo.size();
            bandaPorTipo.put(clave, banda);
        }
        return (banda % 2 == 0)
                ? new Color(0xF2, 0xF6, 0xFB)
                : Color.WHITE;
    }

    private String buscarNombreEpp(int idEpp) {
        for (Epp e : eppCatalogo) if (e.getIdEpp() == idEpp) return e.getNombre();
        return null;
    }

    /**
     * Abre un JDialog con un wizard de edicion de EPPs.
     * Muestra todos los EPPs del catalogo (ordenados alfabeticamente)
     * con un JCheckBox por cada uno, preseleccionando los que ya estaban.
     * Botones: Atras / Siguiente / OK / Cancelar. Devuelve la lista de
     * idEpp tildados, o null si el usuario cancela.
     */
    private java.util.List<Integer> abrirDialogoEpp(
            Riesgo riesgo, EvaluacionRiesgo.Clasificacion cls, int gp,
            java.util.List<Integer> preseleccionados) {
        java.util.List<Epp> eppsOrdenados = new java.util.ArrayList<>(eppCatalogo);
        eppsOrdenados.sort((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));

        // tipo del riesgo (1 sola busqueda al cache que ya cargamos)
        String tipoNombre = "(sin tipo)";
        for (TipoRiesgo t : tiposRiesgo) {
            if (t.getIdTipoRiesgo() == riesgo.getIdTipoRiesgo()) {
                tipoNombre = t.getNombre();
                break;
            }
        }

        JDialog dialog = new JDialog(this, "Editar EPPs requeridos", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(520, 460);
        dialog.setLocationRelativeTo(this);

        // header de solo lectura con el contexto (Riesgo, Tipo, Valoracion)
        JPanel panelHeader = new JPanel();
        panelHeader.setLayout(new javax.swing.BoxLayout(panelHeader, javax.swing.BoxLayout.Y_AXIS));
        panelHeader.setBorder(BorderFactory.createTitledBorder("Contexto (solo lectura)"));
        JLabel lblCat = new JLabel("  Riesgo:    " + riesgo.getCodigo() + " - " + riesgo.getNombre());
        lblCat.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel lblTipo = new JLabel("  Tipo: " + tipoNombre);
        lblTipo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel lblVal = new JLabel("  Valoracion: ver tabla | GP=" + gp + " | Clasificacion=" + cls);
        lblVal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panelHeader.add(lblCat);
        panelHeader.add(lblTipo);
        panelHeader.add(lblVal);

        java.util.List<JCheckBox> checks = new java.util.ArrayList<>();
        JPanel panelChecks = new JPanel();
        panelChecks.setLayout(new javax.swing.BoxLayout(panelChecks, javax.swing.BoxLayout.Y_AXIS));
        panelChecks.setBorder(BorderFactory.createTitledBorder("EPPs requeridos (tildar los que apliquen)"));
        for (Epp e : eppsOrdenados) {
            JCheckBox cb = new JCheckBox(
                    "<html><b>" + e.getNombre() + "</b>"
                            + (e.getDescripcion() != null ? "<br><span style='font-size:10px'>" + e.getDescripcion() + "</span>" : "")
                            + "</html>");
            cb.setSelected(preseleccionados.contains(e.getIdEpp()));
            cb.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            checks.add(cb);
            panelChecks.add(cb);
        }

        JButton btnCancelar = new JButton("Cancelar");
        JButton btnAceptar = new JButton("OK");
        final java.util.concurrent.atomic.AtomicReference<java.util.List<Integer>> resultado =
                new java.util.concurrent.atomic.AtomicReference<>(null);
        btnAceptar.addActionListener(e -> {
            java.util.List<Integer> elegidos = new java.util.ArrayList<>();
            for (int k = 0; k < checks.size(); k++) {
                if (checks.get(k).isSelected()) {
                    elegidos.add(eppsOrdenados.get(k).getIdEpp());
                }
            }
            resultado.set(elegidos);
            dialog.dispose();
        });
        // "Cancelar" mantiene lo que ya estaba seleccionado (no borra nada).
        btnCancelar.addActionListener(e -> dialog.dispose());

        JPanel panelBotones = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        panelBotones.add(btnCancelar);
        panelBotones.add(btnAceptar);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(panelHeader, BorderLayout.NORTH);
        centro.add(new JScrollPane(panelChecks), BorderLayout.CENTER);

        dialog.add(centro, BorderLayout.CENTER);
        dialog.add(panelBotones, BorderLayout.SOUTH);
        dialog.setVisible(true);
        return resultado.get();
    }

    /**
     * Actualiza la celda "EPPs" de la fila i con el resumen de los EPPs
     * elegidos. No toca "Lleva EPP": esa columna la maneja el usuario.
     */
    private void actualizarResumenEppEnFila(int fila, java.util.List<Integer> idsEpp) {
        modelo.setValueAt(resumenEpp(idsEpp), fila, COL_EPPS);
    }

    private void onCalcular() {
        if (arptActual == null) {
            mostrarError("Seleccione una tarea primero.");
            return;
        }
        if (modelo.getRowCount() == 0) {
            mostrarError("No hay filas para calcular.");
            return;
        }
        int procesadas = 0;
        int saltadas = 0;
        eppsNoQuitados = 0;
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (persistirFila(i, true)) procesadas++;
                else saltadas++;
            }
            String msg = "GP calculado y guardado para " + procesadas + " filas";
            if (saltadas > 0) msg += " (" + saltadas + " filas sin P/E/C completos, saltadas)";
            if (eppsNoQuitados > 0) {
                msg += " | " + eppsNoQuitados + " EPP(s) conservados: ya tienen controles registrados";
            }
            mostrarMensaje(msg, new Color(0, 100, 0));
        } catch (IllegalArgumentException ex) {
            mostrarError("Validacion: " + ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error SQL: " + ex.getMessage());
        }
    }

    /**
     * Calcula GP y clasificacion de una fila y la persiste reutilizando la
     * evaluacion que ya existe (nunca inserta una segunda fila para el mismo
     * riesgo dentro del ARPT). Devuelve false si la fila todavia no tiene
     * P, E y C completos.
     */
    private boolean persistirFila(int fila, boolean permitirDialogoEpp)
            throws SQLException {
        Object riesgoObj = modelo.getValueAt(fila, COL_RIESGO);
        if (riesgoObj == null
                || (riesgoObj instanceof String && ((String) riesgoObj).startsWith("--- "))) {
            return false;
        }
        int p = toInt(modelo.getValueAt(fila, COL_P));
        int e = toInt(modelo.getValueAt(fila, COL_E));
        int c = toInt(modelo.getValueAt(fila, COL_C));
        if (p == 0 || e == 0 || c == 0) return false;

        Riesgo riesgo = buscarRiesgoPorTexto(String.valueOf(riesgoObj));
        if (riesgo == null) {
            throw new IllegalArgumentException(
                    "fila " + (fila + 1) + ": no se encontro el riesgo " + riesgoObj);
        }

        EvaluacionRiesgo ev = new EvaluacionRiesgo();
        ev.setIdArpt(arptActual.getIdArpt());
        ev.setIdRiesgo(riesgo.getIdRiesgo());
        Integer idPrevio = idEvaluacionPorRiesgo.get(riesgo.getIdRiesgo());
        if (idPrevio != null && idPrevio > 0) ev.setIdEvaluacion(idPrevio);
        ev.setProbabilidad(p);
        ev.setExposicion(e);
        ev.setConsecuencia(c);
        controlador.evaluar(ev);  // calcula GP y clasificacion (RD-04 y RD-05)
        modelo.setValueAt(ev.getGradoPeligrosidad(), fila, COL_GP);
        modelo.setValueAt(ev.getClasificacion(), fila, COL_CLASIF);

        int idEvaluacion = controlador.guardarOActualizarEvaluacion(ev);
        idEvaluacionPorRiesgo.put(riesgo.getIdRiesgo(), idEvaluacion);

        boolean llevaEpp = "Si".equalsIgnoreCase(
                String.valueOf(modelo.getValueAt(fila, COL_LLEVA_EPP)));
        java.util.List<Integer> actuales = new java.util.ArrayList<>(
                eppPorEvaluacion.getOrDefault(riesgo.getIdRiesgo(), new java.util.ArrayList<>()));

        java.util.List<Integer> deseados;
        if (!llevaEpp) {
            deseados = new java.util.ArrayList<>();
        } else if (permitirDialogoEpp) {
            java.util.List<Integer> elegidos = abrirDialogoEpp(
                    riesgo, ev.getClasificacion(), ev.getGradoPeligrosidad(), actuales);
            deseados = (elegidos != null)
                    ? elegidos
                    : new java.util.ArrayList<>(actuales);   // cancelo: conservamos
        } else {
            deseados = new java.util.ArrayList<>(actuales);
        }
        // Reconciliamos siempre contra la base: es idempotente cuando nada
        // cambio y, si la fila pertenece a una version nueva, copia los EPPs
        // que el usuario tenia seleccionados en pantalla.
        eppsNoQuitados += controlador
                .reemplazarEppPorEvaluacion(idEvaluacion, deseados).size();

        java.util.List<Integer> epps = controlador.cargarEppPorEvaluacion(idEvaluacion);
        eppPorEvaluacion.put(riesgo.getIdRiesgo(), epps);
        if (!llevaEpp) {
            modelo.setValueAt(epps.isEmpty() ? "No" : "Si", fila, COL_LLEVA_EPP);
        }
        actualizarResumenEppEnFila(fila, epps);
        return true;
    }

    private Riesgo buscarRiesgoPorTexto(String texto) {
        if (texto == null) return null;
        // el texto tiene formato "CODIGO - nombre" (ej: "MEC-01 - Contacto...")
        String codigo = texto;
        int guion = texto.indexOf(" - ");
        if (guion > 0) codigo = texto.substring(0, guion);
        for (Riesgo r : riesgosCatalogo) {
            if (r.getCodigo().equals(codigo)) return r;
        }
        return null;
    }

    /** Conversion segura a int: acepta Integer o String (ver VistaControlEPP). */
    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt(((String) v).trim()); }
            catch (NumberFormatException ex) { return 0; }
        }
        return 0;
    }

    private final Map<Integer, Integer> idEvaluacionPorRiesgo = new HashMap<>();

    /**
     * Confirmacion cuando el ARPT ya esta VIGENTE: cualquier guardado crea una
     * version nueva, para no reescribir un documento ya validado.
     */
    private boolean confirmarNuevaVersion(boolean publicarAlConfirmar) {
        String consecuencia = publicarAlConfirmar
                ? "Se va a crear una versión nueva, que quedará VIGENTE al confirmar."
                : "Se va a crear una versión nueva como borrador.";
        int r = JOptionPane.showConfirmDialog(this,
                "Se modificó un ARPT activo.\n\n"
                        + consecuencia + "\n"
                        + "El ARPT #" + arptActual.getIdArpt() + " queda en el historial.\n\n"
                        + "¿Está seguro de proceder?",
                "ARPT activo: versión nueva",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    /**
     * Crea el ARPT nuevo y apunta la vista a el. Las evaluaciones se copian
     * despues, desde la planilla (el estado editado), no desde la base.
     * Devuelve el id del ARPT anterior.
     */
    private int crearVersionEnMemoria() throws SQLException {
        int idAnterior = arptActual.getIdArpt();
        int idNuevo = controlador.crearVersionEnEdicion(
                usuario, arptActual, txtObs.getText(), txtConc.getText());
        Arpt nueva = new Arpt();
        nueva.setIdArpt(idNuevo);
        nueva.setIdTarea(arptActual.getIdTarea());
        nueva.setIdVisita(arptActual.getIdVisita());
        nueva.setFecha(LocalDate.now());
        nueva.setEstado(Estado.EDICION);
        nueva.setObservacionesGenerales(txtObs.getText());
        nueva.setConclusionGeneral(txtConc.getText());
        arptActual = nueva;
        // las evaluaciones de la version nueva todavia no existen: se insertan
        // al persistir la planilla. Los EPPs elegidos en pantalla se conservan.
        idEvaluacionPorRiesgo.clear();
        lblArpt.setText("ARPT #" + idNuevo + " (estado=EDICION) - version de #" + idAnterior);
        return idAnterior;
    }

    /** Persiste todas las filas y devuelve cuantas tenian P, E y C completos. */
    private int persistirTodas(boolean permitirDialogoEpp) throws SQLException {
        int guardadas = 0;
        eppsNoQuitados = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (persistirFila(i, permitirDialogoEpp)) guardadas++;
        }
        return guardadas;
    }

    /**
     * Recorre la planilla y, para cada fila con "Lleva EPP" = Si, abre el
     * wizard de seleccion de EPPs para que el tecnico revise o ajuste la
     * eleccion. NO persiste nada: solo actualiza {@link #eppPorEvaluacion}
     * en memoria. Asi el guardado puede crear antes la version nueva (en
     * un ARPT VIGENTE) sin haber tocado el documento vigente.
     *
     * Devuelve false si el usuario decidio abortar el guardado.
     */
    private boolean wizardEppParaTodas() {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String lleva = String.valueOf(modelo.getValueAt(i, COL_LLEVA_EPP));
            if (!"Si".equalsIgnoreCase(lleva)) continue;
            Object riesgoObj = modelo.getValueAt(i, COL_RIESGO);
            if (riesgoObj == null) continue;
            Riesgo riesgo = buscarRiesgoPorTexto(String.valueOf(riesgoObj));
            if (riesgo == null) continue;
            EvaluacionRiesgo.Clasificacion cls =
                    (EvaluacionRiesgo.Clasificacion) modelo.getValueAt(i, COL_CLASIF);
            Object gpObj = modelo.getValueAt(i, COL_GP);
            int gp = (gpObj instanceof Number) ? ((Number) gpObj).intValue() : 0;
            java.util.List<Integer> actuales = new java.util.ArrayList<>(
                    eppPorEvaluacion.getOrDefault(riesgo.getIdRiesgo(),
                            new java.util.ArrayList<>()));
            java.util.List<Integer> elegidos = abrirDialogoEpp(riesgo, cls, gp, actuales);
            if (elegidos == null) {
                int r = JOptionPane.showConfirmDialog(this,
                        "Cancelaste la seleccion de EPPs en la fila " + (i + 1) + ".\n"
                                + "¿Querés abortar el guardado?",
                        "Cancelar guardado",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r == JOptionPane.YES_OPTION) return false;
                // si dice que no, conservamos los EPPs que ya tenia la fila
            } else {
                eppPorEvaluacion.put(riesgo.getIdRiesgo(), elegidos);
            }
        }
        return true;
    }

    private void onGuardarBorrador() {
        if (arptActual == null) {
            mostrarError("Seleccione una tarea primero.");
            return;
        }
        // 1) Wizard de EPPs: el tecnico revisa/ajusta la seleccion por fila.
        //    Solo actualiza memoria, no toca la base.
        if (!wizardEppParaTodas()) return;

        int idAnterior = 0;
        try {
            // 2) Si el ARPT esta VIGENTE, recien ahora pedimos confirmar la
            //    version nueva: el tecnico ya vio los EPPs que va a llevar.
            if (arptActual.getEstado() == Estado.VIGENTE) {
                if (!confirmarNuevaVersion(false)) return;
                idAnterior = crearVersionEnMemoria();
            }
            int guardadas = persistirTodas(false);
            int incompletas = modelo.getRowCount() - guardadas;
            controlador.dejarEnEdicion(usuario, arptActual.getIdArpt(),
                    txtObs.getText(), txtConc.getText());

            String msg;
            if (idAnterior > 0) {
                msg = "Version nueva ARPT #" + arptActual.getIdArpt() + " en EDICION (el #"
                        + idAnterior + " sigue VIGENTE): " + guardadas + " filas copiadas";
            } else {
                msg = "ARPT #" + arptActual.getIdArpt() + " en EDICION, "
                        + guardadas + " filas persistidas";
            }
            if (incompletas > 0) msg += " (" + incompletas + " sin P/E/C)";
            if (eppsNoQuitados > 0) {
                msg += " | " + eppsNoQuitados + " EPP(s) conservados: ya tienen controles registrados";
            }
            mostrarMensaje(msg, new Color(0, 100, 0));
        } catch (IllegalArgumentException ex) {
            mostrarError("Validacion: " + ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error: " + ex.getMessage());
        }
    }

    /**
     * Abre el modal de alta de plan de accion. Al confirmar, el plan
     * se persiste inmediatamente (INSERT directo) vinculado al id del
     * ARPT que esta abierto en pantalla. Asi no se acumula en
     * memoria y no se pierde si el usuario cierra la vista sin
     * guardar el ARPT.
     */
    private void onCrearPlan() {
        if (arptActual == null || arptActual.getIdArpt() <= 0) {
            mostrarError("Cargue o seleccione un ARPT primero.");
            return;
        }

        JDialog dialog = new JDialog(this, "Crear plan de accion (ARPT #" + arptActual.getIdArpt() + ")", true);
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
        JFormattedTextField txtFecha = new JFormattedTextField(
                new java.text.SimpleDateFormat("yyyy-MM-dd"));
        txtFecha.setValue(new java.util.Date());
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        panelForm.add(txtFecha, gbc);

        dialog.add(panelForm, BorderLayout.CENTER);

        JButton btnAceptar = new JButton("Aceptar");
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());
        btnAceptar.addActionListener(e -> {
            String descripcion = txtDesc.getText().trim();
            String responsable = txtResponsable.getText().trim();
            java.util.Date fechaUtil = (java.util.Date) txtFecha.getValue();
            LocalDate fecha = fechaUtil.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (!controladorAccion.validarFila(descripcion, responsable, fecha)) {
                mostrarError("Datos invalidos: descripcion no vacia, responsable no vacio, fecha >= hoy.");
                return;
            }
            PlanAccion p = new PlanAccion();
            p.setIdEmpresa(idEmpresa);
            p.setTipoOrigen(TipoOrigen.ARPT);
            // El ARPT ya tiene id (sino el boton 'Crear plan' estaria
            // deshabilitado). Vinculamos el plan a el en este momento.
            p.setIdOrigen(arptActual.getIdArpt());
            p.setDescripcion(descripcion);
            p.setResponsable(responsable);
            p.setFechaPlazo(fecha);
            p.setEstado(PlanAccion.Estado.ENTRATAMIENTO);
            try {
                int idPlan = controladorAccion.crearPlan(usuario, p);
                mostrarMensaje("Plan #" + idPlan + " creado y vinculado al ARPT #"
                        + arptActual.getIdArpt() + ".", new Color(0, 100, 0));
                dialog.dispose();
            } catch (SQLException ex) {
                mostrarError("Error creando el plan: " + ex.getMessage());
            }
        });
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panelBotones.add(btnCancelar);
        panelBotones.add(btnAceptar);
        dialog.add(panelBotones, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void onConfirmar() {
        if (arptActual == null) {
            mostrarError("Seleccione una tarea primero.");
            return;
        }
        // 1) Wizard de EPPs: el tecnico revisa/ajusta la seleccion por fila.
        if (!wizardEppParaTodas()) return;

        int idAnterior = 0;
        boolean publicada = false;
        try {
            // 2) Si el ARPT esta VIGENTE, el dialogo de version nueva hace de
            //    confirmacion: el tecnico ya vio los EPPs y acepta crear el
            //    siguiente ARPT.
            if (arptActual.getEstado() == Estado.VIGENTE) {
                if (!confirmarNuevaVersion(true)) return;
                idAnterior = crearVersionEnMemoria();
            } else {
                int r = JOptionPane.showConfirmDialog(this,
                        "Confirmar la planilla del ARPT #" + arptActual.getIdArpt() + "?",
                        "Confirmar planilla", JOptionPane.YES_NO_OPTION);
                if (r != JOptionPane.YES_OPTION) return;
            }
            int guardadas = persistirTodas(false);
            if (guardadas == 0) {
                mostrarError("No hay filas completas: cargá P, E y C en al menos un riesgo.");
                return;
            }
            // La transicion depende del rol (TP2 §38b): responsable/admin
            // llevan a VIGENTE; tecnico deja el ARPT en EDICION.
            if (idAnterior > 0 && controlador.puedeValidar(usuario)) {
                controlador.publicarVersion(usuario, idAnterior, arptActual.getIdArpt(),
                        txtObs.getText(), txtConc.getText());
                arptActual.setEstado(Estado.VIGENTE);
                publicada = true;
            } else {
                controlador.confirmarSegunRol(usuario, arptActual.getIdArpt(),
                        txtObs.getText(), txtConc.getText());
            }

            String detalle;
            if (idAnterior > 0 && publicada) {
                detalle = "ARPT #" + arptActual.getIdArpt() + " creado y VIGENTE ("
                        + guardadas + " filas). El #" + idAnterior + " paso a historico.";
            } else if (idAnterior > 0) {
                detalle = "ARPT #" + arptActual.getIdArpt() + " creado en EDICION ("
                        + guardadas + " filas): tu rol no valida. El #" + idAnterior + " sigue VIGENTE.";
            } else {
                detalle = "ARPT #" + arptActual.getIdArpt() + " confirmado ("
                        + guardadas + " filas) segun tu rol.";
            }
            mostrarMensaje(detalle, new Color(0, 100, 0));
            JOptionPane.showMessageDialog(this, detalle + "\n\nTrazado en la bitacora.",
                    "OK", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException ex) {
            mostrarError("Validacion: " + ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error: " + ex.getMessage());
        }
    }

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
        campo.setValue(3);
        return campo;
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