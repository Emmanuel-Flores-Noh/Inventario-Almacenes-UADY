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
