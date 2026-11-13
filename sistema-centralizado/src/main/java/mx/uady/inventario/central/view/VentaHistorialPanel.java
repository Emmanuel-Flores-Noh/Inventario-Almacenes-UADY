package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

/** Historial de ventas con búsqueda y filtros locales. */
public final class VentaHistorialPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final VentaHistorialTableModel modelo = new VentaHistorialTableModel();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<VentaHistorialTableModel> ordenador = new TableRowSorter<>(modelo);
    private final JTextField serie = new JTextField(12);
    private final JTextField tienda = new JTextField(10);
    private final JTextField producto = new JTextField(10);
    private final JTextField desde = new JTextField(10);
    private final JTextField hasta = new JTextField(10);
    private final JButton actualizar = new JButton("Actualizar");
    private final JButton abrirQr = new JButton("Abrir QR");
    private final JLabel estado = new JLabel(" ");
    private Runnable alActualizar = () -> { };
    private Runnable alAbrirQr = () -> { };

    public VentaHistorialPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        construirControles();
        tabla.setName("tablaHistorialVentas");
        tabla.setRowSorter(ordenador);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> abrirQr.setEnabled(seleccionada().isPresent()));
        abrirQr.setName("abrirQrVenta");
        abrirQr.setEnabled(false);
        abrirQr.addActionListener(e -> alAbrirQr.run());
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        estado.putClientProperty("FlatLaf.styleClass", "small");
        add(estado, BorderLayout.SOUTH);
    }

    private void construirControles() {
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        serie.setName("busquedaSerieHistorial");
        tienda.setName("filtroTiendaVentas");
        producto.setName("filtroProductoVentas");
        desde.setName("filtroDesdeVentas");
        hasta.setName("filtroHastaVentas");
        desde.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
        hasta.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd");
        filtros.add(new JLabel("Serie"));
        filtros.add(serie);
        filtros.add(new JLabel("Tienda"));
        filtros.add(tienda);
        filtros.add(new JLabel("Producto"));
        filtros.add(producto);
        filtros.add(new JLabel("Desde"));
        filtros.add(desde);
        filtros.add(new JLabel("Hasta"));
        filtros.add(hasta);
        actualizar.setName("actualizarHistorialVentas");
        actualizar.addActionListener(e -> alActualizar.run());
        filtros.add(actualizar);
        filtros.add(abrirQr);
        add(filtros, BorderLayout.NORTH);
        DocumentListener cambios = documento(this::aplicarFiltros);
        serie.getDocument().addDocumentListener(cambios);
        tienda.getDocument().addDocumentListener(cambios);
        producto.getDocument().addDocumentListener(cambios);
        desde.getDocument().addDocumentListener(cambios);
        hasta.getDocument().addDocumentListener(cambios);
    }

    private void aplicarFiltros() {
        String serieTexto = serie.getText().strip();
        String tiendaTexto = tienda.getText().strip();
        String productoTexto = producto.getText().strip();
        LocalDate desdeFecha = fecha(desde.getText());
        LocalDate hastaFecha = fecha(hasta.getText());
        ordenador.setRowFilter(new RowFilter<VentaHistorialTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends VentaHistorialTableModel, ? extends Integer> fila) {
                VentaHistorial venta = modelo.getFila(fila.getIdentifier());
                return contiene(venta.numeroSerie(), serieTexto)
                        && contiene(venta.tienda(), tiendaTexto)
                        && contiene(venta.producto(), productoTexto)
                        && (desdeFecha == null || !venta.fechaVenta().toLocalDate().isBefore(desdeFecha))
                        && (hastaFecha == null || !venta.fechaVenta().toLocalDate().isAfter(hastaFecha));
            }
        });
        if (!desde.getText().isBlank() && desdeFecha == null || !hasta.getText().isBlank() && hastaFecha == null) {
            estado.setText("Las fechas deben usar el formato yyyy-MM-dd.");
        } else {
            estado.setText(tabla.getRowCount() + " ventas visibles");
        }
    }

    private static boolean contiene(String valor, String filtro) {
        return filtro.isBlank() || FiltroTexto.contiene(valor, filtro);
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

    public void mostrarVentas(List<VentaHistorial> ventas) {
        modelo.setFilas(ventas);
        aplicarFiltros();
        abrirQr.setEnabled(false);
    }

    public Optional<VentaHistorial> seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? Optional.empty()
                : Optional.of(modelo.getFila(tabla.convertRowIndexToModel(fila)));
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
