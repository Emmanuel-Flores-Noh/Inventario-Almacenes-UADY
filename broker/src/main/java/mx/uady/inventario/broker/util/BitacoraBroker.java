package mx.uady.inventario.broker.util;

import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bitácora de solicitudes del Broker: escribe una línea por cada evento relevante para
 * monitorear el intermediario (solicitudes atendidas, intentos fallidos hacia un proveedor
 * y cambios en el registro de servicios).
 *
 * <p>Cada línea indica la fecha y hora, el hilo que atendió la conexión y, según el evento,
 * el id de la solicitud, el servicio y la operación, el proveedor destino, el estado y el
 * tiempo de atención. Por defecto se escribe en la consola; el destino es inyectable para
 * las pruebas.</p>
 *
 * <p>Es segura para hilos: cada evento produce una sola línea completa, así que las líneas
 * de solicitudes atendidas en paralelo no se mezclan.</p>
 */
public final class BitacoraBroker {

    /** Prefijo de las líneas de solicitudes y de intentos hacia un proveedor. */
    public static final String PREFIJO_SOLICITUD = "[BROKER]";
    /** Prefijo de las líneas de cambios en el registro de servicios. */
    public static final String PREFIJO_REGISTRO = "[REGISTRO]";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int LARGO_MAXIMO_MENSAJE = 120;
    private static final String SIN_DATO = "-";

    private final Consumer<String> destino;
    private final Supplier<LocalDateTime> reloj;

    /**
     * Crea una bitácora que escribe cada línea en el destino indicado.
     *
     * @param destino función que recibe cada línea ya formateada (por ejemplo, la consola)
     * @throws NullPointerException si el destino es nulo
     */
    public BitacoraBroker(Consumer<String> destino) {
        this(destino, LocalDateTime::now);
    }

    /**
     * Crea una bitácora con destino y reloj explícitos (útil para pruebas deterministas).
     *
     * @param destino función que recibe cada línea ya formateada
     * @param reloj   proveedor de la fecha y hora de cada evento
     * @throws NullPointerException si el destino o el reloj son nulos
     */
    public BitacoraBroker(Consumer<String> destino, Supplier<LocalDateTime> reloj) {
        this.destino = Objects.requireNonNull(destino, "El destino de la bitacora es obligatorio");
        this.reloj = Objects.requireNonNull(reloj, "El reloj de la bitacora es obligatorio");
    }

    /**
     * Crea la bitácora que escribe en la salida estándar, la que usa el Broker en ejecución.
     *
     * @return bitácora hacia la consola
     */
    public static BitacoraBroker consola() {
        return new BitacoraBroker(System.out::println);
    }

    /**
     * Registra una solicitud ya atendida por el Broker, con su resultado final.
     *
     * <p>Ejemplo:
     * {@code [BROKER] 2026-11-14 10:15:02.123 [pool-1-thread-3] id=ev-03 inventario.listarInventario
     * -> sistema-centralizado:6000 | EXITO | 37 ms}</p>
     *
     * @param solicitud solicitud recibida; nula si el JSON no se pudo interpretar
     * @param respuesta respuesta devuelta al cliente
     * @param proveedor proveedor destino como {@code host:puerto}, {@code broker} para operaciones
     *                  internas o {@code null} si no hubo proveedor
     * @param millis    tiempo total de atención en milisegundos
     */
    public void registrarSolicitud(Solicitud solicitud, Respuesta respuesta, String proveedor, long millis) {
        String id = solicitud == null ? SIN_DATO : texto(solicitud.getIdSolicitud());
        String operacion = solicitud == null
                ? "(JSON no interpretable)"
                : texto(solicitud.getNombreServicio()) + "." + texto(solicitud.getNombreOperacion());
        escribir(PREFIJO_SOLICITUD, "id=" + id + " " + operacion
                + " -> " + (proveedor == null ? "(sin proveedor)" : proveedor)
                + " | " + resultado(respuesta)
                + " | " + millis + " ms");
    }

