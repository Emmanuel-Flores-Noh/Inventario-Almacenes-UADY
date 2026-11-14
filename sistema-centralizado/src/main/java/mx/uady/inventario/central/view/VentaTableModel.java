package mx.uady.inventario.central.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

/** Modelo de la tabla de mercancías disponibles para venta. */
public final class VentaTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_SERIE = 0;
    public static final int COL_PRODUCTO = 1;
    public static final int COL_ESTADO = 2;

    private static final String[] COLUMNAS = {"Número de serie", "Producto", "Estado"};

    private final List<Mercancia> filas = new ArrayList<>();
    private final Map<Integer, String> nombresProducto = new HashMap<>();

    public void setFilas(List<Mercancia> mercancias, Map<Integer, String> nombres) {
        filas.clear();
        filas.addAll(mercancias);
        nombresProducto.clear();
        nombresProducto.putAll(nombres);
        fireTableDataChanged();
    }

    public Mercancia getFila(int fila) {
        return filas.get(fila);
    }

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

    public static String textoEstado(EstadoMercancia estado) {
        return MercanciaTableModel.texto(estado);
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
        Mercancia mercancia = filas.get(fila);
        return switch (columna) {
            case COL_SERIE -> mercancia.numeroSerie();
            case COL_PRODUCTO -> nombreProducto(mercancia.idProducto());
            case COL_ESTADO -> textoEstado(mercancia.estado());
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columna);
        };
    }
}
