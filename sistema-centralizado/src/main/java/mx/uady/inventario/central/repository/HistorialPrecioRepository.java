package mx.uady.inventario.central.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mx.uady.inventario.common.modelo.HistorialPrecio;

/** Acceso a la tabla {@code historial_precio}. */
public class HistorialPrecioRepository {

    private static final String INSERTAR =
            "INSERT INTO historial_precio (id_producto, precio_historico) VALUES (?, ?)";
    private static final String LISTAR =
            "SELECT id_historial, id_producto, precio_historico, fecha_cambio FROM historial_precio "
                    + "WHERE id_producto = ? ORDER BY fecha_cambio, id_historial";

    /** Guarda el precio que tenía el producto antes de cambiarlo; la fecha la asigna la base de datos. */
    public void insertar(Connection conexion, int idProducto, BigDecimal precioAnterior) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR)) {
            sentencia.setInt(1, idProducto);
            sentencia.setBigDecimal(2, precioAnterior);
            sentencia.executeUpdate();
        }
    }

    /** Historial de un producto, del cambio más antiguo al más reciente. */
    public List<HistorialPrecio> listar(Connection conexion, int idProducto) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(LISTAR)) {
            sentencia.setInt(1, idProducto);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<HistorialPrecio> resultado = new ArrayList<>();
                while (filas.next()) {
                    resultado.add(new HistorialPrecio(filas.getInt("id_historial"), filas.getInt("id_producto"),
                            filas.getBigDecimal("precio_historico"),
                            filas.getTimestamp("fecha_cambio").toLocalDateTime()));
                }
                return resultado;
            }
        }
    }
}
