package mx.uady.inventario.central.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrServiceTest {

    @Test
    void generaPngConPayloadYPermiteLeerSusBytes() throws Exception {
        Path directorio = Files.createTempDirectory("qr-test-");
        QrService servicio = new QrService(directorio);

        String ruta = servicio.generarCodigoQR("SN-001", 7, "Producto ágil");
        Path archivo = Path.of(ruta);

        assertEquals(directorio.resolve("SN-001.png"), archivo);
        assertEquals("ID: 7 | Producto: Producto ágil | SN: SN-001", leerPayload(archivo));
        assertArrayEquals(Files.readAllBytes(archivo), servicio.obtenerImagenQRBytes(ruta));
    }

    @Test
    void rechazaNumeroDeSerieQueIntentaSalirDelDirectorio() throws Exception {
        QrService servicio = new QrService(Files.createTempDirectory("qr-test-"));

        assertThrows(QrGenerationException.class,
                () -> servicio.generarCodigoQR("../fuera", 7, "Producto"));
    }

    private String leerPayload(Path archivo) throws Exception {
        BufferedImage imagen = ImageIO.read(archivo.toFile());
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(imagen))
        );
        Result resultado = new MultiFormatReader().decode(bitmap);
        return resultado.getText();
    }
}
