package mx.uady.inventario.broker.util;

import java.util.Map;
import java.util.Objects;

/**
 * Parametros de resiliencia y red del broker.
 *
 * <p>Agrupa el puerto de escucha, el tamano del pool, los timeouts hacia los
 * proveedores, los reintentos y el umbral para marcar a un proveedor como no
 * disponible. Se centraliza aqui para que {@code Main} y las pruebas usen la
 * misma fuente de verdad y para que ningun valor quede escrito en el codigo de
 * la red.</p>
 *
 * <p>Los reintentos son adicionales: con {@code BROKER_REINTENTOS=2} el broker
 * hace hasta 3 intentos (el original mas dos reintentos).</p>
 *
 * @param puerto                 puerto de escucha ({@code BROKER_PORT}), {@code 0} para puerto efimero
 * @param tamanoPool             hilos de atencion concurrente ({@code BROKER_POOL_SIZE})
 * @param timeoutConexionMillis  timeout de conexion hacia el proveedor ({@code BROKER_TIMEOUT_CONEXION_MS})
 * @param timeoutLecturaMillis   timeout de lectura de la respuesta del proveedor ({@code BROKER_TIMEOUT_LECTURA_MS})
 * @param reintentos             reintentos adicionales por solicitud ({@code BROKER_REINTENTOS})
 * @param esperaReintentoMillis  espera entre intentos en milisegundos ({@code BROKER_ESPERA_REINTENTO_MS})
 * @param fallosParaIndisponible fallos consecutivos para marcar no disponible ({@code BROKER_FALLOS_PARA_INDISPONIBLE})
 * @param tiempoIndisponibleMillis enfriamiento antes de reintentar un proveedor marcado ({@code BROKER_INDISPONIBLE_MS})
 */
public record ConfiguracionBroker(
        int puerto,
        int tamanoPool,
        int timeoutConexionMillis,
        int timeoutLecturaMillis,
        int reintentos,
        long esperaReintentoMillis,
        int fallosParaIndisponible,
        long tiempoIndisponibleMillis) {

    public static final String VAR_PUERTO = "BROKER_PORT";
    public static final String VAR_TAMANO_POOL = "BROKER_POOL_SIZE";
    public static final String VAR_TIMEOUT_CONEXION = "BROKER_TIMEOUT_CONEXION_MS";
    public static final String VAR_TIMEOUT_LECTURA = "BROKER_TIMEOUT_LECTURA_MS";
    public static final String VAR_REINTENTOS = "BROKER_REINTENTOS";
    public static final String VAR_ESPERA_REINTENTO = "BROKER_ESPERA_REINTENTO_MS";
    public static final String VAR_FALLOS_PARA_INDISPONIBLE = "BROKER_FALLOS_PARA_INDISPONIBLE";
    public static final String VAR_TIEMPO_INDISPONIBLE = "BROKER_INDISPONIBLE_MS";

    private static final String PUERTO_POR_DEFECTO = "5000";
    private static final String TAMANO_POOL_POR_DEFECTO = "16";
    private static final String TIMEOUT_CONEXION_POR_DEFECTO = "2000";
    private static final String TIMEOUT_LECTURA_POR_DEFECTO = "5000";
    private static final String REINTENTOS_POR_DEFECTO = "2";
    private static final String ESPERA_REINTENTO_POR_DEFECTO = "200";
    private static final String FALLOS_PARA_INDISPONIBLE_POR_DEFECTO = "3";
    private static final String TIEMPO_INDISPONIBLE_POR_DEFECTO = "30000";

    public ConfiguracionBroker {
        validarPuerto(puerto, VAR_PUERTO);
        if (tamanoPool <= 0) {
            throw new IllegalArgumentException(VAR_TAMANO_POOL + " debe ser positivo: " + tamanoPool);
        }
        if (timeoutConexionMillis <= 0) {
            throw new IllegalArgumentException(VAR_TIMEOUT_CONEXION + " debe ser positivo: " + timeoutConexionMillis);
        }
        if (timeoutLecturaMillis <= 0) {
            throw new IllegalArgumentException(VAR_TIMEOUT_LECTURA + " debe ser positivo: " + timeoutLecturaMillis);
        }
        if (reintentos < 0) {
            throw new IllegalArgumentException(VAR_REINTENTOS + " no puede ser negativo: " + reintentos);
        }
        if (esperaReintentoMillis < 0) {
            throw new IllegalArgumentException(VAR_ESPERA_REINTENTO + " no puede ser negativo: " + esperaReintentoMillis);
        }
        if (fallosParaIndisponible <= 0) {
            throw new IllegalArgumentException(VAR_FALLOS_PARA_INDISPONIBLE + " debe ser positivo: " + fallosParaIndisponible);
        }
        if (tiempoIndisponibleMillis < 0) {
            throw new IllegalArgumentException(VAR_TIEMPO_INDISPONIBLE + " no puede ser negativo: " + tiempoIndisponibleMillis);
        }
    }

    /**
     * Configuracion por defecto, util para pruebas y para constructores antiguos.
     *
     * @return la configuracion con todos los valores por defecto
     */
    public static ConfiguracionBroker porDefecto() {
        return desdeMapa(Map.of());
    }

    /**
     * Lee la configuracion de las variables de entorno del proceso.
     *
     * @return la configuracion lista para arrancar el broker
     * @throws IllegalArgumentException si algun valor no es valido
     */
    public static ConfiguracionBroker desdeEntorno() {
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
    public static ConfiguracionBroker desdeMapa(Map<String, String> variables) {
        Objects.requireNonNull(variables, "El mapa de variables no puede ser nulo");

        return new ConfiguracionBroker(
                parseEntero(valorODefecto(variables.get(VAR_PUERTO), PUERTO_POR_DEFECTO), VAR_PUERTO),
                parseEntero(valorODefecto(variables.get(VAR_TAMANO_POOL), TAMANO_POOL_POR_DEFECTO), VAR_TAMANO_POOL),
                parseEntero(valorODefecto(variables.get(VAR_TIMEOUT_CONEXION), TIMEOUT_CONEXION_POR_DEFECTO), VAR_TIMEOUT_CONEXION),
                parseEntero(valorODefecto(variables.get(VAR_TIMEOUT_LECTURA), TIMEOUT_LECTURA_POR_DEFECTO), VAR_TIMEOUT_LECTURA),
                parseEntero(valorODefecto(variables.get(VAR_REINTENTOS), REINTENTOS_POR_DEFECTO), VAR_REINTENTOS),
                parseLargo(valorODefecto(variables.get(VAR_ESPERA_REINTENTO), ESPERA_REINTENTO_POR_DEFECTO), VAR_ESPERA_REINTENTO),
                parseEntero(valorODefecto(variables.get(VAR_FALLOS_PARA_INDISPONIBLE), FALLOS_PARA_INDISPONIBLE_POR_DEFECTO), VAR_FALLOS_PARA_INDISPONIBLE),
                parseLargo(valorODefecto(variables.get(VAR_TIEMPO_INDISPONIBLE), TIEMPO_INDISPONIBLE_POR_DEFECTO), VAR_TIEMPO_INDISPONIBLE));
    }

    private static void validarPuerto(int puerto, String variable) {
        // Se permite 0 porque las pruebas piden un puerto efimero.
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException(variable + " fuera de rango: " + puerto);
        }
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
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
