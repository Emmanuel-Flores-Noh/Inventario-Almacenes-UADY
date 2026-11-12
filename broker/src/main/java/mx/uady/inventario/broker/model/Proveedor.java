package mx.uady.inventario.broker.model;

/**
 * Endpoint de un proveedor registrado en el broker.
 */
public record Proveedor(String servicio, String host, int puerto) {

    public Proveedor {
        if (servicio == null || servicio.isBlank()) {
            throw new IllegalArgumentException("servicio es obligatorio");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host es obligatorio");
        }
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango");
        }
    }
}
