package mx.uady.inventario.broker;

import mx.uady.inventario.broker.network.BrokerServer;
import mx.uady.inventario.broker.util.ConfiguracionBroker;

/**
 * Punto de entrada para el broker
 */
public class Main {

    public static void main(String[] args) {
        ConfiguracionBroker configuracion = ConfiguracionBroker.desdeEntorno();

        BrokerServer broker = new BrokerServer(configuracion);
        // Agregamos un hook para cuando el broker se detenga lo haga de manera segura
        Runtime.getRuntime().addShutdownHook(new Thread(broker::detener, "broker-shutdown"));

        broker.iniciar();
        System.out.println("Broker iniciado en el puerto " + configuracion.puerto()
                + " con pool de " + configuracion.tamanoPool() + " hilos"
                + " (timeout conexion " + configuracion.timeoutConexionMillis() + " ms"
                + ", timeout lectura " + configuracion.timeoutLecturaMillis() + " ms"
                + ", reintentos " + configuracion.reintentos()
                + ", fallos para no disponible " + configuracion.fallosParaIndisponible()
                + ", enfriamiento " + configuracion.tiempoIndisponibleMillis() + " ms)");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            broker.detener();
            Thread.currentThread().interrupt();
        }
    }
}
