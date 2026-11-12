package mx.uady.inventario.central.service;

import com.google.gson.JsonObject;
import mx.uady.inventario.central.repository.VentaRepositoryException;
import mx.uady.inventario.central.repository.VentaResultado;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VentaServiceTest {

    @Test
    void validaParametrosYDevuelveConfirmacion() throws Exception {
        AtomicReference<String> serieRecibida = new AtomicReference<>();
        VentaService servicio = new VentaService((serie, tienda, usuario) -> {
            serieRecibida.set(serie + ":" + tienda + ":" + usuario);
            return new VentaResultado(serie, tienda, LocalDateTime.parse("2026-11-12T00:00:00"));
        });
        JsonObject parametros = new JsonObject();
        parametros.addProperty("numero_serie", "SN-001");
        parametros.addProperty("id_tienda", 3);
        parametros.addProperty("id_usuario", 12);

        JsonObject resultado = servicio.registrarVenta(parametros);

        assertEquals("SN-001:3:12", serieRecibida.get());
        assertEquals("SN-001", resultado.get("numero_serie").getAsString());
        assertEquals(3, resultado.get("id_tienda").getAsInt());
        assertEquals("2026-11-12T00:00", resultado.get("fecha_venta").getAsString());
    }

    @Test
    void rechazaParametrosFaltantesONegativos() {
        VentaService servicio = new VentaService((serie, tienda, usuario) ->
                new VentaResultado(serie, tienda, LocalDateTime.now()));
        JsonObject parametros = new JsonObject();
        parametros.addProperty("numero_serie", "SN-001");
        parametros.addProperty("id_tienda", 0);
        parametros.addProperty("id_usuario", 12);

        assertThrows(IllegalArgumentException.class, () -> servicio.registrarVenta(parametros));
    }

    @Test
    void propagaFalloDelRepositorio() {
        VentaService servicio = new VentaService((serie, tienda, usuario) -> {
            throw new VentaRepositoryException("fallo de prueba");
        });
        JsonObject parametros = new JsonObject();
        parametros.addProperty("numero_serie", "SN-001");
        parametros.addProperty("id_tienda", 3);
        parametros.addProperty("id_usuario", 12);

        assertThrows(VentaRepositoryException.class, () -> servicio.registrarVenta(parametros));
    }
}
