## Prueba 7: Excepción producida por un proveedor

*Pruebas requeridas por la actividad*

**Evidencia:** `07_excepcion_proveedor.txt`

**Resultado:** Con la tabla `tiendas_almacenes` ausente, el proveedor lanza `SQLException` → `ERROR_INTERNO`; el Broker sigue atendiendo y, al restaurarla, la operación funciona sin reiniciar nada. Caso B: con MariaDB detenida, el Broker responde `TIMEOUT` tras 3 intentos

```text
# Prueba 7: Excepcion producida por un proveedor
Fecha (hora local del equipo): 2026-11-14 09:20:41
Codigo: rama dev/chris, commit bb03b8c

Caso A: se provoca una excepcion real dentro del proveedor (la tabla 'tiendas_almacenes' deja de existir de forma
temporal, asi que la consulta JDBC lanza SQLException). El despachador del Sistema 1 convierte la
excepcion en una respuesta ERROR_INTERNO, conservando el id; el Broker la devuelve al cliente y sigue
atendiendo. Al restaurar la tabla, la misma operacion funciona sin reiniciar el Broker ni el Sistema 1.

Caso B (complemento): si la base de datos se detiene por completo, el proveedor no responde dentro del
timeout de lectura del Broker (5 s); el Broker reintenta 3 veces y responde TIMEOUT.

## Caso A, paso 1: RENAME TABLE tiendas_almacenes TO tiendas_respaldo

## Caso A, paso 2: solicitud mientras la tabla no existe

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-07-c1",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:20:42.0394307"
}
BROKER -> CLIENTE (19 ms)
{
  "idSolicitud": "ev-07-c1",
  "estado": "ERROR",
  "codigoError": "ERROR_INTERNO",
  "mensajeError": "Error al ejecutar la operacion listarTiendas: No se pudo consultar las tiendas",
  "fechaHora": "2026-11-14T15:20:42.080603877"
}
Resumen: estado=ERROR, codigo=ERROR_INTERNO, mensaje="Error al ejecutar la operacion listarTiendas: No se pudo consultar las tiendas"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:20:42.081 [pool-1-thread-15] id=ev-07-c1 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | ERROR ERROR_INTERNO: Error al ejecutar la operacion listarTiendas: No se pudo consultar las tiendas | 5 ms

Consola del Sistema 1 en ese momento (docker logs uady-sistema-centralizado):
Proveedor registrado en el broker broker:5000 como sistema-centralizado:6000 (gestorTiendas, inventario, gestorPrecios)
  Servicios: gestorTiendas, inventario, gestorPrecios
Proveedor registrado en el broker broker:5000 como sistema-centralizado:6000 (gestorTiendas, inventario, gestorPrecios)

## Caso A, paso 3: el Broker sigue atendiendo otras operaciones

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-07-c2",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:20:42.70822"
}
BROKER -> CLIENTE (22 ms)
{
  "idSolicitud": "ev-07-c2",
  "resultado": [
    {
      "numeroSerie": "SN-UADY-0001",
      "idProducto": 1,
      "nombreProducto": "Laptop Lenovo ThinkPad E14",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0006",
      "idProducto": 3,
      "nombreProducto": "Silla ergonomica",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0007",
      "idProducto": 4,
      "nombreProducto": "Impresora laser HP",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0009",
      "idProducto": 5,
      "nombreProducto": "Tablet Samsung Galaxy Tab",
      "estado": "EN_ALMACEN"
    }
  ],
  "estado": "EXITO",
  "fechaHora": "2026-11-14T15:20:42.747698035"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:20:42.748 [pool-1-thread-16] id=ev-07-c2 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 7 ms

## Caso A, paso 4: RENAME TABLE tiendas_respaldo TO tiendas_almacenes

## Caso A, paso 5: la misma solicitud, sin reiniciar nada

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-07-c3",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:20:43.4464432"
}
BROKER -> CLIENTE (21 ms)
{
  "idSolicitud": "ev-07-c3",
  "resultado": [
    {
      "idTienda": 1,
      "nombre": "Almacen Central UADY",
      "direccion": "Calle 60 s/n, Centro, Merida, Yucatan"
    },
    {
      "idTienda": 2,
      "nombre": "Tienda Facultad de Ingenieria",
      "direccion": "Periferico Norte, Campus de Ingenieria, Merida"
    },
    {
      "idTienda": 3,
      "nombre": "Tienda Facultad de Matematicas",
      "direccion": "Campus de Ciencias Exactas, Merida"
    }
  ],
  "estado": "EXITO",
  "fechaHora": "2026-11-14T15:20:43.488581257"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:20:43.489 [pool-1-thread-1] id=ev-07-c3 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 5 ms

## Caso B, paso 1: docker stop uady-mariadb
## Caso B, paso 2: solicitud con la base detenida (cliente con espera de 30 s para ver la respuesta del Broker)

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-07-d1",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:20:44.4460508"
}
BROKER -> CLIENTE (15432 ms)
{
  "idSolicitud": "ev-07-d1",
  "estado": "ERROR",
  "codigoError": "TIMEOUT",
  "mensajeError": "Tiempo de espera agotado al contactar al proveedor",
  "fechaHora": "2026-11-14T15:20:59.898989283"
}
Resumen: estado=ERROR, codigo=TIMEOUT, mensaje="Tiempo de espera agotado al contactar al proveedor"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:20:49.485 [pool-1-thread-2] id=ev-07-d1 inventario.listarInventario -> sistema-centralizado:6000 | intento 1/3 fallo: TIMEOUT (Tiempo de espera agotado al contactar al proveedor)
[BROKER] 2026-11-14 15:20:54.692 [pool-1-thread-2] id=ev-07-d1 inventario.listarInventario -> sistema-centralizado:6000 | intento 2/3 fallo: TIMEOUT (Tiempo de espera agotado al contactar al proveedor)
[BROKER] 2026-11-14 15:20:59.898 [pool-1-thread-2] id=ev-07-d1 inventario.listarInventario -> sistema-centralizado:6000 | intento 3/3 fallo: TIMEOUT (Tiempo de espera agotado al contactar al proveedor)
[BROKER] 2026-11-14 15:20:59.899 [pool-1-thread-2] id=ev-07-d1 inventario.listarInventario -> sistema-centralizado:6000 | ERROR TIMEOUT: Tiempo de espera agotado al contactar al proveedor | 15419 ms

Observacion: el Broker tarda unos 15 s (3 intentos x 5 s) en responder TIMEOUT. El cliente del
Sistema 2 espera 5 s, asi que el usuario veria primero su propio timeout. Es la limitacion #1 de la
revision de errores (alinear el timeout del cliente con el peor caso del Broker y no reintentar
operaciones que ya llegaron al proveedor).

## Caso B, paso 3: docker start uady-mariadb
## Caso B, paso 4: con la base de vuelta, sin reiniciar nada

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-07-d2",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:21:10.9237249"
}
BROKER -> CLIENTE (23 ms)
{
  "idSolicitud": "ev-07-d2",
  "resultado": [
    {
      "numeroSerie": "SN-UADY-0001",
      "idProducto": 1,
      "nombreProducto": "Laptop Lenovo ThinkPad E14",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0006",
      "idProducto": 3,
      "nombreProducto": "Silla ergonomica",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0007",
      "idProducto": 4,
      "nombreProducto": "Impresora laser HP",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0009",
      "idProducto": 5,
      "nombreProducto": "Tablet Samsung Galaxy Tab",
      "estado": "EN_ALMACEN"
    }
  ],
  "estado": "EXITO",
  "fechaHora": "2026-11-14T15:21:10.967079159"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:21:10.967 [pool-1-thread-5] id=ev-07-d2 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 7 ms
```
