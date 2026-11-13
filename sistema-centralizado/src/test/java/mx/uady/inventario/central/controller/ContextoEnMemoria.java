package mx.uady.inventario.central.controller;

import java.nio.file.Path;
import java.time.Duration;

import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.service.memoria.AlmacenMemoria;
import mx.uady.inventario.central.service.memoria.MercanciaServiceMemoria;
import mx.uady.inventario.central.service.memoria.ProductoServiceMemoria;
import mx.uady.inventario.central.service.memoria.VentasMemoria;

/** {@link AppContext} con los datos de {@code 02_seed.sql} en memoria, para pruebas sin base de datos. */
public final class ContextoEnMemoria {

    private ContextoEnMemoria() {
    }

    public static AppContext crear(Path directorioQr, Duration latencia) {
        AlmacenMemoria almacen = AlmacenMemoria.conDatosDeEjemplo(latencia);
        VentasMemoria ventas = new VentasMemoria(almacen);
        return new AppContext(new ProductoServiceMemoria(almacen),
                new MercanciaServiceMemoria(almacen, new QrService(directorioQr)),
                new VentaService(ventas), ventas);
    }
}
