package mx.uady.inventario.cliente.view.components;

import com.formdev.flatlaf.FlatClientProperties;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Diálogo modal independiente diseñado para previsualizar el código QR y los datos básicos
 * de identificación de una unidad de mercancía seleccionada.
 *
 * <p>La imagen llega como bytes PNG desde el Sistema 1 (operación {@code obtenerQr} a través
 * del Broker), por lo que no depende de archivos locales. Permite guardarla como PNG.</p>
 */
public class QrPreviewDialog extends JDialog {

    private static final int TAMANO_IMAGEN = 250;

    private final byte[] png;
    private final String numeroSerie;

    /**
     * Constructor que inicializa el diálogo modal de previsualización del QR.
     *
     * @param parent         Ventana contenedora propietaria de este diálogo.
     * @param numeroSerie    Número de serie único de la unidad de mercancía.
     * @param idProducto     Identificador numérico del producto asociado.
     * @param nombreProducto Nombre del producto asociado; puede ser nulo.
     * @param png            Bytes de la imagen PNG del código QR.
     */
    public QrPreviewDialog(Window parent, String numeroSerie, int idProducto, String nombreProducto, byte[] png) {
        super(parent, "Código QR - " + numeroSerie, ModalityType.APPLICATION_MODAL);
        this.png = png;
        this.numeroSerie = numeroSerie;
        configurarVentana();
        inicializarComponentes(idProducto, nombreProducto);
    }

    /**
     * Configura los parámetros dimensionales, de centrado, de operación de cierre y redimensionamiento del diálogo modal.
     */
    private void configurarVentana() {
        setSize(400, 500);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    /**
     * Inicializa, organiza y aplica diseño moderno con FlatLaf a los componentes internos del diálogo,
     * incluyendo el panel de metadatos, el contenedor de la imagen QR y los botones de guardar y cerrar.
     *
     * @param idProducto     ID del producto asociado.
     * @param nombreProducto Nombre del producto asociado; puede ser nulo.
     */
    private void inicializarComponentes(int idProducto, String nombreProducto) {
        JPanel panelPrincipal = new JPanel(new BorderLayout(15, 15));
        panelPrincipal.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de descripción superior
        JPanel panelInfo = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel lblSerie = new JLabel("N° Serie: " + numeroSerie);
        lblSerie.setFont(lblSerie.getFont().deriveFont(Font.BOLD, 14f));
        String textoProducto = nombreProducto != null && !nombreProducto.isBlank()
                ? nombreProducto + " (ID " + idProducto + ")"
                : "ID " + idProducto;
        JLabel lblProducto = new JLabel("Producto: " + textoProducto);

        panelInfo.add(lblSerie);
        panelInfo.add(lblProducto);
        panelPrincipal.add(panelInfo, BorderLayout.NORTH);

        // Panel de la imagen QR (Centro)
        JPanel panelImagen = new JPanel(new GridBagLayout());
        panelImagen.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #FFFFFF;");
        panelImagen.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        JLabel lblImagenQr = new JLabel();
        lblImagenQr.setHorizontalAlignment(SwingConstants.CENTER);
        BufferedImage imagen = leerImagen(png);
        if (imagen != null) {
            lblImagenQr.setIcon(new ImageIcon(
                    imagen.getScaledInstance(TAMANO_IMAGEN, TAMANO_IMAGEN, Image.SCALE_SMOOTH)));
        } else {
            lblImagenQr.setText("No se pudo mostrar la imagen del código QR");
        }

        panelImagen.add(lblImagenQr);
        panelPrincipal.add(panelImagen, BorderLayout.CENTER);

        // Botones inferiores
        JButton btnGuardar = new JButton("Guardar PNG como...");
        btnGuardar.setEnabled(imagen != null);
        btnGuardar.addActionListener(e -> guardarComoPng());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.add(btnGuardar);
        panelSur.add(btnCerrar);
        panelPrincipal.add(panelSur, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    /**
     * Decodifica los bytes PNG en una imagen.
     *
     * @param bytes contenido del archivo PNG; puede ser nulo
     * @return la imagen, o {@code null} si no hay bytes o no son una imagen válida
     */
    private static BufferedImage leerImagen(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Abre un selector de archivos y guarda el PNG del código QR en la ruta elegida.
     * Agrega la extensión {@code .png} si el usuario no la escribió.
     */
    private void guardarComoPng() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar código QR");
        selector.setFileFilter(new FileNameExtensionFilter("Imagen PNG", "png"));
        selector.setSelectedFile(new File(numeroSerie + ".png"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }
        try {
            Files.write(destino.toPath(), png);
            JOptionPane.showMessageDialog(this, "Código QR guardado en:\n" + destino.getAbsolutePath(),
                    "QR guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + e.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
