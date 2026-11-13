package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.model.VentaHistorial;

class VentaHistorialPanelTest {

    private static final VentaHistorial VENTA = new VentaHistorial(1, "SN-001", "Laptop",
            "Centro", "Ana", LocalDateTime.of(2026, 1, 10, 12, 0), 7);
    private static final VentaHistorial OTRA = new VentaHistorial(2, "SN-002", "Monitor",
            "Norte", "Luis", LocalDateTime.of(2026, 2, 10, 12, 0), 8);

    @Test
    void filtraPorTiendaProductoFechasYSerie() throws Exception {
        MercanciaPanelTest.enEdt(() -> {
            VentaHistorialPanel panel = new VentaHistorialPanel();
            panel.mostrarVentas(List.of(VENTA, OTRA));
            JTable tabla = MercanciaPanelTest.buscar(panel, "tablaHistorialVentas", JTable.class);
            assertEquals(2, tabla.getRowCount());

            JComboBox<?> tienda = MercanciaPanelTest.buscar(panel, "filtroTiendaVentas", JComboBox.class);
            tienda.setSelectedItem("Centro");
            assertEquals(1, tabla.getRowCount());
            assertEquals("SN-001", tabla.getValueAt(0, VentaHistorialTableModel.COL_SERIE));

            JTextField desde = MercanciaPanelTest.buscar(panel, "filtroDesdeVentas", JTextField.class);
            desde.setText("2026-01-15");
            assertEquals(0, tabla.getRowCount());
            desde.setText("2026-01-01");
            assertEquals(1, tabla.getRowCount());

            tienda.setSelectedIndex(0);
            MercanciaPanelTest.buscar(panel, "filtroProductoVentas", JComboBox.class).setSelectedItem("Monitor");
            assertEquals(1, tabla.getRowCount());
            assertEquals("SN-002", tabla.getValueAt(0, VentaHistorialTableModel.COL_SERIE));

            MercanciaPanelTest.buscar(panel, "busquedaSerieHistorial", JTextField.class).setText("001");
            assertEquals(0, tabla.getRowCount());
        });
    }

    @Test
    void losCombosSeLlenanConLosValoresDelHistorialYConservanLaSeleccion() throws Exception {
        MercanciaPanelTest.enEdt(() -> {
            VentaHistorialPanel panel = new VentaHistorialPanel();
            panel.mostrarVentas(List.of(VENTA, OTRA));
            JComboBox<?> tienda = MercanciaPanelTest.buscar(panel, "filtroTiendaVentas", JComboBox.class);
            assertEquals(3, tienda.getItemCount());
            assertEquals(VentaHistorialPanel.TODAS, tienda.getItemAt(0));

            tienda.setSelectedItem("Norte");
            panel.mostrarVentas(List.of(VENTA, OTRA));
            assertEquals("Norte", tienda.getSelectedItem());

            panel.mostrarVentas(List.of(VENTA));
            assertEquals(VentaHistorialPanel.TODAS, tienda.getSelectedItem());
            assertTrue(MercanciaPanelTest.buscar(panel, "tablaHistorialVentas", JTable.class).getRowCount() == 1);
        });
    }
}
