package mx.uady.inventario.central.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import mx.uady.inventario.central.model.ProductoConUnidades;

/** Modelo de la tabla de productos: nombre, descripción, precio actual y unidades. */
public class ProductosTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COL_NOMBRE = 0;
    public static final int COL_DESCRIPCION = 1;
    public static final int COL_PRECIO = 2;
    public static final int COL_UNIDADES = 3;

    private static final String[] COLUMNAS = {"Nombre", "Descripción", "Precio actual", "Unidades"};
    private static final Class<?>[] TIPOS = {String.class, String.class, BigDecimal.class, Integer.class};

    private final List<ProductoConUnidades> filas = new ArrayList<>();

    public void setFilas(List<ProductoConUnidades> nuevas) {
        filas.clear();
        filas.addAll(nuevas);
        fireTableDataChanged();
    }

    public ProductoConUnidades getFila(int fila) {
        return filas.get(fila);
    }

    /** Índice en el modelo del producto con ese id, o -1 si no está. */
    public int indiceDe(int idProducto) {
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).producto().idProducto() == idProducto) {
                return i;
            }
        }
        return -1;
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
    public Class<?> getColumnClass(int columna) {
        return TIPOS[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        ProductoConUnidades p = filas.get(fila);
        return switch (columna) {
            case COL_NOMBRE -> p.producto().nombre();
            case COL_DESCRIPCION -> p.producto().descripcion() == null ? "" : p.producto().descripcion();
            case COL_PRECIO -> p.producto().precioActual();
            case COL_UNIDADES -> p.unidades();
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columna);
        };
    }
}
