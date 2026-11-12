package mx.uady.inventario.central.service;

/**
 * Indica que no fue posible generar o almacenar un código QR.
 */
public class QrGenerationException extends Exception {

    public QrGenerationException(String message) {
        super(message);
    }

    public QrGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
