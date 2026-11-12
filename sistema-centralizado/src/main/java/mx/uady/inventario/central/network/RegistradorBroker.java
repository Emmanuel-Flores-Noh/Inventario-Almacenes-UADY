package mx.uady.inventario.central.network;

import com.google.gson.JsonPrimitive;
import mx.uady.inventario.central.util.ConfiguracionServicio;
import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra el servidor de servicio en el broker mediante {@code registrarProveedor}.
 *
 * <p>El Sistema 1 expone tres servicios ({@code gestorTiendas}, {@code inventario}
 * y {@code gestorPrecios}) sobre el mismo puerto, por lo que se envia un registro
 * por cada nombre. El broker puede arrancar despues que el proveedor, de ahi que
 * el registro se reintente un numero acotado de veces antes de rendirse.</p>
 */
public class RegistradorBroker {

    private static final int TIMEOUT_CONEXION_MILLIS = 5000;

    private final String hostBroker;
    private final int puertoBroker;
    private final String hostServicio;
    private final int puertoServicio;
    private final List<String> servicios;
    private final int maxIntentos;
    private final long esperaReintentoMillis;

    /**
     * Construye el registrador a partir de la configuracion del servicio.
     *
     * @param configuracion parametros de red
     * @param servicios     nombres de servicio a registrar
     */
    public RegistradorBroker(ConfiguracionServicio configuracion, List<String> servicios) {
        this(configuracion.hostBroker(),
                configuracion.puertoBroker(),
                configuracion.hostServicio(),
                configuracion.puertoServicio(),
                servicios,
                configuracion.intentosRegistro(),
                configuracion.esperaReintentoMillis());
    }

    /**
     * Constructor explicito, usado por las pruebas para no depender de variables de entorno.
     *
     * @param hostBroker            host del broker
     * @param puertoBroker          puerto del broker
     * @param hostServicio          host del proveedor anunciado
     * @param puertoServicio        puerto del proveedor
     * @param servicios             nombres de servicio a registrar
     * @param maxIntentos           intentos maximos
     * @param esperaReintentoMillis espera entre intentos
     */
    public RegistradorBroker(String hostBroker, int puertoBroker, String hostServicio, int puertoServicio,
                             List<String> servicios, int maxIntentos, long esperaReintentoMillis) {
        if (hostBroker == null || hostBroker.isBlank()) {
            throw new IllegalArgumentException("hostBroker es obligatorio");
        }
        if (hostServicio == null || hostServicio.isBlank()) {
            throw new IllegalArgumentException("hostServicio es obligatorio");
        }
        if (puertoBroker < 1 || puertoBroker > 65535 || puertoServicio < 1 || puertoServicio > 65535) {
            throw new IllegalArgumentException("Los puertos deben estar entre 1 y 65535");
        }
        if (servicios == null || servicios.isEmpty()) {
            throw new IllegalArgumentException("Debe registrarse al menos un servicio");
        }
        if (maxIntentos <= 0) {
            throw new IllegalArgumentException("maxIntentos debe ser positivo");
        }
        if (esperaReintentoMillis < 0) {
            throw new IllegalArgumentException("esperaReintentoMillis no puede ser negativa");
        }
        this.hostBroker = hostBroker;
        this.puertoBroker = puertoBroker;
        this.hostServicio = hostServicio;
        this.puertoServicio = puertoServicio;
        this.servicios = List.copyOf(servicios);
        this.maxIntentos = maxIntentos;
        this.esperaReintentoMillis = esperaReintentoMillis;
    }

    /**
     * Intenta registrar todos los servicios una sola vez.
     *
     * @return {@code true} si el broker confirmo todos los registros
     */
    public boolean registrarUnaVez() {
        for (String servicio : servicios) {
            if (!registrar(servicio)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reintenta el registro completo hasta agotar {@code maxIntentos}.
     *
     * @return {@code true} si algun intento registro todos los servicios
     */
    public boolean registrarConReintentos() {
        for (int intento = 1; intento <= maxIntentos; intento++) {
            if (registrarUnaVez()) {
                return true;
            }
            if (intento < maxIntentos && !esperar()) {
                return false;
            }
        }
        return false;
    }

    /**
     * Lanza el registro con reintentos en un hilo daemon para no bloquear el arranque
     * del servidor mientras el broker termina de levantar.
     *
     * @return el hilo de registro
     */
    public Thread iniciarEnSegundoPlano() {
        Thread hilo = new Thread(() -> {
            boolean registrado = registrarConReintentos();
            if (registrado) {
                System.out.println("Proveedor registrado en el broker " + hostBroker + ":" + puertoBroker
                        + " como " + hostServicio + ":" + puertoServicio + " (" + String.join(", ", servicios) + ")");
            } else {
                System.err.println("No se pudo registrar en el broker " + hostBroker + ":" + puertoBroker
                        + " tras " + maxIntentos + " intentos; el servicio seguira escuchando en el puerto "
                        + puertoServicio);
            }
        }, "registro-broker");
        hilo.setDaemon(true);
        hilo.start();
        return hilo;
    }

    private boolean registrar(String servicio) {
        Solicitud solicitud = crearSolicitudRegistro(servicio);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostBroker, puertoBroker), TIMEOUT_CONEXION_MILLIS);
            socket.setSoTimeout(TIMEOUT_CONEXION_MILLIS);

            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            salida.println(GsonFactory.DEFAULT.toJson(solicitud));
            salida.flush();

            String linea = entrada.readLine();
            if (linea == null || linea.isBlank()) {
                return false;
            }

            Respuesta respuesta = GsonFactory.DEFAULT.fromJson(linea, Respuesta.class);
            return respuesta != null
                    && respuesta.getEstado() == EstadoRespuesta.EXITO
                    && Objects.equals(respuesta.getIdSolicitud(), solicitud.getIdSolicitud());
        } catch (IOException | RuntimeException e) {
            // El broker puede no estar escuchando todavia: se reintenta desde el llamador.
            return false;
        }
    }

    private Solicitud crearSolicitudRegistro(String servicio) {
        com.google.gson.JsonObject parametros = new com.google.gson.JsonObject();
        parametros.addProperty("servicio", servicio);
        parametros.addProperty("host", hostServicio);
        parametros.addProperty("puerto", puertoServicio);
        return Solicitud.crear(UUID.randomUUID().toString(), Servicios.BROKER,
                Operaciones.REGISTRAR_PROVEEDOR, parametros);
    }

    private boolean esperar() {
        try {
            Thread.sleep(esperaReintentoMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Servicios que expone el Sistema 1 sobre su puerto.
     *
     * @return la lista de nombres de servicio del contrato
     */
    public static List<String> serviciosDelSistema() {
        return List.of(Servicios.GESTOR_TIENDAS, Servicios.INVENTARIO, Servicios.GESTOR_PRECIOS);
    }
}
