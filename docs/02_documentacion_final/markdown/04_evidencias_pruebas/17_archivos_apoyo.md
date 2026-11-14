## Archivos de apoyo

| Archivo | Uso |
|---|---|
| `EvidenciaCliente.java` | Envía una solicitud al Broker y muestra el JSON enviado y recibido |
| `EvidenciaConcurrencia.java` | N clientes simultáneos; verifica que cada uno reciba su propia respuesta |
| `EvidenciaRendimiento.java` | Mide la misma operación directo al proveedor y a través del Broker |
| `comun.sh` | Funciones compartidas por los scripts de evidencias |
| `parte1.sh` | Genera las evidencias 01–06, 11, 13 y 14 (stack sano) |
| `parte2.sh` | Genera las evidencias 08, 09 y 12; deja la consola del segundo proveedor en `proveedor2.log` |
| `parte3.sh` | Genera la evidencia 07 (excepción del proveedor y base de datos detenida) |

### EvidenciaCliente.java

Envía una solicitud al Broker y muestra el JSON enviado y recibido.

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Cliente de evidencias: envía UNA solicitud al Broker por socket TCP y muestra
 * el JSON enviado y el recibido. Solo conoce el Broker (host y puerto), nunca al proveedor.
 *
 * Uso: java EvidenciaCliente <idSolicitud> <servicio> <operacion> <parametrosJson>
 * El destino es localhost:5000; se puede cambiar con -Dhost=... y -Dpuerto=...
 * El tiempo de espera de la respuesta es 15000 ms; se puede cambiar con -DtimeoutMs=...
 */
public class EvidenciaCliente {

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("host", "localhost");
        int puerto = Integer.getInteger("puerto", 5000);
        String id = args[0];
        JsonObject parametros = JsonParser.parseString(args[3]).getAsJsonObject();
        Solicitud solicitud = Solicitud.crear(id, args[1], args[2], parametros);

        String json = GsonFactory.DEFAULT.toJson(solicitud);               // marshalling
        System.out.println("CLIENTE -> BROKER (" + host + ":" + puerto + ")");
        System.out.println(bonito(json));

        long inicio = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 3000);
            socket.setSoTimeout(Integer.getInteger("timeoutMs", 15_000));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida.println(json);
            String recibido = entrada.readLine();
            long ms = (System.nanoTime() - inicio) / 1_000_000;

            Respuesta respuesta = GsonFactory.DEFAULT.fromJson(recibido, Respuesta.class); // unmarshalling
            System.out.println("BROKER -> CLIENTE (" + ms + " ms)");
            System.out.println(bonito(recibido));
            System.out.println("Resumen: estado=" + respuesta.getEstado()
                    + (respuesta.getCodigoError() == null ? ""
                            : ", codigo=" + respuesta.getCodigoError() + ", mensaje=\"" + respuesta.getMensajeError() + "\""));
        } catch (IOException e) {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            System.out.println("SIN RESPUESTA (" + ms + " ms): " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("Resumen: el cliente no pudo comunicarse con el Broker");
        }
    }

    private static String bonito(String json) {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(JsonParser.parseString(json));
    }
}
```

### EvidenciaConcurrencia.java

N clientes simultáneos; verifica que cada uno reciba su propia respuesta.

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Evidencia de atención concurrente: N clientes envían al mismo tiempo una solicitud al Broker
 * (cada uno con su propio socket e id) y se comprueba que cada uno recibe SU respuesta.
 *
 * Uso: java EvidenciaConcurrencia <cantidad> <prefijoId>
 */
public class EvidenciaConcurrencia {

    public static void main(String[] args) throws Exception {
        int cantidad = Integer.parseInt(args[0]);
        String prefijo = args[1];
        String[] operaciones = {"gestorTiendas/listarTiendas", "inventario/listarInventario"};

        ExecutorService ejecutor = Executors.newFixedThreadPool(cantidad);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            String id = prefijo + "-" + String.format("%02d", i);
            String[] destino = operaciones[i % operaciones.length].split("/");
            futuros.add(ejecutor.submit(() -> {
                salida.await();
                long inicio = System.nanoTime();
                Solicitud solicitud = Solicitud.crear(id, destino[0], destino[1], null);
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", 5000), 3000);
                    socket.setSoTimeout(15_000);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    out.println(GsonFactory.DEFAULT.toJson(solicitud));
                    Respuesta r = GsonFactory.DEFAULT.fromJson(in.readLine(), Respuesta.class);
                    long ms = (System.nanoTime() - inicio) / 1_000_000;
                    String coincide = id.equals(r.getIdSolicitud()) ? "coincide" : "NO COINCIDE";
                    return String.format("%s  %-30s -> idRespuesta=%s (%s)  %s  %d ms",
                            id, destino[0] + "." + destino[1], r.getIdSolicitud(), coincide, r.getEstado(), ms);
                }
            }));
        }

        long inicio = System.nanoTime();
        salida.countDown();   // todos los clientes arrancan a la vez
        int correctas = 0;
        for (Future<String> f : futuros) {
            String linea = f.get();
            System.out.println(linea);
            if (linea.contains("(coincide)") && linea.contains("EXITO")) {
                correctas++;
            }
        }
        long total = (System.nanoTime() - inicio) / 1_000_000;
        ejecutor.shutdown();
        System.out.println();
        System.out.println("Resumen: " + correctas + "/" + cantidad
                + " clientes recibieron EXITO con el id de su propia solicitud; tiempo total " + total + " ms");
    }
}
```

