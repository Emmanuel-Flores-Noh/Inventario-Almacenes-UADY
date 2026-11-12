package mx.uady.inventario.broker.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.broker.registry.RegistroServicios;
import mx.uady.inventario.broker.routing.Enrutador;
import mx.uady.inventario.broker.util.JsonBroker;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende una conexion TCP del broker.
 * Implementa Runnable para que pueda ser ejecutada por un thread
 */
public class ManejadorConexion implements Runnable {

    private final Socket socket;
    private final RegistroServicios registro;
    private final Enrutador enrutador;
    private final int timeoutMillis;

    /**
     * Crear un manejador de conexion para atender una peticion hecha por TCP
     * @param socket El socket de la peticion
     * @param registro El registro de servicios del broker
     * @param enrutador El enrutador (Encargado de reenviar la peticion al servicio correspondiente)
     * @param timeoutMillis El tiempo en el que la peticion se marca como timeout en milisegundos
     */
    public ManejadorConexion(Socket socket, RegistroServicios registro, Enrutador enrutador, int timeoutMillis) {
        this.socket = socket;
        this.registro = registro;
        this.enrutador = enrutador;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Logica que el thread debe de ejecutar para manejar la conexion
     */
    @Override
    public void run() {
        // Se separan las declaraciones del try-with-resources en múltiples líneas para evitar el desplazamiento horizontal
        try (
            socket;
            InputStreamReader isr = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(isr);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            // Configuración de resiliencia de red requerida por el nivel 2
            socket.setSoTimeout(timeoutMillis);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                // 1. Interpretar la solicitud entrante
                Respuesta respuesta = procesarSolicitud(linea);

                // 2. Marshalling: Convertir el objeto Respuesta a formato JSON
                String respuestaJson = JsonBroker.serializarRespuesta(respuesta);

                // 3. Enviar la respuesta al cliente y vaciar el buffer
                salida.println(respuestaJson);
                salida.flush();
            }

        } catch (IOException e) {
            // Manejo de desconexiones: se captura la excepción silenciosamente
            // para no tumbar la atención concurrente del Broker.
        }

    }

    private Respuesta procesarOperacionInterna(Solicitud solicitud) {
        if( solicitud.getNombreOperacion().equals(Operaciones.REGISTRAR_PROVEEDOR) ) {
            return registrarDesdeSolicitud(solicitud);
        }

        if( solicitud.getNombreOperacion().equals(Operaciones.ACTUALIZAR_PROVEEDOR) ) {
            return modificarDesdeSolicitud(solicitud);
        }

        if( solicitud.getNombreOperacion().equals(Operaciones.REMOVER_PROVEEDOR) ) {
            return removerDesdeSolicitud(solicitud);
        }

        return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.OPERACION_NO_SOPORTADA, "Operacion no encontrada en el broker");
    }

    /**
     * Procesa una solicitud entrante y enruta si los parametros son correctos
     * @param linea La cadena que contenga un Json valido de {@code solicitud}
     * @return La respuesta que produjo el servicio
     */
    private Respuesta procesarSolicitud(String linea) {
        try {
            Solicitud solicitud = GsonFactory.DEFAULT.fromJson(linea, Solicitud.class);
            if ( solicitud == null ) {
                return Respuesta.error(null, CodigoError.PARAMETROS_INVALIDOS, "JSON malformado");
            }

            // Hacer una operacion del broker
            if ( Servicios.BROKER.equals(solicitud.getNombreServicio()) ) {
                return procesarOperacionInterna(solicitud);
            }

            return enrutador.enrutar(solicitud, registro);
        } catch (JsonSyntaxException | IllegalStateException | NullPointerException e) {
            return Respuesta.error(null, CodigoError.PARAMETROS_INVALIDOS, "JSON malformado");
        } catch (RuntimeException e) {
            return Respuesta.error(null, CodigoError.ERROR_INTERNO, "Error interno del broker: " + e.getMessage());
        }
    }

    /**
     * Registra un servicio al broker desde una solicitud
     * @param solicitud La solicitud
     * @return La respuesta del propio broker
     */
    private Respuesta registrarDesdeSolicitud(Solicitud solicitud) {
        JsonElement params = solicitud.getParametros();

        // Si no tiene parametros o no es un JSON
        if (params == null || !params.isJsonObject()) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PARAMETROS_INVALIDOS, "Parametros invalidos para registro");
        }

        try {
            Proveedor proveedor = GsonFactory.DEFAULT.fromJson(params, Proveedor.class);
            if( proveedor == null ) {
                throw new RuntimeException("El json es null o es vacio");
            }
            registro.registrar(proveedor);
            return Respuesta.exito(solicitud.getIdSolicitud(), new JsonPrimitive("Proveedor registrado"));
        } catch (JsonSyntaxException e) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PARAMETROS_INVALIDOS, "Parametros invalidos para registro: " + e.getMessage());
        }
    }

    private Respuesta modificarDesdeSolicitud(Solicitud solicitud) {
        JsonElement params = solicitud.getParametros();

        // Si no tiene parametros o no es un JSON
        if (params == null || !params.isJsonObject()) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PARAMETROS_INVALIDOS, "Parametros invalidos para registro");
        }

        try {
            Proveedor proveedor = GsonFactory.DEFAULT.fromJson(params, Proveedor.class);
            if( proveedor == null ) {
                throw new RuntimeException("El json es null o es vacio");
            }
            registro.sustituir(proveedor);
            return Respuesta.exito(solicitud.getIdSolicitud(), new JsonPrimitive("Proveedor modificado"));
        } catch (JsonSyntaxException e) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PARAMETROS_INVALIDOS, "Parametros invalidos para registro: " + e.getMessage());
        }
    }


    private Respuesta removerDesdeSolicitud(Solicitud solicitud) {
        JsonElement params = solicitud.getParametros();

        // Si no tiene parametros o no es un JSON
        if (params == null || !params.isJsonObject()) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PARAMETROS_INVALIDOS, "Parametros invalidos para registro");
        }

        try {
            Proveedor proveedor = GsonFactory.DEFAULT.fromJson(params, Proveedor.class);
            if( proveedor == null ) {
                throw new RuntimeException("El json es null o es vacio");
            }
            registro.eliminar(proveedor.servicio());
            return Respuesta.exito(solicitud.getIdSolicitud(), new JsonPrimitive("Proveedor eliminado"));
        } catch (JsonSyntaxException e) {
            return Respuesta.error(solicitud.getIdSolicitud(), CodigoError.PARAMETROS_INVALIDOS, "Parametros invalidos para registro: " + e.getMessage());
        }
    }
}
