package mx.uady.inventario.central.controller;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import mx.uady.inventario.central.view.Mensajes;

/**
 * Ejecuta una operación lenta (base de datos, generación de QR) fuera del hilo de Swing.
 * Mientras corre, deshabilita los controles indicados y muestra el cursor de espera;
 * al terminar, los restaura y llama al callback de éxito o de error en el hilo de Swing.
 *
 * <pre>
 * TareaAsync.de(() -> servicio.listar())
 *         .bloqueando(botonGuardar, tabla)
 *         .alTerminar(this::mostrarProductos)
 *         .ejecutar(panel);
 * </pre>
 *
 * Si no se indica {@link #alFallar}, el error se muestra con {@link Mensajes#mostrarError}.
 */
public final class TareaAsync<T> {

    /** Operación sin resultado que puede lanzar excepciones. */
    @FunctionalInterface
    public interface Accion {
        void ejecutar() throws Exception;
    }

    private final Callable<T> operacion;
    private final List<Component> bloqueados = new ArrayList<>();
    private Consumer<? super T> alExito = resultado -> { };
    private Consumer<Throwable> alError;

    private TareaAsync(Callable<T> operacion) {
        this.operacion = Objects.requireNonNull(operacion, "operacion");
    }

    /** Crea una tarea que produce un resultado. */
    public static <T> TareaAsync<T> de(Callable<T> operacion) {
        return new TareaAsync<>(operacion);
    }

    /** Crea una tarea sin resultado (guardar, registrar, etc.). */
    public static TareaAsync<Void> deAccion(Accion accion) {
        Objects.requireNonNull(accion, "accion");
        return new TareaAsync<>(() -> {
            accion.ejecutar();
            return null;
        });
    }

    /** Controles que se deshabilitan mientras la tarea está en curso. */
    public TareaAsync<T> bloqueando(Component... componentes) {
        bloqueados.addAll(Arrays.asList(componentes));
        return this;
    }

    /** Callback en el hilo de Swing cuando la operación termina bien. */
    public TareaAsync<T> alTerminar(Consumer<? super T> callback) {
        this.alExito = Objects.requireNonNull(callback, "callback");
        return this;
    }

    /** Callback en el hilo de Swing cuando la operación falla (sustituye al diálogo por defecto). */
    public TareaAsync<T> alFallar(Consumer<Throwable> callback) {
        this.alError = Objects.requireNonNull(callback, "callback");
        return this;
    }

    /**
     * Inicia la tarea. Debe llamarse desde el hilo de Swing.
     *
     * @param origen componente desde el que se lanza; se usa para el cursor y para centrar los diálogos.
     */
    public SwingWorker<T, Void> ejecutar(Component origen) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("TareaAsync.ejecutar debe llamarse desde el hilo de Swing");
        }

        boolean[] estadoPrevio = new boolean[bloqueados.size()];
        for (int i = 0; i < bloqueados.size(); i++) {
            estadoPrevio[i] = bloqueados.get(i).isEnabled();
            bloqueados.get(i).setEnabled(false);
        }
        Window ventana = origen == null ? null : SwingUtilities.getWindowAncestor(origen);
        Cursor cursorPrevio = ventana == null ? null : ventana.getCursor();
        if (ventana != null) {
            ventana.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                for (int i = 0; i < bloqueados.size(); i++) {
                    bloqueados.get(i).setEnabled(estadoPrevio[i]);
                }
                if (ventana != null) {
                    ventana.setCursor(cursorPrevio);
                }

                T resultado;
                try {
                    resultado = get();
                } catch (CancellationException e) {
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    notificarError(origen, e.getCause() != null ? e.getCause() : e);
                    return;
                }
                alExito.accept(resultado);
            }
        };
        worker.execute();
        return worker;
    }

    private void notificarError(Component origen, Throwable error) {
        if (alError != null) {
            alError.accept(error);
        } else {
            Mensajes.mostrarError(origen, error);
        }
    }
}
