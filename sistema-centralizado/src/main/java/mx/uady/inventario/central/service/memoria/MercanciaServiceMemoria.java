package mx.uady.inventario.central.service.memoria;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.MercanciaValidator;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

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
            if (!almacen.productos.containsKey(idProducto)) {
                throw new NegocioException("El producto seleccionado ya no existe; actualiza la lista.");
            }
            if (almacen.mercancia.containsKey(AlmacenMemoria.clave(serie))) {
                throw new SerieDuplicadaException(serie);
            }
            // Si el QR falla no se registra nada, igual que un rollback en la versión JDBC.
            Path qr = qrService.generar(serie);
            Mercancia unidad = new Mercancia(serie, idProducto, EstadoMercancia.EN_ALMACEN, qr.toString());
            almacen.agregarMercancia(unidad);
            return unidad;
        }
    }
}