### EvidenciaRendimiento.java

Mide la misma operación directo al proveedor y a través del Broker.

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import mx.uady.inventario.common.contrato.EstadoRespuesta;
import mx.uady.inventario.common.contrato.Respuesta;
import mx.uady.inventario.common.contrato.Solicitud;
import mx.uady.inventario.common.json.GsonFactory;

/**
 * Evidencia del costo del Broker en rendimiento: mide la misma operación enviada N veces
 * directamente al proveedor (Sistema 1) y N veces a través del Broker, con una conexión
 * nueva por solicitud en ambos casos (igual que el cliente real).
 *
 * Uso: java EvidenciaRendimiento <repeticiones> <puertoProveedor> <puertoBroker>
 */
public class EvidenciaRendimiento {

    public static void main(String[] args) throws Exception {
        int n = Integer.parseInt(args[0]);
        int puertoProveedor = Integer.parseInt(args[1]);
        int puertoBroker = Integer.parseInt(args[2]);

        // Calentamiento: las primeras solicitudes incluyen la carga de clases de la JVM.
        medir(5, puertoProveedor, "calentamiento-directo");
        medir(5, puertoBroker, "calentamiento-broker");

        double[] directo = medir(n, puertoProveedor, "rend-directo");
        double[] broker = medir(n, puertoBroker, "rend-broker");

        System.out.println("Operacion: gestorTiendas.listarTiendas, " + n + " solicitudes secuenciales por camino,"
                + " una conexion TCP nueva por solicitud.");
        System.out.println();
        System.out.printf("%-34s %10s %10s %10s %10s%n", "Camino", "promedio", "mediana", "minimo", "maximo");
        imprimir("Directo al Sistema 1 (:" + puertoProveedor + ")", directo);
        imprimir("A traves del Broker (:" + puertoBroker + ")", broker);
        System.out.println();
        System.out.printf("Resumen: el Broker agrega en promedio %.2f ms por solicitud (%.1f%% mas que la llamada directa).%n",
                promedio(broker) - promedio(directo), (promedio(broker) / promedio(directo) - 1) * 100);
    }

    private static double[] medir(int n, int puerto, String prefijo) throws Exception {
        double[] tiempos = new double[n];
        for (int i = 0; i < n; i++) {
            Solicitud s = Solicitud.crear(prefijo + "-" + i, "gestorTiendas", "listarTiendas", null);
            long inicio = System.nanoTime();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", puerto), 3000);
                socket.setSoTimeout(15_000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out.println(GsonFactory.DEFAULT.toJson(s));
                Respuesta r = GsonFactory.DEFAULT.fromJson(in.readLine(), Respuesta.class);
                if (r.getEstado() != EstadoRespuesta.EXITO) {
                    throw new IllegalStateException("Respuesta de error en " + puerto + ": " + r.getMensajeError());
                }
            }
            tiempos[i] = (System.nanoTime() - inicio) / 1_000_000.0;
        }
        return tiempos;
    }

