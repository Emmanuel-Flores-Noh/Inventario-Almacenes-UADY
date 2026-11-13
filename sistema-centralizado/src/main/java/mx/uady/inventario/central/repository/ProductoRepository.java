package mx.uady.inventario.central.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Acceso a la tabla {@code productos}. Las operaciones reciben la conexión para que el
 * servicio controle la transacción.
 */
public class ProductoRepository {

    private static final String COLUMNAS = "p.id_producto, p.nombre, p.descripcion, p.precio_actual";

    private static final String LISTAR_CON_UNIDADES =
            "SELECT " + COLUMNAS + ", COUNT(m.numero_serie) AS unidades "
                    + "FROM productos p LEFT JOIN mercancia m ON m.id_producto = p.id_producto "
                    + "GROUP BY " + COLUMNAS + " ORDER BY p.nombre, p.id_producto";
    private static final String BUSCAR = "SELECT " + COLUMNAS + " FROM productos p WHERE p.id_producto = ?";
    private static final String INSERTAR =
            "INSERT INTO productos (nombre, descripcion, precio_actual) VALUES (?, ?, ?)";
    private static final String ACTUALIZAR =
            "UPDATE productos SET nombre = ?, descripcion = ?, precio_actual = ? WHERE id_producto = ?";

    public List<ProductoConUnidades> listarConUnidades(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(LISTAR_CON_UNIDADES);
             ResultSet filas = sentencia.executeQuery()) {
            List<ProductoConUnidades> resultado = new ArrayList<>();
            while (filas.next()) {
                resultado.add(new ProductoConUnidades(leer(filas), filas.getInt("unidades")));
            }
            return resultado;
        }
    }

    /** Busca un producto; con {@code bloquear} lo reserva hasta el fin de la transacción. */
    public Optional<Producto> buscar(Connection conexion, int idProducto, boolean bloquear) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(bloquear ? BUSCAR + " FOR UPDATE" : BUSCAR)) {
            sentencia.setInt(1, idProducto);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(leer(filas)) : Optional.empty();
            }
        }
    }

    /** @return el producto con el id asignado por la base de datos */
    public Producto insertar(Connection conexion, Producto producto) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(sentencia, producto);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("La base de datos no devolvió el id del producto nuevo");
                }
                return new Producto(claves.getInt(1), producto.nombre(), producto.descripcion(),
                        producto.precioActual());
            }
        }
    }

    /** @return {@code true} si existía y se actualizó */
    public boolean actualizar(Connection conexion, Producto producto) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR)) {
            asignarDatos(sentencia, producto);
            sentencia.setInt(4, producto.idProducto());
            return sentencia.executeUpdate() == 1;
        }
    }

    private static void asignarDatos(PreparedStatement sentencia, Producto producto) throws SQLException {
        sentencia.setString(1, producto.nombre());
        sentencia.setString(2, producto.descripcion());
        sentencia.setBigDecimal(3, producto.precioActual());
    }

    private static Producto leer(ResultSet fila) throws SQLException {
        return new Producto(fila.getInt("id_producto"), fila.getString("nombre"),
                fila.getString("descripcion"), fila.getBigDecimal("precio_actual"));
    }
}
