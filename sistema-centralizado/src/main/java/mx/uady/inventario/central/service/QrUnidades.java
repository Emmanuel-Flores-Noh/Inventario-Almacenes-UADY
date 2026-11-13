package mx.uady.inventario.central.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import mx.uady.inventario.common.modelo.Producto;

/**
 * Generación del QR de una unidad con el {@link QrService} del #4, traduciendo sus errores
 * a los de la capa de servicio para que la interfaz los muestre correctamente.
 */
public final class QrUnidades {

    private QrUnidades() {
    }

    /**
     * @return ruta del PNG generado
     * @throws ValidacionException  si el número de serie no sirve como nombre de archivo
     * @throws AccesoDatosException si no se pudo escribir el archivo
     */
    public static String generar(QrService qrService, String numeroSerie, Producto producto) {
        try {
            return qrService.generarCodigoQR(numeroSerie, producto.idProducto(), producto.nombre());
        } catch (QrGenerationException e) {
            // QrService lanza sin causa los errores de validación y con causa los de E/S o ZXing.
            if (e.getCause() == null) {
                throw new ValidacionException(e.getMessage() + ".");
            }
            throw new AccesoDatosException("No se pudo generar el QR de " + numeroSerie, e);
        } catch (InvalidPathException e) {
            throw new ValidacionException(
                    "El número de serie contiene caracteres que no se pueden usar en el nombre del archivo del QR.");
        }
    }

    /** {@code true} si la ruta apunta a un PNG que todavía existe. */
    public static boolean existe(String rutaQr) {
        try {
            return rutaQr != null && Files.isRegularFile(Path.of(rutaQr));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /** Borra el PNG si existe; se usa al deshacer un registro que ya lo había generado. */
    public static void borrarSilenciosamente(String rutaQr) {
        try {
            if (rutaQr != null) {
                Files.deleteIfExists(Path.of(rutaQr));
            }
        } catch (Exception ignorado) {
            // Un PNG huérfano no afecta los datos; asegurarQr lo regenera si hace falta.
        }
    }
}
