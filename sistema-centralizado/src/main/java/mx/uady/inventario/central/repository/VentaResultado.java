package mx.uady.inventario.central.repository;

import java.time.LocalDateTime;

/**
 * Resultado persistido de una venta registrada.
 *
 * @param numeroSerie número de serie de la mercancía vendida
 * @param idTienda identificador de la tienda destino
 * @param fechaVenta fecha asignada por la base de datos
 */
public record VentaResultado(String numeroSerie, int idTienda, LocalDateTime fechaVenta) {
}
