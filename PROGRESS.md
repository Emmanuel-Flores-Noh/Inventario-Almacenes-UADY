# Progreso del issue

## Estado por fase
- [x] Fase 1: Persistencia
- [x] Fase 2: Servidor de servicio del Sistema 1
- [x] Fase 3: Resiliencia en el Broker
- [ ] Fase 4: Despliegue
- [ ] Fase 5: Pruebas

## Fase actual
Ninguna iniciada. La Fase 3 esta completada y verificada.

## Pasos de la fase actual
- [x] Paso 1: `ConfiguracionBroker` con timeouts de conexion/lectura, reintentos y salud de proveedores + pruebas.
- [x] Paso 2: Salud de proveedores en `RegistroServicios` (fallos consecutivos, enfriamiento y reactivacion) + pruebas.
- [x] Paso 3: `Enrutador` con timeouts separados, reintentos limitados y marca de no disponible + pruebas.
- [x] Paso 4: Cableado de `BrokerServer` y `Main` con `ConfiguracionBroker` desde variables de entorno.
- [x] Paso 5: Verificacion con jars reales y cierre de fase con evidencia por criterio.

## Paso en curso
- Ninguno.

## Siguiente paso
- Esperar instrucciones para iniciar la Fase 4 (despliegue).

## Decisiones y suposiciones
- Los modelos de las 7 tablas se reutilizan de `common.modelo`; no se duplican en `central.model`.
- `Mercancia.estado` es `EstadoMercancia`. Gson serializa enums por nombre, asi que el JSON conserva `"EN_ALMACEN"`/`"VENDIDO"`.
- `DB_HOST`, `DB_PORT` y `DB_NAME` tienen defaults (`localhost`, `3306`, `almacenes_uady`); `DB_USER` y `DB_PASSWORD` son obligatorios. Sin credenciales en el codigo.
- `ConfiguracionBD.desdeMapa(Map)` existe para probar la lectura de variables sin modificar el entorno del JVM; `desdeEntorno()` delega en `System.getenv()`.
- `ConexionBD` no mantiene conexiones estaticas ni pool; cada operacion abre y cierra la suya. En la Fase 2 no se introdujo un pool de conexiones a BD: el pool de `SERVICE_POOL_SIZE` es de hilos de red.
- El seed (`02_seed.sql`) no incluye las tablas `ventas` ni `historial_precio`.
- Hay piezas de mercancia con estado `VENDIDO` que son ilustrativas: no tienen una venta registrada. Cuando se implemente el registro de ventas (fase posterior), hay que decidir si se mantienen asi o se agregan ventas correspondientes en el seed.
- `02_seed.sql` es idempotente con IDs explicitos e `INSERT ... ON DUPLICATE KEY UPDATE`; se aplico dos veces y los conteos no cambiaron.
- `ConexionBDIntegracionTest` se activa solo con `DB_PASSWORD` definida (`@EnabledIfEnvironmentVariable`), de modo que `mvn test` sigue funcionando sin MariaDB.
- Variables nuevas de la Fase 2: `SERVICE_PORT` (6000), `SERVICE_HOST` (localhost), `SERVICE_POOL_SIZE` (16), `BROKER_HOST` (localhost), `BROKER_PORT` (5000), `REGISTRO_INTENTOS` (30) y `REGISTRO_ESPERA_MS` (2000).
- `SERVICE_HOST` es el host con el que el proveedor se anuncia al broker; en Docker debe ser `sistema-centralizado`, pero ese valor se configura en la Fase 4 (despliegue).
- La Fase 2 solo implementa el servidor y el despacho: las 4 operaciones del contrato se reconocen pero responden `OPERACION_NO_SOPORTADA` hasta que existan los repositorios/servicios de negocio.
- `ConfiguracionServicio` acepta `SERVICE_PORT=0` para pedir un puerto efimero en pruebas.
- El proveedor expone los tres servicios (`gestorTiendas`, `inventario`, `gestorPrecios`) en un solo `SERVICE_PORT`; por eso envia tres `registrarProveedor`, uno por cada nombre.
- `DespachadorSolicitudes` toma el catalogo de `Servicios`/`Operaciones`. Una operacion declarada sin manejador responde `OPERACION_NO_SOPORTADA`; `IllegalArgumentException` de un manejador se traduce a `PARAMETROS_INVALIDOS` y cualquier otra excepcion a `ERROR_INTERNO`. En todos los casos se conserva el `idSolicitud`.
- El registro en el broker corre en un hilo daemon para no bloquear el arranque del servidor. Al agotar `REGISTRO_INTENTOS` se escribe el fallo en `stderr` y el servidor sigue escuchando.
- El servidor de servicio no abre conexiones a MariaDB al arrancar: la red y la persistencia fallan de forma independiente.
- No se envia `removerProveedor` al apagar; queda pendiente para una fase posterior si se necesita.
- Variables nuevas de la Fase 3 (broker) con sus valores por defecto:
  - `BROKER_PORT` = `5000` (ya existia; ahora se lee en `ConfiguracionBroker`).
  - `BROKER_POOL_SIZE` = `16` (ya existia; ahora se lee en `ConfiguracionBroker`).
  - `BROKER_TIMEOUT_CONEXION_MS` = `2000`: timeout de `connect` hacia el proveedor.
  - `BROKER_TIMEOUT_LECTURA_MS` = `5000`: timeout de lectura de la respuesta del proveedor.
  - `BROKER_REINTENTOS` = `2`: reintentos **adicionales** tras el primer intento (hasta 3 intentos).
  - `BROKER_ESPERA_REINTENTO_MS` = `200`: espera entre intentos.
  - `BROKER_FALLOS_PARA_INDISPONIBLE` = `3`: fallos consecutivos antes de marcar al proveedor como no disponible.
  - `BROKER_INDISPONIBLE_MS` = `30000`: enfriamiento configurable antes de reintentar un proveedor marcado.
