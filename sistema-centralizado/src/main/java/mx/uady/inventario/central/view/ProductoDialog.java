package mx.uady.inventario.central.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import mx.uady.inventario.central.service.ProductoValidator;
import mx.uady.inventario.central.service.ProductoValidator.Campo;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Formulario modal para crear o editar un producto.
 * Valida los datos antes de entregarlos al callback de guardado; no accede a servicios.
 */
public class ProductoDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** Miles con coma (1,250.50) o sin separador (1250.50). */
    private static final Pattern MILES_CON_COMA = Pattern.compile("\\d{1,3}(,\\d{3})+(\\.\\d+)?");
    static final String PRECIO_ILEGIBLE = "Escribe un precio válido, por ejemplo 1250.50.";

    private final int idProducto;
    private final JTextField nombre = new JTextField(30);
    private final JTextArea descripcion = new JTextArea(4, 30);
    private final JTextField precio = new JTextField(12);
    private final JLabel errorNombre = etiquetaError();
    private final JLabel errorPrecio = etiquetaError();
    private final JButton guardar = new JButton("Guardar");
    private final JButton cancelar = new JButton("Cancelar");

    private Consumer<Producto> alGuardar = producto -> { };

    /** @param inicial producto a editar, o {@code null} para crear uno nuevo */
    public ProductoDialog(Window propietario, Producto inicial) {
        super(propietario, inicial == null ? "Nuevo producto" : "Editar producto", ModalityType.APPLICATION_MODAL);
        this.idProducto = inicial == null ? 0 : inicial.idProducto();

        if (inicial != null) {
            nombre.setText(inicial.nombre());
            descripcion.setText(inicial.descripcion() == null ? "" : inicial.descripcion());
            precio.setText(inicial.precioActual().toPlainString());
        }
        descripcion.setLineWrap(true);
        descripcion.setWrapStyleWord(true);
        precio.putClientProperty("JTextField.placeholderText", "0.00");

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        contenido.add(construirCampos(), BorderLayout.CENTER);
        contenido.add(construirBotones(), BorderLayout.SOUTH);
        setContentPane(contenido);

        limpiarErrorAlEscribir(nombre, errorNombre);
        limpiarErrorAlEscribir(precio, errorPrecio);
        guardar.addActionListener(e -> intentarGuardar());
        cancelar.addActionListener(e -> cerrarSiSePuede());
        getRootPane().setDefaultButton(guardar);
        getRootPane().registerKeyboardAction(e -> cerrarSiSePuede(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarSiSePuede();
            }
        });

        pack();
        setResizable(false);
        setLocationRelativeTo(propietario);
    }

    private JPanel construirCampos() {
        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 2, 12);

        agregarFila(campos, c, 0, "Nombre *", nombre, errorNombre);
        JScrollPane desplazamiento = new JScrollPane(descripcion);
        agregarFila(campos, c, 2, "Descripción", desplazamiento, null);
        agregarFila(campos, c, 4, "Precio (MXN) *", precio, errorPrecio);
        return campos;
    }

    private static void agregarFila(JPanel panel, GridBagConstraints c, int fila, String titulo,
            Component campo, JLabel error) {
        c.gridx = 0;
        c.gridy = fila;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(6, 0, 2, 12);
        panel.add(new JLabel(titulo), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, c);

        c.gridy = fila + 1;
        c.insets = new Insets(0, 0, 4, 0);
        panel.add(error == null ? new JLabel(" ") : error, c);
    }

    private JPanel construirBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.add(cancelar);
        botones.add(guardar);
        return botones;
    }

    private void intentarGuardar() {
        BigDecimal valorPrecio = parsearPrecio(precio.getText());
        Map<Campo, String> errores = ProductoValidator.errores(nombre.getText(), valorPrecio);
        if (valorPrecio == null && !precio.getText().isBlank()) {
            errores.put(Campo.PRECIO, PRECIO_ILEGIBLE);
        }

        marcar(nombre, errorNombre, errores.get(Campo.NOMBRE));
        marcar(precio, errorPrecio, errores.get(Campo.PRECIO));
        if (errores.containsKey(Campo.NOMBRE)) {
            nombre.requestFocusInWindow();
        } else if (errores.containsKey(Campo.PRECIO)) {
            precio.requestFocusInWindow();
        }
        if (errores.isEmpty()) {
            alGuardar.accept(new Producto(idProducto, nombre.getText().strip(), descripcion.getText(), valorPrecio));
        }
    }

    private void cerrarSiSePuede() {
        // Mientras se guarda los botones están bloqueados: no se cierra a medias.
        if (cancelar.isEnabled()) {
            dispose();
        }
    }

    /**
     * Interpreta el texto del precio: admite "$", espacios y comas de miles (1,250.50).
     *
     * @return el precio, o {@code null} si está vacío o no es un número válido
     */
    static BigDecimal parsearPrecio(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(" ", "").strip();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.contains(",")) {
            if (!MILES_CON_COMA.matcher(limpio).matches()) {
                return null;
            }
            limpio = limpio.replace(",", "");
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void marcar(JTextComponent campo, JLabel etiqueta, String error) {
        campo.putClientProperty("JComponent.outline", error == null ? null : "error");
        etiqueta.setText(error == null ? " " : error);
    }

    private static void limpiarErrorAlEscribir(JTextComponent campo, JLabel etiqueta) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { marcar(campo, etiqueta, null); }
            @Override public void removeUpdate(DocumentEvent e) { marcar(campo, etiqueta, null); }
            @Override public void changedUpdate(DocumentEvent e) { marcar(campo, etiqueta, null); }
        });
    }

    private static JLabel etiquetaError() {
        JLabel etiqueta = new JLabel(" ");
        Color color = UIManager.getColor("Component.error.focusedBorderColor");
        etiqueta.setForeground(color != null ? color : new Color(0xC62828));
        etiqueta.putClientProperty("FlatLaf.styleClass", "small");
        return etiqueta;
    }

    // ---- API para el controlador ----

    public void alGuardar(Consumer<Producto> accion) {
        this.alGuardar = accion;
    }

    /** Controles que se bloquean mientras se guarda. */
    public Component[] controles() {
        return new Component[] {nombre, descripcion, precio, guardar, cancelar};
    }
}
