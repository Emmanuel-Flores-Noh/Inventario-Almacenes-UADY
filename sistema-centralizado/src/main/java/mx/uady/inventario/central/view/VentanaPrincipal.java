package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Ventana principal del Administrador del Sistema 1.
 * Agrupa en pestañas las pantallas de Productos, Mercancía y Ventas.
 */
public class VentanaPrincipal extends JFrame {

    private final JTabbedPane pestanas = new JTabbedPane();

    public VentanaPrincipal() {
        super("Inventario UADY - Administrador");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));

        // Las pantallas reales sustituyen a estos marcadores en fases posteriores.
        pestanas.addTab("Productos", marcador("Pantalla de productos (en construcción)"));
        pestanas.addTab("Mercancía", marcador("Pantalla de mercancía (en construcción)"));
        pestanas.addTab("Ventas", marcador("Pantalla de ventas (issue #4)"));

        getContentPane().add(pestanas, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    /** Sustituye el contenido de una pestaña existente por su pantalla real. */
    public void setPestana(String titulo, JComponent contenido) {
        int indice = pestanas.indexOfTab(titulo);
        if (indice < 0) {
            throw new IllegalArgumentException("No existe la pestaña: " + titulo);
        }
        pestanas.setComponentAt(indice, contenido);
    }

    private static JComponent marcador(String texto) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(texto, SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }
}
