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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
