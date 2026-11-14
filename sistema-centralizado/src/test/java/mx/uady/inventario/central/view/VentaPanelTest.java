package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.TiendaAlmacen;
import mx.uady.inventario.common.modelo.Usuario;

class VentaPanelTest {

    private static final Mercancia LAPTOP = new Mercancia(
            "SN-0001", 1, EstadoMercancia.EN_ALMACEN, null);
    private static final TiendaAlmacen TIENDA = new TiendaAlmacen(7, "Tienda Centro", "Calle 1");
    private static final Usuario USUARIO = new Usuario(3, 3, "Lucero", "Chan", "Canul");

    @Test
    void muestraMercanciasYPermiteSeleccionarUnaVenta() throws Exception {
        MercanciaPanelTest.enEdt(() -> {
            VentaPanel panel = new VentaPanel();
            panel.mostrarMercancias(List.of(LAPTOP), Map.of(1, "Laptop"));
            panel.mostrarTiendas(List.of(TIENDA));

            JTable tabla = buscar(panel, "tablaVentas", JTable.class);
            assertEquals(1, tabla.getRowCount());
            assertEquals("Laptop", tabla.getValueAt(0, VentaTableModel.COL_PRODUCTO));

            tabla.setRowSelectionInterval(0, 0);
            panel.mostrarUsuarios(List.of(USUARIO));

            assertTrue(panel.seleccionada().isPresent());
            assertEquals(7, panel.tiendaSeleccionada().orElseThrow().idTienda());
            assertEquals(3, panel.usuarioSeleccionado().orElseThrow().idUsuario());
            assertTrue(buscar(panel, "registrarVenta", JButton.class).isEnabled());
        });
    }

    @Test
    void noPermiteVenderSinMercanciaOTiendaOUsuario() throws Exception {
        MercanciaPanelTest.enEdt(() -> {
            VentaPanel panel = new VentaPanel();
            panel.mostrarMercancias(List.of(LAPTOP), Map.of(1, "Laptop"));
            assertFalse(buscar(panel, "registrarVenta", JButton.class).isEnabled());

            panel.mostrarTiendas(List.of(TIENDA));
            assertFalse(buscar(panel, "registrarVenta", JButton.class).isEnabled());
            buscar(panel, "tablaVentas", JTable.class).setRowSelectionInterval(0, 0);
            assertFalse(buscar(panel, "registrarVenta", JButton.class).isEnabled());

            panel.mostrarUsuarios(List.of(USUARIO));
            assertTrue(buscar(panel, "registrarVenta", JButton.class).isEnabled());
        });
    }

    @Test
    void recargarCatalogosConservaLaTiendaYElUsuarioElegidos() throws Exception {
        TiendaAlmacen otraTienda = new TiendaAlmacen(9, "Tienda Norte", "Calle 2");
        Usuario otroUsuario = new Usuario(5, 2, "Luis", "Can", null);
        MercanciaPanelTest.enEdt(() -> {
            VentaPanel panel = new VentaPanel();
            panel.mostrarTiendas(List.of(TIENDA, otraTienda));
            panel.mostrarUsuarios(List.of(USUARIO, otroUsuario));
            buscar(panel, "tiendaVenta", JComboBox.class).setSelectedIndex(1);
            buscar(panel, "usuarioVenta", JComboBox.class).setSelectedIndex(1);

            panel.mostrarTiendas(List.of(TIENDA, otraTienda));
            panel.mostrarUsuarios(List.of(USUARIO, otroUsuario));

            assertEquals(9, panel.tiendaSeleccionada().orElseThrow().idTienda());
            assertEquals(5, panel.usuarioSeleccionado().orElseThrow().idUsuario());
        });
    }

    @Test
    void filtraPorNumeroDeSerie() throws Exception {
        Mercancia otra = new Mercancia("AB-0002", 1, EstadoMercancia.EN_ALMACEN, null);
        MercanciaPanelTest.enEdt(() -> {
            VentaPanel panel = new VentaPanel();
            panel.mostrarMercancias(List.of(LAPTOP, otra), Map.of(1, "Laptop"));
            buscar(panel, "busquedaVentas", JTextField.class).setText("sn-");
            assertEquals(1, buscar(panel, "tablaVentas", JTable.class).getRowCount());
        });
    }

    static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component componente : raiz.getComponents()) {
            if (nombre.equals(componente.getName()) && tipo.isInstance(componente)) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container hijo) {
                try {
                    return buscar(hijo, nombre, tipo);
                } catch (AssertionError ignorado) {
                    // Continúa buscando en los siguientes componentes.
                }
            }
        }
        throw new AssertionError("No se encontró el componente " + nombre);
    }
}
