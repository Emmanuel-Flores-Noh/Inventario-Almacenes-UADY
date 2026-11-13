package mx.uady.inventario.cliente.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uady.inventario.cliente.model.TiendaDTO;
import mx.uady.inventario.cliente.model.UsuarioDTO;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

/**
 * Suite de pruebas de interfaz de usuario para {@link RegistroVentaFrame} en el Sistema Proveedor (Cliente).
 * Valida la interactividad de la tabla, la gestión de catálogos y el cumplimiento estricto 
 * de las reglas de habilitación del formulario de registro.
 */
class RegistroVentaFrameTest {

    private static final Mercancia LAPTOP = new Mercancia("SN-CLI-001", 1, EstadoMercancia.EN_ALMACEN, "/app/storage/qr/SN-CLI-001.png");
    
    private static final TiendaDTO TIENDA;
    static {
        TIENDA = new TiendaDTO();
        TIENDA.setIdTienda(1L);
        TIENDA.setNombre("Almacén Central UADY");
        TIENDA.setDireccion("Calle 60");
    }

    private static final UsuarioDTO USUARIO;
    static {
        USUARIO = new UsuarioDTO();
    }

    private RegistroVentaFrame frame;

    /**
     * Configura y prepara la ventana principal ejecutando su instanciación de forma segura 
     * dentro del hilo de despacho de eventos (EDT).
     * 
     * @throws Exception Si ocurre algún error durante la invocación síncrona en el EDT.
     */
    @BeforeEach
    void preparar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new RegistroVentaFrame();
        });
    }

    /**
     * Verifica que los componentes gráficos se carguen de manera correcta y que el botón de registro
     * permanezca deshabilitado cuando no se ha seleccionado ninguna unidad en la tabla.
     * 
     * @throws Exception Si ocurre un fallo al interactuar con los componentes en el EDT.
     */
    @Test
    void componentesInicializanYBloqueanRegistroSinSeleccion() throws Exception {
        enEdt(() -> {
            frame.actualizarListasCombos(List.of(TIENDA), List.of(USUARIO));
            frame.actualizarTablaMercancia(List.of(LAPTOP));

            JTable tabla = componente("tablaMercancia", JTable.class);
            assertEquals(1, tabla.getRowCount());

            // Aún con catálogos cargados, si no hay fila seleccionada, el botón debe estar deshabilitado
            assertFalse(componente("btnRegistrarVenta", JButton.class).isEnabled());
            return null;
        });
    }

    /**
     * Valida que al seleccionar una unidad de mercancía en la tabla y contar con una tienda 
     * y un usuario proveedor seleccionados, el botón de registro de venta se habilite correctamente.
     * 
     * @throws Exception Si ocurre un fallo al simular la selección en el EDT.
     */
    @Test
    void habilitarRegistroAlSeleccionarFilaYCatalogos() throws Exception {
        enEdt(() -> {
            frame.actualizarListasCombos(List.of(TIENDA), List.of(USUARIO));
            frame.actualizarTablaMercancia(List.of(LAPTOP));

            JTable tabla = componente("tablaMercancia", JTable.class);
            tabla.setRowSelectionInterval(0, 0);
            
            // Seleccionar elementos en los combos si es necesario
            componente("comboTiendas", JComboBox.class).setSelectedItem(TIENDA);
            componente("comboUsuarios", JComboBox.class).setSelectedItem(USUARIO);

            assertTrue(componente("btnRegistrarVenta", JButton.class).isEnabled());
            return null;
        });
    }

    /**
     * Comprueba que el filtro de texto en tiempo real acote correctamente las filas de la tabla
     * según el criterio de búsqueda ingresado por el usuario en el campo correspondiente.
     * 
     * @throws Exception Si ocurre un fallo durante la prueba de filtrado en el EDT.
     */
    @Test
    void filtraMercanciaPorTexto() throws Exception {
        Mercancia otra = new Mercancia("AB-CLI-002", 2, EstadoMercancia.EN_ALMACEN, null);
        enEdt(() -> {
            frame.actualizarTablaMercancia(List.of(LAPTOP, otra));
            JTextField campoBusqueda = componente("txtBuscarMercancia", JTextField.class);
            campoBusqueda.setText("sn-cli");
            
            JTable tabla = componente("tablaMercancia", JTable.class);
            assertEquals(1, tabla.getRowCount());
            return null;
        });
    }

    // ---- Métodos Auxiliares para pruebas de Swing ----

    /**
     * Busca recursivamente un componente gráfico dentro del contenedor raíz por su nombre y tipo.
     * 
     * @param raiz   Contenedor raíz donde iniciar la búsqueda.
     * @param nombre Nombre lógico asignado al componente.
     * @param tipo   Clase del componente buscado.
     * @param <T>    Tipo genérico que extienda de {@link Component}.
     * @return El componente encontrado, o {@code null} si no existe.
     */
    private static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container hijo) {
                T encontrado = buscar(hijo, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    /**
     * Busca un componente asegurando su existencia; de lo contrario, lanza una excepción de aserción.
     * 
     * @param nombre Nombre lógico del componente.
     * @param tipo   Clase del componente.
     * @param <T>    Tipo genérico de componente.
     * @return El componente encontrado.
     */
    private <T extends Component> T componente(String nombre, Class<T> tipo) {
        T encontrado = buscar(frame, nombre, tipo);
        if (encontrado == null) {
            throw new AssertionError("No se encontró el componente gráfico: " + nombre);
        }
        return encontrado;
    }

    /**
     * Ejecuta una acción de manera síncrona y segura dentro del hilo de despacho de eventos (EDT).
     * 
     * @param consulta {@link Callable} que contiene la operación a ejecutar.
     * @param <T>      Tipo del resultado esperado.
     * @return El resultado de la ejecución.
     * @throws Exception Si la tarea lanzada dentro del EDT falla.
     */
    private static <T> T enEdt(Callable<T> consulta) throws Exception {
        AtomicReference<T> resultado = new AtomicReference<>();
        AtomicReference<Exception> fallo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                resultado.set(consulta.call());
            } catch (Exception e) {
                fallo.set(e);
            }
        });
        if (fallo.get() != null) {
            throw fallo.get();
        }
        return resultado.get();
    }
}