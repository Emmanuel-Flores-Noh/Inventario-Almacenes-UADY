package mx.uady.inventario.cliente.view;

import com.formdev.flatlaf.FlatClientProperties;
import mx.uady.inventario.cliente.controller.RegistroVentaController;
import mx.uady.inventario.cliente.model.TiendaDTO;
import mx.uady.inventario.cliente.model.UsuarioDTO;
import mx.uady.inventario.cliente.model.MercanciaDTO;
import mx.uady.inventario.cliente.view.components.QrPreviewDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana gráfica principal del Sistema Proveedor para la gestión y registro de ventas de mercancía.
 * Permite visualizar el inventario disponible, filtrar registros, consultar códigos QR asociados 
 * a cada unidad mediante doble clic y ejecutar transacciones conectadas al broker central.
 * 
 * @author UADY
 * @version 1.2
 */
public class RegistroVentaFrame extends JFrame {
    private JComboBox<TiendaDTO> comboTiendas;
    private JComboBox<UsuarioDTO> comboUsuarios;
    private JTable tablaMercancia;
    private DefaultTableModel modeloTabla;
    private JTextField txtBuscarMercancia;
    private JButton btnRegistrarVenta;
    private JButton btnRefrescar;
    private JLabel lblIndicadorCarga;

    private final RegistroVentaController controller;
    private QrPreviewDialog qrDialogActual;
    /** Unidades mostradas en la tabla, en el mismo orden que las filas del modelo. */
    private final List<MercanciaDTO> unidadesEnTabla = new ArrayList<>();

    /**
     * Constructor principal que inicializa el controlador asociado, configura la ventana 
     * y carga de manera asíncrona los catálogos iniciales de tiendas, usuarios y mercancías.
     */
    public RegistroVentaFrame() {
        this.controller = new RegistroVentaController(this);
        configurarVentana();
        inicializarComponentes();
        controller.cargarListasIniciales();
    }

    /**
     * Configura los parámetros básicos del JFrame (título, dimensiones, operación de cierre predeterminada y posición centrada).
     */
    private void configurarVentana() {
        setTitle("Sistema Proveedor - Registro de Ventas (UADY)");
        setSize(950, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Inicializa, organiza y aplica diseño moderno con FlatLaf a todos los componentes de la interfaz gráfica.
     */
    private void inicializarComponentes() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel contenedor superior que incluye el título de sección y la barra de búsqueda
        JPanel panelNorteCompleto = new JPanel(new BorderLayout(10, 15));
        panelNorteCompleto.add(crearPanelTitulo(), BorderLayout.NORTH);
        panelNorteCompleto.add(crearPanelSuperior(), BorderLayout.SOUTH);

        panelPrincipal.add(panelNorteCompleto, BorderLayout.NORTH);
        panelPrincipal.add(crearPanelCentro(), BorderLayout.CENTER);
        panelPrincipal.add(crearPanelFormulario(), BorderLayout.SOUTH);

        add(panelPrincipal);
        configurarEventos();
    }

    /**
     * Crea un panel de título institucional para indicar claramente la sección y propósito de la pantalla.
     * 
     * @return Panel superior con el título y subtítulo descriptivo.
     */
    private JPanel crearPanelTitulo() {
        JPanel panelTitulo = new JPanel(new BorderLayout(0, 5));
        
        JLabel lblTituloPrincipal = new JLabel("Módulo de Proveedor - Registro y Trazabilidad de Ventas");
        lblTituloPrincipal.setFont(lblTituloPrincipal.getFont().deriveFont(Font.BOLD, 18f));
        
        JLabel lblSubtitulo = new JLabel("Seleccione una unidad disponible, verifique su código QR con doble clic y registre la venta.");
        lblSubtitulo.setForeground(new Color(110, 110, 110));
        
        panelTitulo.add(lblTituloPrincipal, BorderLayout.NORTH);
        panelTitulo.add(lblSubtitulo, BorderLayout.SOUTH);
        
        return panelTitulo;
    }

    /**
     * Crea y organiza el panel superior que contiene el campo de filtrado por texto y el botón de actualización.
     * 
     * @return Panel superior configurado como un {@link JPanel}.
     */
    private JPanel crearPanelSuperior() {
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 0));
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        txtBuscarMercancia = new JTextField(20);
        txtBuscarMercancia.setName("txtBuscarMercancia");
        txtBuscarMercancia.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por serie o producto...");
        
