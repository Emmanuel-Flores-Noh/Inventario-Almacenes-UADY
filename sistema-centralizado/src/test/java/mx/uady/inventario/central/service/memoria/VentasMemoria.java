package mx.uady.inventario.central.service.memoria;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import mx.uady.inventario.central.model.VentaHistorial;
import mx.uady.inventario.central.repository.VentaResultado;
import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;
import mx.uady.inventario.common.modelo.TiendaAlmacen;
import mx.uady.inventario.common.modelo.Usuario;
import mx.uady.inventario.common.modelo.Venta;

/**
 * Ventas sobre {@link AlmacenMemoria}: registra la venta, marca la unidad como vendida y
 * ofrece el historial y los catálogos, con las mismas reglas que {@code VentaRepository}.
 */
public final class VentasMemoria implements ConsultasVentaService, VentaService.VentaRegistrador {

    private final AlmacenMemoria almacen;

    public VentasMemoria(AlmacenMemoria almacen) {
        this.almacen = Objects.requireNonNull(almacen, "almacen");
    }

    @Override
    public VentaResultado registrar(String numeroSerie, int idTienda, int idUsuario) {
        almacen.simularLatencia();
        synchronized (almacen) {
            Mercancia unidad = almacen.mercancia.get(AlmacenMemoria.clave(numeroSerie));
            if (unidad == null) {
                throw new IllegalArgumentException("La mercancía no existe");
            }
            if (unidad.estado() != EstadoMercancia.EN_ALMACEN) {
                throw new IllegalArgumentException("La mercancía ya fue vendida");
            }
            if (almacen.tiendas.stream().noneMatch(t -> t.idTienda() == idTienda)) {
                throw new IllegalArgumentException("La tienda no existe");
            }
            if (almacen.usuarios.stream().noneMatch(u -> u.idUsuario() == idUsuario)) {
                throw new IllegalArgumentException("El usuario no existe");
            }
            LocalDateTime ahora = LocalDateTime.now().withNano(0);
            almacen.ventas.add(new Venta(almacen.ventas.size() + 1, unidad.numeroSerie(), idTienda, idUsuario, ahora));
            almacen.agregarMercancia(new Mercancia(unidad.numeroSerie(), unidad.idProducto(),
                    EstadoMercancia.VENDIDO, unidad.rutaQr()));
            return new VentaResultado(unidad.numeroSerie(), idTienda, ahora);
        }
    }

    @Override
    public List<VentaHistorial> listarHistorial() {
        almacen.simularLatencia();
        synchronized (almacen) {
            return almacen.ventas.stream()
                    .sorted(Comparator.comparing(Venta::fechaVenta).thenComparing(Venta::idVenta).reversed())
                    .map(this::aHistorial)
                    .toList();
        }
    }

    @Override
    public List<TiendaAlmacen> listarTiendas() {
        synchronized (almacen) {
            return almacen.tiendas.stream().sorted(Comparator.comparing(TiendaAlmacen::nombre)).toList();
        }
    }

    @Override
    public List<Usuario> listarUsuarios() {
        synchronized (almacen) {
            return almacen.usuarios.stream().sorted(Comparator.comparing(Usuario::nombres)).toList();
        }
    }

    private VentaHistorial aHistorial(Venta venta) {
        Mercancia unidad = almacen.mercancia.get(AlmacenMemoria.clave(venta.numeroSerie()));
        Producto producto = almacen.productos.get(unidad.idProducto());
        String tienda = almacen.tiendas.stream().filter(t -> t.idTienda() == venta.idTienda())
                .map(TiendaAlmacen::nombre).findFirst().orElse("Tienda #" + venta.idTienda());
        String usuario = almacen.usuarios.stream().filter(u -> u.idUsuario() == venta.idUsuario())
                .map(ConsultasVentaService::nombreCompleto).findFirst().orElse("Usuario #" + venta.idUsuario());
        return new VentaHistorial(venta.idVenta(), venta.numeroSerie(), producto.nombre(), tienda, usuario,
                venta.fechaVenta(), unidad.idProducto());
    }
}