    private static void imprimir(String camino, double[] t) {
        double[] orden = t.clone();
        Arrays.sort(orden);
        System.out.printf("%-34s %8.2f ms %8.2f ms %8.2f ms %8.2f ms%n", camino, promedio(t),
                orden[orden.length / 2], orden[0], orden[orden.length - 1]);
    }

    private static double promedio(double[] t) {
        return Arrays.stream(t).average().orElse(0);
    }
}
```

### comun.sh

Funciones compartidas por los scripts de evidencias.

```bash
#!/usr/bin/env bash
# Funciones compartidas por los scripts de evidencias.
# Requiere: el stack de docker compose arriba (uady-mariadb, uady-broker, uady-sistema-centralizado)
# y el proyecto compilado (mvn package) para usar el jar del cliente como classpath.

EV="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Raíz del checkout con el código (rama qa o dev/chris). Por defecto, cuatro niveles arriba de esta
# carpeta (docs/02_documentacion_final/markdown/04_evidencias_pruebas); se puede indicar otra con REPO=/ruta/al/repo.
REPO="${REPO:-$(cd "$EV/../../../.." && pwd)}"
CP="$REPO/sistema-cliente/target/sistema-cliente.jar"
JAVA="java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8"

# Envía una solicitud al Broker: cli <id> <servicio> <operacion> <parametrosJson>
cli() { $JAVA -cp "$CP" "$EV/EvidenciaCliente.java" "$@" 2>&1 | grep -v '^WARNING'; }

# Líneas de la bitácora del Broker de una solicitud: bitacora <id>
bitacora() { docker logs uady-broker 2>&1 | grep -F "id=$1 "; }

# Encabezado de un archivo de evidencia: encabezado <archivo> <titulo> <descripcion>
encabezado() {
  {
    echo "# $2"
    echo "Fecha (hora local del equipo): $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Codigo: rama dev/chris, commit $(git -C "$REPO" rev-parse --short HEAD)"
    echo
    echo "$3"
    echo
  } > "$EV/$1"
}

# Agrega una solicitud completa (JSON enviado y recibido) y su bitácora a un archivo:
# paso <archivo> <titulo del paso> <id> <servicio> <operacion> <parametrosJson>
paso() {
  local archivo="$1" titulo="$2"; shift 2
  {
    echo "## $titulo"
    echo
    cli "$@"
    echo
    echo "Bitacora del Broker (docker logs uady-broker):"
    bitacora "$1"
    echo
  } >> "$EV/$archivo"
}

# Consulta SQL en la base del stack: sql "<consulta>"
sql() { docker exec uady-mariadb sh -c "mariadb -uroot -p\"\$MARIADB_ROOT_PASSWORD\" -D \"\$MARIADB_DATABASE\" -e \"$1\""; }
```

### parte1.sh

Genera las evidencias 01–06, 11, 13 y 14 (stack sano).

```bash
#!/usr/bin/env bash
# Parte 1: evidencias con el stack sano (01-06, 11, 13 y 14).
source "$(dirname "$0")/comun.sh"

# 01 Registro de proveedores
encabezado 01_registro_proveedores.txt "Prueba 1: Registro correcto de proveedores" \
"Al arrancar, el Sistema 1 (proveedor) se registra dinamicamente en el Broker enviando la operacion
broker.registrarProveedor por socket, una vez por cada servicio que ofrece. El Broker asocia cada
nombre de servicio con el host y puerto del proveedor."
{
  echo "## Consola del Sistema 1 (docker logs uady-sistema-centralizado)"
  docker logs uady-sistema-centralizado 2>&1 | grep -E "Servicios:|Proveedor registrado|Escuchando"
  echo
  echo "## Bitacora del Broker (docker logs uady-broker)"
  docker logs uady-broker 2>&1 | grep -E "^Broker iniciado|registrarProveedor|\[REGISTRO\] .* registrar "
  echo
  echo "Resumen: los 3 servicios (gestorTiendas, inventario, gestorPrecios) quedan registrados hacia"
  echo "sistema-centralizado:6000. Prueba automatizada relacionada: RegistroServiciosTest (registrarYBuscar)."
} >> "$EV/01_registro_proveedores.txt"

