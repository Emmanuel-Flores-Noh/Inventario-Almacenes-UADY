package mx.uady.inventario.central.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.view.VentaPanel;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.TiendaAlmacen;

class VentaControllerTest {

    @Test
    void cargaDatosAsincronamenteEnLaVista() throws Exception {
        Mercancia mercancia = new Mercancia("SN-0001", 1, EstadoMercancia.EN_ALMACEN, null);
        TiendaAlmacen tienda = new TiendaAlmacen(7, "Tienda Centro", "Calle 1");
        CountDownLatch cargaIniciada = new CountDownLatch(1);
        VentaPanel[] panelHolder = new VentaPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            VentaPanel panel = new VentaPanel();
            panelHolder[0] = panel;
            new VentaController(
                    () -> {
                        cargaIniciada.countDown();
                        return new VentaController.DatosMercancias(
                                List.of(mercancia), Map.of(1, "Laptop"));
                    },
                    () -> List.of(tienda),
                    new VentaService((serie, idTienda, idUsuario) -> null),
                    panel,
                    VentaControllerTest::ignorarConfirmacion
            ).cargar();
        });

        assertTrue(cargaIniciada.await(2, TimeUnit.SECONDS));
        AtomicBoolean seleccionada = new AtomicBoolean();
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!seleccionada.get() && System.nanoTime() < limite) {
            SwingUtilities.invokeAndWait(() -> {
                if (panelHolder[0].seleccionarMercancia("SN-0001")) {
                    assertEquals("SN-0001", panelHolder[0].seleccionada().orElseThrow().numeroSerie());
                    assertEquals(7, panelHolder[0].tiendaSeleccionada().orElseThrow().idTienda());
                    seleccionada.set(true);
                }
            });
            if (!seleccionada.get()) {
                Thread.sleep(20);
            }
        }
        assertTrue(seleccionada.get());
    }

    private static void ignorarConfirmacion(String texto) {
    }
}