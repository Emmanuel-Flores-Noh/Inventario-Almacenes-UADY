package mx.uady.inventario.central.view;

import java.awt.Component;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.NegocioException;

/**
 * Punto único para mostrar mensajes al Administrador.
 * Traduce las excepciones a textos claros y deja el detalle técnico en el registro.
 */
public final class Mensajes {

    static final String ERROR_CONEXION =
            "No se pudo conectar con la base de datos. Verifica que esté en ejecución e inténtalo de nuevo.";
    static final String ERROR_ALMACENAMIENTO =
            "No se pudo guardar o leer la información. Inténtalo de nuevo; si persiste, consulta el registro.";
    static final String ERROR_INESPERADO =
            "Ocurrió un error inesperado. Consulta el registro para más detalles.";

    private static final Logger LOG = Logger.getLogger(Mensajes.class.getName());

    /** Tras cerrar un error, el mismo texto no se repite durante este tiempo. */
    static final long SILENCIO_TRAS_CERRAR_MS = 5_000;

    /** Errores en pantalla y momento en que se cerró cada uno; solo se accede desde el hilo de Swing. */
    private static final Set<String> ERRORES_VISIBLES = new HashSet<>();
    private static final Map<String, Long> ERRORES_CERRADOS = new HashMap<>();

    private Mensajes() {
    }

    /**
     * Muestra un diálogo de error con el texto adecuado para la excepción.
     * Si el mismo texto ya está en pantalla o se cerró hace menos de
     * {@value #SILENCIO_TRAS_CERRAR_MS} ms (p. ej. varias pantallas que fallan sin base de datos),
     * no se repite: cada pantalla sigue indicando el fallo en su línea de estado.
     */
    public static void mostrarError(Component padre, Throwable error) {
        String texto = describir(error);
        if (buscar(error, NegocioException.class) == null) {
            LOG.log(Level.SEVERE, texto, error);
        }
        enEdt(() -> {
            if (!debeMostrarse(texto, System.currentTimeMillis())) {
                return;
            }
            ERRORES_VISIBLES.add(texto);
            try {
                JOptionPane.showMessageDialog(padre, texto, "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                ERRORES_VISIBLES.remove(texto);
                marcarCerrado(texto, System.currentTimeMillis());
            }
        });
    }

    /** Registra el cierre de un error; las pruebas lo usan para simular un diálogo ya cerrado. */
    static void marcarCerrado(String texto, long momento) {
        ERRORES_CERRADOS.put(texto, momento);
    }

    static boolean debeMostrarse(String texto, long ahora) {
        Long cerrado = ERRORES_CERRADOS.get(texto);
        return !ERRORES_VISIBLES.contains(texto) && (cerrado == null || ahora - cerrado >= SILENCIO_TRAS_CERRAR_MS);
    }

    /** Muestra un diálogo informativo. */
    public static void mostrarInfo(Component padre, String texto) {
        enEdt(() -> JOptionPane.showMessageDialog(padre, texto, "Información", JOptionPane.INFORMATION_MESSAGE));
    }

    /** Pide confirmación al usuario; debe llamarse desde el hilo de Swing. */
    public static boolean confirmar(Component padre, String pregunta) {
        // Botones explícitos: los de JOptionPane salen en el idioma de la JVM (p. ej. "Yes/No").
        Object[] opciones = {"Sí", "No"};
        return JOptionPane.showOptionDialog(padre, pregunta, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]) == JOptionPane.YES_OPTION;
    }

    /** Devuelve el texto que verá el usuario para la excepción dada. */
    static String describir(Throwable error) {
        NegocioException negocio = buscar(error, NegocioException.class);
        if (negocio != null && negocio.getMessage() != null) {
            return negocio.getMessage();
        }
        if (esFalloDeConexion(error)) {
            return ERROR_CONEXION;
        }
        if (buscar(error, SQLException.class) != null || buscar(error, AccesoDatosException.class) != null) {
            return ERROR_ALMACENAMIENTO;
        }
        return ERROR_INESPERADO;
    }

    /** La base de datos no responde: socket rechazado o SQLState de la clase 08 (conexión). */
    private static boolean esFalloDeConexion(Throwable error) {
        if (buscar(error, ConnectException.class) != null) {
            return true;
        }
        for (Throwable actual = error; actual != null && actual.getCause() != actual; actual = actual.getCause()) {
            if (actual instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
                return true;
            }
        }
        return false;
    }

    /** Busca el primer elemento del tipo indicado en la cadena de causas. */
    private static <E extends Throwable> E buscar(Throwable error, Class<E> tipo) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return null;
    }

    private static void enEdt(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
        } else {
            SwingUtilities.invokeLater(accion);
        }
    }
}
