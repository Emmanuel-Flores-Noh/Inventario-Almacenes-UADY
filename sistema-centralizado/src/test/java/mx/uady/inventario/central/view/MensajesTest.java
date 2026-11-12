package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.NegocioException;

class MensajesTest {

    @Test
    void usaElMensajeDeLaExcepcionDeNegocio() {
        assertEquals("El número de serie ya existe.",
                Mensajes.describir(new NegocioException("El número de serie ya existe.")));
    }

    @Test
    void encuentraLaExcepcionDeNegocioEnvuelta() {
        Throwable envuelta = new ExecutionException(new RuntimeException(new NegocioException("Precio inválido.")));
        assertEquals("Precio inválido.", Mensajes.describir(envuelta));
    }

    @Test
    void traduceErroresDeBaseDeDatosAMensajeDeConexion() {
        assertEquals(Mensajes.ERROR_CONEXION, Mensajes.describir(new SQLException("Connection refused")));
        assertEquals(Mensajes.ERROR_CONEXION,
                Mensajes.describir(new RuntimeException(new ConnectException("Connection refused"))));
    }

    @Test
    void distingueFallosDeAlmacenamientoDeLosDeConexion() {
        assertEquals(Mensajes.ERROR_ALMACENAMIENTO,
                Mensajes.describir(new AccesoDatosException("QR", new java.io.IOException("disco lleno"))));
        assertEquals(Mensajes.ERROR_CONEXION,
                Mensajes.describir(new AccesoDatosException("listar", new SQLException("Connection refused"))));
    }

    @Test
    void ocultaLosDetallesDeErroresInesperados() {
        assertEquals(Mensajes.ERROR_INESPERADO, Mensajes.describir(new NullPointerException("campo x")));
    }
}
