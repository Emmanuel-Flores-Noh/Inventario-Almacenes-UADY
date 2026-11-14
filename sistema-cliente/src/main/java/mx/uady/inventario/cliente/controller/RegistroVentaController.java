package mx.uady.inventario.cliente.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import mx.uady.inventario.cliente.model.ClienteTCP;
import mx.uady.inventario.cliente.model.ErrorServicioException;
import mx.uady.inventario.cliente.model.MercanciaDTO;
import mx.uady.inventario.cliente.model.TiendaDTO;
import mx.uady.inventario.cliente.model.UsuarioDTO;
import mx.uady.inventario.cliente.util.ConfiguracionNetwork;
import mx.uady.inventario.cliente.util.TraductorErrores;
import mx.uady.inventario.cliente.view.RegistroVentaFrame;
import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;

import javax.swing.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Controlador encargado de coordinar la lógica de negocio, la comunicación de red
 * mediante {@link ClienteTCP} y la actualización asíncrona de la interfaz gráfica
 * en el {@link RegistroVentaFrame}.
 *
 * <p>La vista nunca conoce el socket ni el JSON: recibe DTOs y mensajes ya traducidos
 * con {@link TraductorErrores}.</p>
 */
public class RegistroVentaController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RegistroVentaFrame vista;
    private final ClienteTCP clienteTcp;
    private final Gson gson;

    /**
     * Constructor que inicializa el controlador, vincula la vista y configura
     * el cliente TCP leyendo los parámetros de red mediante {@link ConfiguracionNetwork}.
     *
     * @param vista La instancia de {@link RegistroVentaFrame} que representa la interfaz visual.
     */
    public RegistroVentaController(RegistroVentaFrame vista) {
        this.vista = vista;
        ConfiguracionNetwork configRed = ConfiguracionNetwork.desdeEntorno();
        this.clienteTcp = new ClienteTCP(configRed.brokerHost(), configRed.brokerPuerto(), configRed.timeoutMs());
        this.gson = GsonFactory.DEFAULT;
    }

    /**
     * Consulta y carga de manera asíncrona los catálogos del sistema (tiendas, mercancía disponible
     * y usuarios proveedores) empleando {@link SwingWorker} para evitar congelar la interfaz de usuario.
     * Si alguna consulta falla, se informa al usuario y se ofrece reintentar.
     */
    public void cargarListasIniciales() {
        vista.mostrarIndicadorCarga("Cargando catálogos y mercancía disponible...", true);

        new SwingWorker<Void, Void>() {
            private List<TiendaDTO> tiendas;
            private List<MercanciaDTO> mercancia;
            private List<UsuarioDTO> usuarios;

            @Override
            protected Void doInBackground() throws Exception {
                tiendas = consultarLista(Servicios.GESTOR_TIENDAS, Operaciones.LISTAR_TIENDAS,
                        new TypeToken<List<TiendaDTO>>() {}.getType());
                mercancia = consultarLista(Servicios.INVENTARIO, Operaciones.LISTAR_INVENTARIO,
                        new TypeToken<List<MercanciaDTO>>() {}.getType());
                usuarios = consultarLista(Servicios.GESTOR_TIENDAS, Operaciones.LISTAR_USUARIOS,
                        new TypeToken<List<UsuarioDTO>>() {}.getType());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    vista.actualizarListasCombos(tiendas, usuarios);
                    vista.actualizarTablaMercancia(mercancia);
                    vista.mostrarIndicadorCarga("Listo", false);
                } catch (Exception e) {
                    vista.mostrarIndicadorCarga("No se pudieron cargar las listas.", false);
                    informarError("No se pudieron cargar las listas.", e,
                            RegistroVentaController.this::cargarListasIniciales);
                }
            }
        }.execute();
    }

    /**
     * Envía de forma asíncrona la solicitud de venta de una unidad a una tienda. En caso de éxito
     * muestra la unidad, la tienda y la fecha, limpia el formulario y recarga las listas.
     *
     * @param numeroSerie    número de serie de la unidad seleccionada
     * @param nombreProducto nombre del producto de la unidad, para el mensaje de confirmación
     * @param tienda         tienda destino seleccionada
     * @param usuario        usuario proveedor que registra la venta
     */
    public void ejecutarRegistroVenta(String numeroSerie, String nombreProducto, TiendaDTO tienda, UsuarioDTO usuario) {
        if (numeroSerie == null || numeroSerie.isBlank() || tienda == null || tienda.getIdTienda() == null
                || usuario == null || usuario.getIdUsuario() == null) {
            vista.mostrarMensajeError("Debe seleccionar una unidad de mercancía, una tienda y un usuario proveedor.");
            return;
        }

        vista.mostrarIndicadorCarga("Registrando venta de la unidad...", true);

        new SwingWorker<JsonElement, Void>() {
            @Override
            protected JsonElement doInBackground() throws Exception {
                JsonObject params = new JsonObject();
                params.addProperty("numero_serie", numeroSerie);
                params.addProperty("id_tienda", tienda.getIdTienda());
                params.addProperty("id_usuario", usuario.getIdUsuario());
                return enviar(Servicios.GESTOR_TIENDAS, Operaciones.ACTUALIZAR_TIENDA_VENTA, params);
            }

            @Override
            protected void done() {
                try {
                    JsonElement resultado = get();
                    vista.mostrarIndicadorCarga("Venta registrada", false);
                    vista.mostrarMensajeExito("¡Venta registrada con éxito!"
                            + "\nUnidad: " + numeroSerie + (nombreProducto != null ? " (" + nombreProducto + ")" : "")
                            + "\nTienda: " + tienda.getNombre()
                            + "\nFecha: " + extraerFecha(resultado));
                    vista.limpiarFormulario();
                    cargarListasIniciales();
                } catch (Exception e) {
                    vista.mostrarIndicadorCarga("No se registró la venta.", false);
                    informarError("No se pudo registrar la venta.", e,
                            () -> ejecutarRegistroVenta(numeroSerie, nombreProducto, tienda, usuario));
                }
            }
        }.execute();
    }

    /**
     * Solicita de forma asíncrona al Sistema 1, a través del Broker, la imagen del código QR de una
     * unidad y la muestra en la vista.
     *
     * @param numeroSerie número de serie de la unidad
     */
    public void consultarQr(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isBlank()) {
            return;
        }
        vista.mostrarIndicadorCarga("Obteniendo código QR de " + numeroSerie + "...", true);

        new SwingWorker<JsonObject, Void>() {
            @Override
            protected JsonObject doInBackground() throws Exception {
                JsonObject params = new JsonObject();
                params.addProperty("numero_serie", numeroSerie);
                JsonElement resultado = enviar(Servicios.INVENTARIO, Operaciones.OBTENER_QR, params);
                if (resultado == null || !resultado.isJsonObject()) {
                    throw new ErrorServicioException(null, "El servidor no devolvió el código QR.");
                }
                return resultado.getAsJsonObject();
            }

            @Override
            protected void done() {
                try {
                    JsonObject qr = get();
                    vista.mostrarIndicadorCarga("Listo", false);
                    vista.mostrarQr(
                            qr.get("numeroSerie").getAsString(),
                            qr.get("idProducto").getAsInt(),
                            qr.has("nombreProducto") ? qr.get("nombreProducto").getAsString() : null,
                            Base64.getDecoder().decode(qr.get("pngBase64").getAsString()));
                } catch (Exception e) {
                    vista.mostrarIndicadorCarga("No se pudo obtener el QR.", false);
                    informarError("No se pudo obtener el código QR.", e, () -> consultarQr(numeroSerie));
                }
            }
        }.execute();
    }

    /**
     * Consulta una operación que devuelve un arreglo y lo convierte a una lista de DTOs.
     * Una respuesta exitosa sin resultado se interpreta como lista vacía.
     *
     * @param servicio  nombre del servicio destino
     * @param operacion nombre de la operación
     * @param tipo      tipo genérico de la lista esperada
     * @param <T>       tipo de los elementos
     * @return lista resultante; vacía si no hay elementos
     * @throws Exception si falla la red o el servidor responde con error
     */
    private <T> List<T> consultarLista(String servicio, String operacion, Type tipo) throws Exception {
        JsonElement resultado = enviar(servicio, operacion, null);
        if (resultado == null || resultado.isJsonNull()) {
            return List.of();
        }
        List<T> lista = gson.fromJson(resultado, tipo);
        return lista != null ? lista : List.of();
    }

    /**
     * Envía una solicitud al Broker y devuelve el resultado si la respuesta fue exitosa.
     *
     * @param servicio   nombre del servicio destino
     * @param operacion  nombre de la operación
     * @param parametros parámetros de la operación; puede ser nulo
     * @return resultado de la respuesta (puede ser nulo)
     * @throws ErrorServicioException si la respuesta llega con estado de error o vacía
     * @throws Exception              si falla la comunicación de red
     */
    private JsonElement enviar(String servicio, String operacion, JsonObject parametros) throws Exception {
        Solicitud solicitud = Solicitud.crear(UUID.randomUUID().toString(), servicio, operacion, parametros);
        Respuesta respuesta = clienteTcp.enviarSolicitud(solicitud);
        if (respuesta == null) {
            throw new ErrorServicioException(null, "El servidor no envió respuesta.");
        }
        if (respuesta.getEstado() != EstadoRespuesta.EXITO) {
            throw new ErrorServicioException(respuesta.getCodigoError(), respuesta.getMensajeError());
        }
        return respuesta.getResultado();
    }

    /**
     * Muestra un error al usuario con un mensaje entendible. Si el error es reintentable
     * (red, timeout, servidor caído), ofrece reintentar la operación.
     *
     * @param contexto  frase que indica qué operación falló
     * @param error     excepción recibida
     * @param reintento acción a ejecutar si el usuario elige reintentar
     */
    private void informarError(String contexto, Exception error, Runnable reintento) {
        String mensaje = contexto + "\n" + TraductorErrores.traducir(error);
        if (TraductorErrores.esReintentable(error)) {
            vista.mostrarDialogoReintentar(mensaje, reintento);
        } else {
            vista.mostrarMensajeError(mensaje);
        }
    }

    /**
     * Obtiene la fecha de venta de la confirmación del servidor con formato legible.
     *
     * @param resultado resultado de {@code actualizarTiendaVenta}
     * @return fecha en formato {@code dd/MM/yyyy HH:mm}, el texto original si no se puede
     *         interpretar, o "N/D" si no viene
     */
    private static String extraerFecha(JsonElement resultado) {
        if (resultado == null || !resultado.isJsonObject() || !resultado.getAsJsonObject().has("fecha_venta")) {
            return "N/D";
        }
        String texto = resultado.getAsJsonObject().get("fecha_venta").getAsString();
        try {
            return LocalDateTime.parse(texto).format(FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            return texto;
        }
    }
}
