# Evidencias de las pruebas

Generadas el 2026-11-14 sobre la rama `dev/chris`, commit `bb03b8c`. Se usó el stack de Docker Compose (MariaDB, Broker y Sistema 1, cada uno en su
propio proceso y contenedor) y clientes de evidencias que **solo conocen la dirección del Broker**
(`localhost:5000`). Los clientes usan las clases `Solicitud`, `Respuesta` y `GsonFactory` del módulo
`common`, igual que el Sistema 2.

Cada archivo muestra el JSON enviado, el JSON recibido y las líneas de la **bitácora del Broker**
(`docker logs uady-broker`) que produjo esa solicitud.

## Pruebas requeridas por la actividad

| # | Prueba requerida | Evidencia | Resultado |
|---|---|---|---|
| 1 | Registro correcto de proveedores | `01_registro_proveedores.txt` | Al arrancar, el Sistema 1 registra sus 3 servicios (`gestorTiendas`, `inventario`, `gestorPrecios`) en el Broker; la bitácora muestra cada `[REGISTRO] registrar … -> sistema-centralizado:6000` |
| 2 | Localización de un servicio | `02_localizacion_servicio.txt` | `inventario.listarInventario` → el Broker localiza `sistema-centralizado:6000` → `EXITO` |
| 3 | Ejecución de una operación (una por servicio) | `03_ejecucion_operacion.txt` | `gestorTiendas.listarTiendas`, `inventario.listarInventario` y `gestorPrecios.listarPreciosProducto` → `EXITO` |
| 4 | Respuesta exitosa | `04_respuesta_exitosa.txt` | Venta de `SN-UADY-0003` → `EXITO` con id, resultado, estado y fecha y hora; la unidad queda `VENDIDO` en la BD |
| 5 | Servicio desconocido | `05_servicio_desconocido.txt` | `servicioFantasma` → `ERROR SERVICIO_DESCONOCIDO`, sin contactar a ningún proveedor |
| 6 | Parámetro inválido | `06_parametro_invalido.txt` | Falta `numero_serie`, `id_tienda` negativo y producto inexistente → `PARAMETROS_INVALIDOS` con mensaje claro |
| 7 | Excepción producida por un proveedor | `07_excepcion_proveedor.txt` | Con la tabla `tiendas_almacenes` ausente, el proveedor lanza `SQLException` → `ERROR_INTERNO`; el Broker sigue atendiendo y, al restaurarla, la operación funciona sin reiniciar nada. Caso B: con MariaDB detenida, el Broker responde `TIMEOUT` tras 3 intentos |
| 8 | Cambio de proveedor sin modificar el cliente | `08_cambio_proveedor.txt` | El mismo comando del cliente lo atiende primero `sistema-centralizado:6000` y, tras registrarse un segundo proveedor y apagar el original, `host.docker.internal:6001` |

## Requisitos del Nivel 2 y preguntas de análisis

| Requisito / pregunta | Evidencia | Resultado |
|---|---|---|
| Timeouts, reintentos limitados y desconexiones | `09_proveedor_caido_timeout.txt` | Proveedor apagado: 3 intentos por solicitud (`intento 1/3 … 3/3`), tras 3 solicitudes fallidas el servicio queda `NO DISPONIBLE` y se rechaza al instante; al volver el proveedor se reactiva |
| Pruebas automatizadas | `10_pruebas_automatizadas.txt` | `mvn test` con MariaDB: **195 pruebas, 0 fallos, 0 omitidas** (Common 10, Broker 47, Sistema 1 129, Sistema 2 9) |
| Atención concurrente con hilos | `11_concurrencia_hilos.txt` | 20 clientes simultáneos: **20/20** reciben `EXITO` con el id de su propia solicitud; los atendieron **16 hilos distintos** del pool |
| Pregunta 3 y 4: el Broker deja de funcionar / punto único de falla | `12_broker_caido.txt` | Sin Broker, el cliente no llega a ningún servicio aunque el Sistema 1 siga vivo. Al reiniciar el Broker, su registro en memoria está vacío (`SERVICIO_DESCONOCIDO`) hasta que el Sistema 1 se vuelve a registrar |
| Pregunta 9: solicitudes duplicadas | `13_solicitud_duplicada.txt` | La misma venta dos veces: la segunda se rechaza (`ya fue vendida`) y en la BD queda **una sola** venta |
| Pregunta 7: costo en rendimiento | `14_costo_rendimiento.txt` | 100 solicitudes por camino: directo 3.53 ms, por el Broker 4.45 ms de promedio → **+0.92 ms por solicitud** |
| Bitácora de solicitudes / mensajes en consola / pregunta 10 (monitoreo) | Todas | Cada archivo incluye las líneas `[BROKER]` y `[REGISTRO]` de la bitácora |

## Observaciones

- **Hora UTC en los contenedores.** Los contenedores corren en UTC, así que la bitácora y la `fecha_venta`
  muestran 6 horas más que la hora local del equipo (por ejemplo, 15:14 en lugar de 09:14). La fecha en
  el encabezado de cada archivo es la hora local. Es una limitación conocida (zona horaria, error #6).
- **Timeout del cliente frente al Broker** (`07`, caso B). Cuando el proveedor no responde, el Broker
  tarda unos 15 s (3 intentos × 5 s) en contestar `TIMEOUT`; el Sistema 2 espera 5 s, así que el
  usuario vería antes su propio timeout. Es una limitación conocida (error #1).
- **Re-registro tras reiniciar el Broker** (`12`). El Sistema 1 solo se registra al arrancar. Es una
  limitación conocida (error #2); la mejora es un re-registro periódico.

## Cómo reproducirlas

Requisitos: Docker Desktop, JDK 21+, Maven y Git Bash. Los pasos 1 y 2 se ejecutan en la raíz de un
checkout con el código (rama `qa` o `dev/chris`, que incluyen la bitácora del Broker):

1. `mvn -B package -DskipTests` (los clientes usan `sistema-cliente/target/sistema-cliente.jar` como classpath).
2. `docker compose -p evidencias up --build -d mariadb broker sistema-centralizado`
3. Desde esta carpeta, en Git Bash: `bash parte1.sh`, `bash parte2.sh` y `bash parte3.sh`.
   Los scripts buscan el código tres carpetas arriba de esta; si esta carpeta está en otro checkout
   (por ejemplo, en la rama `docs`), indica la ruta del código: `REPO=/ruta/al/checkout bash parte1.sh`.
   - `parte1.sh`: 01–06, 11, 13 y 14 (stack sano).
   - `parte2.sh`: 08, 09 y 12 (detiene y reinicia componentes; levanta un segundo proveedor en el puerto 6001).
   - `parte3.sh`: 07 (excepción del proveedor y base de datos detenida).
4. La 10 se obtiene con `mvn -B test` y las variables `DB_*` apuntando a una MariaDB con el esquema.
5. Al terminar: `docker compose -p evidencias down -v`.

## Archivos de apoyo

| Archivo | Uso |
|---|---|
| `EvidenciaCliente.java` | Envía una solicitud al Broker y muestra el JSON enviado y recibido |
| `EvidenciaConcurrencia.java` | N clientes simultáneos; verifica que cada uno reciba su propia respuesta |
| `EvidenciaRendimiento.java` | Mide la misma operación directo al proveedor y a través del Broker |
| `comun.sh`, `parte1.sh`, `parte2.sh`, `parte3.sh` | Scripts que generan los archivos de evidencia (`parte2.sh` deja la consola del segundo proveedor en `proveedor2.log`) |
