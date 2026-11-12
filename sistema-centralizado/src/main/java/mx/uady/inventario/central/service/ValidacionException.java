package mx.uady.inventario.central.service;

/** Un dato capturado no cumple las reglas de negocio (nombre vacío, precio no positivo, etc.). */
public class ValidacionException extends NegocioException {

    public ValidacionException(String mensajeParaUsuario) {
        super(mensajeParaUsuario);
    }
}
