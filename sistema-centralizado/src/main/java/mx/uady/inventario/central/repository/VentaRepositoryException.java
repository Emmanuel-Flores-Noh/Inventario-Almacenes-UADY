package mx.uady.inventario.central.repository;

/**
 * Indica que no fue posible completar la persistencia de una venta.
 */
public class VentaRepositoryException extends Exception {

    public VentaRepositoryException(String message) {
        super(message);
    }

    public VentaRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
