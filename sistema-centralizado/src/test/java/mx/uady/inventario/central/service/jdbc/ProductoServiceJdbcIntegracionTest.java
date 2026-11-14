package mx.uady.inventario.central.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import mx.uady.inventario.central.repository.HistorialPrecioRepository;
import mx.uady.inventario.central.repository.ProductoRepository;
import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.common.modelo.Producto;

/** Requiere MariaDB con el esquema y las variables {@code DB_*}; no modifica los datos del seed. */
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Requiere MariaDB real y las variables DB_* definidas")
class ProductoServiceJdbcIntegracionTest {

    private static final String HISTORIAL = "SELECT COUNT(*) FROM historial_precio WHERE id_producto = ?";

    private final BaseDatosDePrueba base = new BaseDatosDePrueba();
    private final ProductoServiceJdbc servicio = new ProductoServiceJdbc(base.conexiones);

    @AfterEach
    void limpiar() throws SQLException {
        base.close();
    }

    @Test
    void creaUnProductoNormalizadoQueApareceConCeroUnidades() {
        Producto nuevo = servicio.guardar(new Producto(0, "  " + base.prefijo + " Monitor  ", "  ",
                new BigDecimal("3200.50")));
        base.recordar(nuevo.idProducto());

        assertTrue(nuevo.idProducto() > 0);
        assertEquals(base.prefijo + " Monitor", nuevo.nombre());
        assertNull(nuevo.descripcion());
        assertTrue(servicio.listarConUnidades().stream()
                .anyMatch(f -> f.producto().idProducto() == nuevo.idProducto() && f.unidades() == 0));
    }

    @Test
    void cambiarElPrecioGuardaElAnteriorEnElHistorial() throws SQLException {
        int id = base.crearProducto("Silla", "3499.00");

        servicio.guardar(new Producto(id, base.prefijo + " Silla", null, new BigDecimal("3999.00")));

        assertEquals(1, base.contar(HISTORIAL, id));
        assertEquals(0, new BigDecimal("3999.00").compareTo(base.precioEnBase(id)));
        try (Connection c = base.conexiones.abrir()) {
            assertEquals(0, new BigDecimal("3499.00")
                    .compareTo(new HistorialPrecioRepository().listar(c, id).get(0).precioHistorico()));
        }
    }

    @Test
    void sinCambioDePrecioNoAgregaHistorial() throws SQLException {
        int id = base.crearProducto("Mesa", "100.00");

        servicio.guardar(new Producto(id, base.prefijo + " Mesa renombrada", null, new BigDecimal("100")));

        assertEquals(0, base.contar(HISTORIAL, id));
    }

    @Test
    void siFallaLaActualizacionTampocoQuedaElHistorial() throws SQLException {
        int id = base.crearProducto("Lampara", "50.00");
        ProductoRepository queFalla = new ProductoRepository() {
            @Override
            public boolean actualizar(Connection conexion, Producto producto) throws SQLException {
                throw new SQLException("fallo simulado después de insertar el historial");
            }
        };
        ProductoServiceJdbc conFallo = new ProductoServiceJdbc(base.conexiones, queFalla,
                new HistorialPrecioRepository());

        assertThrows(AccesoDatosException.class,
                () -> conFallo.guardar(new Producto(id, base.prefijo + " Lampara", null, new BigDecimal("75"))));

        assertEquals(0, base.contar(HISTORIAL, id), "El historial debió deshacerse con el rollback");
        assertEquals(0, new BigDecimal("50.00").compareTo(base.precioEnBase(id)));
    }

    @Test
    void editarUnProductoInexistenteEsErrorDeNegocio() {
        assertThrows(NegocioException.class,
                () -> servicio.guardar(new Producto(Integer.MAX_VALUE, "Fantasma", null, BigDecimal.ONE)));
    }
}
