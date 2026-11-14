package mx.uady.inventario.central.service;

import java.util.List;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

/**
 * Registro y consulta de unidades físicas de mercancía (CU1).
 *
 * <p>Errores: {@link ValidacionException}, {@link SerieDuplicadaException} o {@link NegocioException}
 * para reglas de negocio, {@link AccesoDatosException} para fallos de la base de datos.</p>
 */
public interface MercanciaService {

    /**
     * Unidades filtradas, ordenadas por número de serie.
     *
     * @param idProducto producto a filtrar, o {@code null} para todos
     * @param estado     estado a filtrar, o {@code null} para todos
     */
    List<Mercancia> listar(Integer idProducto, EstadoMercancia estado);

    /**
     * Registra una unidad nueva en estado {@link EstadoMercancia#EN_ALMACEN} y genera su QR.
     *
     * @return la unidad registrada, con la ruta de su QR
     */
    Mercancia registrar(int idProducto, String numeroSerie);

    /**
     * Garantiza que la unidad tenga su PNG de QR: si no tiene ruta o el archivo ya no existe,
     * lo genera y guarda la nueva ruta. Si ya existe, no hace nada.
     *
     * @return la unidad con una ruta de QR válida
     * @throws NegocioException si la unidad no existe
     */
    Mercancia asegurarQr(String numeroSerie);
}
