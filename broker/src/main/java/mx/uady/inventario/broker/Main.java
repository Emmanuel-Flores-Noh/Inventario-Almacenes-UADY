package mx.uady.inventario.broker;

/**
 * Punto de entrada principal para el Broker (Intermediario).
 * Responsable de mantener el registro dinámico de proveedores y servicios,
 * y enrutar las solicitudes de clientes concurrentemente mediante Sockets TCP y Threads.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Iniciando Broker Intermediario");
        System.out.println("  Sistema de Trazabilidad e Inventarios UADY");
        System.out.println("==================================================");
    }
}
