package mx.uady.inventario.central.network;

import com.google.gson.JsonObject;
import mx.uady.inventario.central.repository.VentaResultado;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VentaServicioTcpTest {

    @Test
    void enrutaVentaYConservaElIdDeSolicitud() throws Exception {
        VentaService servicio = new VentaService((serie, tienda, usuario) ->
                new VentaResultado(serie, tienda, LocalDateTime.parse("2026-11-12T00:00:00")));
        DespachadorSolicitudes despachador = DespachadorSolicitudes.estandar(servicio);
        ServidorServicio servidor = new ServidorServicio(0, 2, despachador);
        servidor.iniciar();

        try {
            JsonObject parametros = new JsonObject();
            parametros.addProperty("numero_serie", "SN-TCP-001");
            parametros.addProperty("id_tienda", 1);
            parametros.addProperty("id_usuario", 2);
            Solicitud solicitud = Solicitud.crear(
                    "req-venta-tcp",
                    Servicios.GESTOR_TIENDAS,
                    Operaciones.ACTUALIZAR_TIENDA_VENTA,
                    parametros
            );

            try (Socket socket = new Socket("localhost", servidor.getPuerto());
                 PrintWriter salida = new PrintWriter(
                         socket.getOutputStream(), true, StandardCharsets.UTF_8);
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(
                         socket.getInputStream(), StandardCharsets.UTF_8))) {
                salida.println(GsonFactory.DEFAULT.toJson(solicitud));
                Respuesta respuesta = GsonFactory.DEFAULT.fromJson(entrada.readLine(), Respuesta.class);

                assertEquals("req-venta-tcp", respuesta.getIdSolicitud());
                assertEquals(EstadoRespuesta.EXITO, respuesta.getEstado());
                assertEquals("SN-TCP-001", respuesta.getResultado().getAsJsonObject()
                        .get("numero_serie").getAsString());
            }
        } finally {
            servidor.detener();
        }
    }
}