- Los nombres y defaults de la Fase 3 se acordaron con el usuario y viven en `ConfiguracionBroker.desdeMapa`.

## Problemas abiertos
- `docker-compose.yml` no define `SERVICE_HOST`; dentro de Docker el proveedor se anunciaria como `localhost` y el broker no lo alcanzaria. Debe resolverse en la Fase 4 (despliegue).
- `docker-compose.yml` tampoco define las variables nuevas del broker (`BROKER_TIMEOUT_*`, `BROKER_REINTENTOS`, `BROKER_FALLOS_PARA_INDISPONIBLE`, `BROKER_INDISPONIBLE_MS`); sin ellas el broker usa los defaults. Se ajusta en la Fase 4.
- Las 4 operaciones del contrato siguen sin logica de negocio: responden `OPERACION_NO_SOPORTADA` hasta que existan repositorios y servicios.

## Como verificar lo hecho
- mvn test
- mvn clean package
- `java -jar broker/target/broker.jar` con `BROKER_PORT` y, en otra terminal, `java -jar sistema-centralizado/target/sistema-centralizado.jar` con `SERVICE_PORT`, `SERVICE_HOST`, `BROKER_HOST`, `BROKER_PORT`.
- Resiliencia del broker: `BROKER_TIMEOUT_CONEXION_MS`, `BROKER_TIMEOUT_LECTURA_MS`, `BROKER_REINTENTOS`, `BROKER_ESPERA_REINTENTO_MS`, `BROKER_FALLOS_PARA_INDISPONIBLE`, `BROKER_INDISPONIBLE_MS`. El arranque imprime los valores efectivos.
- Sin broker levantado, el servicio reintenta y luego reporta el fallo en `stderr` sin apagarse.
- docker compose up -d mariadb
- Aplicar `docker/mariadb/init/02_seed.sql` y consultar conteos
- Con `DB_*` exportadas: `mvn -pl sistema-centralizado -am test "-Dtest=ConexionBDIntegracionTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

## Evidencia de la Fase 1
- `mvn clean package`: BUILD SUCCESS; `common` 10 pruebas, `sistema-centralizado` 4 unitarias + 1 integracion omitida sin BD, `broker` 19 pruebas.
- `ConfiguracionBDTest` y `ModelosGsonTest` en verde.
- MariaDB 11.4 arrancada con `docker compose up -d mariadb`; la semilla se aplico en la inicializacion del volumen.
- Conteos verificados: roles 3, usuarios 4, tiendas_almacenes 3, productos 5, mercancia 10 (6 EN_ALMACEN, 4 VENDIDO).
- `ConexionBDIntegracionTest` ejecutado contra MariaDB real: 1 prueba, 0 fallos.

## Evidencia de la Fase 2
- `mvn test`: BUILD SUCCESS. `common` 10 pruebas, `sistema-centralizado` 34 pruebas (1 omitida: la de integracion con MariaDB), `broker` 19 pruebas. Sin fallos.
- Pruebas nuevas en verde: `ConfiguracionServicioTest` (5), `JsonServicioTest` (3), `DespachadorSolicitudesTest` (9), `ServidorServicioTest` (6, incluida concurrencia de 20 clientes) y `RegistradorBrokerTest` (6, con broker falso y reintentos).
- `mvn clean package`: BUILD SUCCESS; genera `broker/target/broker.jar` y `sistema-centralizado/target/sistema-centralizado.jar`.
- Verificacion integrada real (puertos 6100/5100): el servicio arranco antes que el broker y reintento; al levantar el broker imprimio `Proveedor registrado en el broker localhost:5100 como localhost:6100 (gestorTiendas, inventario, gestorPrecios)`.
- Solicitud enviada al broker y enrutada al servicio real: respuesta `{"idSolicitud":"e2e-fase2","estado":"ERROR","codigoError":"OPERACION_NO_SOPORTADA","mensajeError":"Operacion pendiente de implementar: listarInventario",...}`. Esto comprueba registro, enrutamiento broker->proveedor, despacho y conservacion del `idSolicitud`.
- Con el broker apagado y `REGISTRO_INTENTOS=2`, el servicio imprimio en `stderr`: `No se pudo registrar en el broker localhost:5199 tras 2 intentos; el servicio seguira escuchando en el puerto 6101`.

## Evidencia de la Fase 3
- `mvn test`: BUILD SUCCESS. `common` 10 pruebas, `sistema-centralizado` 34 pruebas (1 omitida: la de integracion con MariaDB), `broker` 38 pruebas. Sin fallos.
- `mvn clean package`: BUILD SUCCESS; genera `broker/target/broker.jar`.
- Pruebas nuevas en verde: `ConfiguracionBrokerTest` (6), `RegistroServiciosTest` ampliado (15) y `EnrutadorTest` ampliado (9); `BrokerServerTest` (4) incluye el cableado de la configuracion.
- Criterio "timeouts de conexion y lectura configurables": con un proveedor que no responde, `BROKER_TIMEOUT_LECTURA_MS=800` devolvio `TIMEOUT` en `elapsed_ms=1022`; con `BROKER_TIMEOUT_LECTURA_MS=2500` devolvio `TIMEOUT` en `elapsed_ms=2736`. El arranque imprimio los valores efectivos: `timeout conexion 2000 ms, timeout lectura 800 ms` y luego `2500 ms`.
- Criterio "reintentos limitados": con `BROKER_REINTENTOS=2`, `BROKER_TIMEOUT_LECTURA_MS=500` y un proveedor que cierra cada conexion sin responder, el proveedor registro `conexiones=3` (1 intento + 2 reintentos) y el cliente recibio `ERROR_INTERNO`.
- Criterio "proveedor que falla repetidamente se marca no disponible": con `BROKER_FALLOS_PARA_INDISPONIBLE=2`, `BROKER_REINTENTOS=0` y un proveedor que cierra sin responder, la 1a y 2a solicitud devolvieron `ERROR_INTERNO` y la 3a devolvio `{"codigoError":"PROVEEDOR_NO_REGISTRADO","mensajeError":"Proveedor no disponible: inventario"}`; el proveedor solo conto `conexiones=2`, es decir, la 3a solicitud ya no lo contacto.
- El enfriamiento (`BROKER_INDISPONIBLE_MS`) es configurable y su vencimiento reactiva al proveedor; cubierto por `elEnfriamientoVenceYReactiva` con reloj inyectado. Un nuevo `registrarProveedor`/`actualizarProveedor` tambien lo reactiva.
- La verificacion manual se hizo con un arnes temporal fuera del repositorio (`%TEMP%\opencode\fase3\Verificar.java`), no versionado.
