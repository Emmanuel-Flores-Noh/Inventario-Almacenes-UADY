package mx.uady.inventario.central.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utilidades para codificar datos de trazabilidad en imágenes QR.
 */
public final class QrUtil {

    private QrUtil() {
    }

    /**
     * Codifica el contenido indicado y lo guarda como una imagen PNG.
     *
     * @param contenido contenido que se incrustará en el código QR
     * @param destino ruta del archivo PNG de salida
     * @param ancho ancho de la imagen en píxeles
     * @param alto alto de la imagen en píxeles
     * @throws WriterException si ZXing no puede codificar el contenido
     * @throws IOException si no se puede escribir el archivo
     */
    public static void escribirPng(String contenido, Path destino, int ancho, int alto) throws WriterException, IOException{
        Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        BitMatrix matriz = new MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE, ancho, alto,hints);
        MatrixToImageWriter.writeToPath(matriz, "PNG", destino);
    }
       
        
}
