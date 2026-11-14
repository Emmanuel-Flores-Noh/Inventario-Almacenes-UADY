package mx.uady.inventario.cliente.model;

/**
 * Objeto de transferencia de datos (DTO) de una unidad de mercancía disponible, tal como la
 * devuelve la operación {@code listarInventario} del Sistema 1: incluye el nombre del producto
 * para mostrarlo y buscarlo en la tabla del proveedor.
 */
public class MercanciaDTO {
    private String numeroSerie;
    private Integer idProducto;
    private String nombreProducto;
    private String estado;

    /**
     * Constructor vacío requerido por Gson para el unmarshalling.
     */
    public MercanciaDTO() {
    }

    /**
     * Crea el DTO con todos sus datos.
     *
     * @param numeroSerie    número de serie de la unidad
     * @param idProducto     identificador del producto
     * @param nombreProducto nombre del producto
     * @param estado         estado de la unidad (p. ej. {@code EN_ALMACEN})
     */
    public MercanciaDTO(String numeroSerie, Integer idProducto, String nombreProducto, String estado) {
        this.numeroSerie = numeroSerie;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.estado = estado;
    }

    /**
     * Obtiene el número de serie de la unidad.
     *
     * @return número de serie
     */
    public String getNumeroSerie() {
        return numeroSerie;
    }

    /**
     * Obtiene el identificador del producto.
     *
     * @return id del producto
     */
    public Integer getIdProducto() {
        return idProducto;
    }

    /**
     * Obtiene el nombre del producto.
     *
     * @return nombre del producto
     */
    public String getNombreProducto() {
        return nombreProducto;
    }

    /**
     * Obtiene el estado de la unidad.
     *
     * @return estado como texto
     */
    public String getEstado() {
        return estado;
    }
}
