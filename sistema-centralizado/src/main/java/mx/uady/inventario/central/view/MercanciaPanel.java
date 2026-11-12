package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Pantalla de mercancía: registro de unidades físicas y tabla filtrable por producto y estado.
 * No contiene lógica de negocio; {@code MercanciaController} reacciona a sus eventos.
 */
public class MercanciaPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    static final String TODOS = "Todos";
    static final String SIN_PRODUCTOS = "Primero registra un producto en la pestaña Productos.";

    // Registro
    private final JComboBox<Producto> productoRegistro = new JComboBox<>();
    private final JTextField serie = new JTextField(22);
    private final JButton registrar = new JButton("Registrar");
    private final JLabel errorSerie = new JLabel(" ");

    // Consulta
    private final JComboBox<Object> filtroProducto = new JComboBox<>();
    private final JComboBox<Object> filtroEstado = new JComboBox<>();
    private final JTextField busquedaSerie = new JTextField(16);
    private final JButton actualizar = new JButton("Actualizar");
    private final JButton verQr = new JButton("Ver QR");
    private final MercanciaTableModel modelo = new MercanciaTableModel();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<MercanciaTableModel> ordenador = new TableRowSorter<>(modelo);
    private final JLabel estado = new JLabel(" ");

    private Runnable alRegistrar = () -> { };
    private Runnable alCambiarFiltros = () -> { };
    private Runnable alVerQr = () -> { };
    /** Evita recargar mientras se rellenan los combos por código. */
    private boolean ajustandoCombos;
    private boolean cargado;
    private boolean hayProductos;

    public MercanciaPanel() {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        nombrarComponentes();

        add(construirRegistro(), BorderLayout.NORTH);
        JPanel consulta = new JPanel(new BorderLayout(0, 8));
        consulta.add(construirFiltros(), BorderLayout.NORTH);
        consulta.add(new JScrollPane(construirTabla()), BorderLayout.CENTER);
        estado.putClientProperty("FlatLaf.styleClass", "small");
        consulta.add(estado, BorderLayout.SOUTH);
        add(consulta, BorderLayout.CENTER);

        actualizarBotonRegistrar();
        verQr.setEnabled(false);
    }

    private void nombrarComponentes() {
        productoRegistro.setName("productoRegistro");
        serie.setName("serie");
        registrar.setName("registrar");
        errorSerie.setName("errorSerie");
        filtroProducto.setName("filtroProducto");
        filtroEstado.setName("filtroEstado");
        busquedaSerie.setName("busquedaSerie");
        actualizar.setName("actualizar");
        verQr.setName("verQr");
        tabla.setName("tablaUnidades");
    }

    private JPanel construirRegistro() {
        productoRegistro.setRenderer(new RenderNombres());
        productoRegistro.setPrototypeDisplayValue(new Producto(0, "Laptop Lenovo ThinkPad E14 extra", null, null));
        serie.putClientProperty("JTextField.placeholderText", "Escanea o escribe el número de serie");
        serie.addActionListener(e -> {
            if (registrar.isEnabled()) {
                alRegistrar.run();
            }
        });
        serie.getDocument().addDocumentListener(alCambiar(() -> mostrarErrorSerie(null)));
        registrar.addActionListener(e -> alRegistrar.run());
        Color rojo = UIManager.getColor("Component.error.focusedBorderColor");
        errorSerie.setForeground(rojo != null ? rojo : new Color(0xC62828));
        errorSerie.putClientProperty("FlatLaf.styleClass", "small");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Registrar unidad"),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 0, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("Producto"), c);
        c.gridx = 1;
        panel.add(productoRegistro, c);
        c.gridx = 2;
        panel.add(new JLabel("Número de serie"), c);
        c.gridx = 3; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(serie, c);
        c.gridx = 4; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 0, 0);
        panel.add(registrar, c);

        c.gridx = 3; c.gridy = 1; c.gridwidth = 2; c.insets = new Insets(2, 0, 0, 0);
        panel.add(errorSerie, c);
        return panel;
    }

    private JPanel construirFiltros() {
        filtroProducto.setRenderer(new RenderNombres());
        filtroEstado.setRenderer(new RenderNombres());
        filtroProducto.addItem(TODOS);
        filtroEstado.addItem(TODOS);
        for (EstadoMercancia e : EstadoMercancia.values()) {
            filtroEstado.addItem(e);
        }
        filtroProducto.addActionListener(e -> notificarFiltros());
        filtroEstado.addActionListener(e -> notificarFiltros());

        busquedaSerie.putClientProperty("JTextField.placeholderText", "Buscar número de serie");
        busquedaSerie.putClientProperty("JTextField.showClearButton", true);
        busquedaSerie.getDocument().addDocumentListener(alCambiar(this::aplicarBusqueda));

        actualizar.addActionListener(e -> alCambiarFiltros.run());
        verQr.addActionListener(e -> alVerQr.run());

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        izquierda.add(new JLabel("Producto"));
        izquierda.add(filtroProducto);
        izquierda.add(new JLabel("Estado"));
        izquierda.add(filtroEstado);
        izquierda.add(busquedaSerie);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        derecha.add(actualizar);
        derecha.add(verQr);

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JTable construirTabla() {
        tabla.setRowSorter(ordenador);
        ordenador.setSortKeys(List.of(new RowSorter.SortKey(MercanciaTableModel.COL_SERIE, SortOrder.ASCENDING)));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(26);
        tabla.getColumnModel().getColumn(MercanciaTableModel.COL_SERIE).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(MercanciaTableModel.COL_PRODUCTO).setPreferredWidth(320);
        tabla.getColumnModel().getColumn(MercanciaTableModel.COL_ESTADO).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(MercanciaTableModel.COL_QR).setPreferredWidth(90);

        tabla.getSelectionModel().addListSelectionListener(e -> verQr.setEnabled(seleccionada().isPresent()));
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0 && verQr.isEnabled()) {
                    alVerQr.run();
                }
            }
        });
        return tabla;
    }

    private void notificarFiltros() {
        if (!ajustandoCombos) {
            alCambiarFiltros.run();
        }
    }

    private void aplicarBusqueda() {
        String texto = busquedaSerie.getText();
        ordenador.setRowFilter(texto.isBlank() ? null : new RowFilter<MercanciaTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends MercanciaTableModel, ? extends Integer> fila) {
                return FiltroTexto.contiene(fila.getStringValue(MercanciaTableModel.COL_SERIE), texto);
            }
        });
        actualizarResumen();
    }

    private void actualizarResumen() {
        if (!cargado) {
            return;
        }
        int total = modelo.getRowCount();
        int visibles = tabla.getRowCount();
        boolean hayFiltros = filtroProducto.getSelectedIndex() > 0 || filtroEstado.getSelectedIndex() > 0;
        if (total == 0) {
            estado.setText(hayFiltros ? "No hay unidades con los filtros elegidos." : "No hay unidades registradas.");
        } else if (visibles == 0) {
            estado.setText("Ninguna unidad coincide con «" + busquedaSerie.getText().strip() + "».");
        } else if (visibles < total) {
            estado.setText(visibles + " de " + total + " unidades");
        } else {
            estado.setText(total == 1 ? "1 unidad" : total + " unidades");
        }
    }

    private void actualizarBotonRegistrar() {
        registrar.setEnabled(hayProductos);
        productoRegistro.setEnabled(hayProductos);
        serie.setEnabled(hayProductos);
        if (!hayProductos && cargado) {
            mostrarErrorSerie(SIN_PRODUCTOS);
        } else if (SIN_PRODUCTOS.equals(errorSerie.getText())) {
            mostrarErrorSerie(null);
        }
    }

    // ---- API para el controlador ----

    public void alRegistrar(Runnable accion) {
        this.alRegistrar = accion;
    }

    public void alCambiarFiltros(Runnable accion) {
        this.alCambiarFiltros = accion;
    }

    public void alVerQr(Runnable accion) {
        this.alVerQr = accion;
    }

    /** Rellena ambos combos conservando la selección actual cuando el producto sigue existiendo. */
    public void mostrarProductos(List<Producto> productos) {
        Integer idRegistro = productoRegistro.getSelectedItem() instanceof Producto p ? p.idProducto() : null;
        Integer idFiltro = filtroProducto.getSelectedItem() instanceof Producto p ? p.idProducto() : null;

        ajustandoCombos = true;
        try {
            productoRegistro.removeAllItems();
            filtroProducto.removeAllItems();
            filtroProducto.addItem(TODOS);
            for (Producto p : productos) {
                productoRegistro.addItem(p);
                filtroProducto.addItem(p);
            }
            seleccionarPorId(productoRegistro, idRegistro);
            seleccionarPorId(filtroProducto, idFiltro);
        } finally {
            ajustandoCombos = false;
        }
        hayProductos = !productos.isEmpty();
        cargado = true;
        actualizarBotonRegistrar();
    }

    public void mostrarUnidades(List<Mercancia> unidades, List<Producto> productos) {
        Map<Integer, String> nombres = productos.stream()
                .collect(Collectors.toMap(Producto::idProducto, Producto::nombre, (a, b) -> a));
        cargado = true;
        modelo.setFilas(unidades, nombres);
        actualizarResumen();
    }

    public void actualizarUnidad(Mercancia unidad) {
        modelo.actualizar(unidad);
    }

    /** @return {@code true} si la unidad está visible con los filtros actuales y quedó seleccionada */
    public boolean seleccionarUnidad(String numeroSerie) {
        int enModelo = modelo.indiceDe(numeroSerie);
        int enVista = enModelo < 0 ? -1 : tabla.convertRowIndexToView(enModelo);
        if (enVista < 0) {
            return false;
        }
        tabla.setRowSelectionInterval(enVista, enVista);
        tabla.scrollRectToVisible(tabla.getCellRect(enVista, 0, true));
        return true;
    }

    public Optional<Mercancia> seleccionada() {
        int enVista = tabla.getSelectedRow();
        return enVista < 0 ? Optional.empty() : Optional.of(modelo.getFila(tabla.convertRowIndexToModel(enVista)));
    }

    public String nombreProducto(int idProducto) {
        return modelo.nombreProducto(idProducto);
    }

    public Optional<Producto> productoParaRegistrar() {
        return Optional.ofNullable((Producto) productoRegistro.getSelectedItem());
    }

    public String serieCapturada() {
        return serie.getText();
    }

    public void limpiarSerie() {
        serie.setText("");
        serie.requestFocusInWindow();
    }

    /** Muestra (o borra, con {@code null}) el error del número de serie y resalta el campo. */
    public void mostrarErrorSerie(String error) {
        serie.putClientProperty("JComponent.outline", error == null ? null : "error");
        errorSerie.setText(error == null ? " " : error);
        if (error != null) {
            serie.selectAll();
            serie.requestFocusInWindow();
        }
    }

    /** Producto elegido en el filtro, o {@code null} para todos. */
    public Integer filtroProductoSeleccionado() {
        return filtroProducto.getSelectedItem() instanceof Producto p ? p.idProducto() : null;
    }

    /** Estado elegido en el filtro, o {@code null} para todos. */
    public EstadoMercancia filtroEstadoSeleccionado() {
        return filtroEstado.getSelectedItem() instanceof EstadoMercancia e ? e : null;
    }

    public void setEstado(String texto) {
        estado.setText(texto);
    }

    /** Controles que se bloquean mientras hay una operación en curso. */
    public Component[] controles() {
        return new Component[] {productoRegistro, serie, registrar, filtroProducto, filtroEstado, actualizar, verQr};
    }

    // ---- Auxiliares ----

    private static void seleccionarPorId(JComboBox<?> combo, Integer idProducto) {
        if (idProducto == null) {
            if (combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            }
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i) instanceof Producto p && p.idProducto() == idProducto) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    private static DocumentListener alCambiar(Runnable accion) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { accion.run(); }
            @Override public void removeUpdate(DocumentEvent e) { accion.run(); }
            @Override public void changedUpdate(DocumentEvent e) { accion.run(); }
        };
    }

    /** Muestra productos por nombre y estados con texto legible. */
    private static final class RenderNombres extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                boolean seleccionado, boolean foco) {
            Object texto = valor instanceof Producto p ? p.nombre()
                    : valor instanceof EstadoMercancia e ? MercanciaTableModel.texto(e)
                    : valor;
            return super.getListCellRendererComponent(lista, texto, indice, seleccionado, foco);
        }
    }
}
