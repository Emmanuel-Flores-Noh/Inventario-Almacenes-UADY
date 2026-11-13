package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.TiendaAlmacen;

/**
 * Pantalla visual para seleccionar una mercancía y preparar una venta.
 * La carga de datos y el registro de la venta pertenecen al controlador.
 */
public final class VentaPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final VentaTableModel modelo = new VentaTableModel();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<VentaTableModel> ordenador = new TableRowSorter<>(modelo);
    private final JTextField busqueda = new JTextField(18);
    private final JComboBox<TiendaAlmacen> tienda = new JComboBox<>();
    private final JTextField usuario = new JTextField(8);
    private final JButton actualizar = new JButton("Actualizar");
    private final JButton vender = new JButton("Registrar venta");
    private final JLabel estado = new JLabel(" ");

    private Runnable alActualizar = () -> { };
    private Runnable alVender = () -> { };

    public VentaPanel() {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        nombrarComponentes();

        add(construirFormulario(), BorderLayout.NORTH);
        add(new JScrollPane(construirTabla()), BorderLayout.CENTER);
        estado.putClientProperty("FlatLaf.styleClass", "small");
        add(estado, BorderLayout.SOUTH);
        actualizarDisponibilidad();
    }

    private void nombrarComponentes() {
        busqueda.setName("busquedaVentas");
        tienda.setName("tiendaVenta");
        usuario.setName("usuarioVenta");
        actualizar.setName("actualizarVentas");
        vender.setName("registrarVenta");
        tabla.setName("tablaVentas");
    }

    private JPanel construirFormulario() {
        busqueda.putClientProperty("JTextField.placeholderText", "Buscar número de serie");
        busqueda.putClientProperty("JTextField.showClearButton", true);
        busqueda.getDocument().addDocumentListener(documento(this::aplicarBusqueda));

        tienda.setRenderer(new RenderTienda());
        tienda.addActionListener(e -> actualizarDisponibilidad());
        usuario.putClientProperty("JTextField.placeholderText", "ID usuario");
        usuario.getDocument().addDocumentListener(documento(this::actualizarDisponibilidad));
        actualizar.addActionListener(e -> alActualizar.run());
        vender.addActionListener(e -> alVender.run());

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Registrar venta"),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 0, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        formulario.add(new JLabel("Tienda"), c);
        c.gridx = 1;
        formulario.add(tienda, c);
        c.gridx = 2;
        formulario.add(new JLabel("ID usuario"), c);
        c.gridx = 3;
        formulario.add(usuario, c);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        izquierda.add(busqueda);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        derecha.add(actualizar);
        derecha.add(vender);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.insets = new Insets(8, 0, 0, 0);
        formulario.add(izquierda, c);
        c.gridx = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        formulario.add(derecha, c);
        return formulario;
    }

    private JTable construirTabla() {
        tabla.setRowSorter(ordenador);
        ordenador.setSortKeys(List.of(new RowSorter.SortKey(VentaTableModel.COL_SERIE, SortOrder.ASCENDING)));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(26);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarDisponibilidad());
        return tabla;
    }

    private void aplicarBusqueda() {
        String texto = busqueda.getText();
        ordenador.setRowFilter(texto.isBlank() ? null : new RowFilter<VentaTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends VentaTableModel, ? extends Integer> fila) {
                return FiltroTexto.contiene(fila.getStringValue(VentaTableModel.COL_SERIE), texto);
            }
        });
        actualizarResumen();
    }

    private void actualizarResumen() {
        int total = modelo.getRowCount();
        int visibles = tabla.getRowCount();
        if (total == 0) {
            estado.setText("No hay mercancías disponibles para vender.");
        } else if (visibles == 0) {
            estado.setText("Ninguna mercancía coincide con «" + busqueda.getText().strip() + "».");
        } else if (visibles < total) {
            estado.setText(visibles + " de " + total + " mercancías");
        } else {
            estado.setText(total == 1 ? "1 mercancía disponible" : total + " mercancías disponibles");
        }
    }

    private void actualizarDisponibilidad() {
        vender.setEnabled(seleccionada().isPresent() && tienda.getSelectedItem() != null
                && usuarioValido());
    }

    private boolean usuarioValido() {
        try {
            return Integer.parseInt(usuario.getText().strip()) > 0;
        } catch (NumberFormatException e) {
            return false;
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

    public void alActualizar(Runnable accion) {
        alActualizar = accion;
    }

    public void alVender(Runnable accion) {
        alVender = accion;
    }

    public void mostrarMercancias(List<Mercancia> mercancias, Map<Integer, String> nombresProducto) {
        modelo.setFilas(mercancias, nombresProducto);
        actualizarResumen();
        actualizarDisponibilidad();
    }

    public void mostrarTiendas(List<TiendaAlmacen> tiendas) {
        TiendaAlmacen seleccionada = (TiendaAlmacen) this.tienda.getSelectedItem();
        this.tienda.removeAllItems();
        for (TiendaAlmacen tiendaAlmacen : tiendas) {
            this.tienda.addItem(tiendaAlmacen);
        }
        if (seleccionada != null) {
            this.tienda.setSelectedItem(seleccionada);
        }
        actualizarDisponibilidad();
    }

    public Optional<Mercancia> seleccionada() {
        int enVista = tabla.getSelectedRow();
        return enVista < 0
                ? Optional.empty()
                : Optional.of(modelo.getFila(tabla.convertRowIndexToModel(enVista)));
    }

    public boolean seleccionarMercancia(String numeroSerie) {
        int enModelo = modelo.indiceDe(numeroSerie);
        int enVista = enModelo < 0 ? -1 : tabla.convertRowIndexToView(enModelo);
        if (enVista < 0) {
            return false;
        }
        tabla.setRowSelectionInterval(enVista, enVista);
        tabla.scrollRectToVisible(tabla.getCellRect(enVista, 0, true));
        return true;
    }

    public Optional<TiendaAlmacen> tiendaSeleccionada() {
        return Optional.ofNullable((TiendaAlmacen) tienda.getSelectedItem());
    }

    public String nombreProducto(int idProducto) {
        return modelo.nombreProducto(idProducto);
    }

    public int idUsuario() {
        return Integer.parseInt(usuario.getText().strip());
    }

    public void setEstado(String texto) {
        estado.setText(texto);
    }

    public Component[] controles() {
        return new Component[] {busqueda, tienda, usuario, actualizar, vender, tabla};
    }

    private static final class RenderTienda extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                boolean seleccionado, boolean foco) {
            Object texto = valor instanceof TiendaAlmacen t ? t.idTienda() + " - " + t.nombre() : valor;
            return super.getListCellRendererComponent(lista, texto, indice, seleccionado, foco);
        }
    }
}
