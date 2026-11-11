package mx.uady.inventario.common.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Fabrica centralizada de instancias Gson para todo el proyecto.
 */
public final class GsonFactory {

    private GsonFactory() {}

    private static Gson create() {
        return new GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    public static final Gson DEFAULT = create();
}
