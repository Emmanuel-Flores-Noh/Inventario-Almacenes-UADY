package mx.uady.inventario.cliente.model;

import mx.uady.inventario.common.error.CodigoError;

/**
 * Señala que el Broker o el Sistema 1 respondieron la solicitud con estado de error.
 * Conserva el código y el mensaje del contrato para traducirlos a un texto entendible.
 */
public class ErrorServicioException extends Exception {

    private final CodigoError codigoError;

    /**
     * Crea la excepción a partir de los datos de una respuesta de error.
     *
     * @param codigoError código de error del contrato; puede ser nulo
     * @param mensaje     mensaje de error enviado por el servidor
     */
    public ErrorServicioException(CodigoError codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    /**
     * Obtiene el código de error del contrato.
     *
     * @return código de error, o {@code null} si el servidor no lo indicó
     */
    public CodigoError getCodigoError() {
        return codigoError;
    }
}
