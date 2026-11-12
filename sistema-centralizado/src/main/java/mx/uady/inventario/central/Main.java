package mx.uady.inventario.central;

import mx.uady.inventario.central.controller.UiLauncher;
import mx.uady.inventario.central.network.DespachadorSolicitudes;
import mx.uady.inventario.central.network.RegistradorBroker;
import mx.uady.inventario.central.network.ServidorServicio;
import mx.uady.inventario.central.util.ConfiguracionServicio;

/**
 * Punto de entrada del Sistema 1 (Centralizado / Proveedor).
 *
 * <p>Arranca el servidor TCP en {@code SERVICE_PORT} y registra sus tres servicios
 * en el broker sin bloquear el arranque: si el broker aun no esta listo, el hilo
 * de registro reintenta mientras el servidor ya atiende conexiones.</p>
 */
public class Main {

    public static void main(String[] args) {
        ConfiguracionServicio configuracion;
        try {
            configuracion = ConfiguracionServicio.desdeEntorno();
        } catch (RuntimeException e) {
            System.err.println("Configuracion invalida del Sistema 1: " + e.getMessage());
            System.exit(1);
            return;
        }

        DespachadorSolicitudes despachador = DespachadorSolicitudes.estandar();
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

        UiLauncher.iniciar();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            hiloRegistro.interrupt();
            servidor.detener();
            Thread.currentThread().interrupt();
        }
    }
}
