package mx.uady.inventario.broker.routing;

import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.broker.registry.RegistroServicios;
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

/**
 * Enruta una solicitud al proveedor registrado y devuelve la respuesta.
 */
public class Enrutador {
    private final int timeoutMillis;

    /**
     * Constructor de un enrutador
     * @param timeoutMillis El tiempo en milisegundos para marcar una peticion como timeout
     */
    public Enrutador(int timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis debe ser positivo");
        }
        this.timeoutMillis = timeoutMillis;
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
     *
     * @param solicitud
     * @param registro
     * @throws IllegalArgumentException Si la solicitud tiene una id null
     * @return La respuesta del servicio
     */
    public Respuesta enrutar(Solicitud solicitud, RegistroServicios registro) throws IllegalArgumentException {
        if( !validarSolicitud(solicitud) ) {
            throw new IllegalArgumentException("La solicitud tiene una id null");
        }

        Proveedor proveedor;
        {
            java.util.Optional<Proveedor> proveedorOpt = registro.buscar(solicitud.getNombreServicio());
            if (proveedorOpt.isEmpty()) {
                return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.SERVICIO_DESCONOCIDO, "Servicio desconocido: " + solicitud.getNombreServicio());
            }
            proveedor = proveedorOpt.get();
        }


        try (Socket socket = new Socket()) {
            return redireccionarSolicitud(solicitud, socket, proveedor);
        } catch (SocketTimeoutException e) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.TIMEOUT, "Tiempo de espera agotado al contactar al proveedor");
        } catch (IOException e) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.ERROR_INTERNO, "Error de comunicacion con el proveedor: " + e.getMessage());
        } catch (RuntimeException e) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.ERROR_INTERNO, "Error al procesar la respuesta del proveedor: " + e.getMessage());
        }
    }

    /**
     * Redirecciona la solicitud a un proveedor proporcionado
     * @param solicitud La solicitud a redireccionar
     * @param socket El socket para la conexion al proveedor
     * @param proveedor El proveedor a cual el socket se va a conectar
     * @return La respuesta del servicio, sea exitosa o no
     * @throws IOException
     */
    private Respuesta redireccionarSolicitud(Solicitud solicitud, Socket socket, Proveedor proveedor) throws IOException {
        socket.connect(new InetSocketAddress(proveedor.host(), proveedor.puerto()), timeoutMillis);
        socket.setSoTimeout(timeoutMillis);

        PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        salida.println(GsonFactory.DEFAULT.toJson(solicitud));
        salida.flush();

        String respuestaJson = entrada.readLine();
        if (respuestaJson == null || respuestaJson.isBlank()) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.ERROR_INTERNO, "El proveedor no devolvio respuesta");
        }

        Respuesta respuesta = GsonFactory.DEFAULT.fromJson(respuestaJson, Respuesta.class);
        if (respuesta == null) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.ERROR_INTERNO, "Respuesta invalida del proveedor");
        }

        if (!Objects.equals(respuesta.getIdSolicitud(), solicitud.getIdSolicitud())) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.ERROR_INTERNO,"El id de la respuesta no coincide con la solicitud");
        }

        return respuesta;
    }
}
