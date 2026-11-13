package mx.uady.inventario.cliente.view.components;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Diálogo modal independiente diseñado para previsualizar el código QR y los datos básicos 
 * de identificación de una unidad de mercancía seleccionada.
 */
public class QrPreviewDialog extends JDialog {

    /**
     * Constructor que inicializa el diálogo modal de previsualización del QR.
     * 
     * @param parent      Ventana contenedora propietaria de este diálogo.
     * @param numeroSerie Número de serie único de la unidad de mercancía.
     * @param idProducto  Identificador numérico del producto asociado.
     * @param rutaQr      Ruta absoluta o relativa del archivo de imagen PNG del código QR.
     */
    public QrPreviewDialog(Window parent, String numeroSerie, int idProducto, String rutaQr) {
        super(parent, "Código QR - " + numeroSerie, ModalityType.APPLICATION_MODAL);
        configurarVentana();
        inicializarComponentes(numeroSerie, idProducto, rutaQr);
    }

    /**
     * Configura los parámetros dimensionales, de centrado, de operación de cierre y redimensionamiento del diálogo modal.
     */
    private void configurarVentana() {
        setSize(400, 480);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    /**
     * Inicializa, organiza y aplica diseño moderno con FlatLaf a los componentes internos del diálogo,
     * incluyendo el panel de metadatos, el contenedor de la imagen QR y el botón de cierre.
     * 
     * @param numeroSerie Número de serie de la unidad a mostrar en la cabecera.
     * @param idProducto  ID del producto asociado.
     * @param rutaQr      Ruta del archivo gráfico del código QR a cargar y reescalar.
     */
    private void inicializarComponentes(String numeroSerie, int idProducto, String rutaQr) {
        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de descripción superior
        JPanel panelInfo = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel lblSerie = new JLabel("N° Serie: " + numeroSerie);
        lblSerie.setFont(lblSerie.getFont().deriveFont(Font.BOLD, 14f));
        JLabel lblProducto = new JLabel("ID Producto: " + idProducto);
        
        panelInfo.add(lblSerie);
        panelInfo.add(lblProducto);
        panelPrincipal.add(panelInfo, BorderLayout.NORTH);

        // Panel de la imagen QR (Centro)
        JPanel panelImagen = new JPanel(new GridBagLayout());
        panelImagen.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFFFFF;");
        panelImagen.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        JLabel lblImagenQr = new JLabel();
        lblImagenQr.setHorizontalAlignment(SwingConstants.CENTER);

        // Intentar cargar la imagen si existe la ruta localmente
        if (rutaQr != null && !rutaQr.isBlank()) {
            File archivoQr = new File(rutaQr);
            if (archivoQr.exists()) {
                ImageIcon iconoOriginal = new ImageIcon(archivoQr.getAbsolutePath());
                Image imagenEscalada = iconoOriginal.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
                lblImagenQr.setIcon(new ImageIcon(imagenEscalada));
            } else {
                lblImagenQr.setText("<html><center>Imagen QR no disponible localmente<br><small>(" + rutaQr + ")</small></center></html>");
            }
        } else {
            lblImagenQr.setText("Sin ruta de QR registrada");
        }

        panelImagen.add(lblImagenQr);
        panelPrincipal.add(panelImagen, BorderLayout.CENTER);

        // Botón de cierre inferior
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.add(btnCerrar);
        panelPrincipal.add(panelSur, BorderLayout.SOUTH);

        add(panelPrincipal);
    }
}