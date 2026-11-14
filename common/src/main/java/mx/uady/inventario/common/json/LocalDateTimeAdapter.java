package mx.uady.inventario.common.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Adaptador Gson para java.time.LocalDateTime (formato ISO-8601).
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Escribir un LocalDateTime al formato ISO-8601
     * @param out El json de salida
     * @param value the Java object to write. May be null.
     * @throws IOException
     */
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(FORMATTER));
        }
    }

    /**
     * Leer una fecha en formato ISO-8601 y transformarla en un LocalDateTime
     * @param in El JSON que se lee
     * @return La fecha transformada
     * @throws IOException
     */
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return LocalDateTime.parse(in.nextString(), FORMATTER);
    }
}
