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

import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Evidencia de atención concurrente: N clientes envían al mismo tiempo una solicitud al Broker
 * (cada uno con su propio socket e id) y se comprueba que cada uno recibe SU respuesta.
 *
 * Uso: java EvidenciaConcurrencia <cantidad> <prefijoId>
 */
public class EvidenciaConcurrencia {

    public static void main(String[] args) throws Exception {
        int cantidad = Integer.parseInt(args[0]);
        String prefijo = args[1];
        String[] operaciones = {"gestorTiendas/listarTiendas", "inventario/listarInventario"};

        ExecutorService ejecutor = Executors.newFixedThreadPool(cantidad);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            String id = prefijo + "-" + String.format("%02d", i);
            String[] destino = operaciones[i % operaciones.length].split("/");
            futuros.add(ejecutor.submit(() -> {
                salida.await();
                long inicio = System.nanoTime();
                Solicitud solicitud = Solicitud.crear(id, destino[0], destino[1], null);
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", 5000), 3000);
                    socket.setSoTimeout(15_000);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    out.println(GsonFactory.DEFAULT.toJson(solicitud));
                    Respuesta r = GsonFactory.DEFAULT.fromJson(in.readLine(), Respuesta.class);
                    long ms = (System.nanoTime() - inicio) / 1_000_000;
                    String coincide = id.equals(r.getIdSolicitud()) ? "coincide" : "NO COINCIDE";
                    return String.format("%s  %-30s -> idRespuesta=%s (%s)  %s  %d ms",
                            id, destino[0] + "." + destino[1], r.getIdSolicitud(), coincide, r.getEstado(), ms);
                }
            }));
        }

        long inicio = System.nanoTime();
        salida.countDown();   // todos los clientes arrancan a la vez
        int correctas = 0;
        for (Future<String> f : futuros) {
            String linea = f.get();
            System.out.println(linea);
            if (linea.contains("(coincide)") && linea.contains("EXITO")) {
                correctas++;
            }
        }
        long total = (System.nanoTime() - inicio) / 1_000_000;
        ejecutor.shutdown();
        System.out.println();
        System.out.println("Resumen: " + correctas + "/" + cantidad
                + " clientes recibieron EXITO con el id de su propia solicitud; tiempo total " + total + " ms");
    }
}
