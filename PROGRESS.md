# Progreso del issue

## Estado por fase
- [x] Fase 1: Persistencia
- [ ] Fase 2: Servidor de servicio del Sistema 1
- [ ] Fase 3: Resiliencia en el Broker
- [ ] Fase 4: Despliegue
- [ ] Fase 5: Pruebas

## Fase actual
Fase 2: Servidor de servicio del Sistema 1 (en curso).

## Pasos de la fase actual
- [x] Paso 1: `ConfiguracionServicio` (`SERVICE_PORT`, `SERVICE_HOST`, `SERVICE_POOL_SIZE`, `BROKER_*`, reintentos de registro) + pruebas.
- [x] Paso 2: `JsonServicio` y `DespachadorSolicitudes` con conversion de excepciones a `CodigoError` + pruebas.
- [x] Paso 3: `ServidorServicio` concurrente y `ManejadorConexionServicio` + pruebas de socket.
- [x] Paso 4: `RegistradorBroker` con reintento acotado + pruebas con broker falso.
- [x] Paso 5: Cableado de `Main` (arranque, hook de apagado, registro en segundo plano).
- [x] Paso 6: Verificacion integrada broker + servicio con jars reales.
- [ ] Paso 7: Cierre de fase en PROGRESS.md con evidencia por criterio.

## Paso en curso
- Paso 7: cierre de la fase y evidencia.

## Siguiente paso
- Actualizar PROGRESS.md, marcar la Fase 2 y hacer el commit final.

## Decisiones y suposiciones
- Los modelos de las 7 tablas se reutilizan de `common.modelo`; no se duplican en `central.model`.
- `Mercancia.estado` es `EstadoMercancia`. Gson serializa enums por nombre, asi que el JSON conserva `"EN_ALMACEN"`/`"VENDIDO"`.
- `DB_HOST`, `DB_PORT` y `DB_NAME` tienen defaults (`localhost`, `3306`, `almacenes_uady`); `DB_USER` y `DB_PASSWORD` son obligatorios. Sin credenciales en el codigo.
- `ConfiguracionBD.desdeMapa(Map)` existe para probar la lectura de variables sin modificar el entorno del JVM; `desdeEntorno()` delega en `System.getenv()`.
- `ConexionBD` no mantiene conexiones estaticas ni pool; cada operacion abre y cierra la suya. El pool, si hace falta, es decision de la Fase 2.
- El seed (`02_seed.sql`) no incluye las tablas `ventas` ni `historial_precio`.
- Hay piezas de mercancia con estado `VENDIDO` que son ilustrativas: no tienen una venta registrada. En la Fase 2, cuando se implemente el registro de ventas, hay que decidir si se mantienen asi o se agregan ventas correspondientes en el seed.
- `02_seed.sql` es idempotente con IDs explicitos e `INSERT ... ON DUPLICATE KEY UPDATE`; se aplico dos veces y los conteos no cambiaron.
- `ConexionBDIntegracionTest` se activa solo con `DB_PASSWORD` definida (`@EnabledIfEnvironmentVariable`), de modo que `mvn test` sigue funcionando sin MariaDB.
- Variables nuevas de la Fase 2: `SERVICE_PORT` (6000), `SERVICE_HOST` (localhost), `SERVICE_POOL_SIZE` (16), `BROKER_HOST` (localhost), `BROKER_PORT` (5000), `REGISTRO_INTENTOS` (30) y `REGISTRO_ESPERA_MS` (2000).
- `SERVICE_HOST` es el host con el que el proveedor se anuncia al broker; en Docker debe ser `sistema-centralizado`, pero ese valor se configura en la Fase 4 (despliegue).
- La Fase 2 solo implementa el servidor y el despacho: las 4 operaciones del contrato se reconocen pero responden `OPERACION_NO_SOPORTADA` hasta que existan los repositorios/servicios de negocio.
- `ConfiguracionServicio` acepta `SERVICE_PORT=0` para pedir un puerto efimero en pruebas.

## Problemas abiertos
- (ninguno)

## Como verificar lo hecho
- mvn test
- mvn clean package
- docker compose up -d mariadb
- Aplicar `docker/mariadb/init/02_seed.sql` y consultar conteos
- Con `DB_*` exportadas: `mvn -pl sistema-centralizado -am test "-Dtest=ConexionBDIntegracionTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

## Evidencia de la Fase 1
- `mvn clean package`: BUILD SUCCESS; `common` 10 pruebas, `sistema-centralizado` 4 unitarias + 1 integracion omitida sin BD, `broker` 19 pruebas.
- `ConfiguracionBDTest` y `ModelosGsonTest` en verde.
- MariaDB 11.4 arrancada con `docker compose up -d mariadb`; la semilla se aplico en la inicializacion del volumen.
- Conteos verificados: roles 3, usuarios 4, tiendas_almacenes 3, productos 5, mercancia 10 (6 EN_ALMACEN, 4 VENDIDO).
- `ConexionBDIntegracionTest` ejecutado contra MariaDB real: 1 prueba, 0 fallos.
