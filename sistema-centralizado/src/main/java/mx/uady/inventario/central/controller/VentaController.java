package mx.uady.inventario.central.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.gson.JsonObject;

import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.VentaPanel;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.TiendaAlmacen;
import mx.uady.inventario.common.modelo.Usuario;

/**
 * Coordina la pantalla "Registrar venta": carga las unidades disponibles, las tiendas y los
 * usuarios, y registra la venta con {@link VentaService} en segundo plano.
 */
public final class VentaController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MercanciasCargador cargarMercancias;
    private final CatalogosCargador cargarCatalogos;
    private final VentaService ventaService;
    private final VentaPanel vista;
    private Predicate<String> confirmar;
    private Runnable alRegistrarVenta = () -> { };

    /** Hay una carga en curso; las peticiones que lleguen mientras tanto se agrupan en una sola. */
    private boolean cargando;
    private boolean recargaPendiente;
    private String avisoTrasCargar;

    public VentaController(MercanciasCargador cargarMercancias, CatalogosCargador cargarCatalogos,
            VentaService ventaService, VentaPanel vista) {
        this.cargarMercancias = Objects.requireNonNull(cargarMercancias, "cargarMercancias");
        this.cargarCatalogos = Objects.requireNonNull(cargarCatalogos, "cargarCatalogos");
        this.ventaService = Objects.requireNonNull(ventaService, "ventaService");
        this.vista = Objects.requireNonNull(vista, "vista");
        this.confirmar = pregunta -> Mensajes.confirmar(vista, pregunta);

        vista.alActualizar(this::cargar);
        vista.alVender(this::vender);
    }

    /** Sustituye el diálogo de confirmación (p. ej. en pruebas sin pantalla). */
    public void setConfirmar(Predicate<String> confirmar) {
        this.confirmar = Objects.requireNonNull(confirmar, "confirmar");
    }

    /** Se invoca tras registrar una venta (para refrescar Mercancía, Productos e Historial). */
    public void setAlRegistrarVenta(Runnable accion) {
        this.alRegistrarVenta = Objects.requireNonNull(accion, "accion");
    }

    /** Recarga unidades disponibles, tiendas y usuarios sin bloquear el hilo de Swing. */
    public void cargar() {
        if (cargando) {
            recargaPendiente = true;
            return;
        }
        cargando = true;
        vista.setEstado("Cargando unidades disponibles…");

        TareaAsync.de(() -> new Datos(cargarMercancias.cargar(), cargarCatalogos.cargar()))
                .bloqueando(vista.controles())
                .alTerminar(datos -> {
                    vista.mostrarMercancias(datos.mercancias().mercancias(), datos.mercancias().nombresProducto());
                    vista.mostrarTiendas(datos.catalogos().tiendas());
                    vista.mostrarUsuarios(datos.catalogos().usuarios());
                    if (avisoTrasCargar != null) {
                        vista.setEstado(avisoTrasCargar);
                        avisoTrasCargar = null;
                    }
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
        Mercancia unidad = vista.seleccionada().orElse(null);
        TiendaAlmacen tienda = vista.tiendaSeleccionada().orElse(null);
        Usuario usuario = vista.usuarioSeleccionado().orElse(null);
        if (unidad == null || tienda == null || usuario == null) {
            return;
        }
        String producto = vista.nombreProducto(unidad.idProducto());
        if (!confirmar.test("¿Registrar la venta de " + unidad.numeroSerie() + " (" + producto + ") en "
                + tienda.nombre() + " a nombre de " + ConsultasVentaService.nombreCompleto(usuario) + "?")) {
            return;
        }

        JsonObject parametros = new JsonObject();
        parametros.addProperty("numero_serie", unidad.numeroSerie());
        parametros.addProperty("id_tienda", tienda.idTienda());
        parametros.addProperty("id_usuario", usuario.idUsuario());

        TareaAsync.de(() -> registrar(parametros))
                .bloqueando(vista.controles())
                .alTerminar(resultado -> {
                    avisoTrasCargar = "Venta registrada: " + unidad.numeroSerie() + " en " + tienda.nombre()
                            + " (" + fecha(resultado) + ").";
                    cargar();
                    alRegistrarVenta.run();
                })
                .ejecutar(vista);
    }

    /** VentaService indica las reglas incumplidas con IllegalArgumentException; se muestran tal cual. */
    private JsonObject registrar(JsonObject parametros) throws Exception {
        try {
            return ventaService.registrarVenta(parametros);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage() + ".", e);
        }
    }

    private static String fecha(JsonObject resultado) {
        try {
            return LocalDateTime.parse(resultado.get("fecha_venta").getAsString()).format(FECHA);
        } catch (RuntimeException e) {
            return "ahora";
        }
    }

    private void terminarCarga() {
        cargando = false;
        if (recargaPendiente) {
            recargaPendiente = false;
            cargar();
        }
    }

    private record Datos(DatosMercancias mercancias, Catalogos catalogos) {
    }

    /** Unidades disponibles para vender y el nombre de cada producto. */
    public record DatosMercancias(List<Mercancia> mercancias, Map<Integer, String> nombresProducto) {
        public DatosMercancias {
            mercancias = List.copyOf(mercancias);
            nombresProducto = Map.copyOf(nombresProducto);
        }
    }

    /** Tiendas y usuarios que se pueden elegir al vender. */
    public record Catalogos(List<TiendaAlmacen> tiendas, List<Usuario> usuarios) {
        public Catalogos {
            tiendas = List.copyOf(tiendas);
            usuarios = List.copyOf(usuarios);
        }
    }

    @FunctionalInterface
    public interface MercanciasCargador {
        DatosMercancias cargar() throws Exception;
    }

    @FunctionalInterface
    public interface CatalogosCargador {
        Catalogos cargar() throws Exception;
    }
}
