package mx.uady.inventario.central.service;

import java.util.List;

import mx.uady.inventario.central.model.VentaHistorial;
import mx.uady.inventario.common.modelo.TiendaAlmacen;
import mx.uady.inventario.common.modelo.Usuario;

/**
 * Consultas de la pestaña Ventas: historial y catálogos de tiendas y usuarios.
 *
 * <p>Errores: {@link AccesoDatosException} para fallos de la base de datos.</p>
 */
public interface ConsultasVentaService {

    /** Ventas con producto, tienda y usuario, de la más reciente a la más antigua. */
    List<VentaHistorial> listarHistorial();

    /** Tiendas ordenadas por nombre. */
    List<TiendaAlmacen> listarTiendas();

    /** Usuarios ordenados por nombre. */
    List<Usuario> listarUsuarios();

    /** Nombre del rol (tabla {@code roles}) de los usuarios que operan el Sistema 2. */
    String ROL_PROVEEDOR = "Proveedor Sistema 2";

    /**
     * Lista los usuarios con rol {@link #ROL_PROVEEDOR}, ordenados por nombre.
     *
     * <p>La implementación por defecto no está soportada; la versión sobre MariaDB la sobrescribe.</p>
     *
     * @return usuarios proveedores; lista vacía si no hay ninguno
     * @throws UnsupportedOperationException si la implementación no ofrece esta consulta
     * @throws AccesoDatosException si falla la base de datos
     */
    default List<Usuario> listarUsuariosProveedores() {
        throw new UnsupportedOperationException("Consulta de usuarios proveedores no disponible");
    }

    /** Nombre para mostrar: nombres y apellidos sin espacios de más. */
    static String nombreCompleto(Usuario usuario) {
        StringBuilder nombre = new StringBuilder(usuario.nombres());
        for (String apellido : new String[] {usuario.apellidoPaterno(), usuario.apellidoMaterno()}) {
            if (apellido != null && !apellido.isBlank()) {
                nombre.append(' ').append(apellido.strip());
            }
        }
        return nombre.toString();
    }
}
