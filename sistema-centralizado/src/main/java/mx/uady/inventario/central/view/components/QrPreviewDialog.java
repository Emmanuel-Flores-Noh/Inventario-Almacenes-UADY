package mx.uady.inventario.central.view.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Diálogo modal reutilizable para visualizar y guardar un código QR.
 */
public class QrPreviewDialog extends JDialog {

    private static final int IMAGE_SIZE = 300;

    private final String rutaQr;

    /**
     * Crea un diálogo modal con la información contextual de la unidad.
     *
     * @param owner ventana propietaria
     * @param numeroSerie número de serie de la unidad
     * @param idProducto identificador del producto
     * @param nombreProducto nombre del producto
     * @param rutaQr ruta del PNG generado
     */
    public QrPreviewDialog(
            Window owner,
            String numeroSerie,
            int idProducto,
            String nombreProducto,
            String rutaQr
    ) {
        super(owner, "Código QR - " + numeroSerie, Dialog.ModalityType.APPLICATION_MODAL);
        if (rutaQr == null || rutaQr.isBlank()) {
            throw new IllegalArgumentException("La ruta del código QR es obligatoria");
        }
        this.rutaQr = rutaQr;
        construirVista(numeroSerie, idProducto, nombreProducto);
    }

    private void construirVista(String numeroSerie, int idProducto, String nombreProducto) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JLabel contexto = new JLabel(
                "<html><b>" + escaparHtml(nombreProducto) + "</b>"
                        + "<br>ID del producto: " + idProducto
                        + "<br>Número de serie: " + escaparHtml(numeroSerie) + "</html>",
                SwingConstants.CENTER
        );
        contenido.add(contexto, BorderLayout.NORTH);

        JLabel imagen = new JLabel(cargarImagen(), SwingConstants.CENTER);
        imagen.setPreferredSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
        contenido.add(new JScrollPane(imagen), BorderLayout.CENTER);

        JButton guardar = new JButton("Guardar PNG como...");
        guardar.addActionListener(evento -> guardarComo());

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(evento -> dispose());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(guardar);
        acciones.add(cerrar);
        contenido.add(acciones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setMinimumSize(new Dimension(380, 460));
        setLocationRelativeTo(getOwner());
    }

    private ImageIcon cargarImagen() {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(rutaQr));
            ImageIcon icono = new ImageIcon(bytes);
            return new ImageIcon(icono.getImage().getScaledInstance(
                    IMAGE_SIZE,
                    IMAGE_SIZE,
                    Image.SCALE_SMOOTH
            ));
        } catch (IOException error) {
            throw new IllegalArgumentException("No se pudo leer el código QR: " + rutaQr, error);
        }
    }

    private void guardarComo() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar código QR como PNG");
        selector.setSelectedFile(Path.of(rutaQr).toFile());

        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destino = selector.getSelectedFile().toPath();
        try {
            Files.copy(Path.of(rutaQr), destino, StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(
                    this,
                    "El código QR se guardó correctamente.",
                    "Código QR",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo guardar el código QR: " + error.getMessage(),
                    "Error al guardar",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String escaparHtml(String valor) {
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
