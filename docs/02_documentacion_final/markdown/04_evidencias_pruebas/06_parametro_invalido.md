## Prueba 6: Parámetro inválido

*Pruebas requeridas por la actividad*

**Evidencia:** `06_parametro_invalido.txt`

**Resultado:** Falta `numero_serie`, `id_tienda` negativo y producto inexistente → `PARAMETROS_INVALIDOS` con mensaje claro

```text
# Prueba 6: Parametro invalido
Fecha (hora local del equipo): 2026-11-14 09:18:16
Codigo: rama dev/chris, commit bb03b8c

Se envian ventas con parametros faltantes o invalidos. El proveedor los valida y responde
PARAMETROS_INVALIDOS con un mensaje claro, conservando el id de la solicitud.

## Falta numero_serie

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-06a",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "actualizarTiendaVenta",
  "parametros": {
    "id_tienda": 1,
    "id_usuario": 2
  },
  "fechaHora": "2026-11-14T09:18:17.1976449"
}
BROKER -> CLIENTE (17 ms)
{
  "idSolicitud": "ev-06a",
  "estado": "ERROR",
  "codigoError": "PARAMETROS_INVALIDOS",
  "mensajeError": "Falta el parámetro numero_serie",
  "fechaHora": "2026-11-14T15:18:17.234579654"
}
Resumen: estado=ERROR, codigo=PARAMETROS_INVALIDOS, mensaje="Falta el parámetro numero_serie"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:17.235 [pool-1-thread-13] id=ev-06a gestorTiendas.actualizarTiendaVenta -> sistema-centralizado:6000 | ERROR PARAMETROS_INVALIDOS: Falta el parámetro numero_serie | 2 ms

## id_tienda negativo

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-06b",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "actualizarTiendaVenta",
  "parametros": {
    "numero_serie": "SN-UADY-0001",
    "id_tienda": -5,
    "id_usuario": 2
  },
  "fechaHora": "2026-11-14T09:18:17.7366356"
}
BROKER -> CLIENTE (16 ms)
{
  "idSolicitud": "ev-06b",
  "estado": "ERROR",
  "codigoError": "PARAMETROS_INVALIDOS",
  "mensajeError": "El parámetro id_tienda debe ser positivo",
  "fechaHora": "2026-11-14T15:18:17.774421699"
}
Resumen: estado=ERROR, codigo=PARAMETROS_INVALIDOS, mensaje="El parámetro id_tienda debe ser positivo"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:17.774 [pool-1-thread-14] id=ev-06b gestorTiendas.actualizarTiendaVenta -> sistema-centralizado:6000 | ERROR PARAMETROS_INVALIDOS: El parámetro id_tienda debe ser positivo | 2 ms

## Producto inexistente en gestorPrecios

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-06c",
  "nombreServicio": "gestorPrecios",
  "nombreOperacion": "listarPreciosProducto",
  "parametros": {
    "id_producto": 999
  },
  "fechaHora": "2026-11-14T09:18:18.327309"
}
BROKER -> CLIENTE (22 ms)
{
  "idSolicitud": "ev-06c",
  "estado": "ERROR",
  "codigoError": "PARAMETROS_INVALIDOS",
  "mensajeError": "El producto 999 no existe",
  "fechaHora": "2026-11-14T15:18:18.369878855"
}
Resumen: estado=ERROR, codigo=PARAMETROS_INVALIDOS, mensaje="El producto 999 no existe"

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:18.370 [pool-1-thread-15] id=ev-06c gestorPrecios.listarPreciosProducto -> sistema-centralizado:6000 | ERROR PARAMETROS_INVALIDOS: El producto 999 no existe | 6 ms
```
