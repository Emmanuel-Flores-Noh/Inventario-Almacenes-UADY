package mx.uady.inventario.central.model;

import java.util.Objects;

import mx.uady.inventario.common.modelo.Producto;

/**
 * Fila de la pantalla de productos: el producto y el número de unidades físicas registradas.
 *
 * @param producto producto de la tabla {@code productos}
 * @param unidades número de filas de {@code mercancia} asociadas al producto
 */
public record ProductoConUnidades(Producto producto, int unidades) {

    public ProductoConUnidades {
        Objects.requireNonNull(producto, "producto");
        if (unidades < 0) {
            throw new IllegalArgumentException("unidades no puede ser negativo");
        }
    }
}
