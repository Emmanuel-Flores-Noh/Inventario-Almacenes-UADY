package mx.uady.inventario.central.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.service.ProductoValidator.Campo;
import mx.uady.inventario.common.modelo.Producto;

class ProductoValidatorTest {

    @Test
    void aceptaUnProductoValido() {
        assertTrue(ProductoValidator.errores("Silla", new BigDecimal("10.50")).isEmpty());
        assertDoesNotThrow(() -> ProductoValidator.exigirValido(new Producto(0, "Silla", null, new BigDecimal("1"))));
    }

    @Test
    void exigeNombre() {
        assertTrue(ProductoValidator.errores(null, BigDecimal.ONE).containsKey(Campo.NOMBRE));
        assertTrue(ProductoValidator.errores("   ", BigDecimal.ONE).containsKey(Campo.NOMBRE));
    }

    @Test
    void limitaElNombreAlTamanoDeLaColumna() {
        String largo = "a".repeat(ProductoValidator.LONGITUD_MAXIMA_NOMBRE + 1);
        assertTrue(ProductoValidator.errores(largo, BigDecimal.ONE).containsKey(Campo.NOMBRE));
        String justo = "a".repeat(ProductoValidator.LONGITUD_MAXIMA_NOMBRE);
        assertTrue(ProductoValidator.errores(justo, BigDecimal.ONE).isEmpty());
    }

    @Test
    void exigePrecioMayorQueCero() {
        assertEquals("El precio debe ser mayor que 0.",
                ProductoValidator.errores("Silla", BigDecimal.ZERO).get(Campo.PRECIO));
        assertTrue(ProductoValidator.errores("Silla", new BigDecimal("-1")).containsKey(Campo.PRECIO));
        assertTrue(ProductoValidator.errores("Silla", null).containsKey(Campo.PRECIO));
    }

    @Test
    void respetaLaPrecisionDecimalDeLaColumna() {
        assertTrue(ProductoValidator.errores("Silla", new BigDecimal("1.999")).containsKey(Campo.PRECIO));
        assertTrue(ProductoValidator.errores("Silla", new BigDecimal("1.500")).isEmpty());
        assertTrue(ProductoValidator.errores("Silla", new BigDecimal("100000000")).containsKey(Campo.PRECIO));
    }

    @Test
    void reportaTodosLosCamposConError() {
        Map<Campo, String> errores = ProductoValidator.errores("", BigDecimal.ZERO);
        assertEquals(2, errores.size());
        assertThrows(ValidacionException.class,
                () -> ProductoValidator.exigirValido(new Producto(0, "", null, BigDecimal.ZERO)));
    }
}
