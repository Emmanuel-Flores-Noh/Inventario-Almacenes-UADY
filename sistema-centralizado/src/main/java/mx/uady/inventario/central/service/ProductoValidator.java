package mx.uady.inventario.central.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import mx.uady.inventario.common.modelo.Producto;

/**
 * Reglas de captura de un producto, alineadas con la tabla {@code productos}.
 * La interfaz usa {@link #errores} para marcar cada campo; los servicios usan {@link #exigirValido}.
 */
public final class ProductoValidator {

    /** Campos validables del formulario de producto. */
    public enum Campo { NOMBRE, DESCRIPCION, PRECIO }

    static final int LONGITUD_MAXIMA_NOMBRE = 150;
    static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    private ProductoValidator() {
    }

    /** Devuelve los errores por campo; el mapa vacío significa que los datos son válidos. */
    public static Map<Campo, String> errores(String nombre, BigDecimal precio) {
        Map<Campo, String> errores = new EnumMap<>(Campo.class);

        if (nombre == null || nombre.isBlank()) {
            errores.put(Campo.NOMBRE, "El nombre es obligatorio.");
        } else if (nombre.strip().length() > LONGITUD_MAXIMA_NOMBRE) {
            errores.put(Campo.NOMBRE, "El nombre admite como máximo " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }

        if (precio == null) {
            errores.put(Campo.PRECIO, "El precio es obligatorio.");
        } else if (precio.signum() <= 0) {
            errores.put(Campo.PRECIO, "El precio debe ser mayor que 0.");
        } else if (precio.stripTrailingZeros().scale() > 2) {
            errores.put(Campo.PRECIO, "El precio admite como máximo 2 decimales.");
        } else if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            errores.put(Campo.PRECIO, "El precio no puede ser mayor que " + PRECIO_MAXIMO.toPlainString() + ".");
        }

        return errores;
    }

    /** Lanza {@link ValidacionException} con el primer error encontrado. */
    public static void exigirValido(Producto producto) {
        if (producto == null) {
            throw new ValidacionException("No se indicó el producto.");
        }
        Map<Campo, String> errores = errores(producto.nombre(), producto.precioActual());
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores.values().iterator().next());
        }
    }
}