# 02 Localizacion
encabezado 02_localizacion_servicio.txt "Prueba 2: Localizacion de un servicio" \
"El cliente solo conoce la direccion del Broker (localhost:5000). El Broker busca en su registro el
proveedor del servicio 'inventario' y le reenvia la solicitud. La bitacora muestra el proveedor localizado."
paso 02_localizacion_servicio.txt "inventario.listarInventario" ev-02 inventario listarInventario '{}'

# 03 Una operacion por servicio
encabezado 03_ejecucion_operacion.txt "Prueba 3: Ejecucion de una operacion (una por cada servicio registrado)" \
"Se ejecuta una operacion de cada uno de los 3 servicios registrados. Todas viajan por el Broker
como JSON por socket y llegan al proveedor localizado."
paso 03_ejecucion_operacion.txt "Servicio gestorTiendas: listarTiendas" ev-03a gestorTiendas listarTiendas '{}'
paso 03_ejecucion_operacion.txt "Servicio inventario: listarInventario" ev-03b inventario listarInventario '{}'
paso 03_ejecucion_operacion.txt "Servicio gestorPrecios: listarPreciosProducto" ev-03c gestorPrecios listarPreciosProducto '{"id_producto":1}'

# 04 Respuesta exitosa
encabezado 04_respuesta_exitosa.txt "Prueba 4: Respuesta exitosa" \
"El cliente registra la venta de una unidad (CU2). El Sistema 1 la procesa en una transaccion y la
respuesta incluye id de la solicitud, resultado, estado y fecha y hora."
paso 04_respuesta_exitosa.txt "gestorTiendas.actualizarTiendaVenta" ev-04 gestorTiendas actualizarTiendaVenta \
  '{"numero_serie":"SN-UADY-0003","id_tienda":1,"id_usuario":2}'
{
  echo "Estado en la base de datos:"
  sql "SELECT m.numero_serie, m.estado, v.id_tienda, v.id_usuario, v.fecha_venta FROM mercancia m JOIN ventas v ON v.numero_serie = m.numero_serie WHERE m.numero_serie = 'SN-UADY-0003'"
} >> "$EV/04_respuesta_exitosa.txt"

# 05 Servicio desconocido
encabezado 05_servicio_desconocido.txt "Prueba 5: Servicio desconocido" \
"El cliente pide un servicio que ningun proveedor registro. El Broker lo rechaza sin contactar a
ningun proveedor y sin caerse."
paso 05_servicio_desconocido.txt "servicioFantasma.listar" ev-05 servicioFantasma listar '{}'

# 06 Parametro invalido
encabezado 06_parametro_invalido.txt "Prueba 6: Parametro invalido" \
"Se envian ventas con parametros faltantes o invalidos. El proveedor los valida y responde
PARAMETROS_INVALIDOS con un mensaje claro, conservando el id de la solicitud."
paso 06_parametro_invalido.txt "Falta numero_serie" ev-06a gestorTiendas actualizarTiendaVenta '{"id_tienda":1,"id_usuario":2}'
paso 06_parametro_invalido.txt "id_tienda negativo" ev-06b gestorTiendas actualizarTiendaVenta '{"numero_serie":"SN-UADY-0001","id_tienda":-5,"id_usuario":2}'
paso 06_parametro_invalido.txt "Producto inexistente en gestorPrecios" ev-06c gestorPrecios listarPreciosProducto '{"id_producto":999}'

