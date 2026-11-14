package mx.uady.inventario.central.service;

import com.google.gson.JsonObject;
import mx.uady.inventario.central.repository.VentaRepository;
import mx.uady.inventario.central.repository.VentaRepositoryException;
import mx.uady.inventario.central.repository.VentaResultado;
import mx.uady.inventario.central.util.ConfiguracionBD;

import java.util.Objects;

/**
 * Servicio de aplicación para registrar ventas recibidas por la red.
 */
public class VentaService {

    private final VentaRegistrador registrador;

    /**
     * Crea el servicio usando el repositorio JDBC real.
     */
    public VentaService() {
        this(new VentaRepository()::registrarVenta);
    }

    /**
     * Crea el servicio con una configuración JDBC explícita.
     *
     * @param configuracion configuración de MariaDB
     */
    public VentaService(ConfiguracionBD configuracion) {
        this(new VentaRepository(configuracion)::registrarVenta);
    }

    /**
     * Crea el servicio con un registrador explícito para pruebas o composición.
     *
     * @param registrador componente que persiste la venta
     */
    public VentaService(VentaRegistrador registrador) {
        this.registrador = Objects.requireNonNull(
                registrador,
                "El registrador de ventas no puede ser nulo"
        );
    }

    /**
     * Registra la venta descrita por los parámetros de una solicitud.
     *
     * @param parametros objeto JSON con numero_serie, id_tienda e id_usuario
     * @return confirmación mínima de la venta
     * @throws VentaRepositoryException si falla la persistencia
     * @throws IllegalArgumentException si faltan o son inválidos los parámetros
     */
    public JsonObject registrarVenta(JsonObject parametros)
            throws VentaRepositoryException {
        if (parametros == null) {
            throw new IllegalArgumentException("Los parámetros son obligatorios");
        }

        String numeroSerie = textoObligatorio(parametros, "numero_serie");
        int idTienda = enteroPositivo(parametros, "id_tienda");
        int idUsuario = enteroPositivo(parametros, "id_usuario");

        VentaResultado resultado = registrador.registrar(numeroSerie, idTienda, idUsuario);
        JsonObject confirmacion = new JsonObject();
        confirmacion.addProperty("numero_serie", resultado.numeroSerie());
        confirmacion.addProperty("id_tienda", resultado.idTienda());
        confirmacion.addProperty("fecha_venta", resultado.fechaVenta().toString());
        return confirmacion;
    }

    private String textoObligatorio(JsonObject parametros, String nombre) {
        if (!parametros.has(nombre) || parametros.get(nombre).isJsonNull()) {
            throw new IllegalArgumentException("Falta el parámetro " + nombre);
        }
        String valor = parametros.get(nombre).getAsString();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El parámetro " + nombre + " no puede estar vacío");
        }
        return valor;
    }

    private int enteroPositivo(JsonObject parametros, String nombre) {
        if (!parametros.has(nombre) || parametros.get(nombre).isJsonNull()) {
            throw new IllegalArgumentException("Falta el parámetro " + nombre);
        }
        try {
            int valor = parametros.get(nombre).getAsInt();
            if (valor <= 0) {
                throw new IllegalArgumentException("El parámetro " + nombre + " debe ser positivo");
            }
            return valor;
        } catch (NumberFormatException | UnsupportedOperationException error) {
            throw new IllegalArgumentException("El parámetro " + nombre + " debe ser un entero", error);
        }
    }

    @FunctionalInterface
    public interface VentaRegistrador {
        VentaResultado registrar(String numeroSerie, int idTienda, int idUsuario)
                throws VentaRepositoryException;
    }
}
