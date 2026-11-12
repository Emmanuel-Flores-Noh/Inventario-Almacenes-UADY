package mx.uady.inventario.central.service.memoria;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.HistorialPrecio;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Datos en memoria compartidos por los servicios simulados, mientras no exista la persistencia JDBC.
 * Todas las operaciones se sincronizan sobre la instancia, que hace las veces de transacción.
 */
public final class AlmacenMemoria {

    final Map<Integer, Producto> productos = new LinkedHashMap<>();
    /** Clave en minúsculas: la PK de MariaDB ({@code utf8mb4_unicode_ci}) no distingue mayúsculas. */
    final Map<String, Mercancia> mercancia = new LinkedHashMap<>();
    final List<HistorialPrecio> historial = new ArrayList<>();
    final Duration latencia;

    private int siguienteIdProducto = 1;
    private int siguienteIdHistorial = 1;

    public AlmacenMemoria(Duration latencia) {
        this.latencia = latencia == null ? Duration.ZERO : latencia;
    }

    /** Almacén con los mismos productos y unidades que {@code 02_seed.sql}. */
    public static AlmacenMemoria conDatosDeEjemplo(Duration latencia) {
        AlmacenMemoria almacen = new AlmacenMemoria(latencia);
        almacen.agregarProducto("Laptop Lenovo ThinkPad E14", "Equipo de computo para laboratorio de ingenieria", "18999.00");
        almacen.agregarProducto("Proyector Epson PowerLite", "Proyector portatil para aulas y auditorios", "12500.50");
        almacen.agregarProducto("Silla ergonomica", "Silla con soporte lumbar para oficina", "3499.00");
        almacen.agregarProducto("Impresora laser HP", "Impresora monocromatica de red", "4200.00");
        almacen.agregarProducto("Tablet Samsung Galaxy Tab", "Tablet de 11 pulgadas para consulta de campo", "7899.99");
        almacen.agregarUnidad("SN-UADY-0001", 1, EstadoMercancia.EN_ALMACEN);
        almacen.agregarUnidad("SN-UADY-0002", 1, EstadoMercancia.VENDIDO);
        almacen.agregarUnidad("SN-UADY-0003", 2, EstadoMercancia.EN_ALMACEN);
        almacen.agregarUnidad("SN-UADY-0004", 2, EstadoMercancia.VENDIDO);
        almacen.agregarUnidad("SN-UADY-0005", 3, EstadoMercancia.EN_ALMACEN);
        almacen.agregarUnidad("SN-UADY-0006", 3, EstadoMercancia.EN_ALMACEN);
        almacen.agregarUnidad("SN-UADY-0007", 4, EstadoMercancia.EN_ALMACEN);
        almacen.agregarUnidad("SN-UADY-0008", 4, EstadoMercancia.VENDIDO);
        almacen.agregarUnidad("SN-UADY-0009", 5, EstadoMercancia.EN_ALMACEN);
        almacen.agregarUnidad("SN-UADY-0010", 5, EstadoMercancia.VENDIDO);
        return almacen;
    }

    synchronized Producto agregarProducto(String nombre, String descripcion, String precio) {
        Producto producto = new Producto(siguienteIdProducto++, nombre, descripcion, new BigDecimal(precio));
        productos.put(producto.idProducto(), producto);
        return producto;
    }

    synchronized int nuevoIdProducto() {
        return siguienteIdProducto++;
    }

    synchronized int nuevoIdHistorial() {
        return siguienteIdHistorial++;
    }

    /** Unidad de ejemplo sin QR generado; la pantalla lo genera al pedir verlo. */
    private void agregarUnidad(String numeroSerie, int idProducto, EstadoMercancia estado) {
        agregarMercancia(new Mercancia(numeroSerie, idProducto, estado, null));
    }

    synchronized void agregarMercancia(Mercancia unidad) {
        mercancia.put(clave(unidad.numeroSerie()), unidad);
    }

    /** Historial de precios de un producto, del más antiguo al más reciente. */
    public synchronized List<HistorialPrecio> historial(int idProducto) {
        return historial.stream().filter(h -> h.idProducto() == idProducto).toList();
    }

    static String clave(String numeroSerie) {
        return numeroSerie.toLowerCase(Locale.ROOT);
    }

    /** Simula la latencia de la base de datos para comprobar que la interfaz no se congela. */
    void simularLatencia() {
        if (latencia.isZero() || latencia.isNegative()) {
            return;
        }
        try {
            Thread.sleep(latencia);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