# 13 Solicitud duplicada (pregunta 9)
encabezado 13_solicitud_duplicada.txt "Evidencia adicional: solicitud duplicada (pregunta 9)" \
"La misma venta se envia dos veces. La primera se registra; la segunda se rechaza porque la unidad ya
esta VENDIDO. En la base de datos queda una sola venta: la transaccion con bloqueo pesimista
(SELECT ... FOR UPDATE) evita que una solicitud repetida registre la venta dos veces."
paso 13_solicitud_duplicada.txt "Primer envio" ev-13a gestorTiendas actualizarTiendaVenta '{"numero_serie":"SN-UADY-0005","id_tienda":2,"id_usuario":4}'
paso 13_solicitud_duplicada.txt "Segundo envio (misma venta, id de solicitud distinto)" ev-13b gestorTiendas actualizarTiendaVenta '{"numero_serie":"SN-UADY-0005","id_tienda":2,"id_usuario":4}'
{
  echo "Ventas registradas para SN-UADY-0005 en la base de datos:"
  sql "SELECT COUNT(*) AS ventas FROM ventas WHERE numero_serie = 'SN-UADY-0005'"
} >> "$EV/13_solicitud_duplicada.txt"

# 11 Concurrencia
encabezado 11_concurrencia_hilos.txt "Evidencia adicional: atencion concurrente con hilos" \
"20 clientes abren cada uno su propio socket y envian una solicitud al mismo tiempo. El Broker atiende
cada conexion en un hilo de su pool (16 hilos) y cada cliente recibe la respuesta con el id de SU
solicitud. La bitacora muestra los distintos hilos del pool que atendieron."
{
  echo "## Clientes"
  $JAVA -cp "$CP" "$EV/EvidenciaConcurrencia.java" 20 ev-11 2>&1 | grep -v '^WARNING'
  echo
  echo "## Bitacora del Broker"
  docker logs uady-broker 2>&1 | grep -F "id=ev-11-"
  echo
  echo "Hilos distintos del pool que atendieron: $(docker logs uady-broker 2>&1 | grep -F 'id=ev-11-' | grep -o 'pool-1-thread-[0-9]*' | sort -u | wc -l)"
  echo "Prueba automatizada relacionada: IntegracionFase5Test (veinteClientesSimultaneosATravesDelBrokerTerminanSinErrores)."
} >> "$EV/11_concurrencia_hilos.txt"

# 14 Rendimiento (pregunta 7)
encabezado 14_costo_rendimiento.txt "Evidencia adicional: costo del Broker en rendimiento (pregunta 7)" \
"La misma operacion se envia 100 veces directo al Sistema 1 (puerto 6000, publicado por Docker solo
para esta medicion) y 100 veces a traves del Broker (puerto 5000). La diferencia es el costo del salto
adicional: una conexion TCP mas, marshalling/unmarshalling extra y la busqueda en el registro."
$JAVA -cp "$CP" "$EV/EvidenciaRendimiento.java" 100 6000 5000 2>&1 | grep -v '^WARNING' >> "$EV/14_costo_rendimiento.txt"

echo "parte 1 terminada"
```

### parte2.sh

Genera las evidencias 08, 09 y 12; deja la consola del segundo proveedor en `proveedor2.log`.

```bash
#!/usr/bin/env bash
# Parte 2: evidencias que detienen componentes (07, 08, 09 y 12).
# Al final deja el stack como estaba: MariaDB, Broker y Sistema 1 arriba y registrados.
source "$(dirname "$0")/comun.sh"

esperar_sano() {  # esperar_sano <contenedor>
  sleep 5  # tras docker start el estado tarda en pasar de "healthy" (anterior) a "starting"
  for _ in $(seq 1 60); do
    [ "$(docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null)" = "healthy" ] && return 0
    sleep 1
  done
  echo "TIMEOUT esperando a $1" >&2; return 1
}

esperar_linea() {  # esperar_linea <contenedor> <texto> <cantidad minima>
  for _ in $(seq 1 60); do
    [ "$(docker logs "$1" 2>&1 | grep -cF -- "$2")" -ge "$3" ] && return 0
    sleep 1
  done
  echo "TIMEOUT esperando '$2' en $1" >&2; return 1
}

detener_puerto() {  # detener_puerto <puerto>: detiene el proceso que escucha en ese puerto del equipo
  powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort $1 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force }"
}

