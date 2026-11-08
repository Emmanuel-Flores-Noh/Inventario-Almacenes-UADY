package mx.uady.inventario.central;

/**
 * Punto de entrada principal para el Sistema 1 (Sistema Centralizado / Proveedor).
 * Responsable del almacenamiento en MariaDB, generación de códigos QR (ZXing)
 * y exposición de servicios a través del Broker vía Sockets TCP.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Iniciando Sistema Centralizado / Proveedor");
        System.out.println("  Sistema de Trazabilidad e Inventarios UADY");
        System.out.println("==================================================");
    }
}
