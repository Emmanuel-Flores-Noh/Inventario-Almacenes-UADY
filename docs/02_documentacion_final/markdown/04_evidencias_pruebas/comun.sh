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