# ---------------------------------------------------------------------------------------------
# 07 Excepcion producida por un proveedor
encabezado 07_excepcion_proveedor.txt "Prueba 7: Excepcion producida por un proveedor" \
"Se detiene MariaDB mientras el Sistema 1 sigue arriba. Al atender la solicitud, el proveedor lanza una
excepcion de acceso a datos; su despachador la convierte en una respuesta ERROR_INTERNO y el Broker la
devuelve al cliente sin caerse. Al volver la base, la misma operacion funciona sin reiniciar nada."
echo "## Paso 1: docker stop uady-mariadb" >> "$EV/07_excepcion_proveedor.txt"
docker stop uady-mariadb >/dev/null
paso 07_excepcion_proveedor.txt "Paso 2: solicitud con la base detenida" ev-07a inventario listarInventario '{}'
paso 07_excepcion_proveedor.txt "Paso 3: el Broker sigue atendiendo otras solicitudes" ev-07b servicioFantasma listar '{}'
echo "## Paso 4: docker start uady-mariadb" >> "$EV/07_excepcion_proveedor.txt"
docker start uady-mariadb >/dev/null && esperar_sano uady-mariadb
sleep 5  # margen para que MariaDB acepte conexiones de la red de Docker
paso 07_excepcion_proveedor.txt "Paso 5: la misma solicitud con la base de vuelta (sin reiniciar el Broker ni el Sistema 1)" \
  ev-07c inventario listarInventario '{}'

# ---------------------------------------------------------------------------------------------
# 08 Cambio de proveedor sin modificar el cliente
encabezado 08_cambio_proveedor.txt "Prueba 8: Cambio de proveedor sin modificar el cliente" \
"Un segundo proveedor (otra instancia del Sistema 1, fuera de Docker, en el puerto 6001) se registra
en el Broker para los mismos servicios y sustituye al original; despues se apaga el original. El cliente
ejecuta exactamente el mismo comando antes y despues: solo cambia el proveedor que el Broker localiza."
paso 08_cambio_proveedor.txt "Paso 1: proveedor original" ev-08a gestorTiendas listarTiendas '{}'

echo "## Paso 2: arranca el proveedor 2 (host.docker.internal:6001) y se registra" >> "$EV/08_cambio_proveedor.txt"
set -a; source <(grep -E '^DB_(NAME|USER|PASSWORD)=' "$REPO/.env"); set +a
DB_HOST=localhost DB_PORT=3306 BROKER_HOST=localhost BROKER_PORT=5000 \
SERVICE_HOST=host.docker.internal SERVICE_PORT=6001 QR_DIR="${TMPDIR:-/tmp}/qr-proveedor2" \
  $JAVA -Djava.awt.headless=true -jar "$REPO/sistema-centralizado/target/sistema-centralizado.jar" \
  > "$EV/proveedor2.log" 2>&1 &
esperar_linea uady-broker "-> host.docker.internal:6001" 3
{
  grep -E "Proveedor registrado" "$EV/proveedor2.log"
  docker logs uady-broker 2>&1 | grep -F "[REGISTRO]" | grep -F "host.docker.internal:6001"
  echo
} >> "$EV/08_cambio_proveedor.txt"

echo "## Paso 3: se apaga el proveedor original (docker stop uady-sistema-centralizado)" >> "$EV/08_cambio_proveedor.txt"
docker stop uady-sistema-centralizado >/dev/null
echo >> "$EV/08_cambio_proveedor.txt"
paso 08_cambio_proveedor.txt "Paso 4: el mismo cliente, sin cambios, ahora lo atiende el proveedor 2" ev-08b gestorTiendas listarTiendas '{}'

