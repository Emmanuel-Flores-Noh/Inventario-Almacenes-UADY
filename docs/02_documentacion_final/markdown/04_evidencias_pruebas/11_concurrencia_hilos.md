## Prueba 11: Atención concurrente con hilos

*Requisitos del Nivel 2 y preguntas de análisis*

**Evidencia:** `11_concurrencia_hilos.txt`

**Resultado:** 20 clientes simultáneos: **20/20** reciben `EXITO` con el id de su propia solicitud; los atendieron **16 hilos distintos** del pool

```text
# Evidencia adicional: atencion concurrente con hilos
Fecha (hora local del equipo): 2026-11-14 09:18:19
Codigo: rama dev/chris, commit bb03b8c

20 clientes abren cada uno su propio socket y envian una solicitud al mismo tiempo. El Broker atiende
cada conexion en un hilo de su pool (16 hilos) y cada cliente recibe la respuesta con el id de SU
solicitud. La bitacora muestra los distintos hilos del pool que atendieron.

## Clientes
ev-11-01  inventario.listarInventario    -> idRespuesta=ev-11-01 (coincide)  EXITO  70 ms
ev-11-02  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-02 (coincide)  EXITO  70 ms
ev-11-03  inventario.listarInventario    -> idRespuesta=ev-11-03 (coincide)  EXITO  70 ms
ev-11-04  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-04 (coincide)  EXITO  70 ms
ev-11-05  inventario.listarInventario    -> idRespuesta=ev-11-05 (coincide)  EXITO  82 ms
ev-11-06  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-06 (coincide)  EXITO  70 ms
ev-11-07  inventario.listarInventario    -> idRespuesta=ev-11-07 (coincide)  EXITO  70 ms
ev-11-08  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-08 (coincide)  EXITO  79 ms
ev-11-09  inventario.listarInventario    -> idRespuesta=ev-11-09 (coincide)  EXITO  70 ms
ev-11-10  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-10 (coincide)  EXITO  70 ms
ev-11-11  inventario.listarInventario    -> idRespuesta=ev-11-11 (coincide)  EXITO  70 ms
ev-11-12  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-12 (coincide)  EXITO  79 ms
ev-11-13  inventario.listarInventario    -> idRespuesta=ev-11-13 (coincide)  EXITO  70 ms
ev-11-14  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-14 (coincide)  EXITO  70 ms
ev-11-15  inventario.listarInventario    -> idRespuesta=ev-11-15 (coincide)  EXITO  82 ms
ev-11-16  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-16 (coincide)  EXITO  70 ms
ev-11-17  inventario.listarInventario    -> idRespuesta=ev-11-17 (coincide)  EXITO  70 ms
ev-11-18  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-18 (coincide)  EXITO  70 ms
ev-11-19  inventario.listarInventario    -> idRespuesta=ev-11-19 (coincide)  EXITO  73 ms
ev-11-20  gestorTiendas.listarTiendas    -> idRespuesta=ev-11-20 (coincide)  EXITO  70 ms

Resumen: 20/20 clientes recibieron EXITO con el id de su propia solicitud; tiempo total 83 ms

## Bitacora del Broker
[BROKER] 2026-11-14 15:18:20.337 [pool-1-thread-1] id=ev-11-20 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 11 ms
[BROKER] 2026-11-14 15:18:20.338 [pool-1-thread-11] id=ev-11-02 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 12 ms
[BROKER] 2026-11-14 15:18:20.338 [pool-1-thread-9] id=ev-11-16 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 13 ms
[BROKER] 2026-11-14 15:18:20.340 [pool-1-thread-3] id=ev-11-10 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 14 ms
[BROKER] 2026-11-14 15:18:20.340 [pool-1-thread-16] id=ev-11-07 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 15 ms
[BROKER] 2026-11-14 15:18:20.340 [pool-1-thread-4] id=ev-11-06 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 13 ms
[BROKER] 2026-11-14 15:18:20.341 [pool-1-thread-7] id=ev-11-09 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 16 ms
[BROKER] 2026-11-14 15:18:20.343 [pool-1-thread-14] id=ev-11-04 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 17 ms
[BROKER] 2026-11-14 15:18:20.343 [pool-1-thread-13] id=ev-11-14 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 15 ms
[BROKER] 2026-11-14 15:18:20.343 [pool-1-thread-5] id=ev-11-13 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 17 ms
[BROKER] 2026-11-14 15:18:20.344 [pool-1-thread-8] id=ev-11-18 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 18 ms
[BROKER] 2026-11-14 15:18:20.343 [pool-1-thread-6] id=ev-11-01 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 16 ms
[BROKER] 2026-11-14 15:18:20.345 [pool-1-thread-2] id=ev-11-03 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 19 ms
[BROKER] 2026-11-14 15:18:20.347 [pool-1-thread-15] id=ev-11-11 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 19 ms
[BROKER] 2026-11-14 15:18:20.347 [pool-1-thread-10] id=ev-11-17 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 20 ms
[BROKER] 2026-11-14 15:18:20.351 [pool-1-thread-12] id=ev-11-19 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 24 ms
[BROKER] 2026-11-14 15:18:20.359 [pool-1-thread-15] id=ev-11-08 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 6 ms
[BROKER] 2026-11-14 15:18:20.359 [pool-1-thread-6] id=ev-11-12 gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 6 ms
[BROKER] 2026-11-14 15:18:20.361 [pool-1-thread-13] id=ev-11-15 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 9 ms
[BROKER] 2026-11-14 15:18:20.362 [pool-1-thread-2] id=ev-11-05 inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 9 ms

Hilos distintos del pool que atendieron: 16
Prueba automatizada relacionada: IntegracionFase5Test (veinteClientesSimultaneosATravesDelBrokerTerminanSinErrores).
```
