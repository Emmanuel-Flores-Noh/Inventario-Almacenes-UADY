package mx.uady.inventario.central.controller;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import mx.uady.inventario.central.repository.ProveedorConexion;
import mx.uady.inventario.central.repository.VentaRepository;
import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.service.jdbc.ConsultasVentaJdbc;
import mx.uady.inventario.central.service.jdbc.MercanciaServiceJdbc;
import mx.uady.inventario.central.service.jdbc.ProductoServiceJdbc;
import mx.uady.inventario.central.util.ConfiguracionBD;

/**
 * Único lugar donde se construyen los servicios del Sistema 1.
 * La interfaz y el servidor de red comparten el mismo contexto para trabajar sobre los mismos datos.
 */
public record AppContext(ProductoService productoService, MercanciaService mercanciaService,
        VentaService ventaService, ConsultasVentaService consultasVenta) {

    /** Variable de entorno con la carpeta donde se guardan los PNG de los QR. */
    public static final String VAR_DIRECTORIO_QR = "QR_DIR";
    /** Carpeta del contenedor Docker, montada en el volumen {@code qr_storage}. */
    public static final String DIRECTORIO_QR_POR_DEFECTO = "/app/storage/qr";

    public AppContext {
        Objects.requireNonNull(productoService, "productoService");
        Objects.requireNonNull(mercanciaService, "mercanciaService");
        Objects.requireNonNull(ventaService, "ventaService");
        Objects.requireNonNull(consultasVenta, "consultasVenta");
    }

    /** Servicios sobre MariaDB con la configuración indicada. */
    public static AppContext jdbc(ConfiguracionBD configuracion, Path directorioQr) {
        ProveedorConexion conexiones = ProveedorConexion.de(Objects.requireNonNull(configuracion, "configuracion"));
        QrService qr = new QrService(Objects.requireNonNull(directorioQr, "directorioQr"));
        return new AppContext(
                new ProductoServiceJdbc(conexiones),
                new MercanciaServiceJdbc(conexiones, qr),
                new VentaService(new VentaRepository(conexiones)::registrarVenta),
                new ConsultasVentaJdbc(conexiones));
    }

    /** Carpeta de los QR según {@code QR_DIR}, o {@value #DIRECTORIO_QR_POR_DEFECTO} si no está definida. */
    public static Path directorioQr(Map<String, String> entorno) {
        String valor = entorno.get(VAR_DIRECTORIO_QR);
        return Path.of(valor == null || valor.isBlank() ? DIRECTORIO_QR_POR_DEFECTO : valor.strip());
    }
}
