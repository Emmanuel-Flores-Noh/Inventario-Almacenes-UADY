package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import mx.uady.inventario.central.model.ProductoConUnidades;

/**
 * Pantalla de productos: tabla con búsqueda por nombre y acciones para crear o editar.
 * No contiene lógica de negocio; {@code ProductoController} reacciona a sus eventos.
 */
public class ProductosPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(Locale.of("es", "MX"));

    private final ProductosTableModel modelo = new ProductosTableModel();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ProductosTableModel> ordenador = new TableRowSorter<>(modelo);
    private final JTextField busqueda = new JTextField(24);
    private final JButton nuevo = new JButton("Nuevo");
    private final JButton editar = new JButton("Editar");
    private final JButton actualizar = new JButton("Actualizar");
    private final JLabel estado = new JLabel(" ");

    private Consumer<ProductoConUnidades> alEditar = fila -> { };
    private boolean cargado;

    public ProductosPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(construirBarra(), BorderLayout.NORTH);
        add(new JScrollPane(construirTabla()), BorderLayout.CENTER);
        estado.putClientProperty("FlatLaf.styleClass", "small");
        add(estado, BorderLayout.SOUTH);

        editar.setEnabled(false);
    }

    private JPanel construirBarra() {
        busqueda.putClientProperty("JTextField.placeholderText", "Buscar por nombre");
        busqueda.putClientProperty("JTextField.showClearButton", true);
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltro(); }
        });

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        izquierda.add(busqueda);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        derecha.add(actualizar);
        derecha.add(editar);
        derecha.add(nuevo);

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JTable construirTabla() {
        tabla.setRowSorter(ordenador);
        ordenador.setSortKeys(List.of(new RowSorter.SortKey(ProductosTableModel.COL_NOMBRE, SortOrder.ASCENDING)));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(26);

        tabla.getColumnModel().getColumn(ProductosTableModel.COL_NOMBRE).setPreferredWidth(240);
        tabla.getColumnModel().getColumn(ProductosTableModel.COL_DESCRIPCION).setPreferredWidth(360);
        tabla.getColumnModel().getColumn(ProductosTableModel.COL_PRECIO).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(ProductosTableModel.COL_UNIDADES).setPreferredWidth(80);

        tabla.getColumnModel().getColumn(ProductosTableModel.COL_PRECIO).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object valor) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(valor instanceof BigDecimal precio ? MONEDA.format(precio) : "");
            }
        });
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.getColumnModel().getColumn(ProductosTableModel.COL_UNIDADES).setCellRenderer(centrado);

        tabla.getSelectionModel().addListSelectionListener(e -> editar.setEnabled(seleccionado().isPresent()));
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0 && editar.isEnabled()) {
                    seleccionado().ifPresent(alEditar);
                }
            }
        });
        editar.addActionListener(e -> seleccionado().ifPresent(alEditar));
        return tabla;
    }

    private void aplicarFiltro() {
        String texto = busqueda.getText();
        ordenador.setRowFilter(texto.isBlank() ? null : new RowFilter<ProductosTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends ProductosTableModel, ? extends Integer> fila) {
                return FiltroTexto.contiene(fila.getStringValue(ProductosTableModel.COL_NOMBRE), texto);
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
        if (total == 0) {
            estado.setText("No hay productos registrados. Usa «Nuevo» para agregar uno.");
        } else if (visibles == 0) {
            estado.setText("Ningún producto coincide con «" + busqueda.getText().strip() + "».");
        } else if (visibles < total) {
            estado.setText(visibles + " de " + total + " productos");
        } else {
            estado.setText(total == 1 ? "1 producto" : total + " productos");
        }
    }

    // ---- API para el controlador ----

    public void alNuevo(Runnable accion) {
        nuevo.addActionListener(e -> accion.run());
    }

    public void alEditar(Consumer<ProductoConUnidades> accion) {
        this.alEditar = accion;
    }

    public void alActualizar(Runnable accion) {
        actualizar.addActionListener(e -> accion.run());
    }

    public void mostrarProductos(List<ProductoConUnidades> productos) {
        cargado = true;
        modelo.setFilas(productos);
        actualizarResumen();
    }

    /** Selecciona y hace visible el producto indicado, si está entre las filas visibles. */
    public void seleccionarProducto(int idProducto) {
        int enModelo = modelo.indiceDe(idProducto);
        int enVista = enModelo < 0 ? -1 : tabla.convertRowIndexToView(enModelo);
        if (enVista >= 0) {
            tabla.setRowSelectionInterval(enVista, enVista);
            tabla.scrollRectToVisible(tabla.getCellRect(enVista, 0, true));
        }
    }

    public Optional<ProductoConUnidades> seleccionado() {
        int enVista = tabla.getSelectedRow();
        return enVista < 0 ? Optional.empty() : Optional.of(modelo.getFila(tabla.convertRowIndexToModel(enVista)));
    }

    public void setEstado(String texto) {
        estado.setText(texto);
    }

    /** Controles que se bloquean mientras hay una operación en curso. */
    public Component[] controles() {
        return new Component[] {nuevo, editar, actualizar};
    }

    ProductosTableModel getModelo() {
        return modelo;
    }

    JTable getTabla() {
        return tabla;
    }

    JTextField getBusqueda() {
        return busqueda;
    }
}
