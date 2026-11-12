package mx.uady.inventario.common.modelo;

import java.util.Objects;

/**
 * Modelo para la mercancia presente en los almacenes
 * @param numeroSerie El numero de serie de la mercancia
 * @param idProducto El id del producto
 * @param estado El estado de la mercancia
 * @param rutaQr La ruta del archivo QR en formato png
 */
public record Mercancia(String numeroSerie, int idProducto, EstadoMercancia estado, String rutaQr) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mercancia mercancia)) {
            return false;
        }
        return idProducto == mercancia.idProducto()
                && Objects.equals(numeroSerie, mercancia.numeroSerie())
                && Objects.equals(estado, mercancia.estado())
                && Objects.equals(rutaQr, mercancia.rutaQr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroSerie, idProducto, estado, rutaQr);
    }

    @Override
    public String toString() {
        return "Mercancia{numeroSerie='" + numeroSerie + "', idProducto=" + idProducto + ", estado='" + estado + "'}";
    }
}
