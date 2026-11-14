package mx.uady.inventario.broker.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracionBrokerTest {

    @Test
    void valoresPorDefecto() {
        ConfiguracionBroker config = ConfiguracionBroker.desdeMapa(Map.of());

        assertEquals(5000, config.puerto());
        assertEquals(16, config.tamanoPool());
        assertEquals(2000, config.timeoutConexionMillis());
        assertEquals(5000, config.timeoutLecturaMillis());
        assertEquals(2, config.reintentos());
        assertEquals(200, config.esperaReintentoMillis());
        assertEquals(3, config.fallosParaIndisponible());
        assertEquals(30000, config.tiempoIndisponibleMillis());
    }

    @Test
    void leeTodasLasVariables() {
        ConfiguracionBroker config = ConfiguracionBroker.desdeMapa(Map.of(
                ConfiguracionBroker.VAR_PUERTO, "0",
                ConfiguracionBroker.VAR_TAMANO_POOL, "4",
                ConfiguracionBroker.VAR_TIMEOUT_CONEXION, "150",
                ConfiguracionBroker.VAR_TIMEOUT_LECTURA, "750",
                ConfiguracionBroker.VAR_REINTENTOS, "0",
                ConfiguracionBroker.VAR_ESPERA_REINTENTO, "50",
                ConfiguracionBroker.VAR_FALLOS_PARA_INDISPONIBLE, "1",
                ConfiguracionBroker.VAR_TIEMPO_INDISPONIBLE, "1200"));

        assertEquals(0, config.puerto());
        assertEquals(4, config.tamanoPool());
        assertEquals(150, config.timeoutConexionMillis());
        assertEquals(750, config.timeoutLecturaMillis());
        assertEquals(0, config.reintentos());
        assertEquals(50, config.esperaReintentoMillis());
        assertEquals(1, config.fallosParaIndisponible());
        assertEquals(1200, config.tiempoIndisponibleMillis());
    }

    @Test
    void valoresVaciosUsanDefault() {
        ConfiguracionBroker config = ConfiguracionBroker.desdeMapa(Map.of(
                ConfiguracionBroker.VAR_TIMEOUT_LECTURA, "   ",
                ConfiguracionBroker.VAR_REINTENTOS, ""));

        assertEquals(5000, config.timeoutLecturaMillis());
        assertEquals(2, config.reintentos());
    }

    @Test
    void variableNoNumericaFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionBroker.desdeMapa(Map.of(ConfiguracionBroker.VAR_TIMEOUT_LECTURA, "abc")));
        assertEquals(true, error.getMessage().contains(ConfiguracionBroker.VAR_TIMEOUT_LECTURA));
    }

    @Test
    void validaciones() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(-1, 4, 100, 100, 0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 0, 100, 100, 0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 4, 0, 100, 0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 4, 100, 0, 0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 4, 100, 100, -1, 0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 4, 100, 100, 0, -1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 4, 100, 100, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracionBroker(0, 4, 100, 100, 0, 0, 1, -1));
    }

    @Test
    void mapaNuloFalla() {
        assertThrows(NullPointerException.class, () -> ConfiguracionBroker.desdeMapa(null));
    }
}
