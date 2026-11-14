package mx.uady.inventario.central.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Servidor TCP del Sistema 1 (proveedor).
 *
 * <p>Escucha en {@code SERVICE_PORT} y atiende cada conexion en un hilo del pool,
 * igual que el broker. El protocolo es el mismo: una solicitud JSON por linea y
 * una respuesta JSON por linea sobre el mismo socket. El servidor no conoce la
 * base de datos; solo delega en {@link DespachadorSolicitudes}.</p>
 */
public class ServidorServicio {

    private static final int TIMEOUT_POR_DEFECTO_MILLIS = 5000;

    private final int puerto;
    private final int tamanoPool;
    private final int timeoutMillis;
    private final DespachadorSolicitudes despachador;
    private final ExecutorService pool;

    private volatile boolean activo;
    private ServerSocket serverSocket;
    private Thread hiloAceptor;

    /**
     * Crea el servidor con el timeout por defecto de lectura.
     *
     * @param puerto       puerto de escucha; {@code 0} pide uno efimero
     * @param tamanoPool   cantidad de conexiones concurrentes
     * @param despachador  despachador de operaciones
     */
    public ServidorServicio(int puerto, int tamanoPool, DespachadorSolicitudes despachador) {
        this(puerto, tamanoPool, TIMEOUT_POR_DEFECTO_MILLIS, despachador);
    }

    /**
     * Crea el servidor con un timeout explicito.
     *
     * @param puerto       puerto de escucha; {@code 0} pide uno efimero
     * @param tamanoPool   cantidad de conexiones concurrentes
     * @param timeoutMillis timeout de lectura de cada socket en milisegundos
     * @param despachador  despachador de operaciones
     */
    public ServidorServicio(int puerto, int tamanoPool, int timeoutMillis, DespachadorSolicitudes despachador) {
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango");
        }
        if (tamanoPool <= 0) {
            throw new IllegalArgumentException("tamanoPool debe ser positivo");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis debe ser positivo");
        }
        if (despachador == null) {
            throw new IllegalArgumentException("El despachador no puede ser nulo");
        }
        this.puerto = puerto;
        this.tamanoPool = tamanoPool;
        this.timeoutMillis = timeoutMillis;
        this.despachador = despachador;
        this.pool = Executors.newFixedThreadPool(tamanoPool);
    }

    /**
     * Abre el puerto y arranca el hilo que acepta conexiones.
     *
     * @throws IllegalStateException si el puerto no se puede abrir
     */
    public void iniciar() {
        if (activo) {
            return;
        }

        try {
            serverSocket = new ServerSocket(puerto);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir el ServerSocket en el puerto " + puerto, e);
        }

        activo = true;
        hiloAceptor = new Thread(this::aceptarConexiones, "servicio-acceptor");
        hiloAceptor.setDaemon(false);
        hiloAceptor.start();
    }

    private void aceptarConexiones() {
        while (activo) {
            try {
                Socket cliente = serverSocket.accept();
                pool.submit(new ManejadorConexionServicio(cliente, despachador, timeoutMillis));
            } catch (IOException e) {
                if (!activo) {
                    break;
                }
            }
        }
    }

    /**
     * Cierra el puerto y espera a que terminen las conexiones en curso.
     */
    public void detener() {
        activo = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // El socket ya estaba cerrado: no hay nada que hacer.
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Puerto real de escucha (util cuando se pidio el puerto {@code 0}).
     *
     * @return el puerto asignado
     */
    public int getPuerto() {
        if (serverSocket != null && serverSocket.isBound() && !serverSocket.isClosed()) {
            return serverSocket.getLocalPort();
        }
        return puerto;
    }

    /**
     * Indica si el servidor acepta conexiones.
     *
     * @return {@code true} mientras el servidor está activo
     */
    public boolean isActivo() {
        return activo;
    }

    /**
     * Devuelve el número máximo de conexiones atendidas en paralelo.
     *
     * @return tamaño fijo del pool de trabajadores
     */
    public int getTamanoPool() {
        return tamanoPool;
    }
}