# ---------------------------------------------------------------------------------------------
# 09 Proveedor caido: timeouts, reintentos y marca de no disponible
encabezado 09_proveedor_caido_timeout.txt "Evidencia adicional: proveedor caido, reintentos limitados y desconexion" \
"Se apaga el proveedor activo (el proveedor 2) sin que se de de baja. El Broker reintenta de forma
limitada (3 intentos, 200 ms entre ellos), responde con error y, tras 3 solicitudes fallidas, marca el
servicio como NO DISPONIBLE: la siguiente solicitud se rechaza de inmediato sin esperar. Al volver un
proveedor y registrarse, el servicio se reactiva."
detener_puerto 6001
echo "## Paso 1: se detiene el proveedor 2 (puerto 6001)" >> "$EV/09_proveedor_caido_timeout.txt"
echo >> "$EV/09_proveedor_caido_timeout.txt"
paso 09_proveedor_caido_timeout.txt "Paso 2: primera solicitud (3 intentos)" ev-09a inventario listarInventario '{}'
paso 09_proveedor_caido_timeout.txt "Paso 3: segunda solicitud" ev-09b inventario listarInventario '{}'
paso 09_proveedor_caido_timeout.txt "Paso 4: tercera solicitud (alcanza el umbral)" ev-09c inventario listarInventario '{}'
{
  echo "Marca de no disponible en la bitacora:"
  docker logs uady-broker 2>&1 | grep -F "NO DISPONIBLE"
  echo
} >> "$EV/09_proveedor_caido_timeout.txt"
paso 09_proveedor_caido_timeout.txt "Paso 5: cuarta solicitud (rechazo inmediato, sin contactar al proveedor)" ev-09d inventario listarInventario '{}'

echo "## Paso 6: vuelve el proveedor original (docker start uady-sistema-centralizado) y se registra de nuevo" >> "$EV/09_proveedor_caido_timeout.txt"
antes=$(docker logs uady-broker 2>&1 | grep -cF "registrar inventario -> sistema-centralizado:6000")
docker start uady-sistema-centralizado >/dev/null
esperar_linea uady-broker "registrar inventario -> sistema-centralizado:6000" $((antes + 1))
sleep 1
docker logs uady-broker 2>&1 | grep -F "[REGISTRO]" | tail -3 >> "$EV/09_proveedor_caido_timeout.txt"
echo >> "$EV/09_proveedor_caido_timeout.txt"
paso 09_proveedor_caido_timeout.txt "Paso 7: el servicio vuelve a responder" ev-09e inventario listarInventario '{}'

# ---------------------------------------------------------------------------------------------
# 12 El Broker deja de funcionar (preguntas 3 y 4)
encabezado 12_broker_caido.txt "Evidencia adicional: el Broker deja de funcionar (preguntas 3 y 4)" \
"Se detiene el Broker. El cliente no puede comunicarse con ningun servicio aunque el Sistema 1 siga
funcionando: el Broker es un punto unico de falla. Al reiniciarlo, su registro (en memoria) esta vacio y
el Sistema 1 no se vuelve a registrar por si solo, asi que los servicios aparecen como desconocidos hasta
reiniciar el proveedor. Esta limitacion es una mejora pendiente (re-registro periodico, error #2)."
echo "## Paso 1: docker stop uady-broker" >> "$EV/12_broker_caido.txt"
docker stop uady-broker >/dev/null
paso 12_broker_caido.txt "Paso 2: el cliente intenta una solicitud" ev-12a gestorTiendas listarTiendas '{}'
{
  echo "## Paso 3: el Sistema 1 sigue en ejecucion (docker ps)"
  docker ps --filter name=uady- --format "{{.Names}} | {{.Status}}"
  echo
  echo "## Paso 4: docker start uady-broker"
} >> "$EV/12_broker_caido.txt"
docker start uady-broker >/dev/null && esperar_sano uady-broker
paso 12_broker_caido.txt "Paso 5: el Broker volvio, pero su registro esta vacio" ev-12b gestorTiendas listarTiendas '{}'
echo "## Paso 6: se reinicia el Sistema 1, que se registra al arrancar" >> "$EV/12_broker_caido.txt"
antes=$(docker logs uady-broker 2>&1 | grep -cF "registrar gestorTiendas -> sistema-centralizado:6000")
docker restart uady-sistema-centralizado >/dev/null
esperar_linea uady-broker "registrar gestorTiendas -> sistema-centralizado:6000" $((antes + 1))
sleep 1
echo >> "$EV/12_broker_caido.txt"
paso 12_broker_caido.txt "Paso 7: el servicio vuelve a responder" ev-12c gestorTiendas listarTiendas '{}'

echo "parte 2 terminada"
docker ps --filter name=uady- --format "{{.Names}} | {{.Status}}"
```

### parte3.sh

Genera la evidencia 07 (excepción del proveedor y base de datos detenida).

```bash
#!/usr/bin/env bash
# Parte 3: prueba 7 (excepcion producida por un proveedor), de forma determinista.
source "$(dirname "$0")/comun.sh"

esperar_sano() {
  sleep 5
  for _ in $(seq 1 60); do
    [ "$(docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null)" = "healthy" ] && return 0
    sleep 1
  done
  echo "TIMEOUT esperando a $1" >&2; return 1
}

A=07_excepcion_proveedor.txt
encabezado $A "Prueba 7: Excepcion producida por un proveedor" \
"Caso A: se provoca una excepcion real dentro del proveedor (la tabla 'tiendas_almacenes' deja de existir de forma
temporal, asi que la consulta JDBC lanza SQLException). El despachador del Sistema 1 convierte la
excepcion en una respuesta ERROR_INTERNO, conservando el id; el Broker la devuelve al cliente y sigue
atendiendo. Al restaurar la tabla, la misma operacion funciona sin reiniciar el Broker ni el Sistema 1.

