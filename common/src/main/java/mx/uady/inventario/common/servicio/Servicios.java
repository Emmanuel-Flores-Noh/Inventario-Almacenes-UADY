package mx.uady.inventario.common.servicio;

/**
 * Clase con la lista de servicios disponibles
 */
public final class Servicios {
    /** Servicio que se encarga de listar las tiendas */
    public static final String GESTOR_TIENDAS = "gestorTiendas";
    /** Servicio que se encarga de listar el inventario y registrar a que tienda fue vendida un producto **/
    public static final String INVENTARIO = "inventario";
    /** Servicio que se encarga de ver el historial de precio de un producto */
    public static final String GESTOR_PRECIOS = "gestorPrecios";
    /** Servicio interno del broker para registro de proveedores */
    public static final String BROKER = "broker";

    // Se oculta el constructor
    private Servicios() {}
}
