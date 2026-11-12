package mx.uady.inventario.broker;

import mx.uady.inventario.broker.network.BrokerServer;

/**
 * Punto de entrada para el broker
 */
public class Main {
    /**
     * Metodo para ayudar a obtener un numero a partir de una cadena
     * @param valor La cadena en donde se intetara extraer el numero
     * @param defecto Un valor por defecto por si la cadena falla
     * @return El numero dentro de la cadena o el valor por defecto si falla
     */
    private static int parseEntero(String valor, int defecto) {
        if (valor == null || valor.isBlank()) {
            return defecto;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    public static void main(String[] args) {
        int puerto = parseEntero(System.getenv("BROKER_PORT"), 5000);
        int tamanoPool = parseEntero(System.getenv("BROKER_POOL_SIZE"), 16);

        BrokerServer broker = new BrokerServer(puerto, tamanoPool);
        // Agregamos un hook para cuando el broker se detenga lo haga de manera segura
        Runtime.getRuntime().addShutdownHook(new Thread(broker::detener, "broker-shutdown"));

        broker.iniciar();
        System.out.println("Broker iniciado en el puerto " + puerto + " con pool de " + tamanoPool + " hilos");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            broker.detener();
            Thread.currentThread().interrupt();
        }
    }
}
