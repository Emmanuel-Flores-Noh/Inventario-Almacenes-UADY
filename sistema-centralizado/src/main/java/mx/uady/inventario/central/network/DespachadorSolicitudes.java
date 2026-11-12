package mx.uady.inventario.central.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;
import mx.uady.inventario.central.service.VentaService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Traduce una {@link Solicitud} en la llamada a la operacion correspondiente.
 *
 * <p>El catalogo de servicios y operaciones se toma de {@link Servicios} y
 * {@link Operaciones}; no se aceptan nombres sueltos. Una operacion puede estar
 * en el catalogo pero sin manejador (fase de servidor sin logica de negocio), en
 * cuyo caso se responde {@link CodigoError#OPERACION_NO_SOPORTADA}.</p>
 *
 * <p>Este es el unico punto donde las excepciones de negocio se convierten en
 * respuestas del contrato, de modo que un fallo nunca cierra la conexion.</p>
 */
public class DespachadorSolicitudes {

    /**
     * Centinela para operaciones declaradas sin logica. ConcurrentHashMap no admite
     * valores nulos, por eso se usa un manejador que solo marca la operacion como pendiente.
     */
    private static final ManejadorOperacion PENDIENTE = parametros -> {
        throw new UnsupportedOperationException("Operacion pendiente de implementar");
    };

    private final Map<String, Map<String, ManejadorOperacion>> operaciones = new ConcurrentHashMap<>();

    /**
     * Crea un despachador vacio. Util para pruebas y para registrar solo lo necesario.
     */
    public DespachadorSolicitudes() {
    }

    /**
     * Crea el despachador con el catalogo completo del contrato.
     *
     * <p>Las operaciones quedan reconocidas pero sin manejador: la logica de
     * negocio se conecta despues con {@link #registrarOperacion(String, String, ManejadorOperacion)}.</p>
     *
     * @return el despachador con los servicios y operaciones del contrato
     */
    public static DespachadorSolicitudes estandar() {
        return estandar(null);
    }

    /**
     * Crea el despachador estándar conectando la operación de venta.
     *
     * @param ventaService servicio de venta; si es nulo, la operación queda pendiente
     * @return despachador con el catálogo estándar
     */
    public static DespachadorSolicitudes estandar(VentaService ventaService) {
        DespachadorSolicitudes despachador = new DespachadorSolicitudes();
        despachador.registrarServicio(Servicios.GESTOR_TIENDAS,
                Operaciones.LISTAR_TIENDAS, Operaciones.ACTUALIZAR_TIENDA_VENTA);
        despachador.registrarServicio(Servicios.INVENTARIO,
                Operaciones.LISTAR_INVENTARIO);
        despachador.registrarServicio(Servicios.GESTOR_PRECIOS,
                Operaciones.LISTAR_PRECIOS_PRODUCTO);
        if (ventaService != null) {
            despachador.registrarOperacion(
                    Servicios.GESTOR_TIENDAS,
                    Operaciones.ACTUALIZAR_TIENDA_VENTA,
                    ventaService::registrarVenta
            );
        }
        return despachador;
    }

    /**
     * Declara un servicio y las operaciones que atiende, sin manejador todavia.
     *
     * @param servicio         nombre del servicio
     * @param nombresOperacion operaciones validas para ese servicio
     */
    public void registrarServicio(String servicio, String... nombresOperacion) {
        Map<String, ManejadorOperacion> operacionesDelServicio =
                operaciones.computeIfAbsent(validarNombre(servicio), clave -> new ConcurrentHashMap<>());
        for (String operacion : nombresOperacion) {
            operacionesDelServicio.putIfAbsent(validarNombre(operacion), PENDIENTE);
        }
    }

    /**
     * Asocia una operacion con su manejador. Si el servicio u operacion no estaban
     * declarados, se agregan al catalogo.
     *
     * @param servicio  nombre del servicio
     * @param operacion nombre de la operacion
     * @param manejador logica a ejecutar
     */
    public void registrarOperacion(String servicio, String operacion, ManejadorOperacion manejador) {
        if (manejador == null) {
            throw new IllegalArgumentException("El manejador de la operacion no puede ser nulo");
        }
        operaciones.computeIfAbsent(validarNombre(servicio), clave -> new ConcurrentHashMap<>())
                .put(validarNombre(operacion), manejador);
    }

    /**
     * Indica si el servicio esta declarado.
     *
     * @param servicio nombre del servicio
     * @return {@code true} si el catalogo lo contiene
     */
    public boolean conoceServicio(String servicio) {
        return servicio != null && operaciones.containsKey(servicio);
    }

    /**
     * Indica si la operacion esta declarada para el servicio.
     *
     * @param servicio  nombre del servicio
     * @param operacion nombre de la operacion
     * @return {@code true} si el par existe en el catalogo
     */
    public boolean conoceOperacion(String servicio, String operacion) {
        if (servicio == null || operacion == null) {
            return false;
        }
        Map<String, ManejadorOperacion> delServicio = operaciones.get(servicio);
        return delServicio != null && delServicio.containsKey(operacion);
    }

    /**
     * Ejecuta la operacion solicitada y devuelve siempre una respuesta.
     *
     * @param solicitud solicitud recibida
     * @return respuesta exitosa o de error con el mismo {@code idSolicitud}
     */
    public Respuesta despachar(Solicitud solicitud) {
        if (solicitud == null) {
            return Respuesta.error(null, CodigoError.PARAMETROS_INVALIDOS, "La solicitud es nula");
        }

        String idSolicitud = solicitud.getIdSolicitud();
        String nombreServicio = solicitud.getNombreServicio();
        String nombreOperacion = solicitud.getNombreOperacion();

        Map<String, ManejadorOperacion> delServicio = nombreServicio == null ? null : operaciones.get(nombreServicio);
        if (delServicio == null) {
            return Respuesta.error(idSolicitud, CodigoError.SERVICIO_DESCONOCIDO,
                    "Servicio desconocido: " + nombreServicio);
        }

        if (nombreOperacion == null || !delServicio.containsKey(nombreOperacion)) {
            return Respuesta.error(idSolicitud, CodigoError.OPERACION_NO_SOPORTADA,
                    "Operacion no soportada: " + nombreOperacion + " para el servicio " + nombreServicio);
        }

        ManejadorOperacion manejador = delServicio.get(nombreOperacion);
        if (manejador == PENDIENTE) {
            return Respuesta.error(idSolicitud, CodigoError.OPERACION_NO_SOPORTADA,
                    "Operacion pendiente de implementar: " + nombreOperacion);
        }

        try {
            JsonElement parametros = solicitud.getParametros();
            if (parametros == null) {
                parametros = new JsonObject();
            }
            if (!parametros.isJsonObject()) {
                return Respuesta.error(idSolicitud, CodigoError.PARAMETROS_INVALIDOS,
                        "Los parametros deben ser un objeto JSON");
            }
            return Respuesta.exito(idSolicitud, manejador.ejecutar(parametros.getAsJsonObject()));
        } catch (IllegalArgumentException e) {
            // Los manejadores usan IllegalArgumentException para senalar parametros invalidos.
            return Respuesta.error(idSolicitud, CodigoError.PARAMETROS_INVALIDOS, e.getMessage());
        } catch (Exception e) {
            return Respuesta.error(idSolicitud, CodigoError.ERROR_INTERNO,
                    "Error al ejecutar la operacion " + nombreOperacion + ": " + e.getMessage());
        }
    }

    private static String validarNombre(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El nombre de servicio u operacion es obligatorio");
        }
        return valor.trim();
    }
}
