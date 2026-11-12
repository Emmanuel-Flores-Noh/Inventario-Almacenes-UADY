package mx.uady.inventario.central;

import mx.uady.inventario.central.controller.UiLauncher;
import mx.uady.inventario.central.network.DespachadorSolicitudes;
import mx.uady.inventario.central.network.RegistradorBroker;
import mx.uady.inventario.central.network.ServidorServicio;
import mx.uady.inventario.central.service.VentaService;
import mx.uady.inventario.central.util.ConfiguracionBD;
import mx.uady.inventario.central.util.ConfiguracionServicio;

/**
 * Punto de entrada del Sistema 1 (Centralizado / Proveedor).
 *
 * <p>Arranca el servidor TCP en {@code SERVICE_PORT} y registra sus tres servicios
 * en el broker sin bloquear el arranque: si el broker aun no esta listo, el hilo
 * de registro reintenta mientras el servidor ya atiende conexiones.</p>
 */
public class Main {

    /**
     * Arranca el servidor TCP, registra el proveedor en el Broker y mantiene
     * vivo el proceso hasta recibir una interrupción.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     */
    public static void main(String[] args) {
        ConfiguracionServicio configuracion;
        try {
            configuracion = ConfiguracionServicio.desdeEntorno();
        } catch (RuntimeException e) {
            System.err.println("Configuracion invalida del Sistema 1: " + e.getMessage());
            System.exit(1);
            return;
        }

        ConfiguracionBD configuracionBD;
        try {
            configuracionBD = ConfiguracionBD.desdeEntorno();
        } catch (RuntimeException e) {
            System.err.println("Configuracion invalida de MariaDB: " + e.getMessage());
            System.exit(1);
            return;
        }

        VentaService ventaService = new VentaService(configuracionBD);
        DespachadorSolicitudes despachador = DespachadorSolicitudes.estandar(ventaService);
        ServidorServicio servidor = new ServidorServicio(
                configuracion.puertoServicio(), configuracion.tamanoPool(), despachador);

        try {
            servidor.iniciar();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        RegistradorBroker registrador = new RegistradorBroker(
                configuracion, RegistradorBroker.serviciosDelSistema());
        Thread hiloRegistro = registrador.iniciarEnSegundoPlano();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hiloRegistro.interrupt();
            servidor.detener();
        }, "servicio-shutdown"));

        System.out.println("==================================================");
        System.out.println("  Sistema Centralizado / Proveedor iniciado");
        System.out.println("  Escuchando en el puerto " + servidor.getPuerto()
                + " con pool de " + configuracion.tamanoPool() + " hilos");
        System.out.println("  Broker: " + configuracion.hostBroker() + ":" + configuracion.puertoBroker());
        System.out.println("  Servicios: " + String.join(", ", RegistradorBroker.serviciosDelSistema()));
        System.out.println("==================================================");

        UiLauncher.iniciar(ventaService);
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            hiloRegistro.interrupt();
            servidor.detener();
            Thread.currentThread().interrupt();
        }
    }
}
