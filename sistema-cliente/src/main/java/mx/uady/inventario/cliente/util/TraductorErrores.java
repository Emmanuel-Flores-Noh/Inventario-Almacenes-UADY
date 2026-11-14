package mx.uady.inventario.cliente.util;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import mx.uady.inventario.cliente.model.ErrorServicioException;
import mx.uady.inventario.common.error.CodigoError;

/**
 * Convierte errores técnicos (excepciones de red y códigos de error del contrato) en mensajes
 * que el usuario del Sistema 2 pueda entender, y decide si tiene sentido ofrecer reintentar.
 */
public final class TraductorErrores {

    private TraductorErrores() {
    }

    /**
     * Traduce un error a un mensaje para el usuario.
     *
     * @param error excepción recibida (puede venir envuelta en {@link ExecutionException})
     * @return mensaje entendible en español
     */
    public static String traducir(Throwable error) {
        Throwable causa = desenvolver(error);
        if (causa instanceof ErrorServicioException errorServicio) {
            return traducirCodigo(errorServicio.getCodigoError(), errorServicio.getMessage());
        }
        if (causa instanceof SocketTimeoutException) {
            return "El servidor tardó demasiado en responder. Intente de nuevo en unos momentos.";
        }
        if (causa instanceof ConnectException || causa instanceof UnknownHostException) {
            return "No hay conexión con el servidor central. Verifique que el Broker esté encendido.";
        }
        return "Ocurrió un problema de comunicación con el servidor."
                + (causa.getMessage() != null ? "\nDetalle: " + causa.getMessage() : "");
    }

    /**
     * Indica si conviene ofrecer reintentar la operación: sí para fallas de red o del servidor,
     * no para errores de datos (por ejemplo, una unidad ya vendida).
     *
     * @param error excepción recibida (puede venir envuelta en {@link ExecutionException})
     * @return {@code true} si reintentar podría resolver el problema
     */
    public static boolean esReintentable(Throwable error) {
        Throwable causa = desenvolver(error);
        if (causa instanceof ErrorServicioException errorServicio) {
            CodigoError codigo = errorServicio.getCodigoError();
            return codigo != CodigoError.PARAMETROS_INVALIDOS && codigo != CodigoError.RECURSO_NO_ENCONTRADO;
        }
        return true;
    }

    /**
     * Traduce un código de error del contrato.
     *
     * @param codigo  código recibido; puede ser nulo
     * @param mensaje mensaje enviado por el servidor; puede ser nulo
     * @return mensaje entendible en español
     */
    private static String traducirCodigo(CodigoError codigo, String mensaje) {
        if (codigo == null) {
            return mensaje != null ? mensaje : "El servidor rechazó la operación.";
        }
        return switch (codigo) {
            case PARAMETROS_INVALIDOS, RECURSO_NO_ENCONTRADO ->
                    mensaje != null ? mensaje : "Los datos enviados no son válidos.";
            case TIMEOUT -> "El sistema central tardó demasiado en responder. Intente de nuevo.";
            case PROVEEDOR_NO_REGISTRADO ->
                    "El sistema central no está disponible en este momento. Verifique que esté encendido.";
            case SERVICIO_DESCONOCIDO, OPERACION_NO_SOPORTADA ->
                    "El sistema central no reconoce esta operación. Contacte al administrador."
                            + detalle(mensaje);
            case ERROR_INTERNO -> "Ocurrió un error en el sistema central." + detalle(mensaje);
        };
    }

    /**
     * Da formato al detalle técnico que se agrega al final de un mensaje.
     *
     * @param mensaje detalle técnico; puede ser nulo
     * @return línea con el detalle, o cadena vacía si no hay
     */
    private static String detalle(String mensaje) {
        return mensaje != null && !mensaje.isBlank() ? "\nDetalle: " + mensaje : "";
    }

    /**
     * Obtiene la causa real de un error envuelto por {@code SwingWorker.get()}.
     *
     * @param error excepción recibida
     * @return la causa original, o el mismo error si no estaba envuelto
     */
    private static Throwable desenvolver(Throwable error) {
        Throwable actual = error;
        while (actual instanceof ExecutionException && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }
}