        btnRefrescar = new JButton("Actualizar Listas");
        btnRefrescar.setName("btnRefrescar");

        panelBusqueda.add(new JLabel("Filtrar:"));
        panelBusqueda.add(txtBuscarMercancia);
        panelBusqueda.add(btnRefrescar);

        lblIndicadorCarga = new JLabel("Estado: Listo");
        lblIndicadorCarga.setForeground(new Color(100, 100, 100));

        panelSuperior.add(panelBusqueda, BorderLayout.WEST);
        panelSuperior.add(lblIndicadorCarga, BorderLayout.EAST);
        return panelSuperior;
    }

    /**
     * Crea y configura la tabla central de mercancías con sus respectivas columnas y el gestor de filtrado.
     * 
     * @return Contenedor {@link JScrollPane} que envuelve la tabla de mercancía.
     */
    private JScrollPane crearPanelCentro() {
        String[] columnas = {"N° Serie", "Producto", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaMercancia = new JTable(modeloTabla);
        tablaMercancia.setFillsViewportHeight(true);
        tablaMercancia.setRowHeight(25);
        tablaMercancia.setName("tablaMercancia");
        tablaMercancia.getTableHeader().setReorderingAllowed(false);
        
        // Inicializar gestor de filtrado en tiempo real
        new MercanciaFiltroManager(tablaMercancia, modeloTabla, txtBuscarMercancia);

        JScrollPane scrollTabla = new JScrollPane(tablaMercancia);
        scrollTabla.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        return scrollTabla;
    }

    /**
     * Crea el panel inferior del formulario que agrupa los selectores de tienda, proveedor 
     * y el botón para registrar la venta.
     * 
     * @return Panel de formulario configurado con un {@link GridBagLayout}.
     */
    private JPanel crearPanelFormulario() {
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBorder(BorderFactory.createTitledBorder("Datos de Registro de Venta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        panelFormulario.add(new JLabel("Tienda Destino:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.9;
        comboTiendas = new JComboBox<>();
        comboTiendas.setName("comboTiendas");
        panelFormulario.add(comboTiendas, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        panelFormulario.add(new JLabel("Usuario Proveedor:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.9;
        comboUsuarios = new JComboBox<>();
        comboUsuarios.setName("comboUsuarios");
        panelFormulario.add(comboUsuarios, gbc);

        // Botón Registrar Venta
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        btnRegistrarVenta = new JButton("Registrar Venta de Unidad Seleccionada");
        // Estilo limpio con FlatLaf: aseguramos contraste óptimo sin forzar colores de texto conflictivos
        btnRegistrarVenta.putClientProperty(FlatClientProperties.STYLE, 
                "background: $accentColor; " +
                "foreground: #555555ff; " +
                "font: bold; " +
                "arc: 6");
        btnRegistrarVenta.setName("btnRegistrarVenta");
        btnRegistrarVenta.setEnabled(false); // Inicia deshabilitado hasta cumplir condiciones
        
        panelFormulario.add(btnRegistrarVenta, gbc);

        return panelFormulario;
    }

    /**
     * Configura y registra todos los oyentes de eventos (ActionListeners, ListSelectionListeners y MouseListeners) 
     * para la interacción de los componentes de la interfaz.
     */
    private void configurarEventos() {
        btnRefrescar.addActionListener(e -> controller.cargarListasIniciales());

        tablaMercancia.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                validarFormulario();
            }
        });

        // Oyente de doble clic para desplegar automáticamente el código QR de la unidad seleccionada
        tablaMercancia.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaMercancia.getSelectedRow() != -1) {
                    abrirModalQrSeleccionado();
                }
            }
        });

        comboTiendas.addActionListener(e -> validarFormulario());
        comboUsuarios.addActionListener(e -> validarFormulario());

        btnRegistrarVenta.addActionListener(e -> {
            int filaSeleccionadaVista = tablaMercancia.getSelectedRow();
            if (filaSeleccionadaVista == -1) {
                mostrarMensajeError("Por favor, seleccione una unidad de mercancía de la tabla.");
                return;
            }

            MercanciaDTO unidad = unidadEnFila(filaSeleccionadaVista);
            TiendaDTO tiendaSeleccionada = (TiendaDTO) comboTiendas.getSelectedItem();
            UsuarioDTO usuarioSeleccionado = (UsuarioDTO) comboUsuarios.getSelectedItem();

            controller.ejecutarRegistroVenta(unidad.getNumeroSerie(), unidad.getNombreProducto(),
                    tiendaSeleccionada, usuarioSeleccionado);
        });
    }

    /**
     * Solicita al controlador el código QR de la unidad seleccionada en la tabla.
     * La imagen se obtiene del Sistema 1 a través del Broker y se muestra con {@link #mostrarQr}.
     */
    private void abrirModalQrSeleccionado() {
        int filaSeleccionadaVista = tablaMercancia.getSelectedRow();
        if (filaSeleccionadaVista != -1) {
            controller.consultarQr(unidadEnFila(filaSeleccionadaVista).getNumeroSerie());
        }
    }

    /**
     * Obtiene la unidad correspondiente a una fila visible de la tabla, considerando el filtro activo.
     *
     * @param filaVista índice de la fila en la vista (no en el modelo)
     * @return unidad de mercancía mostrada en esa fila
     */
    private MercanciaDTO unidadEnFila(int filaVista) {
        return unidadesEnTabla.get(tablaMercancia.convertRowIndexToModel(filaVista));
    }

    /**
     * Muestra el diálogo con el código QR de una unidad. Si ya hay un diálogo abierto, lo cierra
     * antes para mantener una única instancia activa.
     *
     * @param numeroSerie    número de serie de la unidad
     * @param idProducto     identificador del producto
     * @param nombreProducto nombre del producto; puede ser nulo
     * @param png            bytes de la imagen PNG del QR
     */
    public void mostrarQr(String numeroSerie, int idProducto, String nombreProducto, byte[] png) {
        if (qrDialogActual != null && qrDialogActual.isVisible()) {
            qrDialogActual.dispose();
        }
        qrDialogActual = new QrPreviewDialog(this, numeroSerie, idProducto, nombreProducto, png);
        qrDialogActual.setVisible(true);
    }

    /**
     * Deja el formulario en su estado inicial: sin unidad seleccionada, sin tienda ni usuario
     * elegidos y sin texto de búsqueda.
     */
    public void limpiarFormulario() {
        tablaMercancia.clearSelection();
        comboTiendas.setSelectedIndex(-1);
        comboUsuarios.setSelectedIndex(-1);
        txtBuscarMercancia.setText("");
        validarFormulario();
    }

    /**
     * Actualiza los elementos de los componentes desplegables (ComboBox) de tiendas y usuarios proveedores.
     * 
     * @param tiendas  Lista de objetos {@link TiendaDTO} a mostrar en el selector correspondiente.
     * @param usuarios Lista de objetos {@link UsuarioDTO} a mostrar en el selector correspondiente.
     */
    public void actualizarListasCombos(List<TiendaDTO> tiendas, List<UsuarioDTO> usuarios) {
        TiendaDTO tiendaPrevia = (TiendaDTO) comboTiendas.getSelectedItem();
        UsuarioDTO usuarioPrevio = (UsuarioDTO) comboUsuarios.getSelectedItem();

        comboTiendas.removeAllItems();
        int indiceTienda = -1;
        for (TiendaDTO t : tiendas) {
            comboTiendas.addItem(t);
            if (tiendaPrevia != null && java.util.Objects.equals(t.getIdTienda(), tiendaPrevia.getIdTienda())) {
                indiceTienda = comboTiendas.getItemCount() - 1;
            }
        }
        comboTiendas.setSelectedIndex(indiceTienda);

        comboUsuarios.removeAllItems();
        int indiceUsuario = -1;
        for (UsuarioDTO u : usuarios) {
            comboUsuarios.addItem(u);
            if (usuarioPrevio != null && java.util.Objects.equals(u.getIdUsuario(), usuarioPrevio.getIdUsuario())) {
                indiceUsuario = comboUsuarios.getItemCount() - 1;
            }
        }
        comboUsuarios.setSelectedIndex(indiceUsuario);
        validarFormulario();
    }

    /**
     * Actualiza la tabla principal con las unidades de mercancía disponibles,
     * indicando la disponibilidad de consulta QR mediante doble clic.
     *
     * @param listaMercancia Lista de objetos {@link MercanciaDTO} a desplegar en la tabla principal.
     */
    public void actualizarTablaMercancia(List<MercanciaDTO> listaMercancia) {
        unidadesEnTabla.clear();
        modeloTabla.setRowCount(0);
        for (MercanciaDTO m : listaMercancia) {
            unidadesEnTabla.add(m);
            modeloTabla.addRow(new Object[]{
                m.getNumeroSerie(),
                m.getNombreProducto(),
                m.getEstado(),
            });
        }
        validarFormulario();
    }

    /**
     * Muestra un indicador visual del estado actual de procesamiento y habilita o deshabilita 
     * los controles de la interfaz para prevenir interacciones concurrentes indeseadas.
     * 
     * @param mensaje  Texto descriptivo del estado actual de la operación en curso.
     * @param cargando Indicador booleano ({@code true} si está ejecutando una tarea de red, {@code false} en caso contrario).
     */
    public void mostrarIndicadorCarga(String mensaje, boolean cargando) {
        lblIndicadorCarga.setText(mensaje);
        btnRefrescar.setEnabled(!cargando);
        txtBuscarMercancia.setEnabled(!cargando);
        comboTiendas.setEnabled(!cargando);
        comboUsuarios.setEnabled(!cargando);
        
        if (cargando) {
            btnRegistrarVenta.setEnabled(false);
        } else {
            validarFormulario();
        }
    }

    /**
     * Muestra un cuadro de diálogo emergente informando al usuario de un error ocurrido durante la ejecución.
     * 
     * @param mensaje El texto detallado y comprensible del error.
     */
    public void mostrarMensajeError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error de Operación", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Muestra un cuadro de diálogo emergente confirmando el éxito de una operación transaccional.
     * 
     * @param mensaje El texto descriptivo del éxito obtenido.
     */
    public void mostrarMensajeExito(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Éxito en el Registro", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Valida las reglas de negocio del formulario: comprueba que no haya selecciones vacías 
     * y que exista una fila activa en la tabla antes de habilitar el botón de envío.
     */
    private void validarFormulario() {
        boolean seleccionValida = tablaMercancia != null && tablaMercancia.getSelectedRow() != -1;
        boolean tiendaValida = comboTiendas != null && comboTiendas.getSelectedItem() != null;
        boolean usuarioValido = comboUsuarios != null && comboUsuarios.getSelectedItem() != null;
        
        if (btnRegistrarVenta != null) {
            btnRegistrarVenta.setEnabled(seleccionValida && tiendaValida && usuarioValido);
        }
    }

    /**
     * Muestra un diálogo de advertencia en caso de fallo de red o timeout, 
     * ofreciendo al usuario la opción de reintentar la acción fallida.
     * 
     * @param mensaje          El texto descriptivo del fallo ocurrido.
     * @param accionReintentar {@link Runnable} que encapsula la tarea a reejecutar si el usuario acepta.
     */
    public void mostrarDialogoReintentar(String mensaje, Runnable accionReintentar) {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                mensaje + "\n¿Desea reintentar la operación?",
                "Fallo de Conexión o Tiempo de Espera",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (opcion == JOptionPane.YES_OPTION) {
            accionReintentar.run();
        }
    }
}