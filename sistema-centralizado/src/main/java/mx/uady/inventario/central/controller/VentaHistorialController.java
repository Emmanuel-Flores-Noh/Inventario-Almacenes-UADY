package mx.uady.inventario.central.controller;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import mx.uady.inventario.central.model.VentaHistorial;
import mx.uady.inventario.central.view.Mensajes;
import mx.uady.inventario.central.view.VentaHistorialPanel;
import mx.uady.inventario.common.modelo.Mercancia;

/** Coordina el historial de ventas: carga en segundo plano y apertura del QR de cada venta. */
public final class VentaHistorialController {

    private final VentasCargador cargarVentas;
    private final VentaHistorialPanel vista;
    private final QrAsegurador asegurarQr;
    private final BiConsumer<VentaHistorial, Mercancia> mostrarQr;

    /** Hay una carga en curso; las peticiones que lleguen mientras tanto se agrupan en una sola. */
    private boolean cargando;
    private boolean recargaPendiente;

    /**
     * @param asegurarQr genera el PNG de la unidad si falta (p. ej. {@code MercanciaService::asegurarQr})
     * @param mostrarQr  abre la vista previa con la venta y la unidad que ya tiene su QR
     */
    public VentaHistorialController(VentasCargador cargarVentas, VentaHistorialPanel vista,
            QrAsegurador asegurarQr, BiConsumer<VentaHistorial, Mercancia> mostrarQr) {
        this.cargarVentas = Objects.requireNonNull(cargarVentas, "cargarVentas");
        this.vista = Objects.requireNonNull(vista, "vista");
        this.asegurarQr = Objects.requireNonNull(asegurarQr, "asegurarQr");
        this.mostrarQr = Objects.requireNonNull(mostrarQr, "mostrarQr");
        vista.alActualizar(this::cargar);
        vista.alAbrirQr(this::abrirQr);
    }

    /** Recarga el historial. Hilo de Swing. */
    public void cargar() {
        if (cargando) {
            recargaPendiente = true;
            return;
        }
        cargando = true;
        vista.setEstado("Cargando ventas…");
        TareaAsync.de(cargarVentas::cargar)
                .bloqueando(vista.controles().actualizar(), vista.controles().abrirQr())
                .alTerminar(ventas -> {
                    vista.mostrarVentas(ventas);
                    terminarCarga();
                })
                .alFallar(error -> {
                    vista.setEstado("No se pudo cargar el historial de ventas.");
                    Mensajes.mostrarError(vista, error);
                    terminarCarga();
                })
                .ejecutar(vista);
    }

    private void abrirQr() {
        VentaHistorial venta = vista.seleccionada().orElse(null);
        if (venta == null) {
            return;
        }
        // Generar el PNG puede tocar disco y base de datos: se hace fuera del hilo de Swing.
        TareaAsync.de(() -> asegurarQr.asegurar(venta.numeroSerie()))
                .bloqueando(vista.controles().actualizar(), vista.controles().abrirQr())
                .alTerminar(unidad -> mostrarQr.accept(venta, unidad))
                .ejecutar(vista);
    }

    private void terminarCarga() {
        cargando = false;
        if (recargaPendiente) {
            recargaPendiente = false;
            cargar();
        }
    }

    @FunctionalInterface
    public interface VentasCargador {
        List<VentaHistorial> cargar() throws Exception;
    }

    @FunctionalInterface
    public interface QrAsegurador {
        Mercancia asegurar(String numeroSerie) throws Exception;
    }
}
