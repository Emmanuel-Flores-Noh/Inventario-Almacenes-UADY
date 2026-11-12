package mx.uady.inventario.central.controller;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import mx.uady.inventario.central.view.ProductosPanel;
import mx.uady.inventario.central.view.VentanaPrincipal;

/**
 * Arranca la interfaz Swing del Administrador.
 * En entornos sin pantalla (p. ej. el contenedor Docker) no hace nada,
 * para que el servidor del Sistema 1 pueda arrancar igualmente.
 */
public final class UiLauncher {

    private UiLauncher() {
    }

    /**
     * Abre la ventana principal en el hilo de eventos de Swing.
     *
     * @return {@code true} si se abrió la interfaz, {@code false} si el entorno es headless.
     */
    public static boolean iniciar() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[UI] Entorno sin pantalla: interfaz gráfica deshabilitada.");
            return false;
        }
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            abrirVentana(AppContext.enMemoria());
        });
        return true;
    }

    /** Construye las pantallas, conecta sus controladores y muestra la ventana. Hilo de Swing. */
    static VentanaPrincipal abrirVentana(AppContext contexto) {
        VentanaPrincipal ventana = new VentanaPrincipal();

        ProductosPanel productos = new ProductosPanel();
        new ProductoController(contexto.productoService(), productos).cargar();
        ventana.setPestana("Productos", productos);

        ventana.setVisible(true);
        return ventana;
    }
}
