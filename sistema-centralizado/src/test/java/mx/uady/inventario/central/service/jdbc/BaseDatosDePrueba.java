package mx.uady.inventario.central.service.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mx.uady.inventario.central.repository.ProveedorConexion;
import mx.uady.inventario.central.util.ConfiguracionBD;

/**
 * Apoyo para las pruebas de integración: cada prueba crea sus propios productos con un
 * prefijo único y los borra al terminar, sin tocar los datos del seed.
 */
final class BaseDatosDePrueba implements AutoCloseable {

    final ProveedorConexion conexiones = ProveedorConexion.de(ConfiguracionBD.desdeEntorno());
    final String prefijo = "PRUEBA-" + UUID.randomUUID().toString().substring(0, 8);
    private final List<Integer> productosCreados = new ArrayList<>();

    /** Registra un producto creado por la prueba para borrarlo al final. */
    int recordar(int idProducto) {
        productosCreados.add(idProducto);
        return idProducto;
    }

    int crearProducto(String nombre, String precio) throws SQLException {
        try (Connection c = conexiones.abrir();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO productos (nombre, precio_actual) VALUES (?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            s.setString(1, prefijo + " " + nombre);
            s.setBigDecimal(2, new BigDecimal(precio));
            s.executeUpdate();
            try (ResultSet k = s.getGeneratedKeys()) {
                k.next();
                return recordar(k.getInt(1));
            }
        }
    }

    BigDecimal precioEnBase(int idProducto) throws SQLException {
        try (Connection c = conexiones.abrir();
             PreparedStatement s = c.prepareStatement("SELECT precio_actual FROM productos WHERE id_producto = ?")) {
            s.setInt(1, idProducto);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getBigDecimal(1) : null;
            }
        }
    }

    int contar(String sql, Object parametro) throws SQLException {
        try (Connection c = conexiones.abrir(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setObject(1, parametro);
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    String rutaQrEnBase(String numeroSerie) throws SQLException {
        try (Connection c = conexiones.abrir();
             PreparedStatement s = c.prepareStatement("SELECT ruta_qr FROM mercancia WHERE numero_serie = ?")) {
            s.setString(1, numeroSerie);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }

    @Override
    public void close() throws SQLException {
        try (Connection c = conexiones.abrir()) {
            for (int id : productosCreados) {
                for (String tabla : List.of("historial_precio", "mercancia", "productos")) {
                    // Los nombres de tabla son constantes de la prueba, no entrada externa.
                    try (PreparedStatement s = c.prepareStatement(
                            "DELETE FROM " + tabla + " WHERE id_producto = ?")) {
                        s.setInt(1, id);
                        s.executeUpdate();
                    }
                }
            }
        }
    }
}
