package mx.uady.inventario.common.error;

/**
 * Catalogo de errores del contrato de comunicacion.
 */
public enum CodigoError {
    SERVICIO_DESCONOCIDO("E001", "Servicio desconocido"),
    OPERACION_NO_SOPORTADA("E002", "Operacion no soportada"),
    PARAMETROS_INVALIDOS("E003", "Parametros invalidos"),
    PROVEEDOR_NO_REGISTRADO("E004", "Proveedor no registrado"),
    TIMEOUT("E005", "Tiempo de espera agotado"),
    RECURSO_NO_ENCONTRADO("E006", "Recurso no encontrado"),
    ERROR_INTERNO("E999", "Error interno del sistema");

    private final String codigo;
    private final String mensajePorDefecto;

    CodigoError(String codigo, String mensajePorDefecto) {
        this.codigo = codigo;
        this.mensajePorDefecto = mensajePorDefecto;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensajePorDefecto() {
        return mensajePorDefecto;
    }
}
