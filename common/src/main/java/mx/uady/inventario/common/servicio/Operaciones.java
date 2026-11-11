package mx.uady.inventario.common.servicio;

/**
 * Clase con todas las operaciones posibles
 */
public final class Operaciones {
    /** Listar las tiendas de la base de datos */
    public static final String LISTAR_TIENDAS = "listarTiendas";
    /** Registrar a que tienda específica de la UADY se le vendió un producto. */
    public static final String ACTUALIZAR_TIENDA_VENTA = "actualizarTiendaVenta";
    /** Listar el inventario de productos */
    public static final String LISTAR_INVENTARIO = "listarInventario";
    /** Listar el historial de precios de un producto */
    public static final String LISTAR_PRECIOS_PRODUCTO = "listarPreciosProducto";

    // Ocultar el constructor publico
    private Operaciones() {}
}