    /**
     * Registra un intento fallido de comunicación con un proveedor, antes de reintentar o de
     * responder con error.
     *
     * @param solicitud solicitud que se intentaba reenviar
     * @param proveedor proveedor contactado
     * @param intento   número del intento que falló (desde 1)
     * @param total     número total de intentos permitidos
     * @param codigo    código de error que se asignó al fallo
     * @param detalle   descripción del fallo
     */
    public void registrarIntentoFallido(Solicitud solicitud, Proveedor proveedor, int intento, int total,
                                        CodigoError codigo, String detalle) {
        escribir(PREFIJO_SOLICITUD, "id=" + texto(solicitud.getIdSolicitud()) + " "
                + texto(solicitud.getNombreServicio()) + "." + texto(solicitud.getNombreOperacion())
                + " -> " + direccion(proveedor)
                + " | intento " + intento + "/" + total + " fallo: " + codigo
                + " (" + recortar(detalle) + ")");
    }

    /**
     * Registra que un servicio quedó marcado como no disponible por fallos repetidos.
     *
     * @param servicio nombre del servicio
     * @param fallos   fallos consecutivos acumulados
     */
    public void registrarNoDisponible(String servicio, int fallos) {
        escribir(PREFIJO_REGISTRO, texto(servicio) + " marcado como NO DISPONIBLE tras " + fallos + " fallos");
    }

    /**
     * Registra un cambio en el registro de servicios hecho a través de una solicitud al Broker.
     *
     * @param accion   acción realizada (por ejemplo {@code registrar}, {@code sustituir} o {@code eliminar})
     * @param servicio nombre del servicio afectado
     * @param proveedor proveedor asociado; nulo si la acción no lo tiene
     * @param aplicado {@code true} si el registro cambió; {@code false} si no había nada que cambiar
     */
    public void registrarCambioRegistro(String accion, String servicio, Proveedor proveedor, boolean aplicado) {
        escribir(PREFIJO_REGISTRO, texto(accion) + " " + texto(servicio)
                + (proveedor == null ? "" : " -> " + direccion(proveedor))
                + (aplicado ? "" : " (sin efecto: el servicio no estaba registrado)"));
    }

    /**
     * Da formato a la dirección de un proveedor.
     *
     * @param proveedor proveedor
     * @return {@code host:puerto}
     */
    public static String direccion(Proveedor proveedor) {
        return proveedor.host() + ":" + proveedor.puerto();
    }

    /**
     * Antepone el prefijo, la fecha y hora y el hilo actual, y entrega la línea al destino.
     *
     * @param prefijo  prefijo de la línea
     * @param contenido texto del evento
     */
    private void escribir(String prefijo, String contenido) {
        destino.accept(prefijo + " " + reloj.get().format(FORMATO_FECHA)
                + " [" + Thread.currentThread().getName() + "] " + contenido);
    }

    /**
     * Resume el resultado de una respuesta: {@code EXITO}, o {@code ERROR} con su código y mensaje.
     *
     * @param respuesta respuesta enviada al cliente
     * @return texto del resultado
     */
    private static String resultado(Respuesta respuesta) {
        if (respuesta == null) {
            return "SIN RESPUESTA";
        }
        if (respuesta.getEstado() == EstadoRespuesta.EXITO) {
            return EstadoRespuesta.EXITO.name();
        }
        return "ERROR " + respuesta.getCodigoError() + ": " + recortar(respuesta.getMensajeError());
    }

    /**
     * Sustituye un texto nulo o vacío por un guion.
     *
     * @param valor texto a mostrar
     * @return el texto, o {@code -} si no hay
     */
    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? SIN_DATO : valor;
    }

    /**
     * Acorta los mensajes largos para mantener cada evento en una línea legible.
     *
     * @param mensaje mensaje original
     * @return mensaje de como máximo {@value #LARGO_MAXIMO_MENSAJE} caracteres, sin saltos de línea
     */
    private static String recortar(String mensaje) {
        String limpio = texto(mensaje).replace('\n', ' ').replace('\r', ' ');
        return limpio.length() <= LARGO_MAXIMO_MENSAJE ? limpio : limpio.substring(0, LARGO_MAXIMO_MENSAJE) + "...";
    }
}
