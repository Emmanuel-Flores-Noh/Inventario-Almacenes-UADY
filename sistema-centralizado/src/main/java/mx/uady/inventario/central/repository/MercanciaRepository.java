package mx.uady.inventario.central.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

/**
 * Acceso a la tabla {@code mercancia}. Las operaciones reciben la conexión para que el
 * servicio controle la transacción.
 */
public class MercanciaRepository {

    /** Código de MariaDB para una clave primaria o única duplicada. */
    static final int ERROR_CLAVE_DUPLICADA = 1062;

    private static final String COLUMNAS = "numero_serie, id_producto, estado, ruta_qr";
    private static final String BUSCAR = "SELECT " + COLUMNAS + " FROM mercancia WHERE numero_serie = ?";
    private static final String INSERTAR =
            "INSERT INTO mercancia (numero_serie, id_producto, estado, ruta_qr) VALUES (?, ?, ?, ?)";
    private static final String ACTUALIZAR_QR = "UPDATE mercancia SET ruta_qr = ? WHERE numero_serie = ?";

    /**
     * Unidades ordenadas por número de serie.
     *
     * @param idProducto producto a filtrar, o {@code null} para todos
     * @param estado     estado a filtrar, o {@code null} para todos
     */
    public List<Mercancia> listar(Connection conexion, Integer idProducto, EstadoMercancia estado)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNAS + " FROM mercancia WHERE 1 = 1");
        if (idProducto != null) {
            sql.append(" AND id_producto = ?");
        }
        if (estado != null) {
            sql.append(" AND estado = ?");
        }
        sql.append(" ORDER BY numero_serie");

        try (PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {
            int indice = 1;
            if (idProducto != null) {
                sentencia.setInt(indice++, idProducto);
            }
            if (estado != null) {
                sentencia.setString(indice, estado.name());
            }
            try (ResultSet filas = sentencia.executeQuery()) {
                List<Mercancia> resultado = new ArrayList<>();
                while (filas.next()) {
                    resultado.add(leer(filas));
                }
                return resultado;
            }
        }
    }

    /** Busca una unidad; con {@code bloquear} la reserva hasta el fin de la transacción. */
    public Optional<Mercancia> buscar(Connection conexion, String numeroSerie, boolean bloquear)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(bloquear ? BUSCAR + " FOR UPDATE" : BUSCAR)) {
            sentencia.setString(1, numeroSerie);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(leer(filas)) : Optional.empty();
            }
        }
    }

    /**
     * Inserta la unidad.
     *
     * @throws SQLException con {@link #esClaveDuplicada} verdadero si el número de serie ya existe
     */
    public void insertar(Connection conexion, Mercancia unidad) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(INSERTAR)) {
            sentencia.setString(1, unidad.numeroSerie());
            sentencia.setInt(2, unidad.idProducto());
            sentencia.setString(3, unidad.estado().name());
            sentencia.setString(4, unidad.rutaQr());
            sentencia.executeUpdate();
        }
    }

    /** @return {@code true} si la unidad existía y se actualizó */
    public boolean actualizarRutaQr(Connection conexion, String numeroSerie, String rutaQr) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR_QR)) {
            sentencia.setString(1, rutaQr);
            sentencia.setString(2, numeroSerie);
            return sentencia.executeUpdate() == 1;
        }
    }

    /** {@code true} si el error es una clave duplicada (el número de serie ya está registrado). */
    public static boolean esClaveDuplicada(SQLException error) {
        for (SQLException actual = error; actual != null; actual = actual.getNextException()) {
            if (actual.getErrorCode() == ERROR_CLAVE_DUPLICADA) {
                return true;
            }
        }
        return false;
    }

    private static Mercancia leer(ResultSet fila) throws SQLException {
        return new Mercancia(fila.getString("numero_serie"), fila.getInt("id_producto"),
                EstadoMercancia.desdeCadena(fila.getString("estado")), fila.getString("ruta_qr"));
    }
}
