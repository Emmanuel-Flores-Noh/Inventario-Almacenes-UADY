package mx.uady.inventario.broker.registry;

import mx.uady.inventario.broker.model.Proveedor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroServiciosTest {

    @Test
    void registrarYBuscar() {
        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        Optional<Proveedor> encontrado = registro.buscar("inventario");
        assertTrue(encontrado.isPresent());
        assertEquals("localhost", encontrado.get().host());
        assertEquals(6000, encontrado.get().puerto());
    }

    @Test
    void sustituirExistente() {
        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor("inventario", "host1", 6000));

        boolean sustituido = registro.sustituir(new Proveedor("inventario", "host2", 6001));
        assertTrue(sustituido);
        assertEquals("host2", registro.buscar("inventario").orElseThrow().host());
        assertEquals(6001, registro.buscar("inventario").orElseThrow().puerto());
    }

    @Test
    void sustituirInexistenteNoRegistra() {
        RegistroServicios registro = new RegistroServicios();

        boolean sustituido = registro.sustituir(new Proveedor("inventario", "host2", 6001));
        assertFalse(sustituido);
        assertTrue(registro.buscar("inventario").isEmpty());
    }

    @Test
    void eliminar() {
        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        assertTrue(registro.eliminar("inventario"));
        assertTrue(registro.buscar("inventario").isEmpty());
        assertFalse(registro.eliminar("inventario"));
    }

    @Test
    void getServiciosEsCopia() {
        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        Map<String, Proveedor> copia = registro.getServicios();
        assertEquals(1, copia.size());
        assertThrows(UnsupportedOperationException.class, () -> copia.put("otro", new Proveedor("otro", "h", 1)));
    }

    @Test
    void buscarServicioVacio() {
        RegistroServicios registro = new RegistroServicios();
        assertTrue(registro.buscar(null).isEmpty());
        assertTrue(registro.buscar("  ").isEmpty());
    }

    @Test
    void concurrenciaBasica() throws InterruptedException {
        RegistroServicios registro = new RegistroServicios();
        int hilos = 8;
        int operaciones = 200;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch latch = new CountDownLatch(hilos);
        AtomicInteger errores = new AtomicInteger();

        for (int i = 0; i < hilos; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < operaciones; j++) {
                        String servicio = "svc-" + (idx * operaciones + j);
                        registro.registrar(new Proveedor(servicio, "localhost", 6000 + (j % 1000)));
                        registro.buscar(servicio);
                    }
                } catch (Exception e) {
                    errores.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();
        assertEquals(0, errores.get());
        assertEquals(hilos * operaciones, registro.getServicios().size());
    }

    @Test
    void marcaNoDisponibleAlAlcanzarUmbral() {
        RegistroServicios registro = new RegistroServicios(3, 30_000);
        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        registro.marcarFallo("inventario");
        registro.marcarFallo("inventario");
        assertTrue(registro.estaDisponible("inventario"));
        assertEquals(2, registro.fallos("inventario"));

        registro.marcarFallo("inventario");
        assertFalse(registro.estaDisponible("inventario"));
        assertTrue(registro.buscar("inventario").isPresent());
    }

    @Test
    void exitoLimpiaLosFallos() {
        RegistroServicios registro = new RegistroServicios(2, 30_000);
        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        registro.marcarFallo("inventario");
        registro.marcarExito("inventario");

        assertEquals(0, registro.fallos("inventario"));
        assertTrue(registro.estaDisponible("inventario"));
    }

    @Test
    void elEnfriamientoVenceYReactiva() {
        AtomicLong ahora = new AtomicLong(1_000);
        RegistroServicios registro = new RegistroServicios(2, 5_000, ahora::get);
        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        registro.marcarFallo("inventario");
        registro.marcarFallo("inventario");
        assertFalse(registro.estaDisponible("inventario"));

        // Un segundo antes de cumplirse el enfriamiento sigue fuera.
        ahora.set(5_999);
        assertFalse(registro.estaDisponible("inventario"));

        ahora.set(6_000);
        assertTrue(registro.estaDisponible("inventario"));

        // Tras el enfriamiento el conteo arranca de cero: hace falta otro fallo nuevo.
        registro.marcarFallo("inventario");
        assertTrue(registro.estaDisponible("inventario"));
        assertEquals(1, registro.fallos("inventario"));

        registro.marcarFallo("inventario");
        assertFalse(registro.estaDisponible("inventario"));
    }

    @Test
    void registrarDeNuevoReactiva() {
        RegistroServicios registro = new RegistroServicios(1, 30_000);
        registro.registrar(new Proveedor("inventario", "localhost", 6000));
        registro.marcarFallo("inventario");
        assertFalse(registro.estaDisponible("inventario"));

        registro.registrar(new Proveedor("inventario", "localhost", 6000));

        assertTrue(registro.estaDisponible("inventario"));
        assertEquals(0, registro.fallos("inventario"));
    }

    @Test
    void sustituirReactiva() {
        RegistroServicios registro = new RegistroServicios(1, 30_000);
        registro.registrar(new Proveedor("inventario", "localhost", 6000));
        registro.marcarFallo("inventario");
        assertFalse(registro.estaDisponible("inventario"));

        assertTrue(registro.sustituir(new Proveedor("inventario", "localhost", 6001)));
        assertTrue(registro.estaDisponible("inventario"));
    }

    @Test
    void servicioNoRegistradoNoEstaDisponible() {
        RegistroServicios registro = new RegistroServicios();
        assertFalse(registro.estaDisponible("inventario"));
        assertFalse(registro.estaDisponible(null));
        assertFalse(registro.estaDisponible("  "));
    }

    @Test
    void validaciones() {
        RegistroServicios registro = new RegistroServicios();
        assertThrows(IllegalArgumentException.class, () -> new Proveedor("", "host", 1));
        assertThrows(IllegalArgumentException.class, () -> new Proveedor("svc", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Proveedor("svc", "host", 0));
        assertThrows(NullPointerException.class, () -> registro.registrar(null));
    }

    @Test
    void validacionesDeResiliencia() {
        assertThrows(IllegalArgumentException.class, () -> new RegistroServicios(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new RegistroServicios(1, -1));
    }
}
