package mx.uady.inventario.central.service;

/** Ya existe una unidad registrada con el mismo número de serie. */
public class SerieDuplicadaException extends NegocioException {

    private final String numeroSerie;

    public SerieDuplicadaException(String numeroSerie) {
        this(numeroSerie, null);
    }

    public SerieDuplicadaException(String numeroSerie, Throwable causa) {
        super("Ya existe una unidad con el número de serie \"" + numeroSerie + "\".", causa);
        this.numeroSerie = numeroSerie;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }
}
