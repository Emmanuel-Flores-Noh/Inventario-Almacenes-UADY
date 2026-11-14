package mx.uady.inventario.cliente.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de pruebas unitarias para la clase {@link ConfiguracionNetwork}.
 * Valida la correcta lectura de los parámetros de red TCP y la aplicación rigurosa
 * de restricciones ante valores de configuración inválidos o fuera de rango.
 */
class ConfiguracionNetworkTest {

    /**
     * Verifica que se utilicen los valores por defecto establecidos cuando el mapa 
     * de configuración se encuentra vacío.
     */
    @Test
    void usaValoresPorDefectoCuandoNoHayVariables() {
        ConfiguracionNetwork configuracion = ConfiguracionNetwork.desdeMapa(Map.of());

        assertEquals("localhost", configuracion.brokerHost());
        assertEquals(5000, configuracion.brokerPuerto());
        assertEquals(5000, configuracion.timeoutMs());
    }

    /**
     * Comprueba que todas las variables de configuración de red sean leídas correctamente
     * a partir de un mapa de propiedades personalizado.
     */
    @Test
    void leeTodasLasVariablesDeRed() {
        ConfiguracionNetwork configuracion = ConfiguracionNetwork.desdeMapa(Map.of(
                "BROKER_HOST", "broker-cluster",
                "BROKER_PORT", "5150",
                "CLIENT_TIMEOUT_MS", "3000"));

        assertEquals("broker-cluster", configuracion.brokerHost());
        assertEquals(5150, configuracion.brokerPuerto());
        assertEquals(3000, configuracion.timeoutMs());
    }

    /**
     * Valida que se rechacen mediante excepciones los puertos fuera de rango, 
     * formatos no numéricos y tiempos de timeout negativos o nulos.
     */
    @Test
    void rechazaPuertosYNumerosDeTimeoutInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionNetwork.desdeMapa(Map.of("BROKER_PORT", "70000")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionNetwork.desdeMapa(Map.of("BROKER_PORT", "no-es-puerto")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionNetwork.desdeMapa(Map.of("CLIENT_TIMEOUT_MS", "-500")));
    }

    /**
     * Comprueba que el mensaje de error arrojado ante una configuración inválida
     * indique claramente el nombre de la variable que falló en la validación.
     */
    @Test
    void mensajeDeErrorIndicaLaVariableFallida() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ConfiguracionNetwork.desdeMapa(Map.of("BROKER_PORT", "99999")));
        assertTrue(error.getMessage().contains("BROKER_PORT"), error.getMessage());
    }
}