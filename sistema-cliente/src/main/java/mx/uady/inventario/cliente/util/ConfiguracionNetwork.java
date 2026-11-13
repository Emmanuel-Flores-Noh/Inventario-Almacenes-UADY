package mx.uady.inventario.cliente.util;

import java.util.Map;
import java.util.Objects;

/**
 * Parámetros de red y configuración de conexión para el Sistema Cliente (Sistema 2).
 * 
 * <p>Permite configurar dinámicamente el host del broker, el puerto y el tiempo límite (timeout) 
 * mediante variables de entorno del proceso o valores por defecto preestablecidos.</p>
 * 
 * @param brokerHost Host o dirección IP donde escucha el Broker intermediario ({@code BROKER_HOST})
 * @param brokerPuerto Puerto de red del Broker ({@code BROKER_PORT})
 * @param timeoutMs    Tiempo límite en milisegundos para conexión y lectura por sockets ({@code CLIENT_TIMEOUT_MS})
 * 
 */
public record ConfiguracionNetwork(
        String brokerHost,
        int brokerPuerto,
        int timeoutMs) {

    public static final String VAR_BROKER_HOST = "BROKER_HOST";
    public static final String VAR_BROKER_PUERTO = "BROKER_PORT";
    public static final String VAR_CLIENT_TIMEOUT = "CLIENT_TIMEOUT_MS";

    private static final String HOST_POR_DEFECTO = "localhost";
    private static final String PUERTO_BROKER_POR_DEFECTO = "5000";
    private static final String TIMEOUT_POR_DEFECTO = "5000";

    public ConfiguracionNetwork {
        brokerHost = requerirTexto(brokerHost, "host del broker");
        validarPuerto(brokerPuerto, VAR_BROKER_PUERTO);
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException(VAR_CLIENT_TIMEOUT + " debe ser positivo: " + timeoutMs);
        }
    }

    /**
     * Lee la configuración de red directamente de las variables de entorno del proceso.
     * 
     * @return la instancia de {@link ConfiguracionNetwork} lista para el cliente TCP.
     * @throws IllegalArgumentException si algún valor numérico o de texto no es válido.
     */
    public static ConfiguracionNetwork desdeEntorno() {
        return desdeMapa(System.getenv());
    }

    /**
     * Construye la configuración de red a partir de un mapa de variables personalizado 
     * (útil para pruebas unitarias sin alterar el entorno global del proceso).
     * 
     * @param variables mapa de variables indexado por nombre.
     * @return la configuración de red resultante.
     */
    public static ConfiguracionNetwork desdeMapa(Map<String, String> variables) {
        Objects.requireNonNull(variables, "El mapa de variables no puede ser nulo");

        return new ConfiguracionNetwork(
                valorODefecto(variables.get(VAR_BROKER_HOST), HOST_POR_DEFECTO),
                parseEntero(valorODefecto(variables.get(VAR_BROKER_PUERTO), PUERTO_BROKER_POR_DEFECTO), VAR_BROKER_PUERTO),
                parseEntero(valorODefecto(variables.get(VAR_CLIENT_TIMEOUT), TIMEOUT_POR_DEFECTO), VAR_CLIENT_TIMEOUT)
        );
    }

    private static void validarPuerto(int puerto, String variable) {
        if (puerto <= 0 || puerto > 65535) {
            throw new IllegalArgumentException(variable + " fuera de rango válido (1-65535): " + puerto);
        }
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser nulo o vacío");
        }
        return valor.trim();
    }

    private static int parseEntero(String valor, String variable) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(variable + " no es un número válido: " + valor, e);
        }
    }
}