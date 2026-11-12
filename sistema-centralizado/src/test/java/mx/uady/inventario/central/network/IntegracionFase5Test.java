package mx.uady.inventario.central.network;

import com.google.gson.JsonPrimitive;
import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.broker.network.BrokerServer;
import mx.uady.inventario.broker.util.ConfiguracionBroker;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion de la Fase 5.
 *
 * <p>A diferencia de las pruebas unitarias, aqui se arrancan las dos piezas
 * reales: el {@link BrokerServer} y el {@link ServidorServicio}. La unica pieza
 * simulada es el manejador de la operacion, porque el contrato todavia no tiene
 * logica de negocio y se necesita controlar cuanto tarda en responder.</p>
 *
 * <p>Todos los puertos son efimeros y los margenes de tiempo son holgados para
 * que las pruebas no dependan de puertos ocupados ni de la velocidad de la
 * maquina.</p>
 */
class IntegracionFase5Test {

    private static final int CLIENTES_SIMULTANEOS = 20;
    private static final int ESPERA_RESPUESTA_SEGUNDOS = 20;
    private static final int TIMEOUT_LECTURA_CAIDA_MS = 500;
    private static final long BLOQUEO_MANEJADOR_CAIDA_MS = 3000;

    /**
     * Abre un socket al broker, envia una solicitud y devuelve la respuesta.
     *
     * @param puertoBroker puerto real del broker
     * @param solicitud    solicitud a enviar
     * @return la respuesta del broker (o del servicio a traves de el)
     * @throws Exception si falla la red o la respuesta no se puede leer
     */
    private static Respuesta enviar(int puertoBroker, Solicitud solicitud) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", puertoBroker), 5000);
            socket.setSoTimeout(15_000);

            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            salida.println(GsonFactory.DEFAULT.toJson(solicitud));
            String linea = entrada.readLine();
            if (linea == null || linea.isBlank()) {
                throw new IllegalStateException("El broker cerro la conexion sin responder");
            }
            return GsonFactory.DEFAULT.fromJson(linea, Respuesta.class);
        }
    }

    /**
     * Configuracion del broker para las pruebas: puerto efimero, pool suficiente
     * para los clientes simultaneos y timeouts holgados.
     */
    private static ConfiguracionBroker configuracionBroker(int tamanoPool) {
        return new ConfiguracionBroker(
                0,          // puerto efimero
                tamanoPool,
                2000,       // timeout de conexion al proveedor
                5000,       // timeout de lectura de la respuesta
                0,          // sin reintentos: las pruebas no dependen de reintentos
                0,          // sin espera entre intentos
                10,         // no marcar al proveedor como no disponible en estas pruebas
                30_000);
    }

    /**
     * Configuracion para la prueba de caida: timeout de lectura corto y sin
     * reintentos, para que el cliente reciba el `TIMEOUT` del primer intento.
     */
    private static ConfiguracionBroker configuracionBrokerCaida() {
        return new ConfiguracionBroker(
                0,
                4,
                2000,
                TIMEOUT_LECTURA_CAIDA_MS,
                0,          // sin reintentos: un reintento contra el servicio apagado daria ERROR_INTERNO
                0,
                10,
                30_000);
    }

    /**
     * Bloquea el hilo del manejador sin responder, ignorando las interrupciones.
     *
     * <p>Simula una operacion que ya entro al servicio y que sigue en curso
     * cuando el Sistema 1 se apaga. ServerServicio.detener() cierra el puerto de
     * escucha pero no el socket de una conexion bloqueada, asi que el broker solo
     * puede cortar por su timeout de lectura.</p>
     *
     * @param millis tiempo de bloqueo
     */
    private static void esperarSinResponder(long millis) {
        long fin = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < fin) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignorada) {
                // Se ignora a proposito: la operacion no responde aunque el
                // servidor este apagandose.
            }
        }
    }

    @Test
    void veinteClientesSimultaneosATravesDelBrokerTerminanSinErrores() throws Exception {
        AtomicInteger atendidas = new AtomicInteger();
        DespachadorSolicitudes despachador = new DespachadorSolicitudes();
        despachador.registrarOperacion(Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO,
                parametros -> {
                    atendidas.incrementAndGet();
                    return new JsonPrimitive("ok");
                });

        ServidorServicio servicio = new ServidorServicio(0, CLIENTES_SIMULTANEOS, despachador);
        servicio.iniciar();

        BrokerServer broker = new BrokerServer(configuracionBroker(CLIENTES_SIMULTANEOS));
        broker.iniciar();
        broker.getRegistro().registrar(new Proveedor(
                Servicios.INVENTARIO, "localhost", servicio.getPuerto()));

        ExecutorService ejecutor = Executors.newFixedThreadPool(CLIENTES_SIMULTANEOS);
        CountDownLatch salida = new CountDownLatch(1);

        try {
            List<Future<Respuesta>> futuros = new ArrayList<>();
            for (int i = 0; i < CLIENTES_SIMULTANEOS; i++) {
                String id = "req-concurrente-" + i;
                futuros.add(ejecutor.submit(() -> {
                    salida.await();
                    return enviar(broker.getPuerto(), Solicitud.crear(
                            id, Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO, null));
                }));
            }

            // Todos los clientes esperan aqui para arrancar a la vez.
            salida.countDown();

            for (int i = 0; i < CLIENTES_SIMULTANEOS; i++) {
                Respuesta respuesta = futuros.get(i).get(ESPERA_RESPUESTA_SEGUNDOS, TimeUnit.SECONDS);
                assertEquals("req-concurrente-" + i, respuesta.getIdSolicitud(),
                        "la respuesta se mezclo con la de otro cliente");
                assertEquals(EstadoRespuesta.EXITO, respuesta.getEstado(),
                        "cliente " + i + " recibio " + respuesta.getCodigoError()
                                + ": " + respuesta.getMensajeError());
            }

            assertEquals(CLIENTES_SIMULTANEOS, atendidas.get(),
                    "el servicio no atendio todas las solicitudes");
        } finally {
            ejecutor.shutdownNow();
            broker.detener();
            servicio.detener();
        }
    }

    @Test
    void apagarElSistemaAUmitadDeUnaSolicitudDevuelveTimeout() throws Exception {
        CountDownLatch recibida = new CountDownLatch(1);
        DespachadorSolicitudes despachador = new DespachadorSolicitudes();
        despachador.registrarOperacion(Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO,
                parametros -> {
                    recibida.countDown();
                    esperarSinResponder(BLOQUEO_MANEJADOR_CAIDA_MS);
                    return new JsonPrimitive("tarde");
                });

        ServidorServicio servicio = new ServidorServicio(0, 2, despachador);
        servicio.iniciar();

        BrokerServer broker = new BrokerServer(configuracionBrokerCaida());
        broker.iniciar();
        broker.getRegistro().registrar(new Proveedor(
                Servicios.INVENTARIO, "localhost", servicio.getPuerto()));

        ExecutorService ejecutor = Executors.newSingleThreadExecutor();
        Thread hiloDetencion = null;

        try {
            long inicio = System.nanoTime();
            Future<Respuesta> futuro = ejecutor.submit(() -> enviar(broker.getPuerto(),
                    Solicitud.crear("req-apagado", Servicios.INVENTARIO,
                            Operaciones.LISTAR_INVENTARIO, null)));

            assertTrue(recibida.await(5, TimeUnit.SECONDS),
                    "el servicio no llego a recibir la solicitud");

            // El Sistema 1 se apaga con la solicitud ya en curso.
            hiloDetencion = new Thread(servicio::detener, "detener-servicio");
            hiloDetencion.start();

            Respuesta respuesta = futuro.get(ESPERA_RESPUESTA_SEGUNDOS, TimeUnit.SECONDS);
            long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;
            System.out.println("Apagado a mitad de solicitud: " + respuesta.getCodigoError()
                    + " en " + transcurridoMs + " ms (timeout configurado "
                    + TIMEOUT_LECTURA_CAIDA_MS + " ms)");

            assertEquals("req-apagado", respuesta.getIdSolicitud());
            assertEquals(EstadoRespuesta.ERROR, respuesta.getEstado());
            assertEquals(CodigoError.TIMEOUT, respuesta.getCodigoError(),
                    "se esperaba TIMEOUT, pero llego " + respuesta.getCodigoError()
                            + ": " + respuesta.getMensajeError());
            assertTrue(transcurridoMs >= TIMEOUT_LECTURA_CAIDA_MS - 100,
                    "el broker respondio antes del timeout configurado: " + transcurridoMs + " ms");
            assertTrue(transcurridoMs < 4000,
                    "el broker tardo demasiado en cortar por timeout: " + transcurridoMs + " ms");
        } finally {
            ejecutor.shutdownNow();
            if (hiloDetencion != null) {
                hiloDetencion.join(10_000);
            }
            broker.detener();
            if (servicio.isActivo()) {
                servicio.detener();
            }
        }
    }
}
