## Prueba 9: Timeouts, reintentos limitados y desconexiones

*Requisitos del Nivel 2 y preguntas de análisis*

**Evidencia:** `09_proveedor_caido_timeout.txt`

**Resultado:** Proveedor apagado: 3 intentos por solicitud (`intento 1/3 … 3/3`), tras 3 solicitudes fallidas el servicio queda `NO DISPONIBLE` y se rechaza al instante; al volver el proveedor se reactiva

```text
# Evidencia adicional: proveedor caido, reintentos limitados y desconexion
Fecha (hora local del equipo): 2026-11-14 09:18:52
Codigo: rama dev/chris, commit bb03b8c

Se apaga el proveedor activo (el proveedor 2) sin que se de de baja. El Broker reintenta de forma
limitada (3 intentos, 200 ms entre ellos), responde con error y, tras 3 solicitudes fallidas, marca el
servicio como NO DISPONIBLE: la siguiente solicitud se rechaza de inmediato sin esperar. Al volver un
proveedor y registrarse, el servicio se reactiva.

## Paso 1: se detiene el proveedor 2 (puerto 6001)

## Paso 2: primera solicitud (3 intentos)

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-09a",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:54.0235668"
}
BROKER -> CLIENTE (420 ms)
{
  "idSolicitud": "ev-09a",
  "estado": "ERROR",
  "codigoError": "ERROR_INTERNO",
  "mensajeError": "Error de comunicacion con el proveedor: Connection refused (connect failed)",
  "fechaHora": "2026-11-14T15:18:54.469871499"
}
Resumen: estado=ERROR, codigo=ERROR_INTERNO, mensaje="Error de comunicacion con el proveedor: Connection refused (connect failed)"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:54.065 [pool-1-thread-4] id=ev-09a inventario.listarInventario -> host.docker.internal:6001 | intento 1/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:54.267 [pool-1-thread-4] id=ev-09a inventario.listarInventario -> host.docker.internal:6001 | intento 2/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:54.469 [pool-1-thread-4] id=ev-09a inventario.listarInventario -> host.docker.internal:6001 | intento 3/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:54.469 [pool-1-thread-4] id=ev-09a inventario.listarInventario -> host.docker.internal:6001 | ERROR ERROR_INTERNO: Error de comunicacion con el proveedor: Connection refused (connect failed) | 406 ms

## Paso 3: segunda solicitud

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-09b",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:54.9872288"
}
BROKER -> CLIENTE (419 ms)
{
  "idSolicitud": "ev-09b",
  "estado": "ERROR",
  "codigoError": "ERROR_INTERNO",
  "mensajeError": "Error de comunicacion con el proveedor: Connection refused (connect failed)",
  "fechaHora": "2026-11-14T15:18:55.425588035"
}
Resumen: estado=ERROR, codigo=ERROR_INTERNO, mensaje="Error de comunicacion con el proveedor: Connection refused (connect failed)"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:55.021 [pool-1-thread-5] id=ev-09b inventario.listarInventario -> host.docker.internal:6001 | intento 1/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:55.223 [pool-1-thread-5] id=ev-09b inventario.listarInventario -> host.docker.internal:6001 | intento 2/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:55.425 [pool-1-thread-5] id=ev-09b inventario.listarInventario -> host.docker.internal:6001 | intento 3/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:55.425 [pool-1-thread-5] id=ev-09b inventario.listarInventario -> host.docker.internal:6001 | ERROR ERROR_INTERNO: Error de comunicacion con el proveedor: Connection refused (connect failed) | 405 ms

## Paso 4: tercera solicitud (alcanza el umbral)

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-09c",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:56.2072198"
}
BROKER -> CLIENTE (419 ms)
{
  "idSolicitud": "ev-09c",
  "estado": "ERROR",
  "codigoError": "ERROR_INTERNO",
  "mensajeError": "Error de comunicacion con el proveedor: Connection refused (connect failed)",
  "fechaHora": "2026-11-14T15:18:56.645846663"
}
Resumen: estado=ERROR, codigo=ERROR_INTERNO, mensaje="Error de comunicacion con el proveedor: Connection refused (connect failed)"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:56.241 [pool-1-thread-7] id=ev-09c inventario.listarInventario -> host.docker.internal:6001 | intento 1/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:56.443 [pool-1-thread-7] id=ev-09c inventario.listarInventario -> host.docker.internal:6001 | intento 2/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:56.645 [pool-1-thread-7] id=ev-09c inventario.listarInventario -> host.docker.internal:6001 | intento 3/3 fallo: ERROR_INTERNO (Error de comunicacion con el proveedor: Connection refused (connect failed))
[BROKER] 2026-11-14 15:18:56.645 [pool-1-thread-7] id=ev-09c inventario.listarInventario -> host.docker.internal:6001 | ERROR ERROR_INTERNO: Error de comunicacion con el proveedor: Connection refused (connect failed) | 406 ms

Marca de no disponible en la bitacora:
[REGISTRO] 2026-11-14 15:18:56.645 [pool-1-thread-7] inventario marcado como NO DISPONIBLE tras 3 fallos

## Paso 5: cuarta solicitud (rechazo inmediato, sin contactar al proveedor)

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-09d",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:57.3860881"
}
BROKER -> CLIENTE (13 ms)
{
  "idSolicitud": "ev-09d",
  "estado": "ERROR",
  "codigoError": "PROVEEDOR_NO_REGISTRADO",
  "mensajeError": "Proveedor no disponible: inventario",
  "fechaHora": "2026-11-14T15:18:57.418638718"
}
Resumen: estado=ERROR, codigo=PROVEEDOR_NO_REGISTRADO, mensaje="Proveedor no disponible: inventario"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:57.418 [pool-1-thread-6] id=ev-09d inventario.listarInventario -> host.docker.internal:6001 | ERROR PROVEEDOR_NO_REGISTRADO: Proveedor no disponible: inventario | 0 ms

## Paso 6: vuelve el proveedor original (docker start uady-sistema-centralizado) y se registra de nuevo
[REGISTRO] 2026-11-14 15:18:58.017 [pool-1-thread-1] registrar gestorTiendas -> sistema-centralizado:6000
[REGISTRO] 2026-11-14 15:18:58.030 [pool-1-thread-15] registrar inventario -> sistema-centralizado:6000
[REGISTRO] 2026-11-14 15:18:58.031 [pool-1-thread-2] registrar gestorPrecios -> sistema-centralizado:6000

## Paso 7: el servicio vuelve a responder

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-09e",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:59.5751667"
}
BROKER -> CLIENTE (95 ms)
{
  "idSolicitud": "ev-09e",
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
  "fechaHora": "2026-11-14T15:18:59.688099393"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:59.688 [pool-1-thread-13] id=ev-09e inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 81 ms
```
