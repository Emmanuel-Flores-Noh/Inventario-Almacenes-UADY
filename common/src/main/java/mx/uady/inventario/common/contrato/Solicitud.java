package mx.uady.inventario.common.contrato;

import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Contrato de solicitud intercambiado entre cliente, broker y proveedor.
 * Los atributos son finales y privados debido a que cuando se construye una solicitud esta debe de ser inmutable
 * no debe de cambiar debido a que ya esta lista para enviarse hacia el broker
 */
public class Solicitud {
    final private String idSolicitud;
    final private String nombreServicio;
    final private String nombreOperacion;
    final private JsonObject parametros;
    final private LocalDateTime fechaHora;

    /**
     * Se encapsula la creacion del objeto solicitud para que se hagan por medio de los metodos que proporciona la clase
     * @param idSolicitud La identificacion de la solicitud
     * @param nombreServicio El nombre del servicio a solicitar
     * @param nombreOperacion El nombre de la operacion que el servicio va a realizar
     * @param parametros Los parametros de la operacion
     * @param fechaHora La fecha en la que se realizo la solicitud
     */
    private Solicitud(String idSolicitud, String nombreServicio, String nombreOperacion,
                     JsonObject parametros, LocalDateTime fechaHora) {
        this.idSolicitud = idSolicitud;
        this.nombreServicio = nombreServicio;
        this.nombreOperacion = nombreOperacion;
        this.parametros = parametros;
        this.fechaHora = fechaHora;
    }


    /**
     * Metodo que valida los argumentos para crear una solicitud.
     * La idea es que la excepcion se propague hasta el codigo del cliente (servicio 2) donde se debe de manejar
     * @param idSolicitud la id de la solicitud
     * @param nombreServicio el nombre del servicio a solicitar al broker
     * @param nombreOperacion el nombre de la operacion que hara el servicio
     * @throws IllegalArgumentException Si alguno de los atributos no cumple con los criterios de aprobacion para crear una solicitud
     */
    private static void validarCreacion(String idSolicitud, String nombreServicio, String nombreOperacion) throws IllegalArgumentException {
        if (idSolicitud == null || idSolicitud.isBlank()) {
            throw new IllegalArgumentException("idSolicitud es obligatorio");
        }
        if (nombreServicio == null || nombreServicio.isBlank()) {
            throw new IllegalArgumentException("nombreServicio es obligatorio");
        }
        if (nombreOperacion == null || nombreOperacion.isBlank()) {
            throw new IllegalArgumentException("nombreOperacion es obligatorio");
        }
    }

    /**
     * Crea una nueva solicitud tomando en cuenta el tiempo actual
     * @param idSolicitud El id de la solicitud
     * @param nombreServicio El nombre del servicio a solicitar al broker
     * @param nombreOperacion El nombre de la operacion que hara el servicio
     * @param parametros Los parametros de la operacion o {@code null} si la operacion no requiere parametros
     * @throws IllegalArgumentException Si la id, nombreServicio o nombreOperacion son invalidos.
     * @return La nueva solicitud creada
     */
    public static Solicitud crear(String idSolicitud, String nombreServicio, String nombreOperacion, JsonObject parametros) throws IllegalArgumentException {
        validarCreacion(idSolicitud, nombreServicio, nombreOperacion);
        return new Solicitud(idSolicitud, nombreServicio, nombreOperacion, parametros, LocalDateTime.now());
    }

    /**
     * Crea una nueva solicitud con la fecha que se le pase como parametro
     * existe para realizar pruebas. Por ello mismo esta en protected y no deberia poderse acceder
     * fuera del paquete
     * @param idSolicitud El id de la solicitud
     * @param nombreServicio El nombre del servicio a solicitar al broker
     * @param nombreOperacion El nombre de la operacion que hara el servicio
     * @param parametros Los parametros de la operacion o {@code null} si la operacion no requiere parametros
     * @param fecha La fecha impuesta por la prueba
     * @throws IllegalArgumentException Si la id, nombreServicio o nombreOperacion son invalidos.
     * @return La nueva solicitud creada
     */
    protected static Solicitud crearConFechaEspecifica(String idSolicitud, String nombreServicio, String nombreOperacion, JsonObject parametros, LocalDateTime fecha) throws IllegalArgumentException {
        validarCreacion(idSolicitud, nombreServicio, nombreOperacion);
        return new Solicitud(idSolicitud, nombreServicio, nombreOperacion, parametros, fecha);
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public String getNombreOperacion() {
        return nombreOperacion;
    }

    public JsonObject getParametros() {
        return parametros;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Solicitud that)) {
            return false;
        }
        return Objects.equals(idSolicitud, that.idSolicitud)
                && Objects.equals(nombreServicio, that.nombreServicio)
                && Objects.equals(nombreOperacion, that.nombreOperacion)
                && Objects.equals(parametros, that.parametros)
                && Objects.equals(fechaHora, that.fechaHora);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSolicitud, nombreServicio, nombreOperacion, parametros, fechaHora);
    }

    @Override
    public String toString() {
        return "Solicitud{" +
                "idSolicitud='" + idSolicitud + '\'' +
                ", nombreServicio='" + nombreServicio + '\'' +
                ", nombreOperacion='" + nombreOperacion + '\'' +
                ", parametros=" + parametros +
                ", fechaHora=" + fechaHora +
                '}';
    }
}
