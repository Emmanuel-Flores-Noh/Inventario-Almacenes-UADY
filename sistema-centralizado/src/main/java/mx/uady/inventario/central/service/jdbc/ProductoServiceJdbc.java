package mx.uady.inventario.central.service.jdbc;

import java.util.List;
import java.util.Objects;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.central.repository.HistorialPrecioRepository;
import mx.uady.inventario.central.repository.ProductoRepository;
import mx.uady.inventario.central.repository.ProveedorConexion;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.ProductoValidator;
import mx.uady.inventario.common.modelo.Producto;

/** {@link ProductoService} sobre MariaDB. */
public final class ProductoServiceJdbc implements ProductoService {

    private final Transacciones transacciones;
    private final ProductoRepository productos;
    private final HistorialPrecioRepository historial;

    public ProductoServiceJdbc(ProveedorConexion conexiones) {
        this(conexiones, new ProductoRepository(), new HistorialPrecioRepository());
    }

    /** Permite sustituir los repositorios en pruebas (p. ej. para forzar un fallo a mitad de la transacción). */
    public ProductoServiceJdbc(ProveedorConexion conexiones, ProductoRepository productos,
            HistorialPrecioRepository historial) {
        this.transacciones = new Transacciones(conexiones);
        this.productos = Objects.requireNonNull(productos, "productos");
        this.historial = Objects.requireNonNull(historial, "historial");
    }

    @Override
    public List<ProductoConUnidades> listarConUnidades() {
        return transacciones.leer("listar los productos", productos::listarConUnidades);
    }

    @Override
    public Producto guardar(Producto producto) {
        Producto limpio = ProductoValidator.normalizar(producto);

        if (limpio.idProducto() == 0) {
            return transacciones.escribir("crear el producto", c -> productos.insertar(c, limpio));
        }
        return transacciones.escribir("actualizar el producto", c -> {
            // El bloqueo evita que dos ediciones simultáneas registren mal el precio anterior.
            Producto actual = productos.buscar(c, limpio.idProducto(), true)
                    .orElseThrow(() -> new NegocioException(
                            "El producto ya no existe; actualiza la lista e inténtalo de nuevo."));
            if (actual.precioActual().compareTo(limpio.precioActual()) != 0) {
                historial.insertar(c, actual.idProducto(), actual.precioActual());
            }
            if (!productos.actualizar(c, limpio)) {
                throw new NegocioException("El producto ya no existe; actualiza la lista e inténtalo de nuevo.");
            }
            return limpio;
        });
    }
}
