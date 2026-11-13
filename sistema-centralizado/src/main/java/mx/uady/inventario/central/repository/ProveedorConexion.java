package mx.uady.inventario.central.repository;

import java.sql.Connection;
import java.sql.SQLException;

import mx.uady.inventario.central.util.ConexionBD;
import mx.uady.inventario.central.util.ConfiguracionBD;

/** Abre conexiones JDBC; permite sustituir la base de datos en pruebas. */
@FunctionalInterface
public interface ProveedorConexion {

    /** @return una conexión abierta que el llamador debe cerrar */
    Connection abrir() throws SQLException;

    /** Conexiones a MariaDB con la configuración indicada. */
    static ProveedorConexion de(ConfiguracionBD configuracion) {
        return () -> ConexionBD.abrir(configuracion);
    }
}
