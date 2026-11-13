package mx.uady.inventario.central.controller;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.service.memoria.AlmacenMemoria;
import mx.uady.inventario.central.service.memoria.MercanciaServiceMemoria;
import mx.uady.inventario.central.service.memoria.ProductoServiceMemoria;
import mx.uady.inventario.central.service.memoria.VentasMemoria;

/**
 * Único lugar donde se construyen los servicios que usa la interfaz.
 * Todas las pestañas deben compartir el mismo contexto para ver los mismos datos.
 */
public record AppContext(ProductoService productoService, MercanciaService mercanciaService,
        VentaService ventaService, ConsultasVentaService consultasVenta) {

    /** Latencia simulada para comprobar que la interfaz no se congela. */
    private static final Duration LATENCIA_SIMULADA = Duration.ofMillis(400);

    public AppContext {
        Objects.requireNonNull(productoService, "productoService");
        Objects.requireNonNull(mercanciaService, "mercanciaService");
        Objects.requireNonNull(ventaService, "ventaService");
        Objects.requireNonNull(consultasVenta, "consultasVenta");
    }

    /**
     * Servicios en memoria con los datos de ejemplo de {@code 02_seed.sql}.
     * Usa el {@link QrService} real (#4) escribiendo en una carpeta temporal local.
     */
    public static AppContext enMemoria() {
        AlmacenMemoria almacen = AlmacenMemoria.conDatosDeEjemplo(LATENCIA_SIMULADA);
        QrService qr = new QrService(Path.of(System.getProperty("java.io.tmpdir"), "inventario-uady", "qr"));
        VentasMemoria ventas = new VentasMemoria(almacen);
        return new AppContext(new ProductoServiceMemoria(almacen), new MercanciaServiceMemoria(almacen, qr),
                new VentaService(ventas), ventas);
    }
}
