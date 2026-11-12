# Progreso del issue

## Estado por fase
- [ ] Fase 1: Persistencia (en curso)
- [ ] Fase 2: Servidor de servicio del Sistema 1
- [ ] Fase 3: Resiliencia en el Broker
- [ ] Fase 4: Despliegue
- [ ] Fase 5: Pruebas

## Fase actual
Fase 1: Persistencia

## Pasos de la fase actual
- [x] Paso 1: Enum `EstadoMercancia` (EN_ALMACEN, VENDIDO) en `common` y adaptacion de `Mercancia` + pruebas.
- [ ] Paso 2: `ConfiguracionBD` y `ConexionBD` en `central/util` leyendo `DB_*` + pruebas unitarias.
- [ ] Paso 3: Script `docker/mariadb/init/02_seed.sql` con roles, usuarios, tiendas, productos y mercancia.
- [ ] Paso 4: `ConexionBDIntegracionTest` contra MariaDB real para verificar conexion y semilla.
- [ ] Paso 5: Cierre de fase en PROGRESS.md y evidencia por criterio.

## Paso en curso
- Paso 2: crear `ConfiguracionBD` y `ConexionBD` en `central/util` para leer `DB_*` y abrir conexiones JDBC, con pruebas unitarias.

## Siguiente paso
- Implementar el Paso 2 y verificar con `mvn -pl sistema-centralizado -am test`.

## Decisiones y suposiciones
- Los modelos de las 7 tablas se reutilizan de `common.modelo`; no se duplican en `central.model`.
- `Mercancia.estado` pasa a `EstadoMercancia`. Gson serializa enums por nombre, asi que el JSON conserva `"EN_ALMACEN"`/`"VENDIDO"`.
- `DB_HOST`, `DB_PORT` y `DB_NAME` tienen defaults (`localhost`, `3306`, `almacenes_uady`); `DB_USER` y `DB_PASSWORD` son obligatorios. Sin credenciales en el codigo.
- La semilla solo cubre las 5 tablas pedidas (roles, usuarios, tiendas, productos, mercancia); no siembra `ventas` ni `historial_precio`.
- `02_seed.sql` es idempotente con IDs explicitos e `INSERT ... ON DUPLICATE KEY UPDATE`.

## Problemas abiertos
- (ninguno)

## Como verificar lo hecho
- mvn test
- mvn clean package
- docker compose up -d mariadb
- Aplicar `docker/mariadb/init/02_seed.sql` y consultar conteos
