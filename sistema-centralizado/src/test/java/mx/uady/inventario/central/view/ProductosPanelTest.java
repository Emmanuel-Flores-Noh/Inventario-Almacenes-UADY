package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.common.modelo.Producto;

class ProductosPanelTest {

    private static final List<ProductoConUnidades> PRODUCTOS = List.of(
            fila(1, "Silla ergonómica", 2),
            fila(2, "Laptop Lenovo", 3),
            fila(3, "Silla de visita", 0));

    @Test
    void muestraLosProductosOrdenadosPorNombre() throws Exception {
        enEdt(() -> {
            ProductosPanel panel = new ProductosPanel();
            panel.mostrarProductos(PRODUCTOS);

            assertEquals(3, panel.getTabla().getRowCount());
            assertEquals("Laptop Lenovo", panel.getTabla().getValueAt(0, ProductosTableModel.COL_NOMBRE));
            assertEquals(new BigDecimal("10.00"), panel.getTabla().getValueAt(0, ProductosTableModel.COL_PRECIO));
            assertEquals(3, panel.getTabla().getValueAt(0, ProductosTableModel.COL_UNIDADES));
        });
    }

    @Test
    void filtraPorNombreSinDistinguirAcentos() throws Exception {
        enEdt(() -> {
            ProductosPanel panel = new ProductosPanel();
            panel.mostrarProductos(PRODUCTOS);

            panel.getBusqueda().setText("ergonomica");
            assertEquals(1, panel.getTabla().getRowCount());

            panel.getBusqueda().setText("silla");
            assertEquals(2, panel.getTabla().getRowCount());

            panel.getBusqueda().setText("");
            assertEquals(3, panel.getTabla().getRowCount());
        });
    }

    @Test
    void editarSoloSeHabilitaConUnaFilaSeleccionada() throws Exception {
        enEdt(() -> {
            ProductosPanel panel = new ProductosPanel();
            panel.mostrarProductos(PRODUCTOS);
            javax.swing.JButton editar = (javax.swing.JButton) panel.controles()[1];
            assertFalse(editar.isEnabled());

            panel.seleccionarProducto(3);
            assertTrue(editar.isEnabled());
            assertEquals(3, panel.seleccionado().orElseThrow().producto().idProducto());

            panel.mostrarProductos(PRODUCTOS);
            assertFalse(editar.isEnabled());
        });
    }

    @Test
    void editarEntregaElProductoSeleccionado() throws Exception {
        AtomicReference<ProductoConUnidades> editado = new AtomicReference<>();
        enEdt(() -> {
            ProductosPanel panel = new ProductosPanel();
            panel.alEditar(editado::set);
            panel.mostrarProductos(PRODUCTOS);
            panel.seleccionarProducto(2);
            ((javax.swing.JButton) panel.controles()[1]).doClick();
        });
        assertEquals("Laptop Lenovo", editado.get().producto().nombre());
    }

    private static ProductoConUnidades fila(int id, String nombre, int unidades) {
        return new ProductoConUnidades(new Producto(id, nombre, null, new BigDecimal("10.00")), unidades);
    }

    private static void enEdt(Runnable prueba) throws Exception {
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
