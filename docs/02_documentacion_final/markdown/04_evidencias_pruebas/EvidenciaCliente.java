import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Cliente de evidencias: envía UNA solicitud al Broker por socket TCP y muestra
 * el JSON enviado y el recibido. Solo conoce el Broker (host y puerto), nunca al proveedor.
 *
 * Uso: java EvidenciaCliente <idSolicitud> <servicio> <operacion> <parametrosJson>
 * El destino es localhost:5000; se puede cambiar con -Dhost=... y -Dpuerto=...
 * El tiempo de espera de la respuesta es 15000 ms; se puede cambiar con -DtimeoutMs=...
 */
public class EvidenciaCliente {

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("host", "localhost");
        int puerto = Integer.getInteger("puerto", 5000);
        String id = args[0];
        JsonObject parametros = JsonParser.parseString(args[3]).getAsJsonObject();
        Solicitud solicitud = Solicitud.crear(id, args[1], args[2], parametros);

        String json = GsonFactory.DEFAULT.toJson(solicitud);               // marshalling
        System.out.println("CLIENTE -> BROKER (" + host + ":" + puerto + ")");
        System.out.println(bonito(json));

        long inicio = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 3000);
            socket.setSoTimeout(Integer.getInteger("timeoutMs", 15_000));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida.println(json);
            String recibido = entrada.readLine();
            long ms = (System.nanoTime() - inicio) / 1_000_000;

            Respuesta respuesta = GsonFactory.DEFAULT.fromJson(recibido, Respuesta.class); // unmarshalling
            System.out.println("BROKER -> CLIENTE (" + ms + " ms)");
            System.out.println(bonito(recibido));
            System.out.println("Resumen: estado=" + respuesta.getEstado()
                    + (respuesta.getCodigoError() == null ? ""
                            : ", codigo=" + respuesta.getCodigoError() + ", mensaje=\"" + respuesta.getMensajeError() + "\""));
        } catch (IOException e) {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            System.out.println("SIN RESPUESTA (" + ms + " ms): " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("Resumen: el cliente no pudo comunicarse con el Broker");
        }
    }

    private static String bonito(String json) {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(JsonParser.parseString(json));
    }
}
