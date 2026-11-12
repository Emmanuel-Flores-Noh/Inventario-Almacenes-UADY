package mx.uady.inventario.central.controller;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.memoria.AlmacenMemoria;
import mx.uady.inventario.central.service.memoria.MercanciaServiceMemoria;
import mx.uady.inventario.central.service.memoria.ProductoServiceMemoria;
import mx.uady.inventario.central.service.memoria.QrServiceTemporal;

/**
 * Único lugar donde se construyen los servicios que usa la interfaz.
 * Al integrar la persistencia (#2) y el QR (#4) solo cambia la fábrica que se invoca aquí.
 */
public record AppContext(ProductoService productoService, MercanciaService mercanciaService, QrService qrService) {

    /** Latencia simulada para comprobar que la interfaz no se congela. */
    private static final Duration LATENCIA_SIMULADA = Duration.ofMillis(400);

    public AppContext {
        Objects.requireNonNull(productoService, "productoService");
        Objects.requireNonNull(mercanciaService, "mercanciaService");
        Objects.requireNonNull(qrService, "qrService");
    }

    /** Servicios en memoria con los datos de ejemplo de {@code 02_seed.sql}. */
    public static AppContext enMemoria() {
        AlmacenMemoria almacen = AlmacenMemoria.conDatosDeEjemplo(LATENCIA_SIMULADA);
        QrService qr = new QrServiceTemporal(Path.of(System.getProperty("java.io.tmpdir"), "inventario-uady", "qr"));
        return new AppContext(new ProductoServiceMemoria(almacen), new MercanciaServiceMemoria(almacen, qr), qr);
    }
}
