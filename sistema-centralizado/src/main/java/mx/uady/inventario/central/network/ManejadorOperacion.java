package mx.uady.inventario.central.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Punto de extension para la logica de negocio de una operacion.
 *
 * <p>El despachador no conoce la base de datos ni los servicios concretos: solo
 * recibe los parametros ya validados como objeto JSON y devuelve el resultado que
 * viajara en la {@code Respuesta}. Las excepciones que lance el manejador se
 * convierten a un {@code CodigoError} en {@link DespachadorSolicitudes}.</p>
 */
@FunctionalInterface
public interface ManejadorOperacion {

    /**
     * Ejecuta la operacion.
     *
     * @param parametros parametros de la solicitud; nunca es {@code null}
     * @return el resultado a enviar al cliente; puede ser {@code null}
     * @throws IllegalArgumentException si los parametros no son validos
     * @throws Exception                si ocurre un fallo interno al ejecutar la operacion
     */
    JsonElement ejecutar(JsonObject parametros) throws Exception;
}
