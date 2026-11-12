package mx.uady.inventario.central.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Punto unico para abrir conexiones JDBC hacia MariaDB.
 *
 * <p>No se mantiene una conexion estatica: cada operacion de persistencia debe
 * abrir y cerrar su propia conexion (try-with-resources) hasta que en una fase
 * posterior se introduzca un pool. Esto evita fugas y conexiones invalidas
 * despues de un reinicio de la base de datos.</p>
 */
public final class ConexionBD {

    private ConexionBD() {}

    /**
     * Abre una conexion usando las variables de entorno {@code DB_*}.
     *
     * @return una conexion abierta; el llamador es responsable de cerrarla
     * @throws SQLException si la base de datos no esta disponible o rechaza las credenciales
     */
    public static Connection abrir() throws SQLException {
        return abrir(ConfiguracionBD.desdeEntorno());
    }

    /**
     * Abre una conexion con una configuracion explicita.
     *
     * <p>El driver de MariaDB se registra automaticamente por SPI, por lo que no
     * se necesita {@code Class.forName}.</p>
     *
     * @param configuracion parametros de conexion
     * @return una conexion abierta; el llamador es responsable de cerrarla
     * @throws SQLException si la base de datos no esta disponible o rechaza las credenciales
     */
    public static Connection abrir(ConfiguracionBD configuracion) throws SQLException {
        Objects.requireNonNull(configuracion, "La configuracion de base de datos no puede ser nula");
        return DriverManager.getConnection(
                configuracion.url(),
                configuracion.usuario(),
                configuracion.contrasena());
    }
}
