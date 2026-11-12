package mx.uady.inventario.central.network;

import com.google.gson.JsonSyntaxException;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.error.CodigoError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende una conexion TCP del servidor de servicio.
 *
 * <p>Lee lineas JSON, las despacha y responde por la misma conexion. Un JSON
 * invalido o una excepcion durante el despacho se convierten en una
 * {@link Respuesta} de error; solo se corta la conexion si falla la red.</p>
 */
public class ManejadorConexionServicio implements Runnable {

    private final Socket socket;
    private final DespachadorSolicitudes despachador;
    private final int timeoutMillis;

    /**
     * Crea el manejador de una conexion.
     *
     * @param socket       socket aceptado por el servidor
     * @param despachador  despachador de operaciones
     * @param timeoutMillis timeout de lectura en milisegundos
     */
    public ManejadorConexionServicio(Socket socket, DespachadorSolicitudes despachador, int timeoutMillis) {
        this.socket = socket;
        this.despachador = despachador;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void run() {
        try (
            socket;
            InputStreamReader isr = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(isr);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            socket.setSoTimeout(timeoutMillis);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                Respuesta respuesta = procesarSolicitud(linea);
                salida.println(JsonServicio.serializarRespuesta(respuesta));
                salida.flush();
            }
        } catch (IOException e) {
            // Desconexion del broker o timeout de lectura: se cierra esta conexion
            // sin afectar al resto de hilos del servidor.
        }
    }

    private Respuesta procesarSolicitud(String linea) {
        try {
            return despachador.despachar(JsonServicio.deserializarSolicitud(linea));
        } catch (JsonSyntaxException | IllegalArgumentException | IllegalStateException | NullPointerException e) {
            return Respuesta.error(null, CodigoError.PARAMETROS_INVALIDOS, "JSON malformado");
        } catch (RuntimeException e) {
            return Respuesta.error(null, CodigoError.ERROR_INTERNO,
                    "Error interno del servicio: " + e.getMessage());
        }
    }
}
