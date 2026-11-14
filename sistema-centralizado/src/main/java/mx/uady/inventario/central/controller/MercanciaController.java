package mx.uady.inventario.central.controller;

import java.util.List;
import java.util.Objects;

import javax.swing.SwingUtilities;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.central.service.ValidacionException;
import mx.uady.inventario.central.view.MercanciaPanel;
import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.components.QrPreviewDialog;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Coordina la pantalla de mercancía con {@link MercanciaService} y {@link ProductoService}.
 * Todas las llamadas a servicios se hacen con {@link TareaAsync} para no congelar la interfaz.
 */
public final class MercanciaController {

    private final MercanciaService mercancia;
    private final ProductoService productos;
    private final MercanciaPanel vista;
    private Runnable alCambiarUnidades = () -> { };

    /** Hay una carga en curso; las peticiones que lleguen mientras tanto se agrupan en una sola. */
    private boolean cargando;
    private boolean recargaPendiente;
    private String serieASeleccionar;
    private String avisoTrasCargar;

    private record Datos(List<Producto> productos, List<Mercancia> unidades) { }

    public MercanciaController(MercanciaService mercancia, ProductoService productos, MercanciaPanel vista) {
        this.mercancia = Objects.requireNonNull(mercancia, "mercancia");
        this.productos = Objects.requireNonNull(productos, "productos");
        this.vista = Objects.requireNonNull(vista, "vista");

        vista.alRegistrar(this::registrar);
        vista.alCambiarFiltros(this::cargar);
        vista.alVerQr(this::verQr);
    }

    /** Se invoca tras registrar una unidad (p. ej. para refrescar las unidades en Productos). */
    public void setAlCambiarUnidades(Runnable accion) {
        this.alCambiarUnidades = Objects.requireNonNull(accion, "accion");
    }

    /** Recarga los productos de los combos y las unidades con los filtros actuales. Hilo de Swing. */
    public void cargar() {
        if (cargando) {
            recargaPendiente = true;
            return;
        }
        cargando = true;
        Integer idProducto = vista.filtroProductoSeleccionado();
        EstadoMercancia estado = vista.filtroEstadoSeleccionado();
        vista.setEstado("Cargando unidades…");

        TareaAsync.de(() -> new Datos(
                        productos.listarConUnidades().stream().map(ProductoConUnidades::producto).toList(),
                        mercancia.listar(idProducto, estado)))
                .bloqueando(vista.controles())
                .alTerminar(datos -> {
                    vista.mostrarProductos(datos.productos());
                    vista.mostrarUnidades(datos.unidades(), datos.productos());
                    seleccionarYAvisar();
                    terminarCarga();
                })
                .alFallar(error -> {
                    vista.setEstado("No se pudieron cargar las unidades.");
                    Mensajes.mostrarError(vista, error);
                    terminarCarga();
                })
                .ejecutar(vista);
    }

    private void seleccionarYAvisar() {
        if (serieASeleccionar != null) {
            boolean visible = vista.seleccionarUnidad(serieASeleccionar);
            if (avisoTrasCargar != null) {
                vista.setEstado(avisoTrasCargar + (visible ? "" : " No se muestra con los filtros actuales."));
            }
        }
        serieASeleccionar = null;
        avisoTrasCargar = null;
    }

    private void terminarCarga() {
        cargando = false;
        if (recargaPendiente) {
            recargaPendiente = false;
            cargar();
        }
    }

    private void registrar() {
        Producto producto = vista.productoParaRegistrar().orElse(null);
        if (producto == null) {
            vista.mostrarErrorSerie("Elige el producto de la unidad.");
            return;
        }
        String serie = vista.serieCapturada();

        TareaAsync.de(() -> mercancia.registrar(producto.idProducto(), serie))
                .bloqueando(vista.controles())
                .alTerminar(unidad -> {
                    vista.limpiarSerie();
                    serieASeleccionar = unidad.numeroSerie();
                    avisoTrasCargar = "Se registró " + unidad.numeroSerie() + " (" + producto.nombre()
                            + ") en almacén.";
                    cargar();
                    alCambiarUnidades.run();
                })
                .alFallar(error -> {
                    // Los errores de la serie se muestran junto al campo; el resto, en un diálogo.
                    if (error instanceof SerieDuplicadaException || error instanceof ValidacionException) {
                        vista.mostrarErrorSerie(error.getMessage());
                    } else {
                        Mensajes.mostrarError(vista, error);
                    }
                })
                .ejecutar(vista);
    }

    private void verQr() {
        Mercancia seleccionada = vista.seleccionada().orElse(null);
        if (seleccionada == null) {
            return;
        }
        // Las unidades sin PNG (p. ej. las del seed) lo generan antes de abrir la vista previa.
        TareaAsync.de(() -> mercancia.asegurarQr(seleccionada.numeroSerie()))
                .bloqueando(vista.controles())
                .alTerminar(unidad -> {
                    vista.actualizarUnidad(unidad);
                    vista.seleccionarUnidad(unidad.numeroSerie());
                    abrirVistaPrevia(unidad);
                })
                .ejecutar(vista);
    }

    private void abrirVistaPrevia(Mercancia unidad) {
        try {
            new QrPreviewDialog(SwingUtilities.getWindowAncestor(vista), unidad.numeroSerie(),
                    unidad.idProducto(), vista.nombreProducto(unidad.idProducto()), unidad.rutaQr())
                    .setVisible(true);
        } catch (IllegalArgumentException e) {
            Mensajes.mostrarError(vista, e);
        }
    }
}
