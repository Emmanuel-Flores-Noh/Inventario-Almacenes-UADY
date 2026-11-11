package mx.uady.inventario.common.contrato;

import com.google.gson.JsonElement;
import mx.uady.inventario.common.error.CodigoError;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Contrato de respuesta intercambiado entre proveedor, broker y cliente.
 * Los atributos son finales y privados debido a que cuando se construye una respuesta esta debe de ser inmutable
 * no debe de cambiar debido a que el sistema 1 debe de crear y enviar una respuesta cuando rechace / realice la operacion y posterior envie la respuesa al
 * broker para que el sistema 2 pueda recibirla
 */
public class Respuesta {
    final private String idSolicitud;
    final private JsonElement resultado;
    final private EstadoRespuesta estado;
    final private CodigoError codigoError;
    final private String mensajeError;
    final private LocalDateTime fechaHora;

    /**
     * Constructor de respuesta
     * Se encapsula para que se generen las respuestas con sus respectivos metodos
     * @param idSolicitud
     * @param resultado
     * @param estado
     * @param codigoError
     * @param mensajeError
     * @param fechaHora
     */
    private Respuesta(String idSolicitud, JsonElement resultado, EstadoRespuesta estado, CodigoError codigoError, String mensajeError, LocalDateTime fechaHora) {
        this.idSolicitud = idSolicitud;
        this.resultado = resultado;
        this.estado = estado;
        this.codigoError = codigoError;
        this.mensajeError = mensajeError;
        this.fechaHora = fechaHora;
    }

    /**
     * Genera una respuesta exitosa
     * @param idSolicitud El id de la solicitud que recibira la respuesta
     * @param resultado El resultado de la operacion
     * @return La respuesta exitosa generada
     */
    public static Respuesta exito(String idSolicitud, JsonElement resultado) {
        return new Respuesta(idSolicitud, resultado, EstadoRespuesta.EXITO, null, null, LocalDateTime.now());
    }

    /**
     * Comprueba si un mensaje de error es valido
     * @param mensajeError El mensaje a evaluar
     * @return {@code true} Si el mensaje es valido
     */
    private static boolean mensajeErrorValido(String mensajeError) {
        return mensajeError != null && ! mensajeError.isBlank();
    }

    /**
     * Comprueba si un codigo de error es valido
     * @param codigoError el codigo de error a evaluar
     * @return {@code true} Si el codigo de error es valido
     */
    private static boolean codigoErrorValido(CodigoError codigoError) {
        return codigoError != null;
    }

    /**
     * Metodo para generar una respuesta de error para enviar al cliente
     * @param idSolicitud El id de la solicitud a quien se le enviara la respuesta
     * @param codigoError El codigo de error
     * @param mensajeError El mensaje de error
     * @return
     */
    public static Respuesta error(String idSolicitud, CodigoError codigoError, String mensajeError) {
        String mensaje;

        /* Aplicamos Descompose conditional */
        if( mensajeErrorValido( mensajeError ) ) {
            mensaje = mensajeError;
        }
        else {
            mensaje = codigoErrorValido(codigoError) ? codigoError.getMensajePorDefecto() : null;
        }

        return new Respuesta(idSolicitud, null, EstadoRespuesta.ERROR, codigoError, mensaje, LocalDateTime.now());
    }

    public String getIdSolicitud() {
        return idSolicitud;
    }

    public JsonElement getResultado() {
        return resultado;
    }

    public EstadoRespuesta getEstado() {
        return estado;
    }

    public CodigoError getCodigoError() {
        return codigoError;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Respuesta otraRespuesta)) {
            return false;
        }
        return Objects.equals(idSolicitud, otraRespuesta.idSolicitud)
                && Objects.equals(resultado, otraRespuesta.resultado)
                && estado == otraRespuesta.estado
                && codigoError == otraRespuesta.codigoError
                && Objects.equals(mensajeError, otraRespuesta.mensajeError)
                && Objects.equals(fechaHora, otraRespuesta.fechaHora);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSolicitud, resultado, estado, codigoError, mensajeError, fechaHora);
    }

    @Override
    public String toString() {
        return "Respuesta {" +
                "idSolicitud='" + idSolicitud + '\'' +
                ", resultado=" + resultado +
                ", estado=" + estado +
                ", codigoError=" + codigoError +
                ", mensajeError='" + mensajeError + '\'' +
                ", fechaHora=" + fechaHora +
                '}';
    }
}