package mx.uady.inventario.cliente.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Gestor encargado de encapsular la lógica de filtrado en tiempo real para las tablas de mercancía.
 * Aplica un {@link TableRowSorter} sobre el modelo de la tabla y escucha los cambios en el documento 
 * del campo de texto de búsqueda para actualizar dinámicamente las filas visibles.
 * 
 * @author UADY
 * @version 1.1
 */
public class MercanciaFiltroManager {

    private final TableRowSorter<DefaultTableModel> sorter;

    /**
     * Constructor principal que inicializa el filtro de la tabla y vincula el escucha de cambios 
     * en el documento del campo de búsqueda para realizar coincidencias parciales e insensible a mayúsculas/minúsculas.
     * 
     * @param tabla         La {@link JTable} sobre la cual se aplicará el filtrado de filas.
     * @param modeloTabla   El {@link DefaultTableModel} que respalda los datos de la tabla.
     * @param campoBusqueda El {@link JTextField} donde el usuario ingresa el texto de filtrado.
     */
    public MercanciaFiltroManager(JTable tabla, DefaultTableModel modeloTabla, JTextField campoBusqueda) {
        this.sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);

        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarFiltro();
            }

            /**
             * Evalúa el texto actual del campo de búsqueda y actualiza el filtro de la tabla.
             */
            private void actualizarFiltro() {
                String texto = campoBusqueda.getText();
                if (texto.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    // Filtra de forma insensible a mayúsculas/minúsculas por cualquier columna (N° Serie o Producto)
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
                }
            }
        });
    }
}