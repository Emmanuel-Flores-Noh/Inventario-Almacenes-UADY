package mx.uady.inventario.central.repository;

import mx.uady.inventario.central.util.ConexionBD;
import mx.uady.inventario.central.util.ConfiguracionBD;
import mx.uady.inventario.common.modelo.EstadoMercancia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Repositorio encargado de registrar ventas de forma atómica en MariaDB.
 */
public class VentaRepository {

    private static final String CONSULTAR_MERCANCIA =
            "SELECT estado FROM mercancia WHERE numero_serie = ? FOR UPDATE";
    private static final String EXISTE_TIENDA =
            "SELECT 1 FROM tiendas_almacenes WHERE id_tienda = ?";
    private static final String EXISTE_USUARIO =
            "SELECT 1 FROM usuarios WHERE id_usuario = ?";
    private static final String INSERTAR_VENTA =
            "INSERT INTO ventas (numero_serie, id_tienda, id_usuario, fecha_venta) "
                    + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
    private static final String ACTUALIZAR_ESTADO =
            "UPDATE mercancia SET estado = ? WHERE numero_serie = ?";

    private final ConnectionProvider connectionProvider;

    /**
     * Crea el repositorio usando la configuración JDBC del proceso.
     */
    public VentaRepository() {
        this(ConexionBD::abrir);
    }

    /**
     * Crea el repositorio con una configuración JDBC explícita.
     *
     * @param configuracion configuración de MariaDB
     */
    public VentaRepository(ConfiguracionBD configuracion) {
        this(() -> ConexionBD.abrir(Objects.requireNonNull(
                configuracion,
                "La configuración de base de datos no puede ser nula"
        )));
    }

    /**
     * Crea el repositorio con un proveedor de conexiones explícito.
     *
     * @param connectionProvider proveedor usado para abrir conexiones
     */
    public VentaRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider,
                "El proveedor de conexiones no puede ser nulo"
        );
    }

    /**
     * Registra una venta y cambia el estado de la mercancía en una transacción.
     *
     * @param numeroSerie número de serie de la mercancía
     * @param idTienda identificador de la tienda destino
     * @param idUsuario identificador del usuario que registra la venta
     * @return datos mínimos de la venta confirmada
     * @throws VentaRepositoryException si la operación no puede completarse
     */
    public VentaResultado registrarVenta(String numeroSerie, int idTienda, int idUsuario) throws VentaRepositoryException {
        try (Connection conexion = connectionProvider.abrir()) {
            conexion.setAutoCommit(false);
            try {
                validarMercanciaDisponible(conexion, numeroSerie);
                validarExistencia(conexion, EXISTE_TIENDA, idTienda, "La tienda no existe");
                validarExistencia(conexion, EXISTE_USUARIO, idUsuario, "El usuario no existe");
                insertarVenta(conexion, numeroSerie, idTienda, idUsuario);
                actualizarEstado(conexion, numeroSerie);
                conexion.commit();
                return new VentaResultado(numeroSerie, idTienda, obtenerFechaVenta(conexion, numeroSerie));
            } catch (IllegalArgumentException error) {
                rollback(conexion, error);
                throw error;
            } catch (SQLException error) {
                rollback(conexion, error);
                throw new VentaRepositoryException("No fue posible registrar la venta", error);
            } catch (RuntimeException error) {
                rollback(conexion, error);
                throw new VentaRepositoryException("No fue posible registrar la venta", error);
            } finally {
                restaurarAutoCommit(conexion);
            }
        } catch (SQLException error) {
            throw new VentaRepositoryException("No fue posible abrir o cerrar la conexión", error);
        }
    }

    private void validarMercanciaDisponible(Connection conexion, String numeroSerie) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(CONSULTAR_MERCANCIA)) {
            sentencia.setString(1, numeroSerie);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException("La mercancía no existe");
                }
                String estado = resultado.getString("estado");
                if (!EstadoMercancia.EN_ALMACEN.name().equals(estado)) {
                    throw new IllegalArgumentException("La mercancía ya fue vendida");
                }
            }
        }
    }

    private void validarExistencia(Connection conexion, String consulta, int id, String mensaje) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(consulta)) {
            sentencia.setInt(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException(mensaje);
                }
            }
        }
    }

    private void insertarVenta(Connection conexion, String numeroSerie, int idTienda, int idUsuario) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR_VENTA)) {
            sentencia.setString(1, numeroSerie);
            sentencia.setInt(2, idTienda);
            sentencia.setInt(3, idUsuario);
            sentencia.executeUpdate();
        }
    }

    private void actualizarEstado(Connection conexion, String numeroSerie) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR_ESTADO)) {
            sentencia.setString(1, EstadoMercancia.VENDIDO.name());
            sentencia.setString(2, numeroSerie);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("La mercancía no pudo actualizarse");
            }
        }
    }

    private LocalDateTime obtenerFechaVenta(Connection conexion, String numeroSerie) throws SQLException {
        String consulta = "SELECT fecha_venta FROM ventas WHERE numero_serie = ? " + "ORDER BY id_venta DESC LIMIT 1";
        try (PreparedStatement sentencia = conexion.prepareStatement(consulta)) {
            sentencia.setString(1, numeroSerie);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("La venta confirmada no pudo recuperarse");
                }
                Timestamp fecha = resultado.getTimestamp("fecha_venta");
                return fecha.toLocalDateTime();
            }
        }
    }

    private void rollback(Connection conexion, Throwable causa) {
        try {
            conexion.rollback();
        } catch (SQLException rollbackError) {
            causa.addSuppressed(rollbackError);
        }
    }

    private void restaurarAutoCommit(Connection conexion) {
        try {
            conexion.setAutoCommit(true);
        } catch (SQLException ignored) {
            // La conexión se cerrará inmediatamente después de este punto.
        }
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        /**
         * Abre una conexión para una operación transaccional.
         *
         * @return conexión JDBC abierta
         * @throws SQLException si no puede abrirse la conexión
         */
        Connection abrir() throws SQLException;
    }
}
