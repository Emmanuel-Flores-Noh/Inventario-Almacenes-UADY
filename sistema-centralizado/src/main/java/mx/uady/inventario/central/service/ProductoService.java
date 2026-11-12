package mx.uady.inventario.central.service;

import java.util.List;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Alta, edición y consulta de productos (CU1).
 *
 * <p>Errores: {@link ValidacionException} o {@link NegocioException} para reglas de negocio,
 * {@link AccesoDatosException} para fallos de la base de datos.</p>
 */
public interface ProductoService {

    /** Todos los productos con su número de unidades, ordenados por nombre. */
    List<ProductoConUnidades> listarConUnidades();

    /**
     * Crea el producto si {@code idProducto == 0}; si no, lo actualiza.
     * Cuando cambia el precio, guarda el anterior en {@code historial_precio}
     * dentro de la misma transacción que la actualización.
     *
     * @return el producto tal como quedó guardado (con su id asignado si es nuevo)
     */
    Producto guardar(Producto producto);
}
