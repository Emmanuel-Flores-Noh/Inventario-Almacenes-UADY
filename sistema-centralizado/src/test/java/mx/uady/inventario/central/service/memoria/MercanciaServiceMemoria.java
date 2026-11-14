package mx.uady.inventario.central.service.memoria;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.MercanciaValidator;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.QrUnidades;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/** {@link MercanciaService} sobre {@link AlmacenMemoria}, para pruebas sin base de datos. */
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
            if (QrUnidades.existe(unidad.rutaQr())) {
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

    private String generarQr(String serie, Producto producto) {
        return QrUnidades.generar(qrService, serie, producto);
    }
}
