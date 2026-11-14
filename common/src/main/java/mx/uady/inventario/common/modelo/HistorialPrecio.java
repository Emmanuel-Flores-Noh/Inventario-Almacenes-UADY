package mx.uady.inventario.common.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo para el historial de un producto
 * @param idHistorial el id del historial
 * @param idProducto el id del producto
 * @param precioHistorico el precio historico consultado
 * @param fechaCambio la fecha en la que se aplico el precio
 */
public record HistorialPrecio(int idHistorial, int idProducto, BigDecimal precioHistorico, LocalDateTime fechaCambio) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistorialPrecio historial)) {
            return false;
        }
        return idHistorial == historial.idHistorial()
                && idProducto == historial.idProducto()
                && Objects.equals(precioHistorico, historial.precioHistorico())
                && Objects.equals(fechaCambio, historial.fechaCambio());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idHistorial, idProducto, precioHistorico, fechaCambio);
    }

    @Override
    public String toString() {
        return "HistorialPrecio{idHistorial=" + idHistorial + ", idProducto=" + idProducto + "}";
    }
}
