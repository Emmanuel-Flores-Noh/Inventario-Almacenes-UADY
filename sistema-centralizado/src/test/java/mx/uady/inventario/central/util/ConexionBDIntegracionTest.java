package mx.uady.inventario.central.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integracion contra una MariaDB real.
 *
 * <p>Se activa solo cuando {@code DB_PASSWORD} esta definida, para que la suite
 * normal (`mvn test`) siga funcionando sin base de datos. Con MariaDB levantada
 * se ejecuta con las mismas variables {@code DB_*} que usa el Sistema 1.</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Requiere MariaDB real y las variables DB_* definidas")
class ConexionBDIntegracionTest {

    @Test
    void conectaYEncuentraLaSemilla() throws SQLException {
        ConfiguracionBD configuracion = ConfiguracionBD.desdeEntorno();

        try (Connection conexion = ConexionBD.abrir(configuracion);
             Statement sentencia = conexion.createStatement()) {

            try (ResultSet resultado = sentencia.executeQuery("SELECT 1")) {
                assertTrue(resultado.next(), "La consulta SELECT 1 no devolvio filas");
                assertEquals(1, resultado.getInt(1));
            }

            assertEquals(3, contar(sentencia, "roles"));
            assertEquals(4, contar(sentencia, "usuarios"));
            assertEquals(3, contar(sentencia, "tiendas_almacenes"));
            assertEquals(5, contar(sentencia, "productos"));
            assertEquals(10, contar(sentencia, "mercancia"));
        }
    }

    private static int contar(Statement sentencia, String tabla) throws SQLException {
        // El nombre de la tabla viene de constantes de la prueba, no de entrada externa.
        try (ResultSet resultado = sentencia.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            assertTrue(resultado.next(), "La tabla " + tabla + " no devolvio conteo");
            return resultado.getInt(1);
        }
    }
}
