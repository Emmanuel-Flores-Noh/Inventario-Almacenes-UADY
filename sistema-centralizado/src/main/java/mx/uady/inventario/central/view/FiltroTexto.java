package mx.uady.inventario.central.view;

import java.text.Normalizer;
import java.util.Locale;

/** Comparación de textos para búsquedas: sin distinguir mayúsculas ni acentos. */
public final class FiltroTexto {

    private FiltroTexto() {
    }

    /** {@code true} si {@code texto} contiene {@code busqueda}; una búsqueda vacía coincide con todo. */
    public static boolean contiene(String texto, String busqueda) {
        String patron = normalizar(busqueda);
        return patron.isEmpty() || normalizar(texto).contains(patron);
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
