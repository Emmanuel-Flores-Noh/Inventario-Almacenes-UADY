package mx.uady.inventario.central.service.memoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.QrService;

/**
 * {@link QrService} provisional: codifica el número de serie en un PNG con ZXing.
 * Se sustituye por la implementación del issue #4 cuando esté disponible.
 */
public final class QrServiceTemporal implements QrService {

    private static final int TAMANO_PX = 300;

    private final Path directorio;

    public QrServiceTemporal(Path directorio) {
        this.directorio = Objects.requireNonNull(directorio, "directorio");
    }

    @Override
    public Path generar(String numeroSerie) {
        Objects.requireNonNull(numeroSerie, "numeroSerie");
        try {
            Files.createDirectories(directorio);
            BitMatrix matriz = new QRCodeWriter().encode(numeroSerie, BarcodeFormat.QR_CODE,
                    TAMANO_PX, TAMANO_PX, Map.of(EncodeHintType.CHARACTER_SET, "UTF-8"));
            Path archivo = directorio.resolve(nombreArchivo(numeroSerie));
            MatrixToImageWriter.writeToPath(matriz, "PNG", archivo);
            return archivo;
        } catch (IOException | WriterException e) {
            throw new AccesoDatosException("No se pudo generar el QR de " + numeroSerie, e);
        }
    }

    /** Nombre de archivo seguro para cualquier sistema operativo. */
    static String nombreArchivo(String numeroSerie) {
        return numeroSerie.replaceAll("[^A-Za-z0-9._-]", "_") + ".png";
    }
}
