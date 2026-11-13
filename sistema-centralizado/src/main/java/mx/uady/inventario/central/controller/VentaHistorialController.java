package mx.uady.inventario.central.controller;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.VentaHistorial;
import mx.uady.inventario.central.view.VentaHistorialPanel;

/** Coordina la carga del historial sin acoplar la pantalla a JDBC. */
public final class VentaHistorialController {

    private final VentasCargador cargarVentas;
    private final VentaHistorialPanel vista;
    private final Consumer<VentaHistorial> mostrarQr;
    private boolean cargando;

    public VentaHistorialController(VentasCargador cargarVentas, VentaHistorialPanel vista,
            Consumer<VentaHistorial> mostrarQr) {
        this.cargarVentas = Objects.requireNonNull(cargarVentas, "cargarVentas");
        this.vista = Objects.requireNonNull(vista, "vista");
        this.mostrarQr = Objects.requireNonNull(mostrarQr, "mostrarQr");
        vista.alActualizar(this::cargar);
        vista.alAbrirQr(() -> vista.seleccionada().ifPresent(mostrarQr));
    }

    public void cargar() {
        if (cargando) {
            return;
        }
        cargando = true;
        TareaAsync.de(cargarVentas::cargar)
                .bloqueando(vista.controles().actualizar(), vista.controles().abrirQr())
                .alTerminar(ventas -> {
                    vista.mostrarVentas(ventas);
                    cargando = false;
                })
                .alFallar(error -> {
                    cargando = false;
                    Mensajes.mostrarError(vista, error);
                })
                .ejecutar(vista);
    }

    @FunctionalInterface
    public interface VentasCargador {
        List<VentaHistorial> cargar() throws Exception;
    }
}
