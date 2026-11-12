package mx.uady.inventario.central.service.memoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.ValidacionException;
import mx.uady.inventario.common.modelo.HistorialPrecio;
import mx.uady.inventario.common.modelo.Producto;

class ProductoServiceMemoriaTest {

    private AlmacenMemoria almacen;
    private ProductoServiceMemoria servicio;

    @BeforeEach
    void preparar() {
        almacen = AlmacenMemoria.conDatosDeEjemplo(Duration.ZERO);
        servicio = new ProductoServiceMemoria(almacen);
    }

    @Test
    void listaLosProductosDelSeedOrdenadosConSusUnidades() {
        List<ProductoConUnidades> lista = servicio.listarConUnidades();

        assertEquals(5, lista.size());
        assertEquals("Impresora laser HP", lista.get(0).producto().nombre());
        assertTrue(lista.stream().allMatch(fila -> fila.unidades() == 2));
    }

    @Test
    void creaUnProductoNuevoConIdAsignadoYSinUnidades() {
        Producto nuevo = servicio.guardar(new Producto(0, "  Monitor Dell  ", "  ", new BigDecimal("3200")));

        assertEquals(6, nuevo.idProducto());
        assertEquals("Monitor Dell", nuevo.nombre());
        assertNull(nuevo.descripcion());
        assertTrue(servicio.listarConUnidades().stream()
                .anyMatch(f -> f.producto().idProducto() == 6 && f.unidades() == 0));
    }

    @Test
    void alCambiarElPrecioGuardaElAnteriorEnElHistorial() {
        servicio.guardar(new Producto(3, "Silla ergonomica", "Silla", new BigDecimal("3999.00")));

        List<HistorialPrecio> historial = almacen.historial(3);
        assertEquals(1, historial.size());
        assertEquals(0, new BigDecimal("3499.00").compareTo(historial.get(0).precioHistorico()));
    }

    @Test
    void sinCambioDePrecioNoAgregaHistorial() {
        servicio.guardar(new Producto(3, "Silla renombrada", "Silla", new BigDecimal("3499")));

        assertTrue(almacen.historial(3).isEmpty());
    }

    @Test
    void rechazaDatosInvalidosSinModificarNada() {
        assertThrows(ValidacionException.class,
                () -> servicio.guardar(new Producto(3, "Silla", null, BigDecimal.ZERO)));
        assertTrue(almacen.historial(3).isEmpty());
    }

    @Test
    void rechazaEditarUnProductoInexistente() {
        assertThrows(NegocioException.class,
                () -> servicio.guardar(new Producto(99, "Fantasma", null, BigDecimal.ONE)));
    }
}
