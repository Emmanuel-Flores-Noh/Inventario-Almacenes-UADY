package mx.uady.inventario.central.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mx.uady.inventario.common.modelo.TiendaAlmacen;
import mx.uady.inventario.common.modelo.Usuario;

/** Catálogos de solo lectura que usa la pantalla de ventas: tiendas y usuarios. */
public class CatalogoRepository {

    private static final String TIENDAS =
            "SELECT id_tienda, nombre, direccion FROM tiendas_almacenes ORDER BY nombre, id_tienda";
    private static final String USUARIOS =
            "SELECT id_usuario, id_rol, nombres, apellido_paterno, apellido_materno FROM usuarios "
                    + "ORDER BY nombres, apellido_paterno, id_usuario";
    private static final String USUARIOS_POR_ROL =
            "SELECT u.id_usuario, u.id_rol, u.nombres, u.apellido_paterno, u.apellido_materno "
                    + "FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol "
                    + "WHERE r.nombre_rol = ? "
                    + "ORDER BY u.nombres, u.apellido_paterno, u.id_usuario";

    public List<TiendaAlmacen> listarTiendas(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(TIENDAS);
             ResultSet filas = sentencia.executeQuery()) {
            List<TiendaAlmacen> tiendas = new ArrayList<>();
            while (filas.next()) {
                tiendas.add(new TiendaAlmacen(filas.getInt("id_tienda"), filas.getString("nombre"),
                        filas.getString("direccion")));
            }
            return tiendas;
        }
    }

    public List<Usuario> listarUsuarios(Connection conexion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(USUARIOS);
             ResultSet filas = sentencia.executeQuery()) {
            return leerUsuarios(filas);
        }
    }

    /**
     * Lista los usuarios cuyo rol tiene el nombre indicado, ordenados por nombre.
     *
     * @param conexion  conexión abierta a MariaDB
     * @param nombreRol nombre exacto del rol en la tabla {@code roles}
     * @return usuarios con ese rol; lista vacía si no hay ninguno
     * @throws SQLException si la consulta falla
     */
    public List<Usuario> listarUsuariosPorRol(Connection conexion, String nombreRol) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(USUARIOS_POR_ROL)) {
            sentencia.setString(1, nombreRol);
            try (ResultSet filas = sentencia.executeQuery()) {
                return leerUsuarios(filas);
            }
        }
    }

    /**
     * Convierte las filas de una consulta de usuarios en objetos {@link Usuario}.
     *
     * @param filas resultado posicionado antes de la primera fila
     * @return usuarios leídos en el orden de la consulta
     * @throws SQLException si falla la lectura de alguna columna
     */
    private static List<Usuario> leerUsuarios(ResultSet filas) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        while (filas.next()) {
            usuarios.add(new Usuario(filas.getInt("id_usuario"), filas.getInt("id_rol"),
                    filas.getString("nombres"), filas.getString("apellido_paterno"),
                    filas.getString("apellido_materno")));
        }
        return usuarios;
    }
}
