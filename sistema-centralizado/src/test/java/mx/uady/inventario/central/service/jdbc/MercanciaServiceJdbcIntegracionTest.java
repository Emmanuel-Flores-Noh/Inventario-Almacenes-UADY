package mx.uady.inventario.central.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

/** Requiere MariaDB con el esquema y las variables {@code DB_*}; no modifica los datos del seed. */
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Requiere MariaDB real y las variables DB_* definidas")
class MercanciaServiceJdbcIntegracionTest {

    private static final String UNIDADES = "SELECT COUNT(*) FROM mercancia WHERE id_producto = ?";

    @TempDir
    Path directorioQr;

    private final BaseDatosDePrueba base = new BaseDatosDePrueba();
    private MercanciaServiceJdbc servicio;
    private int idProducto;

    @BeforeEach
    void preparar() throws SQLException {
        servicio = new MercanciaServiceJdbc(base.conexiones, new QrService(directorioQr));
        idProducto = base.crearProducto("Proyector", "12500.50");
    }

    @AfterEach
    void limpiar() throws SQLException {
        base.close();
    }

    private String serie(String sufijo) {
        return base.prefijo + "-" + sufijo;
    }

    @Test
    void registraLaUnidadEnAlmacenYGuardaLaRutaDelQr() throws SQLException {
        Mercancia unidad = servicio.registrar(idProducto, "  " + serie("A") + " ");

        assertEquals(serie("A"), unidad.numeroSerie());
        assertEquals(EstadoMercancia.EN_ALMACEN, unidad.estado());
        assertEquals(directorioQr.resolve(serie("A") + ".png").toString(), unidad.rutaQr());
        assertTrue(Files.isRegularFile(Path.of(unidad.rutaQr())));
        assertEquals(unidad.rutaQr(), base.rutaQrEnBase(serie("A")));
    }

    @Test
    void rechazaUnaSerieDuplicadaSinImportarMayusculas() throws SQLException {
        servicio.registrar(idProducto, serie("B"));

        assertThrows(SerieDuplicadaException.class,
                () -> servicio.registrar(idProducto, serie("B").toLowerCase()));
        assertEquals(1, base.contar(UNIDADES, idProducto));
    }

    @Test
    void siFallaElQrSeDeshaceElRegistro() throws Exception {
        Path noEsCarpeta = Files.createFile(directorioQr.resolve("ocupado"));
        MercanciaServiceJdbc conQrRoto = new MercanciaServiceJdbc(base.conexiones, new QrService(noEsCarpeta));

        assertThrows(AccesoDatosException.class, () -> conQrRoto.registrar(idProducto, serie("C")));

        assertEquals(0, base.contar(UNIDADES, idProducto), "La unidad debió deshacerse con el rollback");
    }

    @Test
    void rechazaUnProductoInexistente() {
        assertThrows(NegocioException.class, () -> servicio.registrar(Integer.MAX_VALUE, serie("D")));
    }

    @Test
    void asegurarQrGeneraYGuardaLaRutaDeUnaUnidadSinQr() throws SQLException {
        insertarSinQr(serie("E"));
        assertNull(base.rutaQrEnBase(serie("E")));

        Mercancia conQr = servicio.asegurarQr(serie("E"));

        assertTrue(Files.isRegularFile(Path.of(conQr.rutaQr())));
        assertEquals(conQr.rutaQr(), base.rutaQrEnBase(serie("E")));
        assertEquals(conQr, servicio.asegurarQr(serie("E")), "Si el PNG existe no debe regenerarse");
    }

    @Test
    void listaFiltrandoPorProductoYEstado() throws SQLException {
        servicio.registrar(idProducto, serie("F"));
        insertarSinQr(serie("G"));
        try (Connection c = base.conexiones.abrir();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE mercancia SET estado = 'VENDIDO' WHERE numero_serie = ?")) {
            s.setString(1, serie("G"));
            s.executeUpdate();
        }

        assertEquals(2, servicio.listar(idProducto, null).size());
        assertEquals(serie("G"), servicio.listar(idProducto, EstadoMercancia.VENDIDO).get(0).numeroSerie());
        assertEquals(1, servicio.listar(idProducto, EstadoMercancia.EN_ALMACEN).size());
    }

    private void insertarSinQr(String numeroSerie) throws SQLException {
        try (Connection c = base.conexiones.abrir();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO mercancia (numero_serie, id_producto, estado) VALUES (?, ?, 'EN_ALMACEN')")) {
            s.setString(1, numeroSerie);
            s.setInt(2, idProducto);
            s.executeUpdate();
        }
    }
}
