package mx.uady.inventario.common.modelo;

import java.util.Objects;

/**
 * Modelo de la base de datos de una tienda
 * @param idTienda El id de la tienda
 * @param nombre El nombre de la tienda
 * @param direccion La direccion de la tienda
 */
public record TiendaAlmacen(int idTienda, String nombre, String direccion) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TiendaAlmacen tienda)) {
            return false;
        }
        return idTienda == tienda.idTienda()
                && Objects.equals(nombre, tienda.nombre())
                && Objects.equals(direccion, tienda.direccion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTienda, nombre, direccion);
    }

    @Override
    public String toString() {
        return "TiendaAlmacen{idTienda=" + idTienda + ", nombre='" + nombre + "'}";
    }
}
