package mx.uady.inventario.broker.registry;

import mx.uady.inventario.broker.model.Proveedor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro thread-safe de proveedores disponibles en el broker.
 * Esta clase aplica encapsulate collection
 */
public class RegistroServicios {
    private final ConcurrentHashMap<String, Proveedor> servicios = new ConcurrentHashMap<>();

    /**
     * Registra un proveedor usando el modelo {@code Proveedor}
     * @param proveedor El proveedor a agregar
     */
    public void registrar(Proveedor proveedor) {
        Objects.requireNonNull(proveedor, "proveedor es obligatorio");
        servicios.put(proveedor.servicio(), proveedor);
    }

    /**
     * Buscar un proveedor en la lista de proveedores
     * @param servicio El servicio a buscar
     * @return El servicio o null si no se ha encontrado
     */
    public Optional<Proveedor> buscar(String servicio) {
        if (servicio == null || servicio.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(servicios.get(servicio));
    }

    /**
     * Sustituye un proveedor existente. Devuelve true si existia previamente.
     * @param nuevoProveedor El proveedor con los nuevos datos
     */
    public boolean sustituir(Proveedor nuevoProveedor) {
        Objects.requireNonNull(nuevoProveedor, "El proveedor es requerido");
        return servicios.replace(nuevoProveedor.servicio(), nuevoProveedor) != null;
    }

    /**
     * Elimina un servicio del registro
     * @param servicio El nombre del servicio eliminar
     * @return {@code true} si el servicio se ha eliminado adecuadamente
     */
    public boolean eliminar(String servicio) {
        if (servicio == null || servicio.isBlank()) {
            return false;
        }
        return servicios.remove(servicio) != null;
    }

    /**
     * Todos los servicios disponibles registrados en el broker
     * @return Una copia de la lista de todos los servicios disponibles
     */
    public Map<String, Proveedor> getServicios() {
        return Map.copyOf(servicios);
    }
}
