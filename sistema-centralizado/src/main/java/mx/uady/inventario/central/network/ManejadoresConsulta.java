package mx.uady.inventario.central.network;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mx.uady.inventario.central.model.ProductoConUnidades;
import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.central.service.MercanciaService;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.ProductoService;
import mx.uady.inventario.central.service.QrService;
import mx.uady.inventario.common.json.GsonFactory;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;
import mx.uady.inventario.common.modelo.Producto;

/**
 * Fábrica de los manejadores de las operaciones de consulta que el Sistema 1 expone por red
 * (catálogos para el Sistema 2, inventario disponible, QR de una unidad e historial de precios).
 *
 * <p>Cada manejador traduce los parámetros JSON a una llamada de servicio y arma el JSON de
 * resultado. Los errores de negocio se convierten en {@link IllegalArgumentException}, que
 * {@link DespachadorSolicitudes} responde como {@code PARAMETROS_INVALIDOS} con un mensaje legible.</p>
 */
final class ManejadoresConsulta {

    private ManejadoresConsulta() {
    }

    /**
     * Crea el manejador de {@code listarTiendas}.
     *
     * @param consultas servicio de catálogos
     * @return manejador que devuelve el arreglo de tiendas
     */
    static ManejadorOperacion listarTiendas(ConsultasVentaService consultas) {
        Objects.requireNonNull(consultas, "consultas");
        return parametros -> GsonFactory.DEFAULT.toJsonTree(consultas.listarTiendas());
    }

    /**
     * Crea el manejador de {@code listarUsuarios}: solo usuarios con rol Proveedor.
     *
     * @param consultas servicio de catálogos
     * @return manejador que devuelve el arreglo de usuarios proveedores
     */
    static ManejadorOperacion listarUsuariosProveedores(ConsultasVentaService consultas) {
        Objects.requireNonNull(consultas, "consultas");
        return parametros -> GsonFactory.DEFAULT.toJsonTree(consultas.listarUsuariosProveedores());
    }

    /**
     * Crea el manejador de {@code listarInventario}: unidades {@code EN_ALMACEN} con el nombre
     * de su producto. No incluye la ruta del QR, que es interna al contenedor del Sistema 1.
     *
     * @param mercancia servicio de unidades físicas
     * @param productos servicio de productos, para resolver los nombres
     * @return manejador que devuelve un arreglo de
     *         {@code {numeroSerie, idProducto, nombreProducto, estado}}
     */
    static ManejadorOperacion listarInventarioDisponible(MercanciaService mercancia, ProductoService productos) {
        Objects.requireNonNull(mercancia, "mercancia");
        Objects.requireNonNull(productos, "productos");
        return parametros -> {
            Map<Integer, String> nombres = nombresPorId(productos);
            JsonArray resultado = new JsonArray();
            for (Mercancia unidad : mercancia.listar(null, EstadoMercancia.EN_ALMACEN)) {
                JsonObject fila = new JsonObject();
                fila.addProperty("numeroSerie", unidad.numeroSerie());
                fila.addProperty("idProducto", unidad.idProducto());
                fila.addProperty("nombreProducto", nombres.getOrDefault(unidad.idProducto(), "Producto desconocido"));
                fila.addProperty("estado", unidad.estado().name());
                resultado.add(fila);
            }
            return resultado;
        };
    }

