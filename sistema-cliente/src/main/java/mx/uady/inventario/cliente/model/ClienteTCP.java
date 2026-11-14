package mx.uady.inventario.cliente.model;

import com.google.gson.Gson;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Componente de red basado en Sockets TCP para el Sistema Cliente.
 * 
 * <p>Se encarga de establecer la conexión con el Broker intermediario, serializar las solicitudes
 * a formato JSON utilizando la instancia global {@link GsonFactory#DEFAULT}, enviarlas mediante UTF-8 
 * (un mensaje por línea) y recibir la respuesta validando su integridad mediante el cotejo de identificadores únicos.</p>
 *
 * <p>No reintenta automáticamente: si se pierde la respuesta de una venta ya procesada, reenviarla
 * podría intentar registrarla dos veces. Los reintentos acotados hacia el Sistema 1 los hace el Broker,
 * y ante un fallo de red el controlador ofrece al usuario reintentar de forma explícita.</p>
 */
public class ClienteTCP {
    private final String host;
    private final int puerto;
    private final int timeoutMs;
    private final Gson gson;

    /**
     * Constructor para inicializar los parámetros de conexión de red del cliente TCP.
     * 
     * @param host      Dirección IP o nombre de host donde escucha el Broker intermediario.
     * @param puerto    Puerto de red en el que el Broker atiende las solicitudes.
     * @param timeoutMs Tiempo límite (en milisegundos) configurado para la conexión y lectura por sockets.
     */
    public ClienteTCP(String host, int puerto, int timeoutMs) {
        this.host = host;
        this.puerto = puerto;
        this.timeoutMs = timeoutMs;
        this.gson = GsonFactory.DEFAULT;
    }

    /**
     * Envía una solicitud empaquetada al Broker a través de un socket TCP, serializándola a JSON
     * y esperando la respuesta correspondiente con validación de integridad.
     * 
     * @param solicitud El objeto {@link Solicitud} con los datos de la operación a ejecutar.
     * @return El objeto {@link Respuesta} deserializado devuelto por el servidor intermediario.
     * @throws Exception Si ocurre un fallo de red, timeout, cierre inesperado de conexión o discrepancia de IDs.
     */
    public Respuesta enviarSolicitud(Solicitud solicitud) throws Exception {
        String jsonSolicitud = gson.toJson(solicitud);

        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, puerto), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            ) {
                // Envía la solicitud empaquetada en UTF-8, un mensaje por línea
                out.println(jsonSolicitud);

                String jsonRespuesta = in.readLine();
                if (jsonRespuesta == null || jsonRespuesta.trim().isEmpty()) {
                    throw new java.io.IOException("El Broker cerró la conexión sin responder.");
                }

                Respuesta respuesta = gson.fromJson(jsonRespuesta, Respuesta.class);

                // Validación de integridad: el ID de la respuesta debe coincidir con el de la solicitud
                if (respuesta.getIdSolicitud() != null && solicitud.getIdSolicitud() != null 
                        && !respuesta.getIdSolicitud().equals(solicitud.getIdSolicitud())) {
                    throw new IllegalStateException("El ID de la respuesta no coincide con el de la solicitud.");
                }

                return respuesta;
            }
        }
    }
}