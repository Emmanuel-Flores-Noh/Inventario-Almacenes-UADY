package mx.uady.inventario.central.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.gson.JsonObject;

import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.VentaPanel;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.TiendaAlmacen;

/**
 * Coordina la pantalla de ventas con sus proveedores de datos y el servicio de venta.
 * Los proveedores se inyectan para que la persistencia JDBC pertenezca a su propio Issue.
 */
public final class VentaController {

    private final MercanciasCargador cargarMercancias;
    private final TiendasCargador cargarTiendas;
    private final VentaService ventaService;
    private final VentaPanel vista;
    private final Consumer<String> mostrarConfirmacion;
    private final QrAsegurador asegurarQr;
    private final Consumer<Mercancia> mostrarQr;

    private boolean cargando;
    private boolean recargaPendiente;

    public VentaController(MercanciasCargador cargarMercancias, TiendasCargador cargarTiendas,
            VentaService ventaService, VentaPanel vista) {
        this(cargarMercancias, cargarTiendas, ventaService, vista,
                texto -> Mensajes.mostrarInfo(vista, texto),
                numeroSerie -> null,
                unidad -> { });
    }

    public VentaController(MercanciasCargador cargarMercancias, TiendasCargador cargarTiendas,
            VentaService ventaService, VentaPanel vista, Consumer<String> mostrarConfirmacion) {
        this(cargarMercancias, cargarTiendas, ventaService, vista, mostrarConfirmacion,
                numeroSerie -> null, unidad -> { });
    }

    public VentaController(MercanciasCargador cargarMercancias, TiendasCargador cargarTiendas,
            VentaService ventaService, VentaPanel vista, Consumer<String> mostrarConfirmacion,
            QrAsegurador asegurarQr, Consumer<Mercancia> mostrarQr) {
        this.cargarMercancias = Objects.requireNonNull(cargarMercancias, "cargarMercancias");
        this.cargarTiendas = Objects.requireNonNull(cargarTiendas, "cargarTiendas");
        this.ventaService = Objects.requireNonNull(ventaService, "ventaService");
        this.vista = Objects.requireNonNull(vista, "vista");
        this.mostrarConfirmacion = Objects.requireNonNull(mostrarConfirmacion, "mostrarConfirmacion");
        this.asegurarQr = Objects.requireNonNull(asegurarQr, "asegurarQr");
        this.mostrarQr = Objects.requireNonNull(mostrarQr, "mostrarQr");

        vista.alActualizar(this::cargar);
        vista.alVender(this::vender);
    }

    /** Carga mercancías y tiendas sin bloquear el hilo de Swing. */
    public void cargar() {
        if (cargando) {
            recargaPendiente = true;
            return;
        }
        cargando = true;
        vista.setEstado("Cargando mercancías y tiendas...");

        TareaAsync.de(() -> new Datos(cargarMercancias.cargar(), cargarTiendas.cargar()))
                .bloqueando(vista.controles())
                .alTerminar(datos -> {
                    vista.mostrarMercancias(datos.mercancias().mercancias(),
                            datos.mercancias().nombresProducto());
                    vista.mostrarTiendas(datos.tiendas());
                    terminarCarga();
                })
                .alFallar(error -> {
                    vista.setEstado("No se pudieron cargar los datos de ventas.");
                    Mensajes.mostrarError(vista, error);
                    terminarCarga();
                })
                .ejecutar(vista);
    }

    private void vender() {
        Mercancia mercancia = vista.seleccionada().orElse(null);
        TiendaAlmacen tienda = vista.tiendaSeleccionada().orElse(null);
        if (mercancia == null || tienda == null) {
            return;
        }

        int idUsuario;
        try {
            idUsuario = vista.idUsuario();
        } catch (NumberFormatException error) {
            vista.setEstado("El ID del usuario debe ser un entero positivo.");
            return;
        }

        if (!Mensajes.confirmar(vista,
                "¿Registrar la venta de " + mercancia.numeroSerie() + " en " + tienda.nombre() + "?")) {
            return;
        }

        JsonObject parametros = new JsonObject();
        parametros.addProperty("numero_serie", mercancia.numeroSerie());
        parametros.addProperty("id_tienda", tienda.idTienda());
        parametros.addProperty("id_usuario", idUsuario);

        TareaAsync.de(() -> ventaService.registrarVenta(parametros))
                .bloqueando(vista.controles())
                .alTerminar(resultado -> {
                    mostrarConfirmacion.accept("Venta registrada: " + resultado.get("numero_serie").getAsString()
                            + " en " + resultado.get("id_tienda").getAsInt() + ".");
                    abrirQrDespuesDeVenta(mercancia);
                })
                .alFallar(error -> Mensajes.mostrarError(vista, error))
                .ejecutar(vista);
    }

    private void abrirQrDespuesDeVenta(Mercancia mercancia) {
        TareaAsync.de(() -> asegurarQr.asegurar(mercancia.numeroSerie()))
                .bloqueando(vista.controles())
                .alTerminar(unidad -> {
                    mostrarQr.accept(unidad);
                    cargar();
                })
                .alFallar(error -> {
                    Mensajes.mostrarError(vista, error);
                    cargar();
                })
                .ejecutar(vista);
    }

    private void terminarCarga() {
        cargando = false;
        if (recargaPendiente) {
            recargaPendiente = false;
            cargar();
        }
    }

    private record Datos(DatosMercancias mercancias, List<TiendaAlmacen> tiendas) {
    }

    public record DatosMercancias(List<Mercancia> mercancias, Map<Integer, String> nombresProducto) {
        public DatosMercancias {
            mercancias = List.copyOf(mercancias);
            nombresProducto = Map.copyOf(nombresProducto);
        }
    }

    @FunctionalInterface
    public interface MercanciasCargador {
        DatosMercancias cargar() throws Exception;
    }

    @FunctionalInterface
    public interface TiendasCargador {
        List<TiendaAlmacen> cargar() throws Exception;
    }

    @FunctionalInterface
    public interface QrAsegurador {
        Mercancia asegurar(String numeroSerie) throws Exception;
    }
}
