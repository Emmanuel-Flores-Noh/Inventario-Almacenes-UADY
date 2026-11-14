package mx.uady.inventario.central.network;

import com.google.gson.JsonPrimitive;
import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ServidorServicioTest {

    private static DespachadorSolicitudes despachadorDePrueba() {
        DespachadorSolicitudes despachador = new DespachadorSolicitudes();
        despachador.registrarOperacion(Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO,
                parametros -> new JsonPrimitive("ok"));
        return despachador;
    }

    private static Respuesta enviarYLeer(int puerto, Solicitud solicitud) throws Exception {
        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            salida.println(GsonFactory.DEFAULT.toJson(solicitud));
            return GsonFactory.DEFAULT.fromJson(entrada.readLine(), Respuesta.class);
        }
    }

    @Test
    void atiendeUnaSolicitudYConservaElId() throws Exception {
        ServidorServicio servidor = new ServidorServicio(0, 4, despachadorDePrueba());
        servidor.iniciar();
        try {
            Respuesta respuesta = enviarYLeer(servidor.getPuerto(), Solicitud.crear(
                    "req-socket", Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO, null));

            assertEquals("req-socket", respuesta.getIdSolicitud());
            assertEquals(EstadoRespuesta.EXITO, respuesta.getEstado());
            assertEquals("ok", respuesta.getResultado().getAsString());
        } finally {
            servidor.detener();
        }
    }

    @Test
    void jsonMalformadoDevuelveParametrosInvalidosYNoTumbaElServidor() throws Exception {
        ServidorServicio servidor = new ServidorServicio(0, 4, despachadorDePrueba());
        servidor.iniciar();
        int puerto = servidor.getPuerto();
        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            salida.println("{esto no es json");
            Respuesta error = GsonFactory.DEFAULT.fromJson(entrada.readLine(), Respuesta.class);
            assertEquals(CodigoError.PARAMETROS_INVALIDOS, error.getCodigoError());

            // La misma conexion sigue viva despues del error.
            salida.println(GsonFactory.DEFAULT.toJson(Solicitud.crear(
                    "req-2", Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO, null)));
            Respuesta ok = GsonFactory.DEFAULT.fromJson(entrada.readLine(), Respuesta.class);
            assertEquals(EstadoRespuesta.EXITO, ok.getEstado());
        } finally {
            servidor.detener();
        }
    }

    @Test
    void operacionPendienteDevuelveNoSoportada() throws Exception {
        ServidorServicio servidor = new ServidorServicio(0, 4, DespachadorSolicitudes.estandar());
        servidor.iniciar();
        try {
            Respuesta respuesta = enviarYLeer(servidor.getPuerto(), Solicitud.crear(
                    "req-pendiente", Servicios.GESTOR_PRECIOS, Operaciones.LISTAR_PRECIOS_PRODUCTO, null));

            assertEquals(CodigoError.OPERACION_NO_SOPORTADA, respuesta.getCodigoError());
            assertEquals("req-pendiente", respuesta.getIdSolicitud());
        } finally {
            servidor.detener();
        }
    }

    @Test
    void atiendeVariasSolicitudesEnLaMismaConexion() throws Exception {
        ServidorServicio servidor = new ServidorServicio(0, 4, despachadorDePrueba());
        servidor.iniciar();
        try (Socket socket = new Socket("localhost", servidor.getPuerto());
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            for (int i = 0; i < 3; i++) {
                salida.println(GsonFactory.DEFAULT.toJson(Solicitud.crear(
                        "req-" + i, Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO, null)));
                Respuesta respuesta = GsonFactory.DEFAULT.fromJson(entrada.readLine(), Respuesta.class);
                assertEquals("req-" + i, respuesta.getIdSolicitud());
            }
        } finally {
            servidor.detener();
        }
    }

    @Test
    void atiendeClientesConcurrentesSinMezclarRespuestas() throws Exception {
        int clientes = 20;
        ServidorServicio servidor = new ServidorServicio(0, 8, despachadorDePrueba());
        servidor.iniciar();
        ExecutorService ejecutor = Executors.newFixedThreadPool(clientes);
        CountDownLatch listos = new CountDownLatch(1);

        try {
            List<Future<Respuesta>> futuros = new ArrayList<>();
            for (int i = 0; i < clientes; i++) {
                String id = "req-concurrente-" + i;
                futuros.add(ejecutor.submit(() -> {
                    listos.await();
                    return enviarYLeer(servidor.getPuerto(), Solicitud.crear(
                            id, Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO, null));
                }));
            }
            listos.countDown();

            for (int i = 0; i < clientes; i++) {
                Respuesta respuesta = futuros.get(i).get(10, TimeUnit.SECONDS);
                assertEquals("req-concurrente-" + i, respuesta.getIdSolicitud());
                assertEquals(EstadoRespuesta.EXITO, respuesta.getEstado());
            }
        } finally {
            ejecutor.shutdownNow();
            servidor.detener();
        }
    }

    @Test
    void detenerCierraElPuerto() throws Exception {
        ServidorServicio servidor = new ServidorServicio(0, 2, despachadorDePrueba());
        servidor.iniciar();
        int puerto = servidor.getPuerto();
        assertTrue(puerto > 0);

        servidor.detener();

        try (Socket socket = new Socket("localhost", puerto)) {
            fail("El puerto deberia estar cerrado");
        } catch (java.io.IOException esperado) {
            // Conexion rechazada: el servidor se detuvo.
        }
    }
}
