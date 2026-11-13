package mx.uady.inventario.central.view;

import java.time.LocalDateTime;

/** Datos presentados por la pantalla de historial de ventas. */
public record VentaHistorial(int idVenta, String numeroSerie, String producto,
        String tienda, String usuario, LocalDateTime fechaVenta, int idProducto) {
}
