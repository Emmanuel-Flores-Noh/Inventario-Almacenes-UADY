package mx.uady.inventario.central.network;

import com.google.gson.JsonSyntaxException;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Utilidades de marshalling/unmarshalling del servidor de servicio.
 *
 * <p>Se centraliza aqui para que todas las conexiones usen la misma instancia de
 * Gson ({@link GsonFactory#DEFAULT}), que ya tiene registrado el adaptador de
 * {@code LocalDateTime} que necesitan {@code Solicitud} y {@code Respuesta}.</p>
 */
public final class JsonServicio {

    private JsonServicio() {}

    /**
     * Deserializa una linea JSON en una solicitud.
     *
     * @param json linea recibida por el socket
     * @return la solicitud
     * @throws IllegalArgumentException si la linea esta vacia
     * @throws JsonSyntaxException      si el JSON no representa una solicitud
     */
    public static Solicitud deserializarSolicitud(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("La solicitud esta vacia");
        }
        Solicitud solicitud = GsonFactory.DEFAULT.fromJson(json, Solicitud.class);
        if (solicitud == null) {
            throw new JsonSyntaxException("La solicitud es nula");
        }
        return solicitud;
    }

    /**
     * Serializa una respuesta al protocolo JSON por linea.
     *
     * @param respuesta respuesta a enviar
     * @return la representacion JSON
     */
    public static String serializarRespuesta(Respuesta respuesta) {
        return GsonFactory.DEFAULT.toJson(respuesta);
    }
}
