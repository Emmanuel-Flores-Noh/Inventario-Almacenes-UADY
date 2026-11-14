import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Evidencia del costo del Broker en rendimiento: mide la misma operación enviada N veces
 * directamente al proveedor (Sistema 1) y N veces a través del Broker, con una conexión
 * nueva por solicitud en ambos casos (igual que el cliente real).
 *
 * Uso: java EvidenciaRendimiento <repeticiones> <puertoProveedor> <puertoBroker>
 */
public class EvidenciaRendimiento {

    public static void main(String[] args) throws Exception {
        int n = Integer.parseInt(args[0]);
        int puertoProveedor = Integer.parseInt(args[1]);
        int puertoBroker = Integer.parseInt(args[2]);

        // Calentamiento: las primeras solicitudes incluyen la carga de clases de la JVM.
        medir(5, puertoProveedor, "calentamiento-directo");
        medir(5, puertoBroker, "calentamiento-broker");

        double[] directo = medir(n, puertoProveedor, "rend-directo");
        double[] broker = medir(n, puertoBroker, "rend-broker");

        System.out.println("Operacion: gestorTiendas.listarTiendas, " + n + " solicitudes secuenciales por camino,"
                + " una conexion TCP nueva por solicitud.");
        System.out.println();
        System.out.printf("%-34s %10s %10s %10s %10s%n", "Camino", "promedio", "mediana", "minimo", "maximo");
        imprimir("Directo al Sistema 1 (:" + puertoProveedor + ")", directo);
        imprimir("A traves del Broker (:" + puertoBroker + ")", broker);
        System.out.println();
        System.out.printf("Resumen: el Broker agrega en promedio %.2f ms por solicitud (%.1f%% mas que la llamada directa).%n",
                promedio(broker) - promedio(directo), (promedio(broker) / promedio(directo) - 1) * 100);
    }

    private static double[] medir(int n, int puerto, String prefijo) throws Exception {
        double[] tiempos = new double[n];
        for (int i = 0; i < n; i++) {
            Solicitud s = Solicitud.crear(prefijo + "-" + i, "gestorTiendas", "listarTiendas", null);
            long inicio = System.nanoTime();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", puerto), 3000);
                socket.setSoTimeout(15_000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out.println(GsonFactory.DEFAULT.toJson(s));
                Respuesta r = GsonFactory.DEFAULT.fromJson(in.readLine(), Respuesta.class);
                if (r.getEstado() != EstadoRespuesta.EXITO) {
                    throw new IllegalStateException("Respuesta de error en " + puerto + ": " + r.getMensajeError());
                }
            }
            tiempos[i] = (System.nanoTime() - inicio) / 1_000_000.0;
        }
        return tiempos;
    }

    private static void imprimir(String camino, double[] t) {
        double[] orden = t.clone();
        Arrays.sort(orden);
        System.out.printf("%-34s %8.2f ms %8.2f ms %8.2f ms %8.2f ms%n", camino, promedio(t),
                orden[orden.length / 2], orden[0], orden[orden.length - 1]);
    }

    private static double promedio(double[] t) {
        return Arrays.stream(t).average().orElse(0);
    }
}
