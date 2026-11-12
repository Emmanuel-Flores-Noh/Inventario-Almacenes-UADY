package mx.uady.inventario.central.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.memoria.AlmacenMemoria;
import mx.uady.inventario.central.service.memoria.MercanciaServiceMemoria;
import mx.uady.inventario.central.service.memoria.ProductoServiceMemoria;
import mx.uady.inventario.central.view.MercanciaPanel;
import mx.uady.inventario.common.modelo.EstadoMercancia;

class MercanciaControllerTest {

    @TempDir
    Path directorioQr;

    private MercanciaServiceMemoria mercancia;
    private MercanciaPanel panel;
    private MercanciaController controlador;
    private final AtomicInteger avisosDeUnidades = new AtomicInteger();

    @BeforeEach
    void preparar() throws Exception {
        AlmacenMemoria almacen = AlmacenMemoria.conDatosDeEjemplo(Duration.ZERO);
        mercancia = new MercanciaServiceMemoria(almacen, new QrService(directorioQr));
        SwingUtilities.invokeAndWait(() -> {
            panel = new MercanciaPanel();
            controlador = new MercanciaController(mercancia, new ProductoServiceMemoria(almacen), panel);
            controlador.setAlCambiarUnidades(avisosDeUnidades::incrementAndGet);
            controlador.cargar();
        });
        esperar(() -> filas() == 10);
    }

    @Test
    void cargarLlenaLaTablaYLosCombos() throws Exception {
        assertEquals(10, filas());
        assertTrue(enEdt(() -> panel.productoParaRegistrar().isPresent()));
        assertTrue(enEdt(() -> componente("registrar", JButton.class).isEnabled()));
    }

    @Test
    void registrarAgregaLaUnidadLaSeleccionaYAvisaAProductos() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            componente("serie", JTextField.class).setText("SN-NUEVA-77");
            componente("registrar", JButton.class).doClick();
        });
        esperar(() -> filas() == 11);
        esperar(() -> enEdt(() -> panel.seleccionada().isPresent()));

        assertEquals("SN-NUEVA-77", enEdt(() -> panel.seleccionada().orElseThrow().numeroSerie()));
        assertEquals("", enEdt(() -> componente("serie", JTextField.class).getText()));
        assertEquals(1, avisosDeUnidades.get());
        assertEquals(EstadoMercancia.EN_ALMACEN,
                mercancia.listar(null, null).stream()
                        .filter(m -> m.numeroSerie().equals("SN-NUEVA-77")).findFirst().orElseThrow().estado());
    }

    @Test
    void unaSerieDuplicadaSeMuestraJuntoAlCampoSinRegistrar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            componente("serie", JTextField.class).setText("SN-UADY-0001");
            componente("registrar", JButton.class).doClick();
        });
        esperar(() -> enEdt(() -> componente("errorSerie", JLabel.class).getText().contains("Ya existe")));

        assertEquals(10, filas());
        assertEquals("SN-UADY-0001", enEdt(() -> componente("serie", JTextField.class).getText()));
        assertEquals(0, avisosDeUnidades.get());
    }

    @Test
    void cambiarElFiltroDeEstadoRecargaDesdeElServicio() throws Exception {
        SwingUtilities.invokeAndWait(() -> componente("filtroEstado", javax.swing.JComboBox.class)
                .setSelectedItem(EstadoMercancia.VENDIDO));
        esperar(() -> filas() == 4);
        assertEquals(4, filas());
    }

    // ---- Auxiliares ----

    private int filas() throws Exception {
        return enEdt(() -> componente("tablaUnidades", JTable.class).getRowCount());
    }

    private <T extends Component> T componente(String nombre, Class<T> tipo) {
        T encontrado = buscar(panel, nombre, tipo);
        if (encontrado == null) {
            throw new AssertionError("No se encontró " + nombre);
        }
        return encontrado;
    }

    private static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container hijo) {
                T encontrado = buscar(hijo, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
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
