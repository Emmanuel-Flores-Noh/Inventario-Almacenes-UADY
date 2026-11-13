package mx.uady.inventario.central.repository;

/**
 * Indica que no fue posible completar la persistencia de una venta.
 */
public class VentaRepositoryException extends Exception {

    /**
     * Crea una excepción de persistencia con un mensaje descriptivo.
     *
     * @param message detalle del fallo
     */
    public VentaRepositoryException(String message) {
        super(message);
    }

    /**
     * Crea una excepción de persistencia conservando la causa original.
     *
     * @param message detalle del fallo
     * @param cause excepción que originó el fallo
     */
    public VentaRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
