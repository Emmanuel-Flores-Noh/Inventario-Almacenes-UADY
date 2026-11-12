package mx.uady.inventario.broker.routing;

import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.broker.registry.RegistroServicios;
import mx.uady.inventario.broker.util.ConfiguracionBroker;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.json.GsonFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Enruta una solicitud al proveedor registrado y devuelve la respuesta.
 *
 * <p>La resiliencia vive aqui: el timeout de conexion y el de lectura se
 * configuran por separado, los fallos de red se reintentan de forma acotada y
 * cada fallo se reporta al {@link RegistroServicios} para que un proveedor que
 * falla de forma repetida quede marcado como no disponible.</p>
 *
 * <p>Una respuesta de error valida del proveedor no es un fallo de red: no se
 * reintenta y ademas cuenta como comunicacion exitosa.</p>
 */
public class Enrutador {
    private final int timeoutConexionMillis;
    private final int timeoutLecturaMillis;
    private final int reintentos;
    private final long esperaReintentoMillis;

    /**
     * Constructor compatible con el comportamiento anterior: el mismo timeout
     * para conexion y lectura, sin reintentos.
     *
     * @param timeoutMillis El tiempo en milisegundos para marcar una peticion como timeout
     */
    public Enrutador(int timeoutMillis) {
        this(timeoutMillis, timeoutMillis, 0, 0);
    }

    /**
     * Constructor con la configuracion de resiliencia del broker.
     *
     * @param configuracion timeouts, reintentos y espera entre intentos
     */
    public Enrutador(ConfiguracionBroker configuracion) {
        this(configuracion.timeoutConexionMillis(),
                configuracion.timeoutLecturaMillis(),
                configuracion.reintentos(),
                configuracion.esperaReintentoMillis());
    }

    /**
     * Constructor con todos los parametros de red.
     *
     * @param timeoutConexionMillis timeout de conexion al proveedor
     * @param timeoutLecturaMillis timeout de lectura de la respuesta
     * @param reintentos reintentos adicionales tras el primer intento
     * @param esperaReintentoMillis espera entre intentos
     */
    public Enrutador(int timeoutConexionMillis, int timeoutLecturaMillis, int reintentos, long esperaReintentoMillis) {
        if (timeoutConexionMillis <= 0) {
            throw new IllegalArgumentException("timeoutConexionMillis debe ser positivo");
        }
        if (timeoutLecturaMillis <= 0) {
            throw new IllegalArgumentException("timeoutLecturaMillis debe ser positivo");
        }
        if (reintentos < 0) {
            throw new IllegalArgumentException("reintentos no puede ser negativo");
        }
        if (esperaReintentoMillis < 0) {
            throw new IllegalArgumentException("esperaReintentoMillis no puede ser negativo");
        }
        this.timeoutConexionMillis = timeoutConexionMillis;
        this.timeoutLecturaMillis = timeoutLecturaMillis;
        this.reintentos = reintentos;
        this.esperaReintentoMillis = esperaReintentoMillis;
    }

    /**
     * Valida si una solicitud es valida
     * @param solicitud La solicitud a evaluar
     * @return {@code true} si la solicitud es valida
     */
    private boolean validarSolicitud(Solicitud solicitud) {
        return solicitud != null
                && solicitud.getIdSolicitud() != null && !solicitud.getIdSolicitud().isBlank()
                && solicitud.getNombreServicio() != null && !solicitud.getNombreServicio().isBlank()
                && solicitud.getNombreOperacion() != null && !solicitud.getNombreOperacion().isBlank();
    }

