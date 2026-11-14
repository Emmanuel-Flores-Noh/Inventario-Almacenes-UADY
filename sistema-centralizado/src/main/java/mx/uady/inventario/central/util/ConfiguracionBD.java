package mx.uady.inventario.central.util;

import java.util.Map;
import java.util.Objects;

/**
 * Parametros de conexion a MariaDB tomados de las variables {@code DB_*}.
 *
 * <p>Se modela como record inmutable para que la configuracion se construya una
 * sola vez al arrancar el Sistema 1. La lectura se separa en {@link #desdeMapa(Map)}
 * para poder probarla sin modificar las variables de entorno del proceso.</p>
 *
 * @param host       servidor de MariaDB ({@code DB_HOST})
 * @param puerto     puerto TCP ({@code DB_PORT})
 * @param nombre     nombre de la base de datos ({@code DB_NAME})
 * @param usuario    usuario JDBC ({@code DB_USER})
 * @param contrasena contrasena JDBC ({@code DB_PASSWORD})
 */
public record ConfiguracionBD(String host, int puerto, String nombre, String usuario, String contrasena) {

    public static final String VAR_HOST = "DB_HOST";
    public static final String VAR_PUERTO = "DB_PORT";
    public static final String VAR_NOMBRE = "DB_NAME";
    public static final String VAR_USUARIO = "DB_USER";
    public static final String VAR_CONTRASENA = "DB_PASSWORD";

    // Solo host, puerto y nombre tienen default: credenciales nunca se escriben en el codigo.
    private static final String HOST_POR_DEFECTO = "localhost";
    private static final String PUERTO_POR_DEFECTO = "3306";
    private static final String NOMBRE_POR_DEFECTO = "almacenes_uady";

    /**
     * Valida y normaliza los parámetros de conexión.
     */
    public ConfiguracionBD {
        host = requerirTexto(host, "host");
        nombre = requerirTexto(nombre, "nombre de la base de datos");
        usuario = requerirTexto(usuario, "usuario");
        contrasena = requerirTexto(contrasena, "contrasena");
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("Puerto de base de datos invalido: " + puerto);
        }
    }

    /**
     * Lee la configuracion de las variables de entorno del proceso.
     *
     * @return la configuracion lista para abrir conexiones
     * @throws IllegalStateException si falta una variable obligatoria
     */
    public static ConfiguracionBD desdeEntorno() {
        return desdeMapa(System.getenv());
    }

    /**
     * Construye la configuracion a partir de un mapa de variables.
     *
     * @param variables valores indexados por nombre de variable; puede ser el entorno
     * @return la configuracion resultante
     * @throws IllegalStateException    si falta una variable obligatoria
     * @throws IllegalArgumentException si el puerto no es un numero valido
     */
    public static ConfiguracionBD desdeMapa(Map<String, String> variables) {
        Objects.requireNonNull(variables, "El mapa de variables no puede ser nulo");

        String host = valorODefecto(variables.get(VAR_HOST), HOST_POR_DEFECTO);
        String nombre = valorODefecto(variables.get(VAR_NOMBRE), NOMBRE_POR_DEFECTO);
        int puerto = parsearPuerto(valorODefecto(variables.get(VAR_PUERTO), PUERTO_POR_DEFECTO));
        String usuario = valorRequerido(variables.get(VAR_USUARIO), VAR_USUARIO);
        String contrasena = valorRequerido(variables.get(VAR_CONTRASENA), VAR_CONTRASENA);

        return new ConfiguracionBD(host, puerto, nombre, usuario, contrasena);
    }

    /**
     * URL JDBC que usa el conector de MariaDB.
     *
     * @return la URL con host, puerto y base de datos
     */
    public String url() {
        return "jdbc:mariadb://" + host + ":" + puerto + "/" + nombre;
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }

    private static String valorRequerido(String valor, String variable) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("La variable de entorno " + variable + " es obligatoria");
        }
        return valor.trim();
    }

    private static int parsearPuerto(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Puerto de base de datos invalido: " + valor, e);
        }
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser nulo o vacio");
        }
        return valor.trim();
    }
}
