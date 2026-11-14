## Prueba 3: Ejecución de una operación (una por servicio)

*Pruebas requeridas por la actividad*

**Evidencia:** `03_ejecucion_operacion.txt`

**Resultado:** `gestorTiendas.listarTiendas`, `inventario.listarInventario` y `gestorPrecios.listarPreciosProducto` → `EXITO`

```text
# Prueba 3: Ejecucion de una operacion (una por cada servicio registrado)
Fecha (hora local del equipo): 2026-11-14 09:18:13
Codigo: rama dev/chris, commit bb03b8c

Se ejecuta una operacion de cada uno de los 3 servicios registrados. Todas viajan por el Broker
como JSON por socket y llegan al proveedor localizado.

## Servicio gestorTiendas: listarTiendas

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-03a",
  "nombreServicio": "gestorTiendas",
  "nombreOperacion": "listarTiendas",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:14.0694684"
}
BROKER -> CLIENTE (26 ms)
{
  "idSolicitud": "ev-03a",
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
  "fechaHora": "2026-11-14T15:18:14.114145834"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:14.115 [pool-1-thread-7] id=ev-03a gestorTiendas.listarTiendas -> sistema-centralizado:6000 | EXITO | 11 ms

## Servicio inventario: listarInventario

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-03b",
  "nombreServicio": "inventario",
  "nombreOperacion": "listarInventario",
  "parametros": {},
  "fechaHora": "2026-11-14T09:18:14.6735004"
}
BROKER -> CLIENTE (22 ms)
{
  "idSolicitud": "ev-03b",
  "resultado": [
    {
      "numeroSerie": "SN-UADY-0001",
      "idProducto": 1,
      "nombreProducto": "Laptop Lenovo ThinkPad E14",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0003",
      "idProducto": 2,
      "nombreProducto": "Proyector Epson PowerLite",
      "estado": "EN_ALMACEN"
    },
    {
      "numeroSerie": "SN-UADY-0005",
      "idProducto": 3,
      "nombreProducto": "Silla ergonomica",
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
  "fechaHora": "2026-11-14T15:18:14.714569046"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:14.715 [pool-1-thread-9] id=ev-03b inventario.listarInventario -> sistema-centralizado:6000 | EXITO | 8 ms

## Servicio gestorPrecios: listarPreciosProducto

CLIENTE -> BROKER (localhost:5000)
{
  "idSolicitud": "ev-03c",
  "nombreServicio": "gestorPrecios",
  "nombreOperacion": "listarPreciosProducto",
  "parametros": {
    "id_producto": 1
  },
  "fechaHora": "2026-11-14T09:18:15.2262593"
}
BROKER -> CLIENTE (39 ms)
{
  "idSolicitud": "ev-03c",
  "resultado": {
    "idProducto": 1,
    "nombre": "Laptop Lenovo ThinkPad E14",
    "precioActual": 18999.00,
    "historial": []
  },
  "estado": "EXITO",
  "fechaHora": "2026-11-14T15:18:15.283064142"
}
Resumen: estado=EXITO

Bitacora del Broker (docker logs uady-broker):
[BROKER] 2026-11-14 15:18:15.283 [pool-1-thread-10] id=ev-03c gestorPrecios.listarPreciosProducto -> sistema-centralizado:6000 | EXITO | 25 ms
```
