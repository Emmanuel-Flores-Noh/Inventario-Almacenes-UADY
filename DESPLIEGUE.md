# Despliegue

Guia para levantar MariaDB, el Broker y el Sistema 1 con Docker Compose, y para
abrir el cliente Swing (Sistema 2) desde Windows.

> Estado actual: el `Main` del cliente es un esqueleto que solo imprime un mensaje
> de arranque. Las secciones sobre el cliente describen como ejecutarlo hoy y como
> abrirlo cuando la interfaz Swing exista; no hay pantalla que mostrar todavia.

## Requisitos

- Docker Desktop con Docker Compose v2 (`docker compose version`).
- JDK 21 o superior y Maven 3.9+, solo si vas a compilar fuera de Docker.
  El `pom.xml` compila con `release 21`.
- Para el cliente en Windows: un servidor X11 (WSLg, VcXsrv o X410) o ejecutarlo
  directamente desde el IDE.

## 1. Configurar las credenciales

Las credenciales viven en `.env`, que esta ignorado por git. Parte de la plantilla:

```powershell
Copy-Item .env.example .env
```

```bash
cp .env.example .env
```

Edita `.env` y reemplaza los marcadores:

- `DB_ROOT_PASSWORD`: contrasena del usuario `root` de MariaDB.
- `DB_PASSWORD`: contrasena del usuario de la aplicacion (`DB_USER`).

Ambas son **obligatorias**. Si falta alguna, `docker compose` se detiene con un
mensaje como:

```text
error while interpolating services.mariadb.environment.MARIADB_PASSWORD:
required variable DB_PASSWORD is missing a value: define DB_PASSWORD en .env
```

No uses las contrasenas de ejemplo en un entorno real y no subas `.env` al repo.

## 2. Levantar el stack

```bash
docker compose up --build
```

El arranque respeta este orden:

1. `mariadb` arranca y espera a que su healthcheck (`mariadb-admin ping`) pase.
2. `broker` arranca y espera a que su healthcheck (`nc -z 127.0.0.1 $BROKER_PORT`)
   confirme que escucha.
3. `sistema-centralizado` arranca cuando MariaDB y el broker estan `healthy`, y se
   registra en el broker con `SERVICE_HOST=sistema-centralizado`.

Para dejarlo en segundo plano:

```bash
docker compose up --build -d
docker compose ps
```

`docker compose ps` debe mostrar `uady-mariadb`, `uady-broker` y
`uady-sistema-centralizado` como `running`/`healthy`.

## 3. Verificar el orden y el registro

```bash
docker compose logs broker
docker compose logs sistema-centralizado
```

- El broker imprime `Broker iniciado en el puerto 5000 ...` con sus timeouts.
- El Sistema 1 imprime `Escuchando en el puerto 6000 ...` y luego
  `Proveedor registrado en el broker broker:5000 como sistema-centralizado
  (gestorTiendas, inventario, gestorPrecios)`.

Si el broker tarda en estar listo, el Sistema 1 reintenta con `REGISTRO_INTENTOS`
y `REGISTRO_ESPERA_MS` sin dejar de escuchar.

## 4. Puertos y variables

| Componente | Puerto host | Variables principales |
|---|---|---|
| MariaDB | 3306 | `DB_ROOT_PASSWORD`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` |
| Broker | `BROKER_PORT` (5000) | `BROKER_TIMEOUT_*`, `BROKER_REINTENTOS`, `BROKER_FALLOS_PARA_INDISPONIBLE`, `BROKER_INDISPONIBLE_MS` |
| Sistema 1 | `SERVICE_PORT` (6000) | `DB_*`, `BROKER_HOST`, `BROKER_PORT`, `SERVICE_HOST`, `SERVICE_POOL_SIZE`, `REGISTRO_*` |

`BROKER_PORT` y `SERVICE_PORT` se usan tanto para el puerto interno como para el
mapeo al host; si los cambias en `.env`, ambos lados quedan alineados.

## 5. Cliente Swing en Windows

Windows no trae servidor X11, y una aplicacion Swing necesita uno. Hay dos caminos.

### Opcion A: desde el IDE (recomendada para desarrollo)

1. Abre el proyecto en IntelliJ IDEA, Eclipse o VS Code.
2. Configura el proyecto con **JDK 21**. Las configuraciones de `.run/*.xml`
   apuntan al JRE 17; si las usas, cambia el JRE a 21 (el `pom.xml` compila con 21).
3. Crea una configuracion de ejecucion para `mx.uady.inventario.cliente.Main`
   con estas variables de entorno, apuntando al broker publicado por Docker:
   - `BROKER_HOST=localhost`
   - `BROKER_PORT=5000`
4. Ejecuta. La ventana Swing se dibuja directamente en el escritorio de Windows,
   sin X11 ni contenedor.

Tambien puedes compilar el fat-jar y ejecutarlo en Windows:

```powershell
mvn -pl sistema-cliente -am clean package
java -jar sistema-cliente/target/sistema-cliente.jar
```

### Opcion B: en Docker con un servidor X11

Util cuando quieres el cliente dentro de la misma red de Docker. El compose ya
trae el perfil `client` con la imagen que incluye las librerias X11.

**Con WSLg (Windows 11 + WSL2):** ejecuta desde una terminal WSL2 con integracion
de Docker Desktop. El socket `/tmp/.X11-unix` se comparte automaticamente:

```bash
export DISPLAY=:0
docker compose --profile client up --build
```

**Con VcXsrv o X410 (Windows nativo):** inicia el servidor X permitiendo conexiones
TCP (en XLaunch: *Multiple windows*, *Start no client*, marca *Disable access
control*). Luego, en PowerShell:

```powershell
$env:DISPLAY = "host.docker.internal:0.0"
docker compose --profile client up --build
```

El montaje `/tmp/.X11-unix` de `docker-compose.yml` solo se usa en WSL/Linux; en
Windows nativo el cliente se conecta por TCP usando `DISPLAY`. Si el Firewall de
Windows pregunta, permite la conexion del servidor X.

## 6. Detener y limpiar

```bash
docker compose down          # detiene los contenedores y conserva los datos
docker compose down -v       # ADVERTENCIA: borra el volumen de MariaDB
```

`docker compose down -v` es destructivo: al recrear el volumen se vuelven a ejecutar
`01_schema.sql` y `02_seed.sql`, y se pierde cualquier dato agregado despues.
