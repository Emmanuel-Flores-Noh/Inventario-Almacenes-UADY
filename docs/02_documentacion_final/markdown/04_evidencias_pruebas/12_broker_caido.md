## Prueba 12: Pregunta 3 y 4: el Broker deja de funcionar / punto único de falla

*Requisitos del Nivel 2 y preguntas de análisis*

**Evidencia:** `12_broker_caido.txt`

**Resultado:** Sin Broker, el cliente no llega a ningún servicio aunque el Sistema 1 siga vivo. Al reiniciar el Broker, su registro en memoria está vacío (`SERVICIO_DESCONOCIDO`) hasta que el Sistema 1 se vuelve a registrar

```text
# Evidencia adicional: el Broker deja de funcionar (preguntas 3 y 4)
Fecha (hora local del equipo): 2026-11-14 09:18:59
Codigo: rama dev/chris, commit bb03b8c

Se detiene el Broker. El cliente no puede comunicarse con ningun servicio aunque el Sistema 1 siga
funcionando: el Broker es un punto unico de falla. Al reiniciarlo, su registro (en memoria) esta vacio y
el Sistema 1 no se vuelve a registrar por si solo, asi que los servicios aparecen como desconocidos hasta
reiniciar el proveedor. Esta limitacion es una mejora pendiente (re-registro periodico, error #2).

## Paso 1: docker stop uady-broker
## Paso 2: el cliente intenta una solicitud

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-12a",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:19:00.6149458"
}
SIN RESPUESTA (13 ms): ConnectException: Connection refused: getsockopt
Resumen: el cliente no pudo comunicarse con el Broker

Bitacora del Broker (docker logs uady-broker):

## Paso 3: el Sistema 1 sigue en ejecucion (docker ps)
uady-sistema-centralizado | Up 3 seconds
uady-mariadb | Up 22 seconds (healthy)
uady-mariadb-pruebas | Up 24 minutes (healthy)

## Paso 4: docker start uady-broker
## Paso 5: el Broker volvio, pero su registro esta vacio

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-12b",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:19:06.6972985"
}
BROKER -> CLIENTE (75 ms)
{
  "idSolicitud": "ev-12b",
  "estado": "ERROR",
  "codigoError": "SERVICIO_DESCONOCIDO",
  "mensajeError": "Servicio desconocido: gestorTiendas",
  "fechaHora": "2026-11-14T15:19:06.779518031"
}
Resumen: estado=ERROR, codigo=SERVICIO_DESCONOCIDO, mensaje="Servicio desconocido: gestorTiendas"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:19:06.787 [pool-1-thread-2] id=ev-12b gestorTiendas.listarTiendas -> (sin proveedor) | ERROR SERVICIO_DESCONOCIDO: Servicio desconocido: gestorTiendas | 46 ms

## Paso 6: se reinicia el Sistema 1, que se registra al arrancar

## Paso 7: el servicio vuelve a responder

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-12c",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:19:09.147447"
}
BROKER -> CLIENTE (107 ms)
{
  "idSolicitud": "ev-12c",
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
  "fechaHora": "2026-11-14T15:19:09.27751569"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:19:09.278 [pool-1-thread-6] id=ev-12c gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 88 ms
```
