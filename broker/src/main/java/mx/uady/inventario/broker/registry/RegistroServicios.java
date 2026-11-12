package mx.uady.inventario.broker.registry;

import mx.uady.inventario.broker.model.Proveedor;
import mx.uady.inventario.broker.util.ConfiguracionBroker;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Registro thread-safe de proveedores disponibles en el broker.
 * Esta clase aplica encapsulate collection.
 *
 * <p>Ademas de la tabla de servicios, mantiene la salud de cada proveedor: los
 * fallos consecutivos de comunicacion y la marca de no disponible. La marca se
 * libera sola al cumplirse el enfriamiento configurado, de modo que un corte
 * temporal no deje al proveedor fuera del sistema para siempre. Un nuevo
 * registro o una sustitucion lo reactivan de inmediato.</p>
 */
public class RegistroServicios {
    private final ConcurrentHashMap<String, Proveedor> servicios = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Salud> salud = new ConcurrentHashMap<>();

    private final int fallosParaIndisponible;
    private final long tiempoIndisponibleMillis;
    // Se inyecta el reloj para poder probar el enfriamiento sin esperas reales.
    private final LongSupplier reloj;

    /**
     * Registro con los valores por defecto del broker.
     */
    public RegistroServicios() {
        this(ConfiguracionBroker.porDefecto());
    }

    /**
     * Registro con la configuracion de resiliencia del broker.
     * @param configuracion configuracion con el umbral de fallos y el enfriamiento
     */
    public RegistroServicios(ConfiguracionBroker configuracion) {
        this(configuracion.fallosParaIndisponible(), configuracion.tiempoIndisponibleMillis());
    }

    /**
     * Registro con umbral y enfriamiento explicitos.
     * @param fallosParaIndisponible fallos consecutivos para marcar no disponible
     * @param tiempoIndisponibleMillis tiempo de enfriamiento en milisegundos
     */
    public RegistroServicios(int fallosParaIndisponible, long tiempoIndisponibleMillis) {
        this(fallosParaIndisponible, tiempoIndisponibleMillis, System::currentTimeMillis);
    }

    RegistroServicios(int fallosParaIndisponible, long tiempoIndisponibleMillis, LongSupplier reloj) {
        if (fallosParaIndisponible <= 0) {
            throw new IllegalArgumentException("fallosParaIndisponible debe ser positivo");
        }
        if (tiempoIndisponibleMillis < 0) {
            throw new IllegalArgumentException("tiempoIndisponibleMillis no puede ser negativo");
        }
        this.fallosParaIndisponible = fallosParaIndisponible;
        this.tiempoIndisponibleMillis = tiempoIndisponibleMillis;
        this.reloj = Objects.requireNonNull(reloj, "reloj es obligatorio");
    }

    /**
     * Registra un proveedor usando el modelo {@code Proveedor}.
     * Un registro nuevo limpia la salud previa: el proveedor vuelve a estar disponible.
     * @param proveedor El proveedor a agregar
     */
    public void registrar(Proveedor proveedor) {
        Objects.requireNonNull(proveedor, "proveedor es obligatorio");
        servicios.put(proveedor.servicio(), proveedor);
        salud.remove(proveedor.servicio());
    }

    /**
     * Buscar un proveedor en la lista de proveedores
     * @param servicio El servicio a buscar
     * @return El servicio o vacio si no se ha encontrado
     */
    public Optional<Proveedor> buscar(String servicio) {
        if (servicio == null || servicio.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(servicios.get(servicio));
    }

    /**
     * Sustituye un proveedor existente. Devuelve true si existia previamente.
     * La sustitucion reactiva al proveedor igual que un registro nuevo.
     * @param nuevoProveedor El proveedor con los nuevos datos
     */
    public boolean sustituir(Proveedor nuevoProveedor) {
        Objects.requireNonNull(nuevoProveedor, "El proveedor es requerido");
        boolean existia = servicios.replace(nuevoProveedor.servicio(), nuevoProveedor) != null;
        if (existia) {
            salud.remove(nuevoProveedor.servicio());
        }
        return existia;
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
        boolean eliminado = servicios.remove(servicio) != null;
        salud.remove(servicio);
        return eliminado;
    }

    /**
     * Todos los servicios disponibles registrados en el broker
     * @return Una copia de la lista de todos los servicios disponibles
     */
    public Map<String, Proveedor> getServicios() {
        return Map.copyOf(servicios);
    }

    /**
     * Consulta si un proveedor registrado puede recibir solicitudes.
     *
     * @param servicio nombre del servicio
     * @return {@code false} si no esta registrado o si sigue en enfriamiento
     */
    public boolean estaDisponible(String servicio) {
        if (servicio == null || servicio.isBlank() || !servicios.containsKey(servicio)) {
            return false;
        }
        Salud estado = salud.get(servicio);
        return estado == null || estado.indisponibleHasta() <= reloj.getAsLong();
    }

    /**
     * Registra un fallo de comunicacion con el proveedor.
     * Al alcanzar el umbral el proveedor queda marcado como no disponible durante
     * {@code tiempoIndisponibleMillis}. Si el enfriamiento anterior ya expiro, el
     * conteo arranca de nuevo.
     *
     * @param servicio nombre del servicio que fallo
     */
    public void marcarFallo(String servicio) {
        if (servicio == null || servicio.isBlank()) {
            return;
        }
        long ahora = reloj.getAsLong();
        salud.compute(servicio, (clave, anterior) -> {
            int fallosPrevios = anterior == null ? 0 : anterior.fallos();
            // Un enfriamiento vencido reinicia el conteo: la marca se gana de nuevo.
            if (anterior != null && anterior.indisponibleHasta() > 0 && anterior.indisponibleHasta() <= ahora) {
                fallosPrevios = 0;
            }
            int fallos = fallosPrevios + 1;
            long indisponibleHasta = 0;
            if (fallos >= fallosParaIndisponible) {
                indisponibleHasta = ahora + tiempoIndisponibleMillis;
            }
            return new Salud(fallos, indisponibleHasta);
        });
    }

    /**
     * Registra una comunicacion exitosa y limpia el historial de fallos.
     * @param servicio nombre del servicio que respondio
     */
    public void marcarExito(String servicio) {
        if (servicio != null && !servicio.isBlank()) {
            salud.remove(servicio);
        }
    }

    /**
     * Fallos consecutivos acumulados de un servicio, para diagnostico y pruebas.
     * @param servicio nombre del servicio
     * @return el contador de fallos, {@code 0} si no hay historial
     */
    public int fallos(String servicio) {
        Salud estado = servicio == null ? null : salud.get(servicio);
        return estado == null ? 0 : estado.fallos();
    }

    /**
     * Estado de salud inmutable de un proveedor.
     *
     * @param fallos fallos consecutivos
     * @param indisponibleHasta instante (epoch ms) hasta el que no esta disponible; 0 si sigue disponible
     */
    private record Salud(int fallos, long indisponibleHasta) {
    }
}
