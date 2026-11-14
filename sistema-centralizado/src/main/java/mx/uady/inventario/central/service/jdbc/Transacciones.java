package mx.uady.inventario.central.service.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import mx.uady.inventario.central.repository.ProveedorConexion;
import mx.uady.inventario.central.service.AccesoDatosException;

/**
 * Ejecuta trabajo JDBC con una conexión propia.
 *
 * <ul>
 *   <li>{@link #leer}: consulta sin transacción explícita.</li>
 *   <li>{@link #escribir}: transacción con commit al terminar y rollback ante cualquier excepción.</li>
 * </ul>
 * Los {@link SQLException} se convierten en {@link AccesoDatosException}; las excepciones de negocio
 * se propagan tal cual después del rollback.
 */
public final class Transacciones {

    /** Trabajo que se ejecuta con una conexión abierta. */
    @FunctionalInterface
    public interface Trabajo<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private final ProveedorConexion conexiones;

    public Transacciones(ProveedorConexion conexiones) {
        this.conexiones = Objects.requireNonNull(conexiones, "conexiones");
    }

    /** @param operacion descripción para el registro de errores, p. ej. "listar los productos" */
    public <T> T leer(String operacion, Trabajo<T> trabajo) {
        try (Connection conexion = conexiones.abrir()) {
            return trabajo.ejecutar(conexion);
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo " + operacion, e);
        }
    }

    /** @param operacion descripción para el registro de errores, p. ej. "guardar el producto" */
    public <T> T escribir(String operacion, Trabajo<T> trabajo) {
        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                deshacer(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo " + operacion, e);
        }
    }

    private static void deshacer(Connection conexion, Exception causa) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            causa.addSuppressed(e);
        }
    }
}
