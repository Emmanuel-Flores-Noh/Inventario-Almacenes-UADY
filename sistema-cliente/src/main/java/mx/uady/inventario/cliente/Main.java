package mx.uady.inventario.cliente;

import com.formdev.flatlaf.FlatIntelliJLaf;
import mx.uady.inventario.cliente.util.ConfiguracionNetwork;
import mx.uady.inventario.cliente.view.RegistroVentaFrame;

import javax.swing.*;

/**
 * Punto de entrada principal para el Sistema 2 (Cliente del Proveedor).
 * 
 * <p>Valida la configuración de red.</p>
 */
public class Main {

    public static void main(String[] args) {
        ConfiguracionNetwork configRed;
        try {
            configRed = ConfiguracionNetwork.desdeEntorno();
        } catch (RuntimeException e) {
            System.err.println("Configuración de red inválida en el Sistema Cliente: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("==================================================");
        System.out.println("  Iniciando Sistema Cliente del Proveedor");
        System.out.println("  Sistema de Trazabilidad e Inventarios UADY");
        System.out.println("  Broker Destino: " + configRed.brokerHost() + ":" + configRed.brokerPuerto());
        System.out.println("  Timeout configurado: " + configRed.timeoutMs() + " ms");
        System.out.println("==================================================");

        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            
            // Propiedades estéticas globales para componentes FlatLaf
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arrowType", "chevron");
            
        } catch (Exception ex) {
            System.err.println("Fallo al inicializar FlatLaf: " + ex.getMessage());
        }

        // Lanzar la ventana en el hilo de despacho de eventos de Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            RegistroVentaFrame ventana = new RegistroVentaFrame();
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setVisible(true);
        });
        // Bloquear el hilo principal para que el proceso no muera y la app permanezca viva
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}