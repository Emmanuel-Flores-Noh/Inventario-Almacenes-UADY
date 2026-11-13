package mx.uady.inventario.central.service.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.NegocioException;

class TransaccionesTest {

    private final List<String> llamadas = new ArrayList<>();
    private final Transacciones transacciones = new Transacciones(this::conexionFalsa);

    @Test
    void escribirHaceCommitAlTerminarBien() {
        assertEquals("ok", transacciones.escribir("probar", c -> "ok"));
        assertEquals(List.of("setAutoCommit(false)", "commit", "close"), llamadas);
    }

    @Test
    void escribirHaceRollbackYPropagaLosErroresDeNegocio() {
        NegocioException negocio = new NegocioException("regla");
        assertSame(negocio, assertThrows(NegocioException.class,
                () -> transacciones.escribir("probar", c -> { throw negocio; })));
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), llamadas);
    }

    @Test
    void escribirHaceRollbackYConvierteLosErroresSql() {
        SQLException sql = new SQLException("falla");
        AccesoDatosException e = assertThrows(AccesoDatosException.class,
                () -> transacciones.escribir("guardar algo", c -> { throw sql; }));
        assertSame(sql, e.getCause());
        assertEquals("No se pudo guardar algo", e.getMessage());
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), llamadas);
    }

    @Test
    void leerNoAbreTransaccionYConvierteLosErroresSql() {
        Integer resultado = transacciones.leer("contar", c -> 7);
        assertEquals(7, resultado);
        assertEquals(List.of("close"), llamadas);

        assertThrows(AccesoDatosException.class,
                () -> transacciones.leer("contar", c -> { throw new SQLException("falla"); }));
    }

    @Test
    void siNoSePuedeAbrirLaConexionEsErrorDeAccesoADatos() {
        Transacciones sinBase = new Transacciones(() -> {
            throw new SQLException("Socket fail to connect", "08000");
        });
        assertThrows(AccesoDatosException.class, () -> sinBase.escribir("guardar", c -> 1));
    }

    private Connection conexionFalsa() {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, metodo, args) -> {
                    String nombre = metodo.getName();
                    llamadas.add(nombre.equals("setAutoCommit") ? "setAutoCommit(" + args[0] + ")" : nombre);
                    return null;
                });
    }
}
