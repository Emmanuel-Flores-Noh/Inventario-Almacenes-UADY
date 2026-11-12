package mx.uady.inventario.common.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de una venta de la base de datos
 * @param idVenta El id de la venta
 * @param numeroSerie El numero de serie del producto vendido
 * @param idTienda El id de la tienda a la que fue vendido
 * @param idUsuario El id del usuario que vendio la pieza
 * @param fechaVenta El precio de la venta
 */
public record Venta(int idVenta, String numeroSerie, int idTienda, int idUsuario, LocalDateTime fechaVenta) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venta venta)) {
            return false;
        }
        return idVenta == venta.idVenta()
                && idTienda == venta.idTienda()
                && idUsuario == venta.idUsuario()
                && Objects.equals(numeroSerie, venta.numeroSerie())
                && Objects.equals(fechaVenta, venta.fechaVenta());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVenta, numeroSerie, idTienda, idUsuario, fechaVenta);
    }

    @Override
    public String toString() {
        return "Venta{idVenta=" + idVenta + ", numeroSerie='" + numeroSerie + "'}";
    }
}
