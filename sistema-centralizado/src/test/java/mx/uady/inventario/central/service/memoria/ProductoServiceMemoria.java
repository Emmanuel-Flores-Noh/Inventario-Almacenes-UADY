package mx.uady.inventario.central.service.memoria;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.ProductoValidator;
import mx.uady.inventario.common.modelo.HistorialPrecio;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/** {@link ProductoService} sobre {@link AlmacenMemoria}, para pruebas sin base de datos. */
public final class ProductoServiceMemoria implements ProductoService {

    private final AlmacenMemoria almacen;

    public ProductoServiceMemoria(AlmacenMemoria almacen) {
        this.almacen = Objects.requireNonNull(almacen, "almacen");
    }

    @Override
    public List<ProductoConUnidades> listarConUnidades() {
        almacen.simularLatencia();
        synchronized (almacen) {
            Map<Integer, Long> unidades = almacen.mercancia.values().stream()
                    .collect(Collectors.groupingBy(Mercancia::idProducto, Collectors.counting()));
            return almacen.productos.values().stream()
                    .sorted(Comparator.comparing(Producto::nombre, String.CASE_INSENSITIVE_ORDER))
                    .map(p -> new ProductoConUnidades(p, unidades.getOrDefault(p.idProducto(), 0L).intValue()))
                    .toList();
        }
    }

    @Override
    public Producto guardar(Producto producto) {
        Producto limpio = ProductoValidator.normalizar(producto);
        almacen.simularLatencia();

        String nombre = limpio.nombre();
        String descripcion = limpio.descripcion();

        synchronized (almacen) {
            if (producto.idProducto() == 0) {
                Producto nuevo = new Producto(almacen.nuevoIdProducto(), nombre, descripcion, producto.precioActual());
                almacen.productos.put(nuevo.idProducto(), nuevo);
                return nuevo;
            }

            Producto actual = almacen.productos.get(producto.idProducto());
            if (actual == null) {
                throw new NegocioException("El producto ya no existe; actualiza la lista e inténtalo de nuevo.");
            }
            if (actual.precioActual().compareTo(producto.precioActual()) != 0) {
                almacen.historial.add(new HistorialPrecio(almacen.nuevoIdHistorial(), actual.idProducto(),
                        actual.precioActual(), LocalDateTime.now()));
            }
            Producto actualizado = new Producto(actual.idProducto(), nombre, descripcion, producto.precioActual());
            almacen.productos.put(actualizado.idProducto(), actualizado);
            return actualizado;
        }
    }
}
