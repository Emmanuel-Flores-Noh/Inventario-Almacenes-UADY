package mx.uady.inventario.central.network;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonServicioTest {

    @Test
    void deserializaUnaSolicitudDelContrato() {
        Solicitud original = Solicitud.crear("req-json", Servicios.INVENTARIO,
                Operaciones.LISTAR_INVENTARIO, null);

        Solicitud recuperada = JsonServicio.deserializarSolicitud(
                mx.uady.inventario.common.json.GsonFactory.DEFAULT.toJson(original));

        assertEquals(original, recuperada);
    }

    @Test
    void serializaUnaRespuestaConservandoElId() {
        Respuesta respuesta = Respuesta.exito("req-json", new JsonPrimitive("ok"));

        Respuesta recuperada = mx.uady.inventario.common.json.GsonFactory.DEFAULT
                .fromJson(JsonServicio.serializarRespuesta(respuesta), Respuesta.class);

        assertEquals("req-json", recuperada.getIdSolicitud());
        assertEquals("ok", recuperada.getResultado().getAsString());
    }

    @Test
    void rechazaJsonVacioOMalformado() {
        assertThrows(IllegalArgumentException.class, () -> JsonServicio.deserializarSolicitud(" "));
        assertThrows(JsonSyntaxException.class, () -> JsonServicio.deserializarSolicitud("{no es json"));
    }
}
