package mx.uady.inventario.central.view;

import mx.uady.inventario.central.model.VentaHistorial;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/** Modelo de la tabla de ventas históricas. */
public final class VentaHistorialTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final int COL_ID = 0;
    public static final int COL_SERIE = 1;
    public static final int COL_PRODUCTO = 2;
    public static final int COL_TIENDA = 3;
    public static final int COL_USUARIO = 4;
    public static final int COL_FECHA = 5;

    private static final String[] COLUMNAS = {"ID", "Número de serie", "Producto",
        "Tienda", "Usuario", "Fecha"};
    private final List<VentaHistorial> filas = new ArrayList<>();

    public void setFilas(List<VentaHistorial> ventas) {
        filas.clear();
        filas.addAll(ventas);
        fireTableDataChanged();
    }

    public VentaHistorial getFila(int fila) {
        return filas.get(fila);
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
        VentaHistorial venta = filas.get(fila);
        return switch (columna) {
            case COL_ID -> venta.idVenta();
            case COL_SERIE -> venta.numeroSerie();
            case COL_PRODUCTO -> venta.producto();
            case COL_TIENDA -> venta.tienda();
            case COL_USUARIO -> venta.usuario();
            case COL_FECHA -> venta.fechaVenta().format(FORMATO);
            default -> throw new IndexOutOfBoundsException("Columna inexistente: " + columna);
        };
    }
}
