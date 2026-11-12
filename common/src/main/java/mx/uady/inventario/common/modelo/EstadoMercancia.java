package mx.uady.inventario.common.modelo;

/**
 * Estado en el que puede encontrarse una pieza de mercancia.
 *
 * <p>Los nombres coinciden con los valores almacenados en la columna
 * {@code mercancia.estado} de MariaDB y con lo que viaja en el JSON, por lo
 * que no deben renombrarse sin migrar la base de datos y el contrato.</p>
 */
public enum EstadoMercancia {
    EN_ALMACEN,
    VENDIDO;

    /**
     * Convierte el valor de la base de datos o del JSON al enum.
     *
     * @param valor texto con el estado, por ejemplo {@code "en_almacen"}
     * @return el estado correspondiente
     * @throws IllegalArgumentException si el valor es nulo, vacio o desconocido
     */
    public static EstadoMercancia desdeCadena(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El estado de la mercancia no puede ser nulo o vacio");
        }
        try {
            // Se normaliza para tolerar diferencias de mayusculas o espacios
            // provenientes de la base de datos o de capturas manuales.
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de mercancia desconocido: " + valor, e);
        }
    }
}