    /**
     * Enruta la solicitud al proveedor del servicio, con reintentos acotados.
     *
     * @param solicitud solicitud a enviar
     * @param registro registro de proveedores y su salud
     * @throws IllegalArgumentException si la solicitud no tiene id, servicio u operacion
     * @return la respuesta del servicio o una respuesta de error del broker
     */
    public Respuesta enrutar(Solicitud solicitud, RegistroServicios registro) throws IllegalArgumentException {
        if (!validarSolicitud(solicitud)) {
            throw new IllegalArgumentException("La solicitud no tiene id, servicio u operacion");
        }
        Objects.requireNonNull(registro, "El registro es obligatorio");

        Optional<Proveedor> proveedorOpt = registro.buscar(solicitud.getNombreServicio());
        if (proveedorOpt.isEmpty()) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.SERVICIO_DESCONOCIDO,
                    "Servicio desconocido: " + solicitud.getNombreServicio());
        }
        if (!registro.estaDisponible(solicitud.getNombreServicio())) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PROVEEDOR_NO_REGISTRADO,
                    "Proveedor no disponible: " + solicitud.getNombreServicio());
        }

        Proveedor proveedor = proveedorOpt.get();
        int intentosTotales = reintentos + 1;
        CodigoError ultimoCodigo = CodigoError.ERROR_INTERNO;
        String ultimoMensaje = "Error de comunicacion con el proveedor";

        for (int intento = 1; intento <= intentosTotales; intento++) {
            try (Socket socket = new Socket()) {
                Respuesta respuesta = redireccionarSolicitud(solicitud, socket, proveedor);
                // La comunicacion funciono aunque el proveedor haya respondido con error.
                registro.marcarExito(solicitud.getNombreServicio());
                return respuesta;
            } catch (SocketTimeoutException e) {
                ultimoCodigo = CodigoError.TIMEOUT;
                ultimoMensaje = "Tiempo de espera agotado al contactar al proveedor";
            } catch (IOException | RuntimeException e) {
                ultimoCodigo = CodigoError.ERROR_INTERNO;
                ultimoMensaje = "Error de comunicacion con el proveedor: " + e.getMessage();
            }

            if (intento < intentosTotales && !esperarAntesDelSiguienteIntento()) {
                break;
            }
        }

        registro.marcarFallo(solicitud.getNombreServicio());
        return Respuesta.error(solicitud.getIdSolicitud(), ultimoCodigo, ultimoMensaje);
    }

    /**
     * Espera entre intentos. Devuelve {@code false} si el hilo fue interrumpido,
     * en cuyo caso se dejan de reintentar para no demorar el apagado.
     */
    private boolean esperarAntesDelSiguienteIntento() {
        if (esperaReintentoMillis <= 0) {
            return true;
        }
        try {
            Thread.sleep(esperaReintentoMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Redirecciona la solicitud a un proveedor proporcionado
     * @param solicitud La solicitud a redireccionar
     * @param socket El socket para la conexion al proveedor
     * @param proveedor El proveedor a cual el socket se va a conectar
     * @return La respuesta del servicio, sea exitosa o no
     * @throws IOException si no se pudo conectar, leer o la respuesta no es utilizable
     */
    private Respuesta redireccionarSolicitud(Solicitud solicitud, Socket socket, Proveedor proveedor) throws IOException {
        socket.connect(new InetSocketAddress(proveedor.host(), proveedor.puerto()), timeoutConexionMillis);
        socket.setSoTimeout(timeoutLecturaMillis);

        PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        salida.println(GsonFactory.DEFAULT.toJson(solicitud));
        salida.flush();

        String respuestaJson = entrada.readLine();
        if (respuestaJson == null || respuestaJson.isBlank()) {
            throw new IOException("El proveedor no devolvio respuesta");
        }

        Respuesta respuesta = GsonFactory.DEFAULT.fromJson(respuestaJson, Respuesta.class);
        if (respuesta == null) {
            throw new IOException("Respuesta invalida del proveedor");
        }

        if (!Objects.equals(respuesta.getIdSolicitud(), solicitud.getIdSolicitud())) {
            throw new IOException("El id de la respuesta no coincide con la solicitud");
        }

        return respuesta;
    }
}
