## Prueba 8: Cambio de proveedor sin modificar el cliente

*Pruebas requeridas por la actividad*

**Evidencia:** `08_cambio_proveedor.txt`

**Resultado:** El mismo comando del cliente lo atiende primero `sistema-centralizado:6000` y, tras registrarse un segundo proveedor y apagar el original, `host.docker.internal:6001`

```text
# Prueba 8: Cambio de proveedor sin modificar el cliente
Fecha (hora local del equipo): 2026-11-14 09:18:49
Codigo: rama dev/chris, commit bb03b8c

Un segundo proveedor (otra instancia del Sistema 1, fuera de Docker, en el puerto 6001) se registra
en el Broker para los mismos servicios y sustituye al original; despues se apaga el original. El cliente
ejecuta exactamente el mismo comando antes y despues: solo cambia el proveedor que el Broker localiza.

## Paso 1: proveedor original

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-08a",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:50.1432026"
}
BROKER -> CLIENTE (17 ms)
{
  "idSolicitud": "ev-08a",
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
  "fechaHora": "2026-11-14T15:18:50.182351758"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:50.182 [pool-1-thread-10] id=ev-08a gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 3 ms

## Paso 2: arranca el proveedor 2 (host.docker.internal:6001) y se registra
Proveedor registrado en el broker localhost:5000 como host.docker.internal:6001 (gestorTiendas, inventario, gestorPrecios)
[REGISTRO] 2026-11-14 15:18:50.503 [pool-1-thread-12] registrar gestorTiendas -> host.docker.internal:6001
[REGISTRO] 2026-11-14 15:18:50.512 [pool-1-thread-14] registrar inventario -> host.docker.internal:6001
[REGISTRO] 2026-11-14 15:18:50.514 [pool-1-thread-9] registrar gestorPrecios -> host.docker.internal:6001

## Paso 3: se apaga el proveedor original (docker stop uady-sistema-centralizado)

## Paso 4: el mismo cliente, sin cambios, ahora lo atiende el proveedor 2

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-08b",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:52.5416426"
}
BROKER -> CLIENTE (78 ms)
{
  "idSolicitud": "ev-08b",
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
  "fechaHora": "2026-11-14T09:18:52.638081"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:52.639 [pool-1-thread-3] id=ev-08b gestorTiendas.listarTiendas -> host.docker.internal:6001 | EXITO | 64 ms
```