    /**
     * Crea el manejador de {@code obtenerQr}. Genera el PNG si la unidad aún no lo tiene.
     *
     * @param mercancia servicio de unidades físicas
     * @param productos servicio de productos, para el nombre del producto
     * @param qr        servicio que lee el PNG del almacenamiento
     * @return manejador que recibe {@code numero_serie} y devuelve
     *         {@code {numeroSerie, idProducto, nombreProducto, pngBase64}}
     */
    static ManejadorOperacion obtenerQr(MercanciaService mercancia, ProductoService productos, QrService qr) {
        Objects.requireNonNull(mercancia, "mercancia");
        Objects.requireNonNull(productos, "productos");
        Objects.requireNonNull(qr, "qr");
        return parametros -> {
            String numeroSerie = textoObligatorio(parametros, "numero_serie");
            Mercancia unidad;
            try {
                unidad = mercancia.asegurarQr(numeroSerie);
            } catch (NegocioException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            byte[] png;
            try {
                png = qr.obtenerImagenQRBytes(unidad.rutaQr());
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el QR de la unidad " + numeroSerie, e);
            }
            JsonObject resultado = new JsonObject();
            resultado.addProperty("numeroSerie", unidad.numeroSerie());
            resultado.addProperty("idProducto", unidad.idProducto());
            resultado.addProperty("nombreProducto",
                    nombresPorId(productos).getOrDefault(unidad.idProducto(), "Producto desconocido"));
            resultado.addProperty("pngBase64", Base64.getEncoder().encodeToString(png));
            return resultado;
        };
    }

    /**
     * Crea el manejador de {@code listarPreciosProducto}.
     *
     * @param productos servicio de productos
     * @return manejador que recibe {@code id_producto} y devuelve
     *         {@code {idProducto, nombre, precioActual, historial: [...]}}
     */
    static ManejadorOperacion listarPreciosProducto(ProductoService productos) {
        Objects.requireNonNull(productos, "productos");
        return parametros -> {
            int idProducto = enteroPositivo(parametros, "id_producto");
            Producto producto = productos.listarConUnidades().stream()
                    .map(ProductoConUnidades::producto)
                    .filter(p -> p.idProducto() == idProducto)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El producto " + idProducto + " no existe"));
            JsonObject resultado = new JsonObject();
            resultado.addProperty("idProducto", producto.idProducto());
            resultado.addProperty("nombre", producto.nombre());
            resultado.addProperty("precioActual", producto.precioActual());
            resultado.add("historial", GsonFactory.DEFAULT.toJsonTree(productos.listarHistorialPrecios(idProducto)));
            return resultado;
        };
    }

    /**
     * Construye el mapa id → nombre de todos los productos.
     *
     * @param productos servicio de productos
     * @return mapa con el nombre de cada producto por su id
     */
    private static Map<Integer, String> nombresPorId(ProductoService productos) {
        List<ProductoConUnidades> lista = productos.listarConUnidades();
        return lista.stream()
                .map(ProductoConUnidades::producto)
                .collect(Collectors.toMap(Producto::idProducto, Producto::nombre, (a, b) -> a));
    }

    /**
     * Lee un parámetro de texto obligatorio.
     *
     * @param parametros objeto JSON de la solicitud
     * @param nombre     nombre del parámetro
     * @return valor sin espacios al inicio ni al final
     * @throws IllegalArgumentException si falta o está vacío
     */
    private static String textoObligatorio(JsonObject parametros, String nombre) {
        return leer(parametros, nombre, valor -> {
            String texto = valor.getAsString().strip();
            if (texto.isEmpty()) {
                throw new IllegalArgumentException("El parámetro " + nombre + " no puede estar vacío");
            }
            return texto;
        });
    }

    /**
     * Lee un parámetro entero positivo obligatorio.
     *
     * @param parametros objeto JSON de la solicitud
     * @param nombre     nombre del parámetro
     * @return valor mayor que cero
     * @throws IllegalArgumentException si falta, no es entero o no es positivo
     */
    private static int enteroPositivo(JsonObject parametros, String nombre) {
        return leer(parametros, nombre, valor -> {
            int numero;
            try {
                numero = valor.getAsInt();
            } catch (NumberFormatException | UnsupportedOperationException e) {
                throw new IllegalArgumentException("El parámetro " + nombre + " debe ser un entero", e);
            }
            if (numero <= 0) {
                throw new IllegalArgumentException("El parámetro " + nombre + " debe ser positivo");
            }
            return numero;
        });
    }

    /**
     * Verifica que el parámetro exista y aplica la conversión indicada.
     *
     * @param parametros objeto JSON de la solicitud
     * @param nombre     nombre del parámetro
     * @param conversion función que convierte y valida el valor
     * @param <T>        tipo del valor convertido
     * @return valor convertido
     * @throws IllegalArgumentException si el parámetro falta o es nulo
     */
    private static <T> T leer(JsonObject parametros, String nombre,
            Function<com.google.gson.JsonElement, T> conversion) {
        if (parametros == null || !parametros.has(nombre) || parametros.get(nombre).isJsonNull()) {
            throw new IllegalArgumentException("Falta el parámetro " + nombre);
        }
        return conversion.apply(parametros.get(nombre));
    }
}
