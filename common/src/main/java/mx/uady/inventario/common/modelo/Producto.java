package mx.uady.inventario.common.modelo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Modelo de un producto en especifico
 * @param idProducto El id del producto
 * @param nombre El nombre del producto
 * @param descripcion La descripcion del producto
 * @param precioActual El precio actual del producto
 */
public record Producto(int idProducto, String nombre, String descripcion, BigDecimal precioActual) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Producto producto)) {
            return false;
        }
        return idProducto == producto.idProducto()
                && Objects.equals(nombre, producto.nombre())
                && Objects.equals(descripcion, producto.descripcion())
                && Objects.equals(precioActual, producto.precioActual());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto, nombre, descripcion, precioActual);
    }

    @Override
    public String toString() {
        return "Producto{idProducto=" + idProducto + ", nombre='" + nombre + "', precioActual=" + precioActual + "}";
    }
}
