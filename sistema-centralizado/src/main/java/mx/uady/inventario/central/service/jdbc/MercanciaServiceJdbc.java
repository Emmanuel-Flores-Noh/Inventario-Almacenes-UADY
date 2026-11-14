package mx.uady.inventario.central.service.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import mx.uady.inventario.central.repository.MercanciaRepository;
import mx.uady.inventario.central.repository.ProductoRepository;
import mx.uady.inventario.central.repository.ProveedorConexion;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.MercanciaValidator;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.QrUnidades;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/** {@link MercanciaService} sobre MariaDB, con el QR generado por el {@link QrService} del #4. */
public final class MercanciaServiceJdbc implements MercanciaService {

    private final Transacciones transacciones;
    private final QrService qrService;
    private final MercanciaRepository mercancia = new MercanciaRepository();
    private final ProductoRepository productos = new ProductoRepository();

    public MercanciaServiceJdbc(ProveedorConexion conexiones, QrService qrService) {
        this.transacciones = new Transacciones(conexiones);
        this.qrService = Objects.requireNonNull(qrService, "qrService");
    }

    @Override
    public List<Mercancia> listar(Integer idProducto, EstadoMercancia estado) {
        return transacciones.leer("listar las unidades", c -> mercancia.listar(c, idProducto, estado));
    }

    /**
     * Inserta la unidad, genera su QR y guarda la ruta en una sola transacción.
     * Si algo falla se hace rollback y se borra el PNG que se hubiera generado.
     */
    @Override
    public Mercancia registrar(int idProducto, String numeroSerie) {
        String serie = MercanciaValidator.normalizarSerie(numeroSerie);
        AtomicReference<String> pngGenerado = new AtomicReference<>();
        try {
            return transacciones.escribir("registrar la unidad " + serie, c -> {
                Producto producto = productos.buscar(c, idProducto, false)
                        .orElseThrow(() -> new NegocioException(
                                "El producto seleccionado ya no existe; actualiza la lista."));
                insertar(c, new Mercancia(serie, idProducto, EstadoMercancia.EN_ALMACEN, null));

                String rutaQr = QrUnidades.generar(qrService, serie, producto);
                pngGenerado.set(rutaQr);
                mercancia.actualizarRutaQr(c, serie, rutaQr);
                return new Mercancia(serie, idProducto, EstadoMercancia.EN_ALMACEN, rutaQr);
            });
        } catch (RuntimeException e) {
            QrUnidades.borrarSilenciosamente(pngGenerado.get());
            throw e;
        }
    }

    @Override
    public Mercancia asegurarQr(String numeroSerie) {
        Objects.requireNonNull(numeroSerie, "numeroSerie");
        return transacciones.escribir("generar el QR de " + numeroSerie, c -> {
            Mercancia unidad = mercancia.buscar(c, numeroSerie, true)
                    .orElseThrow(() -> new NegocioException(
                            "La unidad " + numeroSerie + " ya no existe; actualiza la lista."));
            if (QrUnidades.existe(unidad.rutaQr())) {
                return unidad;
            }
            Producto producto = productos.buscar(c, unidad.idProducto(), false)
                    .orElseThrow(() -> new NegocioException(
                            "El producto de la unidad " + numeroSerie + " ya no existe."));
            String rutaQr = QrUnidades.generar(qrService, unidad.numeroSerie(), producto);
            mercancia.actualizarRutaQr(c, unidad.numeroSerie(), rutaQr);
            return new Mercancia(unidad.numeroSerie(), unidad.idProducto(), unidad.estado(), rutaQr);
        });
    }

    private void insertar(Connection conexion, Mercancia unidad) throws SQLException {
        try {
            mercancia.insertar(conexion, unidad);
        } catch (SQLException e) {
            // La intercalación utf8mb4_unicode_ci hace que "sn-1" y "SN-1" choquen igual que en memoria.
            if (MercanciaRepository.esClaveDuplicada(e)) {
                throw new SerieDuplicadaException(unidad.numeroSerie(), e);
            }
            throw e;
        }
    }
}
