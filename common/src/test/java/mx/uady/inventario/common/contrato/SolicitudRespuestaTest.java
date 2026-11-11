package mx.uady.inventario.common.contrato;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mx.uady.inventario.common.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitudRespuestaTest {

    @Test
    void roundTripSolicitud() {
        JsonObject params = new JsonObject();
        params.addProperty("numero_serie", "SN-001");
        params.addProperty("id_tienda", 5);

        Solicitud original = Solicitud.crear("req-1", "inventario", "actualizarTiendaVenta", params);

        String json = GsonFactory.DEFAULT.toJson(original);
        Solicitud copia = GsonFactory.DEFAULT.fromJson(json, Solicitud.class);

        assertEquals(original, copia);
        assertEquals("SN-001", copia.getParametros().get("numero_serie").getAsString());
    }

    @Test
    void roundTripRespuestaExito() {
        Respuesta original = Respuesta.exito("req-2", new JsonPrimitive("OK"));
        String json = GsonFactory.DEFAULT.toJson(original);
        Respuesta copia = GsonFactory.DEFAULT.fromJson(json, Respuesta.class);

        assertEquals(original.getIdSolicitud(), copia.getIdSolicitud());
        assertEquals(EstadoRespuesta.EXITO, copia.getEstado());
        assertEquals("OK", copia.getResultado().getAsString());
    }

    @Test
    void roundTripRespuestaError() {
        Respuesta original = Respuesta.error("req-3", mx.uady.inventario.common.error.CodigoError.PARAMETROS_INVALIDOS, null);
        String json = GsonFactory.DEFAULT.toJson(original);
        Respuesta copia = GsonFactory.DEFAULT.fromJson(json, Respuesta.class);

        assertEquals(EstadoRespuesta.ERROR, copia.getEstado());
        assertEquals(mx.uady.inventario.common.error.CodigoError.PARAMETROS_INVALIDOS, copia.getCodigoError());
    }

    @Test
    void fechaHoraSeSerializa() {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 11, 10, 30, 0);
        Solicitud solicitud = Solicitud.crearConFechaEspecifica("id", "svc", "op", new JsonObject(), fecha);
        String json = GsonFactory.DEFAULT.toJson(solicitud);
        Solicitud copia = GsonFactory.DEFAULT.fromJson(json, Solicitud.class);
        assertEquals(fecha, copia.getFechaHora());
    }
}
