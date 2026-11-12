package mx.uady.inventario.central.service;

/**
 * Error esperado de una regla de negocio (dato inválido, número de serie duplicado, etc.).
 * Su mensaje está redactado para mostrarse tal cual al usuario.
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensajeParaUsuario) {
        super(mensajeParaUsuario);
    }

    public NegocioException(String mensajeParaUsuario, Throwable causa) {
        super(mensajeParaUsuario, causa);
    }
}
