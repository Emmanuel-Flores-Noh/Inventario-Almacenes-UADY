package mx.uady.inventario.broker.routing;

import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.broker.registry.RegistroServicios;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrutadorTest {

    @Test
    void enrutaAlProveedorYDevuelveRespuesta() throws Exception {
        ServerSocket provider = new ServerSocket(0);
        int port = provider.getLocalPort();
        CountDownLatch listo = new CountDownLatch(1);

        Thread hilo = new Thread(() -> {
            try {
                Socket socket = provider.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                String linea = in.readLine();
                Solicitud recibida = GsonFactory.DEFAULT.fromJson(linea, Solicitud.class);
                Respuesta respuesta = Respuesta.exito(recibida.getIdSolicitud(),
                        new com.google.gson.JsonPrimitive("OK"));
                out.println(GsonFactory.DEFAULT.toJson(respuesta));
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                listo.countDown();
            }
        });
        hilo.start();

        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));

        Enrutador enrutador = new Enrutador(2000);
        Solicitud solicitud = Solicitud.crear("req-1", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);

        assertEquals("req-1", respuesta.getIdSolicitud());
        assertEquals(mx.uady.inventario.common.contrato.EstadoRespuesta.EXITO, respuesta.getEstado());
        assertEquals("OK", respuesta.getResultado().getAsString());

        listo.await();
        provider.close();
    }

    @Test
    void servicioDesconocido() {
        RegistroServicios registro = new RegistroServicios();
        Enrutador enrutador = new Enrutador(2000);

        Solicitud solicitud = Solicitud.crear("req-2", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);

        assertEquals(mx.uady.inventario.common.error.CodigoError.SERVICIO_DESCONOCIDO, respuesta.getCodigoError());
    }

    @Test
    void solicitudInvalidaLanza() {
        RegistroServicios registro = new RegistroServicios();
        Enrutador enrutador = new Enrutador(2000);

        assertThrows(IllegalArgumentException.class, () -> enrutador.enrutar(null, registro));
    }

    @Test
    void proveedorSinRespuesta() throws Exception {
        ServerSocket provider = new ServerSocket(0);
        int port = provider.getLocalPort();

        Thread hilo = new Thread(() -> {
            try {
                Socket socket = provider.accept();
                socket.close();
            } catch (Exception ignored) {
            }
        });
        hilo.start();

        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));

        Enrutador enrutador = new Enrutador(500);
        Solicitud solicitud = Solicitud.crear("req-4", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);

        assertTrue(respuesta.getEstado() == mx.uady.inventario.common.contrato.EstadoRespuesta.ERROR);

        provider.close();
        hilo.join();
    }

    @Test
    void reintentaHastaAgotarLosIntentos() throws Exception {
        ServerSocket provider = new ServerSocket(0);
        int port = provider.getLocalPort();
        int intentosEsperados = 3;
        AtomicInteger conexiones = new AtomicInteger();
        CountDownLatch aceptadas = new CountDownLatch(intentosEsperados);

        Thread hilo = new Thread(() -> {
            try {
                while (conexiones.get() < intentosEsperados) {
                    Socket socket = provider.accept();
                    conexiones.incrementAndGet();
                    // Se cierra sin responder: simula un proveedor que se cae.
                    socket.close();
                    aceptadas.countDown();
                }
            } catch (Exception ignored) {
            }
        });
        hilo.start();

        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));
        // reintentos = 2 -> 3 intentos en total
        Enrutador enrutador = new Enrutador(500, 500, 2, 10);

        Solicitud solicitud = Solicitud.crear("req-retry", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);

        assertTrue(aceptadas.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(intentosEsperados, conexiones.get());
        assertEquals(mx.uady.inventario.common.contrato.EstadoRespuesta.ERROR, respuesta.getEstado());
        assertEquals("req-retry", respuesta.getIdSolicitud());

        provider.close();
        hilo.join();
    }

    @Test
    void unaRespuestaDeErrorNoSeReintenta() throws Exception {
        ServerSocket provider = new ServerSocket(0);
        int port = provider.getLocalPort();
        AtomicInteger conexiones = new AtomicInteger();

        Thread hilo = new Thread(() -> {
            try {
                Socket socket = provider.accept();
                conexiones.incrementAndGet();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Solicitud recibida = GsonFactory.DEFAULT.fromJson(in.readLine(), Solicitud.class);
                // El proveedor responde con un error de negocio valido.
                Respuesta respuesta = Respuesta.error(recibida.getIdSolicitud(),
                        mx.uady.inventario.common.error.CodigoError.OPERACION_NO_SOPORTADA, "no soportada");
                out.println(GsonFactory.DEFAULT.toJson(respuesta));
                socket.close();
            } catch (Exception ignored) {
            }
        });
        hilo.start();

        RegistroServicios registro = new RegistroServicios(3, 60_000);
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));
        Enrutador enrutador = new Enrutador(2000, 2000, 2, 10);

        Solicitud solicitud = Solicitud.crear("req-error", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);

        assertEquals(mx.uady.inventario.common.error.CodigoError.OPERACION_NO_SOPORTADA, respuesta.getCodigoError());
        assertEquals(1, conexiones.get());
        // La comunicacion fue exitosa: no debe acumular fallos.
        assertEquals(0, registro.fallos(Servicios.INVENTARIO));

        provider.close();
        hilo.join();
    }

    @Test
    void usaElTimeoutDeLecturaConfigurado() throws Exception {
        ServerSocket provider = new ServerSocket(0);
        int port = provider.getLocalPort();

        Thread hilo = new Thread(() -> {
            try {
                Socket socket = provider.accept();
                Thread.sleep(1500);
                socket.close();
            } catch (Exception ignored) {
            }
        });
        hilo.start();

        RegistroServicios registro = new RegistroServicios();
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));
        // Conexion holgada y lectura corta: si se usara el timeout de conexion, tardaria mucho mas.
        Enrutador enrutador = new Enrutador(5000, 300, 0, 0);

        Solicitud solicitud = Solicitud.crear("req-timeout", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        long inicio = System.nanoTime();
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);
        long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;

        assertEquals(mx.uady.inventario.common.error.CodigoError.TIMEOUT, respuesta.getCodigoError());
        assertTrue(transcurridoMs < 1500, "debio cortar por timeout de lectura, tardo " + transcurridoMs + " ms");

        provider.close();
        hilo.join();
    }

    @Test
    void proveedorNoDisponibleNoSeContacta() throws Exception {
        ServerSocket proveedor = new ServerSocket(0);
        int port = proveedor.getLocalPort();
        RegistroServicios registro = new RegistroServicios(1, 60_000);
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));
        registro.marcarFallo(Servicios.INVENTARIO);

        Enrutador enrutador = new Enrutador(500, 500, 2, 10);
        Solicitud solicitud = Solicitud.crear("req-nd", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject());
        Respuesta respuesta = enrutador.enrutar(solicitud, registro);

        assertEquals(mx.uady.inventario.common.error.CodigoError.PROVEEDOR_NO_REGISTRADO, respuesta.getCodigoError());
        assertTrue(respuesta.getMensajeError().contains("no disponible"));
        // El proveedor sigue escuchando: nadie se conecto.
        proveedor.setSoTimeout(300);
        assertThrows(java.net.SocketTimeoutException.class, proveedor::accept);
        proveedor.close();
    }

    @Test
    void fallosRepetidosMarcanElProveedorNoDisponible() throws Exception {
        // Puerto cerrado: las conexiones se rechazan de inmediato.
        ServerSocket temporal = new ServerSocket(0);
        int port = temporal.getLocalPort();
        temporal.close();

        RegistroServicios registro = new RegistroServicios(2, 60_000);
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));
        Enrutador enrutador = new Enrutador(500, 500, 0, 0);

        Respuesta primera = enrutador.enrutar(
                Solicitud.crear("req-f1", Servicios.INVENTARIO, "listarInventario", new com.google.gson.JsonObject()),
                registro);
        assertEquals(mx.uady.inventario.common.contrato.EstadoRespuesta.ERROR, primera.getEstado());
        assertTrue(registro.estaDisponible(Servicios.INVENTARIO));

        Respuesta segunda = enrutador.enrutar(
                Solicitud.crear("req-f2", Servicios.INVENTARIO, "listarInventario", new com.google.gson.JsonObject()),
                registro);
        assertEquals(mx.uady.inventario.common.contrato.EstadoRespuesta.ERROR, segunda.getEstado());
        assertFalse(registro.estaDisponible(Servicios.INVENTARIO));
        assertEquals(2, registro.fallos(Servicios.INVENTARIO));

        Respuesta tercera = enrutador.enrutar(
                Solicitud.crear("req-f3", Servicios.INVENTARIO, "listarInventario", new com.google.gson.JsonObject()),
                registro);
        assertEquals(mx.uady.inventario.common.error.CodigoError.PROVEEDOR_NO_REGISTRADO, tercera.getCodigoError());
    }

    /**
     * La bitacora anota cada intento fallido hacia el proveedor y, al alcanzar el umbral,
     * que el servicio quedo marcado como no disponible (una sola vez).
     */
    @Test
    void laBitacoraAnotaLosIntentosFallidosYLaMarcaDeNoDisponible() throws Exception {
        ServerSocket temporal = new ServerSocket(0);
        int port = temporal.getLocalPort();
        temporal.close();

        java.util.List<String> lineas = new java.util.ArrayList<>();
        RegistroServicios registro = new RegistroServicios(1, 60_000);
        registro.registrar(new Proveedor(Servicios.INVENTARIO, "localhost", port));
        // reintentos = 1 -> 2 intentos en total
        Enrutador enrutador = new Enrutador(500, 500, 1, 0,
                new mx.uady.inventario.broker.util.BitacoraBroker(lineas::add));

        enrutador.enrutar(Solicitud.crear("req-b1", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject()), registro);
        enrutador.enrutar(Solicitud.crear("req-b2", Servicios.INVENTARIO, "listarInventario",
                new com.google.gson.JsonObject()), registro);

        String todas = String.join("\n", lineas);
        assertEquals(3, lineas.size(), todas);
        assertTrue(lineas.get(0).contains("id=req-b1 inventario.listarInventario -> localhost:" + port
                + " | intento 1/2 fallo: ERROR_INTERNO"), todas);
        assertTrue(lineas.get(1).contains("| intento 2/2 fallo: ERROR_INTERNO"), todas);
        assertTrue(lineas.get(2).startsWith("[REGISTRO]")
                && lineas.get(2).endsWith("inventario marcado como NO DISPONIBLE tras 1 fallos"), todas);
        // La segunda solicitud ya no contacta al proveedor: no agrega intentos.
    }
}
