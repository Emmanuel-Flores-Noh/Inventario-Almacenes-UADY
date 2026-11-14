package mx.uady.inventario.central.network;

import com.google.gson.JsonPrimitive;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistradorBrokerTest {

    /**
     * Broker minimo que responde error a las primeras conexiones y exito despues.
     * Permite verificar de forma determinista la logica de reintentos sin depender
     * del broker real.
     */
    private static final class BrokerFalso implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final int fallosIniciales;
        private final AtomicInteger conexiones = new AtomicInteger();
        private final List<Solicitud> solicitudes = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean activo = new AtomicBoolean(true);
        private final Thread hilo;

        BrokerFalso(int fallosIniciales) throws IOException {
            this.serverSocket = new ServerSocket(0);
            this.fallosIniciales = fallosIniciales;
            this.hilo = new Thread(this::atender, "broker-falso");
            this.hilo.setDaemon(true);
            this.hilo.start();
        }

        int getPuerto() {
            return serverSocket.getLocalPort();
        }

        List<Solicitud> getSolicitudes() {
            return List.copyOf(solicitudes);
        }

        private void atender() {
            while (activo.get()) {
                try (Socket socket = serverSocket.accept()) {
                    int numero = conexiones.incrementAndGet();
                    BufferedReader entrada = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

                    String linea = entrada.readLine();
                    if (linea == null || linea.isBlank()) {
                        continue;
                    }
                    Solicitud solicitud = GsonFactory.DEFAULT.fromJson(linea, Solicitud.class);
                    solicitudes.add(solicitud);

                    Respuesta respuesta = numero <= fallosIniciales
                            ? Respuesta.error(solicitud.getIdSolicitud(), CodigoError.ERROR_INTERNO, "aun no listo")
                            : Respuesta.exito(solicitud.getIdSolicitud(), new JsonPrimitive("Proveedor registrado"));
                    salida.println(GsonFactory.DEFAULT.toJson(respuesta));
                } catch (IOException e) {
                    // El socket del servidor se cerro: fin del hilo.
                }
            }
        }

        @Override
        public void close() throws Exception {
            activo.set(false);
            serverSocket.close();
            hilo.join(2000);
        }
    }

    @Test
    void registraLosTresServiciosConElHostYPuertoDelProveedor() throws Exception {
        try (BrokerFalso broker = new BrokerFalso(0)) {
            RegistradorBroker registrador = new RegistradorBroker(
                    "localhost", broker.getPuerto(), "sistema-centralizado", 6000,
                    RegistradorBroker.serviciosDelSistema(), 3, 10);

            assertTrue(registrador.registrarUnaVez());

            List<Solicitud> solicitudes = broker.getSolicitudes();
            assertEquals(3, solicitudes.size());

            Set<String> servicios = solicitudes.stream()
                    .map(s -> s.getParametros().get("servicio").getAsString())
                    .collect(Collectors.toSet());
            assertEquals(Set.of(Servicios.GESTOR_TIENDAS, Servicios.INVENTARIO, Servicios.GESTOR_PRECIOS), servicios);

            for (Solicitud solicitud : solicitudes) {
                assertEquals(Servicios.BROKER, solicitud.getNombreServicio());
                assertEquals(Operaciones.REGISTRAR_PROVEEDOR, solicitud.getNombreOperacion());
                assertEquals("sistema-centralizado", solicitud.getParametros().get("host").getAsString());
                assertEquals(6000, solicitud.getParametros().get("puerto").getAsInt());
            }
        }
    }

    @Test
    void reintentaCuandoElBrokerRechazaYTerminaAlTercerIntento() throws Exception {
        try (BrokerFalso broker = new BrokerFalso(2)) {
            RegistradorBroker registrador = new RegistradorBroker(
                    "localhost", broker.getPuerto(), "localhost", 6000,
                    RegistradorBroker.serviciosDelSistema(), 5, 10);

            assertTrue(registrador.registrarConReintentos());

            // Dos conexiones rechazadas (una por intento fallido) y tres registros exitosos.
            assertEquals(5, broker.getSolicitudes().size());
        }
    }

    @Test
    void seRindeTrasAgotarLosIntentos() throws Exception {
        try (BrokerFalso broker = new BrokerFalso(Integer.MAX_VALUE)) {
            RegistradorBroker registrador = new RegistradorBroker(
                    "localhost", broker.getPuerto(), "localhost", 6000,
                    RegistradorBroker.serviciosDelSistema(), 2, 10);

            assertFalse(registrador.registrarConReintentos());
            assertEquals(2, broker.getSolicitudes().size());
        }
    }

    @Test
    void devuelveFalsoSiElBrokerNoEstaEscuchando() throws Exception {
        int puertoLibre;
        try (ServerSocket temporal = new ServerSocket(0)) {
            puertoLibre = temporal.getLocalPort();
        }

        RegistradorBroker registrador = new RegistradorBroker(
                "localhost", puertoLibre, "localhost", 6000,
                List.of(Servicios.INVENTARIO), 2, 10);

        assertFalse(registrador.registrarConReintentos());
    }

    @Test
    void registraEnSegundoPlanoSinBloquearAlLlamador() throws Exception {
        try (BrokerFalso broker = new BrokerFalso(0)) {
            RegistradorBroker registrador = new RegistradorBroker(
                    "localhost", broker.getPuerto(), "localhost", 6000,
                    RegistradorBroker.serviciosDelSistema(), 3, 10);

            Thread hilo = registrador.iniciarEnSegundoPlano();
            hilo.join(2000);

            assertFalse(hilo.isAlive(), "El hilo de registro deberia terminar");
            assertEquals(3, broker.getSolicitudes().size());
        }
    }

    @Test
    void validaSusParametros() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RegistradorBroker("localhost", 5000, "localhost", 6000, List.of(), 3, 10));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new RegistradorBroker("localhost", 5000, "localhost", 6000,
                        List.of(Servicios.INVENTARIO), 0, 10));
    }
}
