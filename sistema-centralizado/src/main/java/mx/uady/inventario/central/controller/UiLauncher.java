package mx.uady.inventario.central.controller;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.central.view.MercanciaPanel;
import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.ProductosPanel;
import mx.uady.inventario.central.view.VentaHistorialPanel;
import mx.uady.inventario.central.view.VentaPanel;
import mx.uady.inventario.central.view.VentanaPrincipal;
import mx.uady.inventario.central.view.components.QrPreviewDialog;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

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
     * @param contexto los mismos servicios que usa el servidor de red, para trabajar sobre los mismos datos
     * @return {@code true} si se abrió la interfaz, {@code false} si el entorno es headless
     */
    public static boolean iniciar(AppContext contexto) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[UI] Entorno sin pantalla: interfaz gráfica deshabilitada.");
            return false;
        }
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            abrirVentana(contexto);
        });
        return true;
    }

    /** Construye las pantallas, conecta sus controladores y muestra la ventana. Hilo de Swing. */
    static VentanaPrincipal abrirVentana(AppContext contexto) {
        VentanaPrincipal ventana = new VentanaPrincipal();

        ProductosPanel productos = new ProductosPanel();
        ProductoController productoController = new ProductoController(contexto.productoService(), productos);
        ventana.setPestana("Productos", productos);

        MercanciaPanel mercancia = new MercanciaPanel();
        MercanciaController mercanciaController = new MercanciaController(
                contexto.mercanciaService(), contexto.productoService(), mercancia);
        ventana.setPestana("Mercancía", mercancia);

        VentaPanel ventas = new VentaPanel();
        VentaController ventaController = new VentaController(
                () -> new VentaController.DatosMercancias(
                        contexto.mercanciaService().listar(null, EstadoMercancia.EN_ALMACEN),
                        nombresProducto(contexto)),
                () -> new VentaController.Catalogos(
                        contexto.consultasVenta().listarTiendas(), contexto.consultasVenta().listarUsuarios()),
                contexto.ventaService(),
                ventas);
        VentaHistorialPanel historial = new VentaHistorialPanel();
        VentaHistorialController historialController = new VentaHistorialController(
                contexto.consultasVenta()::listarHistorial,
                historial,
                contexto.mercanciaService()::asegurarQr,
                (venta, unidad) -> mostrarQr(historial, unidad, venta.producto()));
        JTabbedPane secciones = new JTabbedPane();
        secciones.addTab("Registrar venta", ventas);
        secciones.addTab("Historial", historial);
        ventana.setPestana("Ventas", secciones);

        // Cada cambio refresca las pestañas cuyos datos dependen de él.
        productoController.setAlCambiarProductos(() -> {
            mercanciaController.cargar();
            ventaController.cargar();
            historialController.cargar();
        });
        mercanciaController.setAlCambiarUnidades(() -> {
            productoController.cargar();
            ventaController.cargar();
        });
        ventaController.setAlRegistrarVenta(() -> {
            mercanciaController.cargar();
            productoController.cargar();
            historialController.cargar();
        });

        productoController.cargar();
        mercanciaController.cargar();
        ventaController.cargar();
        historialController.cargar();

        ventana.setVisible(true);
        return ventana;
    }

    private static Map<Integer, String> nombresProducto(AppContext contexto) {
        return contexto.productoService().listarConUnidades().stream()
                .map(ProductoConUnidades::producto)
                .collect(Collectors.toMap(Producto::idProducto, Producto::nombre, (a, b) -> a));
    }

    private static void mostrarQr(Component origen, Mercancia unidad, String producto) {
        try {
            new QrPreviewDialog(SwingUtilities.getWindowAncestor(origen), unidad.numeroSerie(),
                    unidad.idProducto(), producto, unidad.rutaQr()).setVisible(true);
        } catch (IllegalArgumentException error) {
            Mensajes.mostrarError(origen, error);
        }
    }
}
