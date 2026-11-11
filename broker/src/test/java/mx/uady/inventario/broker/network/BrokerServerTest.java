package mx.uady.inventario.broker.network;

import mx.uady.inventario.broker.registry.RegistroServicios;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerServerTest {

    @Test
    void iniciaYRespondeJsonMalformado() throws Exception {
        BrokerServer broker = new BrokerServer(0, 4);
        broker.iniciar();
        int puerto = broker.getPuerto();

        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("{esto no es json");
            String respuesta = in.readLine();
            Respuesta r = GsonFactory.DEFAULT.fromJson(respuesta, Respuesta.class);
            assertEquals(mx.uady.inventario.common.error.CodigoError.PARAMETROS_INVALIDOS, r.getCodigoError());
        } finally {
            broker.detener();
        }
    }

    @Test
    void registraProveedorYEnruta() throws Exception {
        // proveedor falso
        java.net.ServerSocket provider = new java.net.ServerSocket(0);
        int providerPort = provider.getLocalPort();
        Thread hilo = new Thread(() -> {
            try {
                java.net.Socket s = provider.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                String linea = in.readLine();
                Solicitud sol = GsonFactory.DEFAULT.fromJson(linea, Solicitud.class);
                Respuesta resp = Respuesta.exito(sol.getIdSolicitud(), new com.google.gson.JsonPrimitive("OK"));
                out.println(GsonFactory.DEFAULT.toJson(resp));
                s.close();
            } catch (Exception ignored) {
            }
        });
        hilo.start();

        BrokerServer broker = new BrokerServer(0, 4);
        broker.iniciar();
        int puerto = broker.getPuerto();

        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // registro
            Solicitud registro = Solicitud.crear("reg-1", Servicios.BROKER,
                    mx.uady.inventario.common.servicio.Operaciones.REGISTRAR_PROVEEDOR,
                    new com.google.gson.JsonObject());
            registro.getParametros().addProperty("servicio", Servicios.INVENTARIO);
            registro.getParametros().addProperty("host", "localhost");
            registro.getParametros().addProperty("puerto", providerPort);
            out.println(GsonFactory.DEFAULT.toJson(registro));
            String regResp = in.readLine();
            Respuesta r1 = GsonFactory.DEFAULT.fromJson(regResp, Respuesta.class);
            assertEquals(mx.uady.inventario.common.contrato.EstadoRespuesta.EXITO, r1.getEstado());

            // solicitud
            Solicitud solicitud = Solicitud.crear("req-1", Servicios.INVENTARIO, "listarInventario",
                    new com.google.gson.JsonObject());
            out.println(GsonFactory.DEFAULT.toJson(solicitud));
            String respLine = in.readLine();
            Respuesta r2 = GsonFactory.DEFAULT.fromJson(respLine, Respuesta.class);
            assertEquals("req-1", r2.getIdSolicitud());
            assertEquals(mx.uady.inventario.common.contrato.EstadoRespuesta.EXITO, r2.getEstado());
        } finally {
            broker.detener();
            provider.close();
            hilo.join();
        }
    }

    @Test
    void servicioDesconocidoNoTumbaServidor() throws Exception {
        BrokerServer broker = new BrokerServer(0, 4);
        broker.iniciar();
        int puerto = broker.getPuerto();

        try (Socket socket = new Socket("localhost", puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            Solicitud solicitud = Solicitud.crear("req-2", Servicios.INVENTARIO, "listarInventario",
                    new com.google.gson.JsonObject());
            out.println(GsonFactory.DEFAULT.toJson(solicitud));
            String resp = in.readLine();
            Respuesta r = GsonFactory.DEFAULT.fromJson(resp, Respuesta.class);
            assertEquals(mx.uady.inventario.common.error.CodigoError.SERVICIO_DESCONOCIDO, r.getCodigoError());
        } finally {
            broker.detener();
        }
    }
}
