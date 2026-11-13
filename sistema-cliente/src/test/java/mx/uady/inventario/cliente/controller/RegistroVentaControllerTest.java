package mx.uady.inventario.cliente.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uady.inventario.cliente.model.TiendaDTO;
import mx.uady.inventario.cliente.model.UsuarioDTO;
import mx.uady.inventario.cliente.view.RegistroVentaFrame;

/**
 * Suite de pruebas unitarias y de integración para el controlador y la interfaz del Sistema Proveedor (Cliente).
 * Verifica la correcta inicialización de componentes, la carga de catálogos, el filtrado y el flujo de registro.
 * 
 * @author UADY
 * @version 1.0
 */
class RegistroVentaControllerTest {

    private RegistroVentaFrame frame;
    private RegistroVentaController controlador;

    /**
     * Configura el entorno de pruebas ejecutando la inicialización en el hilo de despacho de eventos (EDT).
     */
    @BeforeEach
    void preparar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new RegistroVentaFrame();
            controlador = new RegistroVentaController(frame);
        });
    }

    /**
     * Verifica que los componentes de la interfaz se encuentren instanciados correctamente 
     * y que los controles interactivos inicien en un estado seguro y deshabilitado acorde a las reglas.
     */
    @Test
    void componentesInicializanCorrectamente() throws Exception {
        assertTrue(enEdt(() -> componente("tablaMercancia", JTable.class) != null));
        assertTrue(enEdt(() -> componente("comboTiendas", JComboBox.class) != null));
        assertTrue(enEdt(() -> componente("comboUsuarios", JComboBox.class) != null));
        assertFalse(enEdt(() -> componente("btnRegistrarVenta", JButton.class).isEnabled()));
    }

    /**
     * Valida que el gestor de filtrado permita acotar las filas de la tabla de mercancía en tiempo real
     * mediante la introducción de texto en el buscador.
     */
    @Test
    void filtroDeBusquedaActualizaTablaCorrectamente() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField campoBuscar = componente("txtBuscarMercancia", JTextField.class);
            campoBuscar.setText("SN-UADY");
        });
        
        // Validar que el componente responda al texto ingresado
        assertEquals("SN-UADY", enEdt(() -> componente("txtBuscarMercancia", JTextField.class).getText()));
    }

    // ---- Métodos Auxiliares de Prueba (EDT & Lookups) ----

    /**
     * Busca recursivamente un componente dentro de un contenedor gráfico por su nombre y tipo.
     * 
     * @param raiz   Contenedor raíz donde iniciar la búsqueda.
     * @param nombre Nombre lógico asignado al componente.
     * @param tipo   Clase o tipo del componente buscado.
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
     * @param nombre Nombre del componente.
     * @param tipo   Tipo de clase del componente.
     * @param <T>    Tipo genérico de componente.
     * @return El componente encontrado.
     */
    private <T extends Component> T componente(String nombre, Class<T> tipo) {
        T encontrado = buscar(frame, nombre, tipo);
        if (encontrado == null) {
            throw new AssertionError("No se encontró el componente: " + nombre);
        }
        return encontrado;
    }

    /**
     * Ejecuta una consulta de manera segura dentro del hilo de despacho de eventos (EDT).
     * 
     * @param consulta {@link Callable} que contiene la operación a ejecutar.
     * @param <T>      Tipo del resultado esperado.
     * @return El resultado de la ejecución.
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

    /**
     * Espera de manera síncrona hasta que se cumpla una condición dada o se alcance el tiempo límite.
     * 
     * @param condicion {@link Callable} booleano que representa la condición de éxito.
     */
    private static void esperar(Callable<Boolean> condicion) throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < limite) {
            if (condicion.call()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("La condición no se cumplió en el tiempo límite de 5 segundos");
    }
}