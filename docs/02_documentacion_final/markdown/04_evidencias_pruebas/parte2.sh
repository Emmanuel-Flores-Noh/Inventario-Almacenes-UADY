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
