package mx.uady.inventario.central.service;

/** Reglas de captura de una unidad física, alineadas con la tabla {@code mercancia}. */
public final class MercanciaValidator {

    static final int LONGITUD_MAXIMA_SERIE = 100;

    private MercanciaValidator() {
    }

    /**
     * Normaliza el número de serie (sin espacios al inicio o al final) y lo valida.
     *
     * @return el número de serie normalizado
     * @throws ValidacionException si está vacío o es demasiado largo
     */
    public static String normalizarSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isBlank()) {
            throw new ValidacionException("El número de serie es obligatorio.");
        }
        String serie = numeroSerie.strip();
        if (serie.length() > LONGITUD_MAXIMA_SERIE) {
            throw new ValidacionException(
                    "El número de serie admite como máximo " + LONGITUD_MAXIMA_SERIE + " caracteres.");
        }
        return serie;
    }
}
