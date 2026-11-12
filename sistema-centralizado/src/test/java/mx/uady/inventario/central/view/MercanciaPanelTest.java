package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

class MercanciaPanelTest {

    private static final Producto LAPTOP = new Producto(1, "Laptop", null, BigDecimal.TEN);
    private static final Producto SILLA = new Producto(2, "Silla", null, BigDecimal.ONE);
    private static final List<Mercancia> UNIDADES = List.of(
            new Mercancia("SN-0001", 1, EstadoMercancia.EN_ALMACEN, null),
            new Mercancia("SN-0002", 1, EstadoMercancia.VENDIDO, null),
            new Mercancia("AB-0003", 2, EstadoMercancia.EN_ALMACEN, null));

    @Test
    void sinProductosNoSePuedeRegistrarYLoIndica() throws Exception {
        enEdt(() -> {
            MercanciaPanel panel = new MercanciaPanel();
            panel.mostrarProductos(List.of());

            assertFalse(buscar(panel, "registrar", JButton.class).isEnabled());
            assertEquals(MercanciaPanel.SIN_PRODUCTOS, buscar(panel, "errorSerie", JLabel.class).getText());

            panel.mostrarProductos(List.of(LAPTOP));
            assertTrue(buscar(panel, "registrar", JButton.class).isEnabled());
            assertEquals(" ", buscar(panel, "errorSerie", JLabel.class).getText());
        });
    }

    @Test
    void recargarProductosConservaLasSeleccionesYNoDisparaFiltros() throws Exception {
        AtomicInteger cambiosDeFiltro = new AtomicInteger();
        enEdt(() -> {
            MercanciaPanel panel = new MercanciaPanel();
            panel.mostrarProductos(List.of(LAPTOP, SILLA));
            panel.alCambiarFiltros(cambiosDeFiltro::incrementAndGet);
            buscar(panel, "productoRegistro", JComboBox.class).setSelectedItem(SILLA);
            buscar(panel, "filtroProducto", JComboBox.class).setSelectedItem(SILLA);
            int cambiosAntes = cambiosDeFiltro.get();

            panel.mostrarProductos(List.of(LAPTOP, SILLA, new Producto(3, "Proyector", null, BigDecimal.ONE)));

            assertEquals(2, panel.productoParaRegistrar().orElseThrow().idProducto());
            assertEquals(2, panel.filtroProductoSeleccionado());
            assertEquals(cambiosAntes, cambiosDeFiltro.get());
        });
        assertEquals(1, cambiosDeFiltro.get());
    }

    @Test
    void losFiltrosDevuelvenNullCuandoSeEligeTodos() throws Exception {
        enEdt(() -> {
            MercanciaPanel panel = new MercanciaPanel();
            panel.mostrarProductos(List.of(LAPTOP));
            assertNull(panel.filtroProductoSeleccionado());
            assertNull(panel.filtroEstadoSeleccionado());

            buscar(panel, "filtroEstado", JComboBox.class).setSelectedItem(EstadoMercancia.VENDIDO);
            assertEquals(EstadoMercancia.VENDIDO, panel.filtroEstadoSeleccionado());
        });
    }

    @Test
    void buscaPorNumeroDeSerieYSeleccionaUnidades() throws Exception {
        enEdt(() -> {
            MercanciaPanel panel = new MercanciaPanel();
            panel.mostrarProductos(List.of(LAPTOP, SILLA));
            panel.mostrarUnidades(UNIDADES, List.of(LAPTOP, SILLA));
            JTable tabla = buscar(panel, "tablaUnidades", JTable.class);
            assertEquals(3, tabla.getRowCount());
            assertEquals("Silla", tabla.getValueAt(0, MercanciaTableModel.COL_PRODUCTO));

            buscar(panel, "busquedaSerie", JTextField.class).setText("sn-");
            assertEquals(2, tabla.getRowCount());
            assertFalse(panel.seleccionarUnidad("AB-0003"));
            assertTrue(panel.seleccionarUnidad("SN-0002"));
            assertTrue(buscar(panel, "verQr", JButton.class).isEnabled());
            assertEquals("SN-0002", panel.seleccionada().orElseThrow().numeroSerie());
        });
    }

    @Test
    void enterEnLaSerieRegistra() throws Exception {
        AtomicInteger registros = new AtomicInteger();
        enEdt(() -> {
            MercanciaPanel panel = new MercanciaPanel();
            panel.alRegistrar(registros::incrementAndGet);
            panel.mostrarProductos(List.of(LAPTOP));
            buscar(panel, "serie", JTextField.class).postActionEvent();
        });
        assertEquals(1, registros.get());
    }

    static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container hijo) {
                T encontrado = buscarONull(hijo, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        throw new AssertionError("No se encontró el componente " + nombre);
    }

    private static <T extends Component> T buscarONull(Container raiz, String nombre, Class<T> tipo) {
        try {
            return buscar(raiz, nombre, tipo);
        } catch (AssertionError e) {
            return null;
        }
    }

    static void enEdt(Runnable prueba) throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                prueba.run();
            } catch (Throwable t) {
                fallo.set(t);
            }
        });
        if (fallo.get() instanceof Error error) {
            throw error;
        }
        if (fallo.get() != null) {
            throw new AssertionError(fallo.get());
        }
    }
}
