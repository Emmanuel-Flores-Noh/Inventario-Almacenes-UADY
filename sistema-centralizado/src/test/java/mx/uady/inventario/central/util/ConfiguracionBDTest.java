package mx.uady.inventario.central.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionBDTest {

    @Test
    void construyeUrlConTodasLasVariables() {
        ConfiguracionBD configuracion = ConfiguracionBD.desdeMapa(Map.of(
                "DB_HOST", "mariadb",
                "DB_PORT", "3307",
                "DB_NAME", "inventario",
                "DB_USER", "uady_user",
                "DB_PASSWORD", "secreto"));

        assertEquals("jdbc:mariadb://mariadb:3307/inventario", configuracion.url());
        assertEquals("uady_user", configuracion.usuario());
        assertEquals("secreto", configuracion.contrasena());
    }

    @Test
    void usaValoresPorDefectoParaHostPuertoYNombre() {
        ConfiguracionBD configuracion = ConfiguracionBD.desdeMapa(Map.of(
                "DB_USER", "uady_user",
                "DB_PASSWORD", "secreto"));

        assertEquals("jdbc:mariadb://localhost:3306/almacenes_uady", configuracion.url());
    }

    @Test
    void exigeUsuarioYContrasena() {
        Map<String, String> sinUsuario = new HashMap<>();
        sinUsuario.put("DB_PASSWORD", "secreto");
        IllegalStateException errorUsuario = assertThrows(IllegalStateException.class,
                () -> ConfiguracionBD.desdeMapa(sinUsuario));
        assertTrue(errorUsuario.getMessage().contains("DB_USER"), errorUsuario.getMessage());

        Map<String, String> sinContrasena = new HashMap<>();
        sinContrasena.put("DB_USER", "uady_user");
        IllegalStateException errorContrasena = assertThrows(IllegalStateException.class,
                () -> ConfiguracionBD.desdeMapa(sinContrasena));
        assertTrue(errorContrasena.getMessage().contains("DB_PASSWORD"), errorContrasena.getMessage());
    }

    @Test
    void rechazaPuertoInvalido() {
        Map<String, String> variables = Map.of(
                "DB_PORT", "no-es-un-puerto",
                "DB_USER", "uady_user",
                "DB_PASSWORD", "secreto");

        assertThrows(IllegalArgumentException.class, () -> ConfiguracionBD.desdeMapa(variables));
    }
}
