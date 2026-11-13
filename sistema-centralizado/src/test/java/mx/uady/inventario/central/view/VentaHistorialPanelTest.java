package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

class VentaHistorialPanelTest {

    @Test
    void filtraPorSerieTiendaProductoYFechas() throws Exception {
        VentaHistorial venta = new VentaHistorial(1, "SN-001", "Laptop",
                "Centro", "Ana", LocalDateTime.of(2026, 1, 10, 12, 0), 7);
        VentaHistorial otra = new VentaHistorial(2, "SN-002", "Monitor",
                "Norte", "Luis", LocalDateTime.of(2026, 2, 10, 12, 0), 8);

        MercanciaPanelTest.enEdt(() -> {
            VentaHistorialPanel panel = new VentaHistorialPanel();
            panel.mostrarVentas(List.of(venta, otra));
            JTable tabla = MercanciaPanelTest.buscar(panel, "tablaHistorialVentas", JTable.class);
            assertEquals(2, tabla.getRowCount());

            JTextField filtro = MercanciaPanelTest.buscar(panel, "filtroTiendaVentas", JTextField.class);
            filtro.setText("centro");
            assertEquals(1, tabla.getRowCount());
            assertEquals("SN-001", tabla.getValueAt(0, VentaHistorialTableModel.COL_SERIE));

            JTextField desde = MercanciaPanelTest.buscar(panel, "filtroDesdeVentas", JTextField.class);
            desde.setText("2026-01-15");
            assertEquals(0, tabla.getRowCount());
            desde.setText("2026-01-01");
            assertTrue(tabla.getRowCount() > 0);
        });
    }
}
