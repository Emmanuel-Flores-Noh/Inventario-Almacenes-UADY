package mx.uady.inventario.central.repository;

import mx.uady.inventario.central.util.ConfiguracionBD;
import mx.uady.inventario.central.util.ConexionBD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica la transacción contra una MariaDB real cuando el entorno está disponible.
 */
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Requiere MariaDB real y las variables DB_* definidas")
class VentaRepositoryIntegracionTest {

    private static final String NUMERO_SERIE = "SN-UADY-0001";

    @AfterEach
    void restaurarMercancia() throws Exception {
        try (Connection conexion = ConexionBD.abrir(ConfiguracionBD.desdeEntorno());
             PreparedStatement eliminar = conexion.prepareStatement(
                     "DELETE FROM ventas WHERE numero_serie = ?");
             PreparedStatement restaurar = conexion.prepareStatement(
                     "UPDATE mercancia SET estado = 'EN_ALMACEN' WHERE numero_serie = ?")) {
            eliminar.setString(1, NUMERO_SERIE);
            eliminar.executeUpdate();
            restaurar.setString(1, NUMERO_SERIE);
            restaurar.executeUpdate();
        }
    }

    @Test
    void soloUnaVentaConcurrentePuedeBloquearLaMismaMercancia() throws Exception {
        VentaRepository repositorio = new VentaRepository();
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int usuario = i + 1;
                resultados.add(ejecutor.submit(() -> {
                    inicio.await();
                    try {
                        repositorio.registrarVenta(NUMERO_SERIE, 1, usuario);
                        return true;
                    } catch (IllegalArgumentException esperado) {
                        return false;
                    }
                }));
            }
            inicio.countDown();

            long exitos = 0;
            for (Future<Boolean> resultado : resultados) {
                try {
                    if (resultado.get()) {
                        exitos++;
                    }
                } catch (ExecutionException error) {
                    throw new AssertionError("Falló una transacción concurrente", error.getCause());
                }
            }
            assertEquals(1, exitos);
        } finally {
            ejecutor.shutdownNow();
        }
    }
}
