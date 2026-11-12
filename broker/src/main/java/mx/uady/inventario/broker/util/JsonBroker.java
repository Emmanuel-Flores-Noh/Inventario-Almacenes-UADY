package mx.uady.inventario.broker.util;

import com.google.gson.JsonSyntaxException;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Utilidades de marshalling/unmarshalling para el broker.
 */
public final class JsonBroker {

    private JsonBroker() {}

    /**
     * Deserealiza una cadena que contiene un json en un objeto Solicitud
     * @param json EL json contenido en una cadena
     * @return El objeto {@code Solicitud}
     */
    public static Solicitud deserializarSolicitud(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON vacio");
        }
        Solicitud solicitud = GsonFactory.DEFAULT.fromJson(json, Solicitud.class);
        if (solicitud == null) {
            throw new JsonSyntaxException("Solicitud nula");
        }
        return solicitud;
    }

    /**
     * Serializa la respuesta en un formato JSON
     * @param respuesta La respuesta a serializar
     * @return La respuesta en formato string
     */
    public static String serializarRespuesta(Respuesta respuesta) {
        return GsonFactory.DEFAULT.toJson(respuesta);
    }
}
