package mx.uady.inventario.central.service;

/**
 * Fallo técnico al acceder a la base de datos o al almacenamiento (conexión caída, error SQL, disco).
 * La causa original se conserva para el registro; la interfaz muestra un mensaje genérico.
 */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String detalle, Throwable causa) {
        super(detalle, causa);
    }
}
