package mx.uady.inventario.common.servicio;

/**
 * Clase con todas las operaciones posibles
 */
public final class Operaciones {
    /**
     * Operaciones servicio gestorTiendas
     */
    /** Listar las tiendas de la base de datos */
    public static final String LISTAR_TIENDAS = "listarTiendas";
    /** Registrar a que tienda específica de la UADY se le vendió un producto. */
    public static final String ACTUALIZAR_TIENDA_VENTA = "actualizarTiendaVenta";

    /**
     * Operaciones servicio inventario
     */
    /** Listar el inventario de productos */
    public static final String LISTAR_INVENTARIO = "listarInventario";

    /**
     * Operaciones servicio gestorPrecios
     */
    /** Listar el historial de precios de un producto */
    public static final String LISTAR_PRECIOS_PRODUCTO = "listarPreciosProducto";

    /**
     * Operaciones del broker
     */
    /** Registrar un proveedor en el broker */
    public static final String REGISTRAR_PROVEEDOR = "registrarProveedor";
    /** Eliminar un servicio en el broker */
    public static final String REMOVER_PROVEEDOR = "removerProveedor";
    /** Actualizar un proveedor en el broker */
    public static final String ACTUALIZAR_PROVEEDOR = "actualizarProveedor";

    // Ocultar el constructor publico
    private Operaciones() {}
}