package mx.uady.inventario.central.service;

import java.nio.file.Path;

/**
 * Generación del código QR de una unidad física.
 * La implementación definitiva la entrega el issue #4 (trazabilidad); esta firma es el acuerdo.
 */
public interface QrService {

    /**
     * Genera (o regenera) el PNG del QR de la unidad.
     *
     * @return ruta del archivo PNG generado
     * @throws AccesoDatosException si no se pudo escribir el archivo
     */
    Path generar(String numeroSerie);
}
