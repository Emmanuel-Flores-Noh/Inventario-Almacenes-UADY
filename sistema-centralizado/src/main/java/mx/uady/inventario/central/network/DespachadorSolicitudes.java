package mx.uady.inventario.central.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.error.CodigoError;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;
import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.QrService;
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
        return estandar(null, null, null, null, null);
    }

    /**
     * Crea el despachador estándar conectando solo la operación de venta.
     *
     * @param ventaService servicio de venta; si es nulo, la operación queda pendiente
     * @return despachador con el catálogo estándar
     */
    public static DespachadorSolicitudes estandar(VentaService ventaService) {
        return estandar(ventaService, null, null, null, null);
    }

    /**
     * Crea el despachador con el catálogo completo y conecta cada operación cuyo servicio
     * se proporcione. Un servicio nulo deja sus operaciones declaradas pero pendientes.
     *
     * @param ventaService     registra ventas ({@code actualizarTiendaVenta})
     * @param consultasVenta   catálogos de tiendas y usuarios ({@code listarTiendas}, {@code listarUsuarios})
     * @param mercanciaService unidades físicas ({@code listarInventario}, {@code obtenerQr})
     * @param productoService  productos; necesario para nombres y precios
     *                         ({@code listarInventario}, {@code obtenerQr}, {@code listarPreciosProducto})
     * @param qrService        lectura de los PNG ({@code obtenerQr})
     * @return despachador con el catálogo estándar
     */
    public static DespachadorSolicitudes estandar(
            VentaService ventaService,
            ConsultasVentaService consultasVenta,
            MercanciaService mercanciaService,
            ProductoService productoService,
            QrService qrService) {

        DespachadorSolicitudes despachador = new DespachadorSolicitudes();
        despachador.registrarServicio(Servicios.GESTOR_TIENDAS,
                Operaciones.LISTAR_TIENDAS, Operaciones.ACTUALIZAR_TIENDA_VENTA, Operaciones.LISTAR_USUARIOS);
        despachador.registrarServicio(Servicios.INVENTARIO,
                Operaciones.LISTAR_INVENTARIO, Operaciones.OBTENER_QR);
        despachador.registrarServicio(Servicios.GESTOR_PRECIOS,
                Operaciones.LISTAR_PRECIOS_PRODUCTO);

        if (ventaService != null) {
            despachador.registrarOperacion(Servicios.GESTOR_TIENDAS,
                    Operaciones.ACTUALIZAR_TIENDA_VENTA, ventaService::registrarVenta);
        }
        if (consultasVenta != null) {
            despachador.registrarOperacion(Servicios.GESTOR_TIENDAS,
                    Operaciones.LISTAR_TIENDAS, ManejadoresConsulta.listarTiendas(consultasVenta));
            despachador.registrarOperacion(Servicios.GESTOR_TIENDAS,
                    Operaciones.LISTAR_USUARIOS, ManejadoresConsulta.listarUsuariosProveedores(consultasVenta));
        }
        if (mercanciaService != null && productoService != null) {
            despachador.registrarOperacion(Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO,
                    ManejadoresConsulta.listarInventarioDisponible(mercanciaService, productoService));
            if (qrService != null) {
                despachador.registrarOperacion(Servicios.INVENTARIO, Operaciones.OBTENER_QR,
                        ManejadoresConsulta.obtenerQr(mercanciaService, productoService, qrService));
            }
        }
        if (productoService != null) {
            despachador.registrarOperacion(Servicios.GESTOR_PRECIOS, Operaciones.LISTAR_PRECIOS_PRODUCTO,
                    ManejadoresConsulta.listarPreciosProducto(productoService));
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
