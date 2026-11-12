package mx.uady.inventario.central.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

/** Modelo de la tabla de unidades: número de serie, producto, estado y QR. */
public class MercanciaTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_SERIE = 0;
    public static final int COL_PRODUCTO = 1;
    public static final int COL_ESTADO = 2;
    public static final int COL_QR = 3;

    private static final String[] COLUMNAS = {"Número de serie", "Producto", "Estado", "QR"};

    private final List<Mercancia> filas = new ArrayList<>();
    private final Map<Integer, String> nombresProducto = new HashMap<>();

    public void setFilas(List<Mercancia> unidades, Map<Integer, String> nombres) {
        filas.clear();
        filas.addAll(unidades);
        nombresProducto.clear();
        nombresProducto.putAll(nombres);
        fireTableDataChanged();
    }

    /** Sustituye la fila de la misma unidad (p. ej. tras generar su QR). */
    public void actualizar(Mercancia unidad) {
        int indice = indiceDe(unidad.numeroSerie());
        if (indice >= 0) {
            filas.set(indice, unidad);
            fireTableRowsUpdated(indice, indice);
        }
    }

    public Mercancia getFila(int fila) {
        return filas.get(fila);
    }

    /** Índice en el modelo de la unidad con ese número de serie (sin distinguir mayúsculas), o -1. */
    public int indiceDe(String numeroSerie) {
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).numeroSerie().equalsIgnoreCase(numeroSerie)) {
                return i;
            }
        }
        return -1;
    }

    public String nombreProducto(int idProducto) {
        return nombresProducto.getOrDefault(idProducto, "Producto #" + idProducto);
    }

    /** Texto legible del estado. */
    public static String texto(EstadoMercancia estado) {
        return switch (estado) {
            case EN_ALMACEN -> "En almacén";
            case VENDIDO -> "Vendido";
        };
    }

    @Override
    public int getRowCount() {
        return filas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Mercancia m = filas.get(fila);
        return switch (columna) {
            case COL_SERIE -> m.numeroSerie();
            case COL_PRODUCTO -> nombreProducto(m.idProducto());
            case COL_ESTADO -> texto(m.estado());
            case COL_QR -> m.rutaQr() == null ? "Pendiente" : "Generado";
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columna);
        };
    }
}
