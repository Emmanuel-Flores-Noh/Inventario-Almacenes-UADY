package mx.uady.inventario.broker.network;

import mx.uady.inventario.broker.registry.RegistroServicios;
import mx.uady.inventario.broker.routing.Enrutador;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Servidor TCP del broker con atencion concurrente mediante pool de hilos.
 */
public class BrokerServer {

    private final int puerto;
    private final int tamanoPool;
    private final int timeoutMillis;
    private final RegistroServicios registro;
    private final Enrutador enrutador;
    private final ExecutorService pool;

    // Volatile para que sea visible a otros threads
    private volatile boolean activo;
    private ServerSocket serverSocket;
    private Thread hiloAceptor;


    /**
     * Constructor para crear un broker
     * @param puerto El puerto en el que se abrira el servicio
     * @param tamanoPool La cantidad de peticiones que va admitir a la vez
     */
    public BrokerServer(int puerto, int tamanoPool) {
        this(puerto, tamanoPool, 5000);
    }

    /**
     * Constructor para crear un broker
     * @param puerto El puerto en el que se abrira el servicio
     * @param tamanoPool La cantidad de peticiones que va admitir a la vez (cantidad de threads)
     * @param timeoutMillis EL tiempo en milisegundos que sobrepasados hara timeout
     */
    public BrokerServer(int puerto, int tamanoPool, int timeoutMillis) {
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango");
        }
        if (tamanoPool <= 0) {
            throw new IllegalArgumentException("tamanoPool debe ser positivo");
        }
        this.puerto = puerto;
        this.tamanoPool = tamanoPool;
        this.timeoutMillis = timeoutMillis;
        this.registro = new RegistroServicios();
        this.enrutador = new Enrutador(timeoutMillis);
        this.pool = Executors.newFixedThreadPool(tamanoPool);
    }

    /**
     * Inicia el broker
     * Inicializa el server socket y crea un hilo para aceptar las peticiones
     */
    public void iniciar() {
        if ( activo ) {
            return;
        }

        try {
            serverSocket = new ServerSocket(puerto);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir el ServerSocket en el puerto " + puerto, e);
        }

        activo = true;

        hiloAceptor = new Thread(this::aceptarConexiones, "broker-acceptor");
        hiloAceptor.setDaemon(false);
        hiloAceptor.start();
    }

    /**
     * Metodo que maneja las peticiones
     * Las ingresa a una pool de peticiones.
     */
    private void aceptarConexiones() {
        while (activo) {
            try {
                Socket cliente = serverSocket.accept();
                // Sube las peticiones a la pool. Si un threads esta disponible manejara esa peticion
                pool.submit(new ManejadorConexion(cliente, registro, enrutador, timeoutMillis));
            } catch (IOException e) {
                if (!activo) {
                    break;
                }
            }
        }
    }

    /**
     * Detiene el broker de manera segura
     * Cierra el server socket y finaliza las transacciones
     */
    public void detener() {
        activo = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        pool.shutdown();

        /*
        // Le da 5 segundo a los threads para que terminen sus operaciones.
        // Si no han terminado despues de 5 segundos el broker fuerza su apagado
         */
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public RegistroServicios getRegistro() {
        return registro;
    }

    public Enrutador getEnrutador() {
        return enrutador;
    }

    public int getPuerto() {
        if (serverSocket != null && serverSocket.isBound() && !serverSocket.isClosed()) {
            return serverSocket.getLocalPort();
        }
        return puerto;
    }

    public boolean isActivo() {
        return activo;
    }

    public RegistroServicios getRegistry() {
        return getRegistro();
    }

    public Enrutador getRouter() {
        return getEnrutador();
    }
}
