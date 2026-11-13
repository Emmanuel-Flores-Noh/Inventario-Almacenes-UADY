package mx.uady.inventario.central.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import mx.uady.inventario.central.model.VentaHistorial;
import mx.uady.inventario.central.repository.VentaRepository;
import mx.uady.inventario.central.repository.VentaRepositoryException;

/** Requiere MariaDB con el esquema y las variables {@code DB_*}; no modifica los datos del seed. */
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Requiere MariaDB real y las variables DB_* definidas")
class ConsultasVentaJdbcIntegracionTest {

    private final BaseDatosDePrueba base = new BaseDatosDePrueba();
    private final ConsultasVentaJdbc consultas = new ConsultasVentaJdbc(base.conexiones);

    @AfterEach
    void limpiar() throws SQLException {
        try (Connection c = base.conexiones.abrir();
             PreparedStatement s = c.prepareStatement("DELETE FROM ventas WHERE numero_serie LIKE ?")) {
            s.setString(1, base.prefijo + "%");
            s.executeUpdate();
        }
        base.close();
    }

    @Test
    void losCatalogosCoincidenConElSeed() {
        assertEquals(3, consultas.listarTiendas().size());
        assertEquals(4, consultas.listarUsuarios().size());
        assertEquals("Almacen Central UADY", consultas.listarTiendas().get(0).nombre());
    }

    @Test
    void elHistorialUneProductoTiendaYUsuario() throws SQLException, VentaRepositoryException {
        int idProducto = base.crearProducto("Proyector", "100.00");
        String serie = base.prefijo + "-V1";
        try (Connection c = base.conexiones.abrir();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO mercancia (numero_serie, id_producto, estado) VALUES (?, ?, 'EN_ALMACEN')")) {
            s.setString(1, serie);
            s.setInt(2, idProducto);
            s.executeUpdate();
        }

        new VentaRepository(base.conexiones).registrarVenta(serie, 2, 3);

        List<VentaHistorial> historial = consultas.listarHistorial();
        VentaHistorial venta = historial.stream().filter(v -> v.numeroSerie().equals(serie)).findFirst()
                .orElseThrow();
        assertEquals(base.prefijo + " Proyector", venta.producto());
        assertEquals("Tienda Facultad de Ingenieria", venta.tienda());
        assertEquals("Lucero Chan Canul", venta.usuario());
        assertEquals(idProducto, venta.idProducto());
        assertTrue(historial.indexOf(venta) == 0, "La venta más reciente debe aparecer primero");
    }
}
