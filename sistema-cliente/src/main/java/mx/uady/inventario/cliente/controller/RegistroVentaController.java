package mx.uady.inventario.cliente.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import mx.uady.inventario.cliente.model.ClienteTCP;
import mx.uady.inventario.cliente.model.TiendaDTO;
import mx.uady.inventario.cliente.model.UsuarioDTO;
import mx.uady.inventario.cliente.util.ConfiguracionNetwork;
import mx.uady.inventario.cliente.view.RegistroVentaFrame;
import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.servicio.Operaciones;
import mx.uady.inventario.common.servicio.Servicios;

import javax.swing.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

/**
 * Controlador encargado de coordinar la lógica de negocio, la comunicación de red 
 * mediante {@link ClienteTCP} y la actualización asíncrona de la interfaz gráfica 
 * en el {@link RegistroVentaFrame}.
 */
public class RegistroVentaController {
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
     */
    public void cargarListasIniciales() {
        vista.mostrarIndicadorCarga("Cargando catálogos y mercancía disponible...", true);

        new SwingWorker<Void, Void>() {
            private List<TiendaDTO> tiendas;
            private List<Mercancia> mercancia;
            private List<UsuarioDTO> usuarios;

            @Override
            protected Void doInBackground() throws Exception {
                tiendas = consultarTiendas();
                mercancia = consultarMercanciaDisponible();
                usuarios = consultarUsuariosProveedores();
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
                    vista.mostrarIndicadorCarga("Error de conexión con el Broker.", false);
                    vista.mostrarDialogoReintentar(
                            "No se pudo conectar con el servidor a través del Broker.\nDetalle: " + e.getMessage(),
                            () -> RegistroVentaController.this.cargarListasIniciales()
                    );
                }
            }
        }.execute();
    }

    /**
     * Consulta la lista de tiendas y almacenes disponibles a través del Broker.
     * 
     * @return Lista de objetos {@link TiendaDTO}.
     * @throws Exception Si ocurre un error de comunicación de red o serialización.
     */
    private List<TiendaDTO> consultarTiendas() throws Exception {
        Solicitud solicitud = Solicitud.crear(
                UUID.randomUUID().toString(),
                Servicios.GESTOR_TIENDAS,
                Operaciones.LISTAR_TIENDAS,
                null
        );

        Respuesta resp = clienteTcp.enviarSolicitud(solicitud);
        if (resp == null) {
            System.err.println("[CLIENTE] Error: La respuesta del Broker fue nula.");
            return List.of();
        }

        if (resp.getEstado() == EstadoRespuesta.EXITO && resp.getResultado() != null) {
            Type listType = new TypeToken<List<TiendaDTO>>() {}.getType();
            return gson.fromJson(resp.getResultado(), listType);
        } else {
            // ¡Aquí destapamos el error que venía oculto!
            System.err.println("[CLIENTE] Error del servidor al listar tiendas: " + resp.getMensajeError() + " [Código: " + resp.getCodigoError() + "]");
        }
        return List.of();
    }

    /**
     * Consulta la lista de mercancías en estado disponible (EN_ALMACEN) a través del Broker.
     * 
     * @return Lista de objetos {@link Mercancia}.
     * @throws Exception Si ocurre un error de comunicación de red o serialización.
     */
    private List<Mercancia> consultarMercanciaDisponible() throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("estado", EstadoMercancia.EN_ALMACEN.name());

        Solicitud solicitud = Solicitud.crear(
                UUID.randomUUID().toString(),
                Servicios.INVENTARIO,
                Operaciones.LISTAR_INVENTARIO,
                params
        );

        Respuesta resp = clienteTcp.enviarSolicitud(solicitud);
        if (resp == null) {
            System.err.println("[CLIENTE] Error: La respuesta del Broker fue nula.");
            return List.of();
        }

        if (resp.getEstado() == EstadoRespuesta.EXITO && resp.getResultado() != null) {
            Type listType = new TypeToken<List<Mercancia>>() {}.getType();
            return gson.fromJson(resp.getResultado(), listType);
        } else {
            // ¡Aquí destapamos el error que venía oculto!
            System.err.println("[CLIENTE] Error del servidor al listar mercancias disponibles: " + resp.getMensajeError() + " [Código: " + resp.getCodigoError() + "]");
        }
        return List.of();
    }

    /**
     * Consulta la lista de usuarios con privilegios de proveedor a través del Broker.
     * 
     * @return Lista de objetos {@link UsuarioDTO}.
     * @throws Exception Si ocurre un error de comunicación de red o serialización.
     */
    private List<UsuarioDTO> consultarUsuariosProveedores() throws Exception {
        Solicitud solicitud = Solicitud.crear(
                UUID.randomUUID().toString(),
                Servicios.GESTOR_TIENDAS,
                Operaciones.LISTAR_USUARIOS, 
                null
        );

        Respuesta resp = clienteTcp.enviarSolicitud(solicitud);
        if (resp == null) {
            System.err.println("[CLIENTE] Error: La respuesta del Broker fue nula.");
            return List.of();
        }

        if (resp.getEstado() == EstadoRespuesta.EXITO && resp.getResultado() != null) {
            Type listType = new TypeToken<List<UsuarioDTO>>() {}.getType();
            return gson.fromJson(resp.getResultado(), listType);
        } else {
            // ¡Aquí destapamos el error que venía oculto!
            System.err.println("[CLIENTE] Error del servidor al listar los proveedores: " + resp.getMensajeError() + " [Código: " + resp.getCodigoError() + "]");
        }
        return List.of();
    }

    /**
     * Ejecuta de forma asíncrona la solicitud para registrar la venta de una unidad física de mercancía,
     * enviando los parámetros seleccionados en la interfaz por medio del Broker.
     * 
     * @param numeroSerie El número de serie único de la mercancía.
     * @param idTienda    El identificador único de la tienda destino.
     * @param idUsuario   El identificador único del usuario proveedor que registra la venta.
     */
    public void ejecutarRegistroVenta(String numeroSerie, Long idTienda, Long idUsuario) {
        if (numeroSerie == null || numeroSerie.trim().isEmpty() || idTienda == null || idUsuario == null) {
            vista.mostrarMensajeError("Debe seleccionar una unidad de mercancía, una tienda y un usuario proveedor.");
            return;
        }

        vista.mostrarIndicadorCarga("Registrando venta de la unidad...", true);

        new SwingWorker<Respuesta, Void>() {
            @Override
            protected Respuesta doInBackground() throws Exception {
                JsonObject params = new JsonObject();
                params.addProperty("numero_serie", numeroSerie);
                params.addProperty("id_tienda", idTienda);
                params.addProperty("id_usuario", idUsuario);

                Solicitud solicitud = Solicitud.crear(
                        UUID.randomUUID().toString(),
                        Servicios.GESTOR_TIENDAS,
                        Operaciones.ACTUALIZAR_TIENDA_VENTA,
                        params
                );

                return clienteTcp.enviarSolicitud(solicitud);
            }

            @Override
            protected void done() {
                try {
                    Respuesta resp = get();
                    if (resp != null && resp.getEstado() == EstadoRespuesta.EXITO) {
                        String fecha = "N/D";
                        if (resp.getResultado() != null && resp.getResultado().isJsonObject()) {
                            JsonObject resObj = resp.getResultado().getAsJsonObject();
                            if (resObj.has("fecha_venta")) {
                                fecha = resObj.get("fecha_venta").getAsString();
                            }
                        }
                        vista.mostrarMensajeExito("¡Venta registrada con éxito!\nUnidad: " + numeroSerie + "\nFecha: " + fecha);
                        RegistroVentaController.this.cargarListasIniciales();
                    } else {
                        String errorMsg = resp != null ? resp.getMensajeError() : "Respuesta nula del servidor";
                        vista.mostrarMensajeError("Error del servidor: " + errorMsg);
                        vista.mostrarIndicadorCarga("Error en registro", false);
                    }
                } catch (Exception e) {
                    vista.mostrarDialogoReintentar(
                            "Error de red o timeout al registrar la venta.\nDetalle: " + e.getMessage(),
                            () -> ejecutarRegistroVenta(numeroSerie, idTienda, idUsuario)
                    );
                }
            }
        }.execute();
    }
}