package mx.uady.inventario.central.controller;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import mx.uady.inventario.central.view.MercanciaPanel;
import mx.uady.inventario.central.view.ProductosPanel;
import mx.uady.inventario.central.view.VentaPanel;
import mx.uady.inventario.central.view.VentanaPrincipal;
import mx.uady.inventario.central.view.components.QrPreviewDialog;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.TiendaAlmacen;

/**
 * Arranca la interfaz Swing del Administrador.
 * En entornos sin pantalla (p. ej. el contenedor Docker) no hace nada,
 * para que el servidor del Sistema 1 pueda arrancar igualmente.
 */
public final class UiLauncher {

    private UiLauncher() {
    }

    /**
     * Abre la ventana principal en el hilo de eventos de Swing.
     *
     * @return {@code true} si se abrió la interfaz, {@code false} si el entorno es headless.
     */
    public static boolean iniciar() {
        return iniciar(new VentaService());
    }

    /**
     * Abre la interfaz usando el servicio de venta ya configurado por el arranque.
     *
     * @param ventaService servicio transaccional de ventas
     * @return {@code true} si se abrió la interfaz, {@code false} si el entorno es headless
     */
    public static boolean iniciar(VentaService ventaService) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[UI] Entorno sin pantalla: interfaz gráfica deshabilitada.");
            return false;
        }
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            abrirVentana(AppContext.enMemoria(), ventaService);
        });
        return true;
    }

    /** Construye las pantallas, conecta sus controladores y muestra la ventana. Hilo de Swing. */
    static VentanaPrincipal abrirVentana(AppContext contexto) {
        return abrirVentana(contexto, new VentaService());
    }

    static VentanaPrincipal abrirVentana(AppContext contexto, VentaService ventaService) {
        VentanaPrincipal ventana = new VentanaPrincipal();

        ProductosPanel productos = new ProductosPanel();
        ProductoController productoController = new ProductoController(contexto.productoService(), productos);
        ventana.setPestana("Productos", productos);

        MercanciaPanel mercancia = new MercanciaPanel();
        MercanciaController mercanciaController = new MercanciaController(
                contexto.mercanciaService(), contexto.productoService(), mercancia);
        ventana.setPestana("Mercancía", mercancia);

        VentaPanel ventas = new VentaPanel();
        VentaController ventasController = new VentaController(
                () -> {
                    var productosConUnidades = contexto.productoService().listarConUnidades();
                    var disponibles = contexto.mercanciaService().listar(null, EstadoMercancia.EN_ALMACEN);
                    var nombres = productosConUnidades.stream().collect(
                            java.util.stream.Collectors.toMap(
                                    producto -> producto.producto().idProducto(),
                                    producto -> producto.producto().nombre(),
                                    (primero, segundo) -> primero));
                    return new VentaController.DatosMercancias(disponibles, nombres);
                },
                () -> java.util.List.of(
                        new TiendaAlmacen(1, "Tienda principal", "Datos temporales de Fase 3")),
                ventaService,
                ventas,
                texto -> mx.uady.inventario.central.view.Mensajes.mostrarInfo(ventas, texto),
                numeroSerie -> contexto.mercanciaService().asegurarQr(numeroSerie),
                unidad -> {
                    try {
                        new QrPreviewDialog(
                                SwingUtilities.getWindowAncestor(ventas),
                                unidad.numeroSerie(),
                                unidad.idProducto(),
                                ventas.nombreProducto(unidad.idProducto()),
                                unidad.rutaQr()).setVisible(true);
                    } catch (IllegalArgumentException error) {
                        mx.uady.inventario.central.view.Mensajes.mostrarError(ventas, error);
                    }
                });
        ventana.setPestana("Ventas", ventas);

        // Un producto nuevo aparece en los combos de Mercancía; una unidad nueva cambia el conteo en Productos.
        productoController.setAlCambiarProductos(mercanciaController::cargar);
        mercanciaController.setAlCambiarUnidades(productoController::cargar);

        productoController.cargar();
        mercanciaController.cargar();
        ventasController.cargar();

        ventana.setVisible(true);
        return ventana;
    }
}
