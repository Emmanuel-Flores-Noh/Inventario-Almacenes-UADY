package mx.uady.inventario.central.service;

import mx.uady.inventario.central.util.QrUtil;

import com.google.zxing.WriterException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servicio encargado de la generación y lectura de códigos QR para la trazabilidad de productos.
 */
public class QrService {

    private static final int QR_SIZE = 300;
    private static final String DEFAULT_STORAGE_DIRECTORY = "/app/storage/qr";

    private final Path storageDirectory;

    /**
     * Crea el servicio usando el almacenamiento configurado para el contenedor.
     */
    public QrService() {
        this(Paths.get(DEFAULT_STORAGE_DIRECTORY));
    }

    /**
     * Crea el servicio usando un directorio específico de almacenamiento.
     *
     * @param storageDirectory directorio donde se guardarán los PNG
     */
    public QrService(Path storageDirectory) {
        if (storageDirectory == null) {
            throw new IllegalArgumentException("El directorio de almacenamiento no puede ser nulo");
        }
        this.storageDirectory = storageDirectory;
    }

    /**
     * Genera una imagen PNG con el código QR que incrusta los datos del producto y su número de serie.
     *
     * @param numeroSerie identificador único de la unidad física
     * @param idProducto identificador numérico del producto en la base de datos
     * @param nombreProducto nombre descriptivo del producto
     * @return ruta del sistema de archivos donde se almacenó el archivo PNG generado
     * @throws QrGenerationException si ocurre un error durante la codificación o escritura
     */
    public String generarCodigoQR(String numeroSerie, int idProducto, String nombreProducto)
            throws QrGenerationException {
        validarDatos(numeroSerie, idProducto, nombreProducto);

        Path destino = storageDirectory.resolve(numeroSerie + ".png").normalize();
        if (!destino.getParent().equals(storageDirectory.normalize())) {
            throw new QrGenerationException("El número de serie contiene una ruta no válida");
        }

        String payload = "ID: " + idProducto
                + " | Producto: " + nombreProducto
                + " | SN: " + numeroSerie;

        try {
            Files.createDirectories(storageDirectory);
            QrUtil.escribirPng(payload, destino, QR_SIZE, QR_SIZE);
            return destino.toString();
        } catch (IOException | WriterException exception) {
            throw new QrGenerationException(
                    "No fue posible generar el código QR para la unidad " + numeroSerie,
                    exception
            );
        }
    }

    /**
     * Lee un archivo de código QR almacenado y lo devuelve como un arreglo de bytes.
     *
     * @param rutaQr ruta física del archivo PNG en el almacenamiento
     * @return arreglo de bytes de la imagen PNG
     * @throws IOException si el archivo no existe o no puede leerse
     */
    public byte[] obtenerImagenQRBytes(String rutaQr) throws IOException {
        if (rutaQr == null || rutaQr.isBlank()) {
            throw new IllegalArgumentException("La ruta del código QR es obligatoria");
        }
        return Files.readAllBytes(Path.of(rutaQr));
    }

    private void validarDatos(String numeroSerie, int idProducto, String nombreProducto)
            throws QrGenerationException {
        if (numeroSerie == null || numeroSerie.isBlank()) {
            throw new QrGenerationException("El número de serie es obligatorio");
        }
        if (idProducto <= 0) {
            throw new QrGenerationException("El id del producto debe ser positivo");
        }
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new QrGenerationException("El nombre del producto es obligatorio");
        }
    }
}