Caso B (complemento): si la base de datos se detiene por completo, el proveedor no responde dentro del
timeout de lectura del Broker (5 s); el Broker reintenta 3 veces y responde TIMEOUT."

{
  echo "## Caso A, paso 1: RENAME TABLE tiendas_almacenes TO tiendas_respaldo"
  sql "RENAME TABLE tiendas_almacenes TO tiendas_respaldo"
  echo
} >> "$EV/$A"
paso $A "Caso A, paso 2: solicitud mientras la tabla no existe" ev-07-c1 gestorTiendas listarTiendas '{}'
{
  echo "Consola del Sistema 1 en ese momento (docker logs uady-sistema-centralizado):"
  docker logs uady-sistema-centralizado 2>&1 | grep -iE "tiendas|SQL" | tail -3
  echo
} >> "$EV/$A"
paso $A "Caso A, paso 3: el Broker sigue atendiendo otras operaciones" ev-07-c2 inventario listarInventario '{}'
{
  echo "## Caso A, paso 4: RENAME TABLE tiendas_respaldo TO tiendas_almacenes"
  sql "RENAME TABLE tiendas_respaldo TO tiendas_almacenes"
  echo
} >> "$EV/$A"
paso $A "Caso A, paso 5: la misma solicitud, sin reiniciar nada" ev-07-c3 gestorTiendas listarTiendas '{}'

echo "## Caso B, paso 1: docker stop uady-mariadb" >> "$EV/$A"
docker stop uady-mariadb >/dev/null
{
  echo "## Caso B, paso 2: solicitud con la base detenida (cliente con espera de 30 s para ver la respuesta del Broker)"
  echo
  $JAVA -DtimeoutMs=30000 -cp "$CP" "$EV/EvidenciaCliente.java" ev-07-d1 inventario listarInventario '{}' 2>&1 | grep -v '^WARNING'
  echo
  echo "Bitacora del Broker (docker logs uady-broker):"
  bitacora ev-07-d1
  echo
  echo "Observacion: el Broker tarda unos 15 s (3 intentos x 5 s) en responder TIMEOUT. El cliente del"
  echo "Sistema 2 espera 5 s, asi que el usuario veria primero su propio timeout. Es la limitacion #1 de la"
  echo "revision de errores (alinear el timeout del cliente con el peor caso del Broker y no reintentar"
  echo "operaciones que ya llegaron al proveedor)."
  echo
  echo "## Caso B, paso 3: docker start uady-mariadb"
} >> "$EV/$A"
docker start uady-mariadb >/dev/null && esperar_sano uady-mariadb
sleep 5
paso $A "Caso B, paso 4: con la base de vuelta, sin reiniciar nada" ev-07-d2 inventario listarInventario '{}'

echo "parte 3 terminada"
```
