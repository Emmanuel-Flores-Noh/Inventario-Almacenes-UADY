package mx.uady.inventario.broker.util;

import com.google.gson.JsonObject;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonBrokerTest {

    @Test
    void roundTripSolicitud() {
        JsonObject params = new JsonObject();
        params.addProperty("numero_serie", "SN-001");
        Solicitud original = Solicitud.crear("req-1", Servicios.INVENTARIO, "listarInventario", params);

        String json = GsonFactory.DEFAULT.toJson(original);
        Solicitud copia = JsonBroker.deserializarSolicitud(json);

        assertEquals(original, copia);
    }

    @Test
    void roundTripRespuesta() {
        Respuesta original = Respuesta.exito("req-2", new com.google.gson.JsonPrimitive("OK"));
        String json = JsonBroker.serializarRespuesta(original);
        Respuesta copia = GsonFactory.DEFAULT.fromJson(json, Respuesta.class);

        assertEquals(original.getIdSolicitud(), copia.getIdSolicitud());
        assertEquals(original.getEstado(), copia.getEstado());
    }

    @Test
    void deserializarJsonVacioLanza() {
        assertThrows(IllegalArgumentException.class, () -> JsonBroker.deserializarSolicitud(null));
        assertThrows(IllegalArgumentException.class, () -> JsonBroker.deserializarSolicitud("  "));
    }

    @Test
    void serializarRespuestaContieneId() {
        Respuesta original = Respuesta.error("req-3", mx.uady.inventario.common.error.CodigoError.PARAMETROS_INVALIDOS, null);
        String json = JsonBroker.serializarRespuesta(original);
        assertTrue(json.contains("req-3"));
    }
}
