package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FiltroTextoTest {

    @Test
    void ignoraMayusculasAcentosYEspacios() {
        assertTrue(FiltroTexto.contiene("Silla ergonómica", "ERGONOMICA"));
        assertTrue(FiltroTexto.contiene("Silla ergonomica", "  ergonómica "));
    }

    @Test
    void unaBusquedaVaciaCoincideConTodo() {
        assertTrue(FiltroTexto.contiene("Laptop", ""));
        assertTrue(FiltroTexto.contiene("Laptop", null));
    }

    @Test
    void noCoincideSiNoContieneElTexto() {
        assertFalse(FiltroTexto.contiene("Laptop", "proyector"));
        assertFalse(FiltroTexto.contiene(null, "x"));
    }
}
