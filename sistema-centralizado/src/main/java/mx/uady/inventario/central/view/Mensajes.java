package mx.uady.inventario.central.view;

import java.awt.Component;
import java.net.ConnectException;
import java.sql.SQLException;
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

    private Mensajes() {
    }

    /** Muestra un diálogo de error con el texto adecuado para la excepción. */
    public static void mostrarError(Component padre, Throwable error) {
        String texto = describir(error);
        if (buscar(error, NegocioException.class) == null) {
            LOG.log(Level.SEVERE, texto, error);
        }
        enEdt(() -> JOptionPane.showMessageDialog(padre, texto, "Error", JOptionPane.ERROR_MESSAGE));
    }

    /** Muestra un diálogo informativo. */
    public static void mostrarInfo(Component padre, String texto) {
        enEdt(() -> JOptionPane.showMessageDialog(padre, texto, "Información", JOptionPane.INFORMATION_MESSAGE));
    }

    /** Pide confirmación al usuario; debe llamarse desde el hilo de Swing. */
    public static boolean confirmar(Component padre, String pregunta) {
        return JOptionPane.showConfirmDialog(padre, pregunta, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Devuelve el texto que verá el usuario para la excepción dada. */
    static String describir(Throwable error) {
        NegocioException negocio = buscar(error, NegocioException.class);
        if (negocio != null && negocio.getMessage() != null) {
            return negocio.getMessage();
        }
        if (buscar(error, SQLException.class) != null || buscar(error, ConnectException.class) != null) {
            return ERROR_CONEXION;
        }
        if (buscar(error, AccesoDatosException.class) != null) {
            return ERROR_ALMACENAMIENTO;
        }
        return ERROR_INESPERADO;
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
