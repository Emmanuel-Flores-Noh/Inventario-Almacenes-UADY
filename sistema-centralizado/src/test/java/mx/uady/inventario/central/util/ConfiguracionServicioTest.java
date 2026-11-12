package mx.uady.inventario.central.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionServicioTest {

    @Test
    void usaValoresPorDefectoCuandoNoHayVariables() {
        ConfiguracionServicio configuracion = ConfiguracionServicio.desdeMapa(Map.of());

        assertEquals(6000, configuracion.puertoServicio());
        assertEquals("localhost", configuracion.hostServicio());
        assertEquals(16, configuracion.tamanoPool());
        assertEquals("localhost", configuracion.hostBroker());
        assertEquals(5000, configuracion.puertoBroker());
        assertEquals(30, configuracion.intentosRegistro());
        assertEquals(2000, configuracion.esperaReintentoMillis());
    }

    @Test
    void leeTodasLasVariables() {
        ConfiguracionServicio configuracion = ConfiguracionServicio.desdeMapa(Map.of(
                "SERVICE_PORT", "6100",
                "SERVICE_HOST", "sistema-centralizado",
                "SERVICE_POOL_SIZE", "8",
                "BROKER_HOST", "broker",
                "BROKER_PORT", "5100",
                "REGISTRO_INTENTOS", "5",
                "REGISTRO_ESPERA_MS", "250"));

        assertEquals(6100, configuracion.puertoServicio());
        assertEquals("sistema-centralizado", configuracion.hostServicio());
        assertEquals(8, configuracion.tamanoPool());
        assertEquals("broker", configuracion.hostBroker());
        assertEquals(5100, configuracion.puertoBroker());
        assertEquals(5, configuracion.intentosRegistro());
        assertEquals(250, configuracion.esperaReintentoMillis());
    }

    @Test
    void rechazaPuertosYNumerosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionServicio.desdeMapa(Map.of("SERVICE_PORT", "70000")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionServicio.desdeMapa(Map.of("BROKER_PORT", "no-es-puerto")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionServicio.desdeMapa(Map.of("SERVICE_POOL_SIZE", "0")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionServicio.desdeMapa(Map.of("REGISTRO_INTENTOS", "0")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionServicio.desdeMapa(Map.of("REGISTRO_ESPERA_MS", "-1")));
    }

    @Test
    void permitePuertoEfimeroYExigeHosts() {
        ConfiguracionServicio configuracion = ConfiguracionServicio.desdeMapa(Map.of("SERVICE_PORT", "0"));
        assertEquals(0, configuracion.puertoServicio());

        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionServicio(6000, " ", 4, "localhost", 5000, 3, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionServicio(6000, "localhost", 4, "", 5000, 3, 100));
    }

    @Test
    void mensajeDeErrorIndicaLaVariable() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionServicio.desdeMapa(Map.of("SERVICE_POOL_SIZE", "abc")));
        assertTrue(error.getMessage().contains("SERVICE_POOL_SIZE"), error.getMessage());
    }
}
