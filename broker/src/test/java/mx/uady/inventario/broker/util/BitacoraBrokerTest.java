package mx.uady.inventario.broker.util;

import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.servicio.Servicios;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del formato de cada línea de la {@link BitacoraBroker}.
 */
class BitacoraBrokerTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 14, 10, 15, 2, 123_000_000);
    private static final Proveedor PROVEEDOR = new Proveedor(Servicios.INVENTARIO, "sistema-centralizado", 6000);

    private final List<String> lineas = new ArrayList<>();
    private final BitacoraBroker bitacora = new BitacoraBroker(lineas::add, () -> AHORA);

    /**
     * Una solicitud exitosa muestra fecha, hilo, id, servicio.operacion, proveedor, estado y tiempo.
     */
    @Test
    void registraUnaSolicitudExitosa() {
        Solicitud solicitud = Solicitud.crear("ev-03", Servicios.INVENTARIO, "listarInventario", null);

        bitacora.registrarSolicitud(solicitud, Respuesta.exito("ev-03", new JsonPrimitive("ok")),
                "sistema-centralizado:6000", 37);

        assertEquals(List.of("[BROKER] 2026-11-14 10:15:02.123 [" + Thread.currentThread().getName() + "] "
                + "id=ev-03 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 37 ms"), lineas);
    }

    /**
     * Una respuesta de error incluye el código y el mensaje, y la falta de proveedor se indica.
     */
    @Test
    void registraUnErrorSinProveedor() {
        Solicitud solicitud = Solicitud.crear("ev-05", "servicioFantasma", "listar", null);

        bitacora.registrarSolicitud(solicitud, Respuesta.error("ev-05", CodigoError.SERVICIO_DESCONOCIDO,
                "Servicio desconocido: servicioFantasma"), null, 2);

        assertTrue(lineas.get(0).endsWith("id=ev-05 servicioFantasma.listar -> (sin proveedor) "
                + "| ERROR SERVICIO_DESCONOCIDO: Servicio desconocido: servicioFantasma | 2 ms"), lineas.get(0));
    }

    /**
     * Si el JSON no se pudo interpretar, la línea lo indica en lugar del servicio y la operación.
     */
    @Test
    void registraUnJsonNoInterpretable() {
        bitacora.registrarSolicitud(null, Respuesta.error(null, CodigoError.PARAMETROS_INVALIDOS, "JSON malformado"),
                null, 0);

        assertTrue(lineas.get(0).contains("id=- (JSON no interpretable) -> (sin proveedor) "
                + "| ERROR PARAMETROS_INVALIDOS: JSON malformado"), lineas.get(0));
    }

    /**
     * Un intento fallido muestra el número de intento, el total, el código y el detalle.
     */
    @Test
    void registraUnIntentoFallido() {
        Solicitud solicitud = Solicitud.crear("ev-09", Servicios.INVENTARIO, "listarInventario", null);

        bitacora.registrarIntentoFallido(solicitud, PROVEEDOR, 1, 3, CodigoError.TIMEOUT,
                "Tiempo de espera agotado al contactar al proveedor");

        assertTrue(lineas.get(0).startsWith("[BROKER] 2026-11-14 10:15:02.123 "));
        assertTrue(lineas.get(0).endsWith("id=ev-09 inventario.listarInventario -> sistema-centralizado:6000 "
                + "| intento 1/3 fallo: TIMEOUT (Tiempo de espera agotado al contactar al proveedor)"), lineas.get(0));
    }

    /**
     * Los cambios del registro y la marca de no disponible usan el prefijo de registro.
     */
    @Test
    void registraLosCambiosDelRegistro() {
        bitacora.registrarCambioRegistro("registrar", Servicios.INVENTARIO, PROVEEDOR, true);
        bitacora.registrarCambioRegistro("sustituir", Servicios.GESTOR_PRECIOS, PROVEEDOR, false);
        bitacora.registrarCambioRegistro("eliminar", Servicios.INVENTARIO, null, true);
        bitacora.registrarNoDisponible(Servicios.INVENTARIO, 3);

        assertTrue(lineas.get(0).startsWith("[REGISTRO] "));
        assertTrue(lineas.get(0).endsWith("registrar inventario -> sistema-centralizado:6000"), lineas.get(0));
        assertTrue(lineas.get(1).endsWith("sustituir gestorPrecios -> sistema-centralizado:6000 "
                + "(sin efecto: el servicio no estaba registrado)"), lineas.get(1));
        assertTrue(lineas.get(2).endsWith("eliminar inventario"), lineas.get(2));
        assertTrue(lineas.get(3).endsWith("inventario marcado como NO DISPONIBLE tras 3 fallos"), lineas.get(3));
    }

    /**
     * Los mensajes largos o con saltos de línea se recortan a una sola línea legible.
     */
    @Test
    void recortaLosMensajesLargosEnUnaSolaLinea() {
        String largo = "x".repeat(200) + "\nsegunda linea";
        Solicitud solicitud = Solicitud.crear("ev-1", Servicios.INVENTARIO, "op", null);

        bitacora.registrarSolicitud(solicitud, Respuesta.error("ev-1", CodigoError.ERROR_INTERNO, largo), null, 1);

        assertEquals(1, lineas.size());
        assertTrue(lineas.get(0).contains("x".repeat(120) + "..."), lineas.get(0));
        assertTrue(!lineas.get(0).contains("\n"));
    }

    /**
     * El destino y el reloj son obligatorios.
     */
    @Test
    void validaSusDependencias() {
        assertThrows(NullPointerException.class, () -> new BitacoraBroker(null));
        assertThrows(NullPointerException.class, () -> new BitacoraBroker(lineas::add, null));
    }
}
