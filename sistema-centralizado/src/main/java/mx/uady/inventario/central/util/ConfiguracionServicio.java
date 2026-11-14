package mx.uady.inventario.central.util;

import java.util.Map;
import java.util.Objects;

/**
 * Parametros de red del servidor de servicio del Sistema 1.
 *
 * <p>Agrupa el puerto en el que escucha el proveedor, la direccion con la que se
 * anuncia al broker y los datos del broker al que debe registrarse. Se separa de
 * {@link ConfiguracionBD} porque la base de datos y la red se configuran y fallan
 * de forma independiente: el servidor puede arrancar y escuchar aunque MariaDB
 * todavia no este disponible.</p>
 *
 * @param puertoServicio        puerto de escucha ({@code SERVICE_PORT}), {@code 0} para puerto efimero
 * @param hostServicio          host anunciado al broker ({@code SERVICE_HOST})
 * @param tamanoPool            hilos de atencion concurrente ({@code SERVICE_POOL_SIZE})
 * @param hostBroker            host del broker ({@code BROKER_HOST})
 * @param puertoBroker          puerto del broker ({@code BROKER_PORT})
 * @param intentosRegistro      intentos maximos de registro ({@code REGISTRO_INTENTOS})
 * @param esperaReintentoMillis espera entre intentos en milisegundos ({@code REGISTRO_ESPERA_MS})
 */
public record ConfiguracionServicio(
        int puertoServicio,
        String hostServicio,
        int tamanoPool,
        String hostBroker,
        int puertoBroker,
        int intentosRegistro,
        long esperaReintentoMillis) {

    public static final String VAR_PUERTO_SERVICIO = "SERVICE_PORT";
    public static final String VAR_HOST_SERVICIO = "SERVICE_HOST";
    public static final String VAR_TAMANO_POOL = "SERVICE_POOL_SIZE";
    public static final String VAR_HOST_BROKER = "BROKER_HOST";
    public static final String VAR_PUERTO_BROKER = "BROKER_PORT";
    public static final String VAR_INTENTOS_REGISTRO = "REGISTRO_INTENTOS";
    public static final String VAR_ESPERA_REINTENTO = "REGISTRO_ESPERA_MS";

    private static final String HOST_POR_DEFECTO = "localhost";
    private static final String PUERTO_SERVICIO_POR_DEFECTO = "6000";
    private static final String TAMANO_POOL_POR_DEFECTO = "16";
    private static final String PUERTO_BROKER_POR_DEFECTO = "5000";
    private static final String INTENTOS_REGISTRO_POR_DEFECTO = "30";
    private static final String ESPERA_REINTENTO_POR_DEFECTO = "2000";

    /**
     * Valida los parámetros de red y reintentos del servicio.
     */
    public ConfiguracionServicio {
        hostServicio = requerirTexto(hostServicio, "host del servicio");
        hostBroker = requerirTexto(hostBroker, "host del broker");
        validarPuerto(puertoServicio, VAR_PUERTO_SERVICIO);
        validarPuerto(puertoBroker, VAR_PUERTO_BROKER);
        if (tamanoPool <= 0) {
            throw new IllegalArgumentException(VAR_TAMANO_POOL + " debe ser positivo: " + tamanoPool);
        }
        if (intentosRegistro <= 0) {
            throw new IllegalArgumentException(VAR_INTENTOS_REGISTRO + " debe ser positivo: " + intentosRegistro);
        }
        if (esperaReintentoMillis < 0) {
            throw new IllegalArgumentException(VAR_ESPERA_REINTENTO + " no puede ser negativo: " + esperaReintentoMillis);
        }
    }

    /**
     * Lee la configuracion de las variables de entorno del proceso.
     *
     * @return la configuracion lista para arrancar el servidor
     * @throws IllegalArgumentException si algun valor no es valido
     */
    public static ConfiguracionServicio desdeEntorno() {
        return desdeMapa(System.getenv());
    }

    /**
     * Construye la configuracion a partir de un mapa de variables.
     *
     * <p>Se usa un mapa en lugar de {@code System.getenv()} para poder probar la
     * lectura de variables sin modificar el entorno del proceso.</p>
     *
     * @param variables valores indexados por nombre de variable
     * @return la configuracion resultante
     */
    public static ConfiguracionServicio desdeMapa(Map<String, String> variables) {
        Objects.requireNonNull(variables, "El mapa de variables no puede ser nulo");

        return new ConfiguracionServicio(
                parseEntero(valorODefecto(variables.get(VAR_PUERTO_SERVICIO), PUERTO_SERVICIO_POR_DEFECTO), VAR_PUERTO_SERVICIO),
                valorODefecto(variables.get(VAR_HOST_SERVICIO), HOST_POR_DEFECTO),
                parseEntero(valorODefecto(variables.get(VAR_TAMANO_POOL), TAMANO_POOL_POR_DEFECTO), VAR_TAMANO_POOL),
                valorODefecto(variables.get(VAR_HOST_BROKER), HOST_POR_DEFECTO),
                parseEntero(valorODefecto(variables.get(VAR_PUERTO_BROKER), PUERTO_BROKER_POR_DEFECTO), VAR_PUERTO_BROKER),
                parseEntero(valorODefecto(variables.get(VAR_INTENTOS_REGISTRO), INTENTOS_REGISTRO_POR_DEFECTO), VAR_INTENTOS_REGISTRO),
                parseLargo(valorODefecto(variables.get(VAR_ESPERA_REINTENTO), ESPERA_REINTENTO_POR_DEFECTO), VAR_ESPERA_REINTENTO));
    }

    private static void validarPuerto(int puerto, String variable) {
        // Se permite 0 porque el servidor y las pruebas lo usan para pedir un puerto efimero.
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException(variable + " fuera de rango: " + puerto);
        }
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser nulo o vacio");
        }
        return valor.trim();
    }

    private static int parseEntero(String valor, String variable) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(variable + " no es un numero valido: " + valor, e);
        }
    }

    private static long parseLargo(String valor, String variable) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(variable + " no es un numero valido: " + valor, e);
        }
    }
}
