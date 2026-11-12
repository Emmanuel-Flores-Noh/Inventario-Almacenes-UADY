# AGENTS.md

## Contexto del repositorio

- Sistema distribuido con patrón Broker para inventario/trazabilidad UADY. Java 21 y Maven multi-módulo: `common`, `broker`, `sistema-centralizado`, `sistema-cliente`.
- Solo `broker` está implementado; `common` contiene contrato, errores y modelos; `sistema-centralizado` y `sistema-cliente` son esqueletos cuyo `Main` únicamente imprime un mensaje. No asumas que ya existen persistencia, QR, UI Swing ni clientes de red.
- El `README.md` de la raíz documenta la plantilla de generación de PDFs, no el proyecto. El diseño real está en `docs/markdown/DiseñoDeSistema/`.
- No hay CI, lint ni formatter configurados. La verificación es `mvn test`; hoy solo hay pruebas en `common` y `broker`.

## Comandos

```bash
mvn test
mvn clean package
mvn -pl broker -am test "-Dtest=BrokerServerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Usa siempre `-pl <modulo> -am` para compilar/probar un módulo: resuelve `common` primero.
- En PowerShell, entrecomilla los argumentos `-D...`; sin comillas, Maven interpreta `.failIfNoSpecifiedTests=false` como una fase de lifecycle.
- `mvn package` genera fat-jars en `<modulo>/target/<modulo>.jar`. El broker se ejecuta con `java -jar broker/target/broker.jar`.
- Variables del broker: `BROKER_PORT` (5000) y `BROKER_POOL_SIZE` (16). El cliente usa `BROKER_HOST`/`BROKER_PORT`; el centralizado usa `DB_*`, `BROKER_*` y `SERVICE_PORT`.
- Docker: `docker compose up --build` levanta MariaDB, broker y sistema centralizado. El cliente Swing está en los profiles `client`/`all` y requiere X11 (`DISPLAY`); no es viable como GUI en Windows sin servidor X.

## Contrato de comunicación

- El protocolo es JSON por línea sobre TCP en UTF-8: una solicitud por línea y una respuesta por línea en el mismo socket. Usa `println`/`readLine`, no frames binarios.
- Serializa siempre con `GsonFactory.DEFAULT`, nunca con `new Gson()`: allí se registra el adaptador de `LocalDateTime` que necesita `Solicitud`/`Respuesta`.
- Los nombres de servicios y operaciones viven en `Servicios` y `Operaciones` (`common`). No escribas strings sueltos.
- El servicio interno `broker` atiende `registrarProveedor`, `actualizarProveedor` y `removerProveedor`. Un proveedor se representa como `Proveedor(servicio, host, puerto)`.
- Toda `Respuesta` debe conservar el `idSolicitud` de la solicitud original; los errores se modelan con `CodigoError`.

## Persistencia y despliegue

- `docker/mariadb/init/01_schema.sql` solo se ejecuta la primera vez que se crea el volumen de MariaDB. Para aplicar cambios de esquema usa `docker compose down -v` (destructivo) o aplica el SQL manualmente.
- Los Dockerfiles copian únicamente `common` y el módulo objetivo. Si agregas una dependencia entre módulos, actualiza el Dockerfile correspondiente.
- `docs/out/` es salida generada (PDFs/PNGs) y se versiona. No la edites a mano: ejecuta `./procesar-docs.sh` o `procesar-docs.cmd`, que reconstruyen `docs/out/` desde `docs/markdown/` y `docs/diagramas/` dentro de Docker.
- El hook de pre-commit compila la documentación y hace `git add docs/`; se activa con `git config core.hooksPath .githooks`.

## Convenciones

- Código, comentarios, documentación y mensajes de commit están en español. Los paquetes siguen `mx.uady.inventario.<modulo>`.
- Respeta la separación por capas ya creada: `central/{model,service,repository,network,util}`, `cliente/{model,view,controller,network,util}`, `broker/{model,registry,network,routing,util}`.
- Las configuraciones `.run/*.xml` fijan JRE 17, pero el `pom.xml` compila con release 21. Usa JDK 21 o superior; ignora el JRE 17 de esas configuraciones.

## Issue en curso: BD del Sistema 1, conexión con el Broker y despliegue

"Sistema 1" = módulo `sistema-centralizado` (usa DB_*, BROKER_*, SERVICE_PORT). Si el código indica otra cosa, avisa antes de seguir.

### Fase 1: Persistencia
- La conexión JDBC a MariaDB se configura con las variables DB_*.
- Existen los modelos de todas las tablas, con el estado de la mercancía como enum (EN_ALMACEN, VENDIDO).
- Hay un script `docker/mariadb/init/02_seed.sql` con roles, usuarios, tiendas, productos y mercancía de prueba (compatible con `01_schema.sql`).

### Fase 2: Servidor de servicio del Sistema 1
- Escucha en el puerto de SERVICE_PORT y atiende con hilos.
- Se registra en el Broker al arrancar (`registrarProveedor`) y reintenta si el Broker todavía no está listo.
- Envía cada operación a su servicio y convierte las excepciones en respuestas de error (`CodigoError`).

### Fase 3: Resiliencia en el Broker
- Los timeouts de conexión y de lectura hacia el proveedor se pueden configurar.
- Hay reintentos limitados, y un proveedor que falla de forma repetida se marca como no disponible.

### Fase 4: Despliegue
- `docker compose up` levanta MariaDB, el Broker y el Sistema 1 en orden (healthcheck + depends_on), con credenciales en `.env` (agregar `.env.example`, `.gitignore`).
- Documentar cómo abrir las interfaces Swing en Windows (X11 o desde el IDE).

### Fase 5: Pruebas
- Una prueba con al menos 20 clientes simultáneos termina sin errores.
- Apagar el Sistema 1 a mitad de una solicitud devuelve un error de timeout al cliente.

## Reglas de trabajo

- Trabaja SOLO la fase que pida el usuario. No adelantes otras.
- Antes de escribir código, lee lo que ya existe (`broker`, `common`, `01_schema.sql`, `docker-compose.yml`). No inventes lo que puedas leer.
- Al terminar un paso: compila/prueba con los comandos de este archivo y muestra comando y salida real. No digas "listo" sin ejecutarlo.
- Sin credenciales en el código: variables de entorno y `.env`.
- Si algo requiere una suposición, dilo explícitamente.
- No modifiques archivos ajenos a la fase. No edites `docs/out/` a mano.

## Comentarios

- Comentarios y Javadoc en español. Explica el porqué de las decisiones no obvias, no lo que el código ya dice.

## Continuidad entre sesiones (MUY IMPORTANTE)

Puedo ser interrumpido en cualquier momento (se acaban los tokens y entra otro modelo). Por eso:

1. Al empezar: leo PROGRESS.md, reviso `git status` y `git diff`, y resumo en 3 líneas dónde quedó el trabajo ANTES de tocar código.
2. Divido la fase en pasos pequeños (1 a 3 archivos cada uno).
3. ANTES de cada paso: anoto en PROGRESS.md, en "Paso en curso", qué voy a hacer.
4. DESPUÉS de cada paso: compilo/pruebo, actualizo PROGRESS.md y hago commit. Un paso sin commit no cuenta como terminado.
5. Si un paso queda a medias, commit "WIP: ..." y anoto en PROGRESS.md lo que falta.
6. Nunca avanzo al siguiente paso con código que no compila.
7. Las decisiones importantes (nombres de variables de entorno, puertos, formatos) van en "Decisiones" de PROGRESS.md, no solo en la conversación.
8. Al retomar trabajo de otro modelo, no asumo que está bien: compilo y verifico el último paso primero.