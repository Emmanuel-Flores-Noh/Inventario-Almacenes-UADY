package mx.uady.inventario.central.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.service.memoria.AlmacenMemoria;
import mx.uady.inventario.central.service.memoria.MercanciaServiceMemoria;
import mx.uady.inventario.central.service.memoria.VentasMemoria;
import mx.uady.inventario.central.view.VentaPanel;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

class VentaControllerTest {

    private MercanciaServiceMemoria mercancia;
    private VentaPanel panel;
    private VentaController controlador;
    private final AtomicInteger ventasAvisadas = new AtomicInteger();
    private final AtomicReference<String> preguntaConfirmada = new AtomicReference<>();

    @BeforeEach
    void preparar() throws Exception {
        AlmacenMemoria almacen = AlmacenMemoria.conDatosDeEjemplo(Duration.ZERO);
        mercancia = new MercanciaServiceMemoria(almacen, new QrService(Files.createTempDirectory("qr-venta-")));
        VentasMemoria ventas = new VentasMemoria(almacen);
        SwingUtilities.invokeAndWait(() -> {
            panel = new VentaPanel();
            controlador = new VentaController(
                    () -> new VentaController.DatosMercancias(
                            mercancia.listar(null, EstadoMercancia.EN_ALMACEN), Map.of(1, "Laptop")),
                    () -> new VentaController.Catalogos(ventas.listarTiendas(), ventas.listarUsuarios()),
                    new VentaService(ventas),
                    panel);
            controlador.setConfirmar(pregunta -> {
                preguntaConfirmada.set(pregunta);
                return true;
            });
            controlador.setAlRegistrarVenta(ventasAvisadas::incrementAndGet);
            controlador.cargar();
        });
        esperar(() -> filas() == 6);
    }

    @Test
    void cargaUnidadesDisponiblesTiendasYUsuarios() throws Exception {
        assertEquals(6, filas());
        assertTrue(enEdt(() -> panel.tiendaSeleccionada().isPresent()));
        assertTrue(enEdt(() -> panel.usuarioSeleccionado().isPresent()));
    }

    @Test
    void venderMarcaLaUnidadComoVendidaYAvisaALasDemasPestanas() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.seleccionarMercancia("SN-UADY-0001");
            boton("registrarVenta").doClick();
        });
        esperar(() -> filas() == 5);

        assertEquals(1, ventasAvisadas.get());
        assertTrue(preguntaConfirmada.get().contains("SN-UADY-0001"));
        assertEquals(EstadoMercancia.VENDIDO, estadoDe("SN-UADY-0001"));
        esperar(() -> enEdt(() -> etiquetaEstado().startsWith("Venta registrada: SN-UADY-0001")));
    }

    @Test
    void siSeCancelaLaConfirmacionNoSeVende() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            controlador.setConfirmar(pregunta -> false);
            panel.seleccionarMercancia("SN-UADY-0003");
            boton("registrarVenta").doClick();
        });
        Thread.sleep(100);
        assertEquals(EstadoMercancia.EN_ALMACEN, estadoDe("SN-UADY-0003"));
        assertEquals(0, ventasAvisadas.get());
    }

    // ---- Auxiliares ----

    private EstadoMercancia estadoDe(String serie) {
        return mercancia.listar(null, null).stream().filter(m -> m.numeroSerie().equals(serie))
                .map(Mercancia::estado).findFirst().orElseThrow();
    }

    private int filas() throws Exception {
        return enEdt(() -> buscar(panel, "tablaVentas", JTable.class).getRowCount());
    }

    private JButton boton(String nombre) {
        return buscar(panel, nombre, JButton.class);
    }

    private static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container hijo) {
                T encontrado = buscarONull(hijo, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        throw new AssertionError("No se encontró " + nombre);
    }

    private static <T extends Component> T buscarONull(Container raiz, String nombre, Class<T> tipo) {
        try {
            return buscar(raiz, nombre, tipo);
        } catch (AssertionError e) {
            return null;
        }
    }

    private String etiquetaEstado() {
        JLabel ultima = null;
        for (var c : panel.getComponents()) {
            if (c instanceof JLabel l) {
                ultima = l;
            }
        }
        return ultima == null ? "" : ultima.getText();
    }

    private static <T> T enEdt(Callable<T> consulta) throws Exception {
        AtomicReference<T> resultado = new AtomicReference<>();
        AtomicReference<Exception> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                resultado.set(consulta.call());
            } catch (Exception e) {
                fallo.set(e);
            }
        });
        if (fallo.get() != null) {
            throw fallo.get();
        }
        return resultado.get();
    }

    private static void esperar(Callable<Boolean> condicion) throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < limite) {
            if (condicion.call()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("La condición no se cumplió en 5 segundos");
    }
}
