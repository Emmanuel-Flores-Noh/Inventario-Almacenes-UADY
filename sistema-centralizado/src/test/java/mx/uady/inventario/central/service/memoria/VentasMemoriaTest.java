package mx.uady.inventario.central.service.memoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.model.VentaHistorial;
import mx.uady.inventario.common.modelo.EstadoMercancia;

class VentasMemoriaTest {

    private AlmacenMemoria almacen;
    private VentasMemoria ventas;

    @BeforeEach
    void preparar() {
        almacen = AlmacenMemoria.conDatosDeEjemplo(Duration.ZERO);
        ventas = new VentasMemoria(almacen);
    }

    @Test
    void registrarMarcaLaUnidadComoVendidaYApareceEnElHistorial() {
        ventas.registrar("sn-uady-0001", 2, 3);

        assertEquals(EstadoMercancia.VENDIDO, almacen.mercancia.get("sn-uady-0001").estado());
        List<VentaHistorial> historial = ventas.listarHistorial();
        assertEquals(1, historial.size());
        assertEquals("Laptop Lenovo ThinkPad E14", historial.get(0).producto());
        assertEquals("Tienda Facultad de Ingenieria", historial.get(0).tienda());
        assertEquals("Lucero Chan Canul", historial.get(0).usuario());
    }

    @Test
    void aplicaLasMismasReglasQueLaBaseDeDatos() {
        assertEquals("La mercancía ya fue vendida",
                assertThrows(IllegalArgumentException.class, () -> ventas.registrar("SN-UADY-0002", 1, 1))
                        .getMessage());
        assertThrows(IllegalArgumentException.class, () -> ventas.registrar("NO-EXISTE", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> ventas.registrar("SN-UADY-0001", 99, 1));
        assertThrows(IllegalArgumentException.class, () -> ventas.registrar("SN-UADY-0001", 1, 99));
        assertEquals(EstadoMercancia.EN_ALMACEN, almacen.mercancia.get("sn-uady-0001").estado());
    }

    @Test
    void losCatalogosCoincidenConElSeed() {
        assertEquals(3, ventas.listarTiendas().size());
        assertEquals(4, ventas.listarUsuarios().size());
    }
}
