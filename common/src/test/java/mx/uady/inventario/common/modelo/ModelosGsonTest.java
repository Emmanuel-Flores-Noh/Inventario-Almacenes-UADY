package mx.uady.inventario.common.modelo;

import mx.uady.inventario.common.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelosGsonTest {

    @Test
    void roundTripProducto() {
        Producto original = new Producto(1, "Laptop", "Equipo", new BigDecimal("12345.67"));
        Producto copia = GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(original), Producto.class);
        assertEquals(original, copia);
    }

    @Test
    void roundTripMercancia() {
        Mercancia original = new Mercancia("SN-001", 1, "EN_ALMACEN", "/qr/SN-001.png");
        Mercancia copia = GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(original), Mercancia.class);
        assertEquals(original, copia);
    }

    @Test
    void roundTripHistorialPrecio() {
        HistorialPrecio original = new HistorialPrecio(1, 2, new BigDecimal("99.99"),
                LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        HistorialPrecio copia = GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(original), HistorialPrecio.class);
        assertEquals(original, copia);
    }

    @Test
    void roundTripVenta() {
        Venta original = new Venta(1, "SN-001", 3, 4, LocalDateTime.of(2026, 5, 6, 7, 8, 9));
        Venta copia = GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(original), Venta.class);
        assertEquals(original, copia);
    }

    @Test
    void roundTripRolUsuarioTienda() {
        Rol rol = new Rol(1, "Admin", "desc");
        Usuario u = new Usuario(2, 1, "Ana", "Paz", "Lopez");
        TiendaAlmacen t = new TiendaAlmacen(3, "Tienda Centro", "Calle 1");
        assertEquals(rol, GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(rol), Rol.class));
        assertEquals(u, GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(u), Usuario.class));
        assertEquals(t, GsonFactory.DEFAULT.fromJson(GsonFactory.DEFAULT.toJson(t), TiendaAlmacen.class));
    }
}
