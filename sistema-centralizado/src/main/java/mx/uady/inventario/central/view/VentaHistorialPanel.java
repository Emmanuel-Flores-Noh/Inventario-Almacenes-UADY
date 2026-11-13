package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import mx.uady.inventario.central.model.VentaHistorial;

/** Historial de ventas con filtros por tienda, producto, rango de fechas y número de serie. */
public final class VentaHistorialPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    static final String TODAS = "Todas";
    static final String TODOS = "Todos";

    private final VentaHistorialTableModel modelo = new VentaHistorialTableModel();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<VentaHistorialTableModel> ordenador = new TableRowSorter<>(modelo);
    private final JTextField serie = new JTextField(12);
    private final JComboBox<String> tienda = new JComboBox<>();
    private final JComboBox<String> producto = new JComboBox<>();
    private final JTextField desde = new JTextField(9);
    private final JTextField hasta = new JTextField(9);
    private final JButton actualizar = new JButton("Actualizar");
    private final JButton abrirQr = new JButton("Abrir QR");
    private final JLabel estado = new JLabel(" ");
    private Runnable alActualizar = () -> { };
    private Runnable alAbrirQr = () -> { };
    private boolean ajustandoCombos;

    public VentaHistorialPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        construirControles();
        tabla.setName("tablaHistorialVentas");
        tabla.setRowSorter(ordenador);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(26);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> abrirQr.setEnabled(seleccionada().isPresent()));
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0 && abrirQr.isEnabled()) {
                    alAbrirQr.run();
                }
            }
        });
        abrirQr.setName("abrirQrVenta");
        abrirQr.setEnabled(false);
        abrirQr.addActionListener(e -> alAbrirQr.run());
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        estado.putClientProperty("FlatLaf.styleClass", "small");
        add(estado, BorderLayout.SOUTH);
    }

    private void construirControles() {
        serie.setName("busquedaSerieHistorial");
        tienda.setName("filtroTiendaVentas");
        producto.setName("filtroProductoVentas");
        desde.setName("filtroDesdeVentas");
        hasta.setName("filtroHastaVentas");
        actualizar.setName("actualizarHistorialVentas");

        serie.putClientProperty("JTextField.placeholderText", "Número de serie");
        serie.putClientProperty("JTextField.showClearButton", true);
        desde.putClientProperty("JTextField.placeholderText", "aaaa-mm-dd");
        hasta.putClientProperty("JTextField.placeholderText", "aaaa-mm-dd");
        tienda.addItem(TODAS);
        producto.addItem(TODOS);

        DocumentListener cambios = documento(this::aplicarFiltros);
        serie.getDocument().addDocumentListener(cambios);
        desde.getDocument().addDocumentListener(cambios);
        hasta.getDocument().addDocumentListener(cambios);
        tienda.addActionListener(e -> {
            if (!ajustandoCombos) {
                aplicarFiltros();
            }
        });
        producto.addActionListener(e -> {
            if (!ajustandoCombos) {
                aplicarFiltros();
            }
        });
        actualizar.addActionListener(e -> alActualizar.run());

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtros.add(serie);
        filtros.add(new JLabel("Tienda"));
        filtros.add(tienda);
        filtros.add(new JLabel("Producto"));
        filtros.add(producto);
        filtros.add(new JLabel("Desde"));
        filtros.add(desde);
        filtros.add(new JLabel("Hasta"));
        filtros.add(hasta);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acciones.add(actualizar);
        acciones.add(abrirQr);

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(filtros, BorderLayout.CENTER);
        barra.add(acciones, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);
    }

    private void aplicarFiltros() {
        String serieTexto = serie.getText().strip();
        String tiendaElegida = elegido(tienda);
        String productoElegido = elegido(producto);
        LocalDate desdeFecha = fecha(desde.getText());
        LocalDate hastaFecha = fecha(hasta.getText());
        ordenador.setRowFilter(new RowFilter<VentaHistorialTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends VentaHistorialTableModel, ? extends Integer> fila) {
                VentaHistorial venta = modelo.getFila(fila.getIdentifier());
                return FiltroTexto.contiene(venta.numeroSerie(), serieTexto)
                        && (tiendaElegida == null || tiendaElegida.equals(venta.tienda()))
                        && (productoElegido == null || productoElegido.equals(venta.producto()))
                        && (desdeFecha == null || !venta.fechaVenta().toLocalDate().isBefore(desdeFecha))
                        && (hastaFecha == null || !venta.fechaVenta().toLocalDate().isAfter(hastaFecha));
            }
        });
        actualizarResumen(!desde.getText().isBlank() && desdeFecha == null
                || !hasta.getText().isBlank() && hastaFecha == null);
    }

    private void actualizarResumen(boolean fechaInvalida) {
        int total = modelo.getRowCount();
        int visibles = tabla.getRowCount();
        if (fechaInvalida) {
            estado.setText("Las fechas deben escribirse como aaaa-mm-dd, por ejemplo 2026-11-12.");
        } else if (total == 0) {
            estado.setText("Todavía no hay ventas registradas.");
        } else if (visibles == 0) {
            estado.setText("Ninguna venta coincide con los filtros.");
        } else if (visibles < total) {
            estado.setText(visibles + " de " + total + " ventas");
        } else {
            estado.setText(total == 1 ? "1 venta" : total + " ventas");
        }
    }

    /** Valor elegido en un combo de filtro, o {@code null} si es "Todas"/"Todos". */
    private static String elegido(JComboBox<String> combo) {
        return combo.getSelectedIndex() > 0 ? (String) combo.getSelectedItem() : null;
    }

    /** Rellena un combo con los valores distintos del historial, conservando la opción elegida. */
    private void rellenar(JComboBox<String> combo, String opcionTodos, List<VentaHistorial> ventas,
            Function<VentaHistorial, String> valor) {
        String anterior = elegido(combo);
        combo.removeAllItems();
        combo.addItem(opcionTodos);
        TreeSet<String> valores = new TreeSet<>(ventas.stream().map(valor).filter(Objects::nonNull).toList());
        valores.forEach(combo::addItem);
        combo.setSelectedItem(anterior != null && valores.contains(anterior) ? anterior : opcionTodos);
    }

    private static LocalDate fecha(String texto) {
        if (texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.strip());
        } catch (DateTimeParseException error) {
            return null;
        }
    }

    private static DocumentListener documento(Runnable accion) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { accion.run(); }
            @Override public void removeUpdate(DocumentEvent e) { accion.run(); }
            @Override public void changedUpdate(DocumentEvent e) { accion.run(); }
        };
    }

    // ---- API para el controlador ----

    public void mostrarVentas(List<VentaHistorial> ventas) {
        ajustandoCombos = true;
        try {
            rellenar(tienda, TODAS, ventas, VentaHistorial::tienda);
            rellenar(producto, TODOS, ventas, VentaHistorial::producto);
        } finally {
            ajustandoCombos = false;
        }
        modelo.setFilas(ventas);
        aplicarFiltros();
    }

    public Optional<VentaHistorial> seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? Optional.empty()
                : Optional.of(modelo.getFila(tabla.convertRowIndexToModel(fila)));
    }

    public void setEstado(String texto) {
        estado.setText(texto);
    }

    public void alActualizar(Runnable accion) {
        alActualizar = accion;
    }

    public void alAbrirQr(Runnable accion) {
        alAbrirQr = accion;
    }

    public Componentes controles() {
        return new Componentes(actualizar, abrirQr, tabla);
    }

    public record Componentes(JButton actualizar, JButton abrirQr, JTable tabla) {
    }
}
