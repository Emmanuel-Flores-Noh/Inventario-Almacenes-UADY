package mx.uady.inventario.central.controller;

import java.util.Objects;

import javax.swing.SwingUtilities;

import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.ProductoDialog;
import mx.uady.inventario.central.view.ProductosPanel;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Coordina la pantalla de productos con {@link ProductoService}.
 * Todas las llamadas al servicio se hacen con {@link TareaAsync} para no congelar la interfaz.
 */
public final class ProductoController {

    private final ProductoService servicio;
    private final ProductosPanel vista;
    private Runnable alCambiarProductos = () -> { };

    /** Hay una carga en curso; las peticiones que lleguen mientras tanto se agrupan en una sola. */
    private boolean cargando;
    private boolean recargaPendiente;
    private Integer idASeleccionar;

    public ProductoController(ProductoService servicio, ProductosPanel vista) {
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.vista = Objects.requireNonNull(vista, "vista");

        vista.alNuevo(() -> abrirFormulario(null));
        vista.alEditar(fila -> abrirFormulario(fila.producto()));
        vista.alActualizar(this::cargar);
    }

    /** Se invoca tras crear o editar un producto (p. ej. para refrescar la pantalla de mercancía). */
    public void setAlCambiarProductos(Runnable accion) {
        this.alCambiarProductos = Objects.requireNonNull(accion, "accion");
    }

    /** Recarga la tabla de productos. Debe llamarse desde el hilo de Swing. */
    public void cargar() {
        cargar(null);
    }

    private void cargar(Integer idASeleccionar) {
        if (idASeleccionar != null) {
            this.idASeleccionar = idASeleccionar;
        }
        // Dos cargas a la vez dejarían los botones en un estado incorrecto al restaurarlos.
        if (cargando) {
            recargaPendiente = true;
            return;
        }
        cargando = true;
        vista.setEstado("Cargando productos…");
        TareaAsync.de(servicio::listarConUnidades)
                .bloqueando(vista.controles())
                .alTerminar(productos -> {
                    vista.mostrarProductos(productos);
                    if (this.idASeleccionar != null) {
                        vista.seleccionarProducto(this.idASeleccionar);
                        this.idASeleccionar = null;
                    }
                    terminarCarga();
                })
                .alFallar(error -> {
                    vista.setEstado("No se pudieron cargar los productos.");
                    Mensajes.mostrarError(vista, error);
                    terminarCarga();
                })
                .ejecutar(vista);
    }

    private void terminarCarga() {
        cargando = false;
        if (recargaPendiente) {
            recargaPendiente = false;
            cargar(null);
        }
    }

    private void abrirFormulario(Producto inicial) {
        ProductoDialog dialogo = new ProductoDialog(SwingUtilities.getWindowAncestor(vista), inicial);
        dialogo.alGuardar(producto -> guardar(dialogo, producto));
        dialogo.setVisible(true);
    }

    private void guardar(ProductoDialog dialogo, Producto producto) {
        // Si falla, Mensajes muestra el error sobre el diálogo y este sigue abierto para corregir.
        TareaAsync.de(() -> servicio.guardar(producto))
                .bloqueando(dialogo.controles())
                .alTerminar(guardado -> {
                    dialogo.dispose();
                    cargar(guardado.idProducto());
                    alCambiarProductos.run();
                })
                .ejecutar(dialogo);
    }
}
