package mx.uady.inventario.central.service.memoria;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.MercanciaValidator;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.QrGenerationException;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.central.service.ValidacionException;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/** {@link MercanciaService} sobre {@link AlmacenMemoria}; se sustituye por la versión JDBC en la Fase 6. */
public final class MercanciaServiceMemoria implements MercanciaService {

    private final AlmacenMemoria almacen;
    private final QrService qrService;

    public MercanciaServiceMemoria(AlmacenMemoria almacen, QrService qrService) {
        this.almacen = Objects.requireNonNull(almacen, "almacen");
        this.qrService = Objects.requireNonNull(qrService, "qrService");
    }

    @Override
    public List<Mercancia> listar(Integer idProducto, EstadoMercancia estado) {
        almacen.simularLatencia();
        synchronized (almacen) {
            return almacen.mercancia.values().stream()
                    .filter(m -> idProducto == null || m.idProducto() == idProducto)
                    .filter(m -> estado == null || m.estado() == estado)
                    .sorted(Comparator.comparing(Mercancia::numeroSerie, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    @Override
    public Mercancia registrar(int idProducto, String numeroSerie) {
        String serie = MercanciaValidator.normalizarSerie(numeroSerie);
        almacen.simularLatencia();

        synchronized (almacen) {
            Producto producto = almacen.productos.get(idProducto);
            if (producto == null) {
                throw new NegocioException("El producto seleccionado ya no existe; actualiza la lista.");
            }
            if (almacen.mercancia.containsKey(AlmacenMemoria.clave(serie))) {
                throw new SerieDuplicadaException(serie);
            }
            // Si el QR falla no se registra nada, igual que un rollback en la versión JDBC.
            String rutaQr = generarQr(serie, producto);
            Mercancia unidad = new Mercancia(serie, idProducto, EstadoMercancia.EN_ALMACEN, rutaQr);
            almacen.agregarMercancia(unidad);
            return unidad;
        }
    }

    @Override
    public Mercancia asegurarQr(String numeroSerie) {
        Objects.requireNonNull(numeroSerie, "numeroSerie");
        synchronized (almacen) {
            Mercancia unidad = almacen.mercancia.get(AlmacenMemoria.clave(numeroSerie));
            if (unidad == null) {
                throw new NegocioException("La unidad " + numeroSerie + " ya no existe; actualiza la lista.");
            }
            if (unidad.rutaQr() != null && Files.isRegularFile(Path.of(unidad.rutaQr()))) {
                return unidad;
            }
            Producto producto = almacen.productos.get(unidad.idProducto());
            if (producto == null) {
                throw new NegocioException("El producto de la unidad " + numeroSerie + " ya no existe.");
            }
            Mercancia conQr = new Mercancia(unidad.numeroSerie(), unidad.idProducto(), unidad.estado(),
                    generarQr(unidad.numeroSerie(), producto));
            almacen.agregarMercancia(conQr);
            return conQr;
        }
    }

    /**
     * Genera el QR con el servicio del #4 y traduce sus errores a los de la capa de servicio:
     * los de validación (sin causa) a {@link ValidacionException} y los de escritura a
     * {@link AccesoDatosException}.
     */
    private String generarQr(String serie, Producto producto) {
        try {
            return qrService.generarCodigoQR(serie, producto.idProducto(), producto.nombre());
        } catch (QrGenerationException e) {
            if (e.getCause() == null) {
                throw new ValidacionException(e.getMessage() + ".");
            }
            throw new AccesoDatosException("No se pudo generar el QR de " + serie, e);
        } catch (InvalidPathException e) {
            throw new ValidacionException(
                    "El número de serie contiene caracteres que no se pueden usar en el nombre del archivo del QR.");
        }
    }
}
