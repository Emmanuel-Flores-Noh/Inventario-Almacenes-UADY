## Prueba 14: Pregunta 7: costo en rendimiento

*Requisitos del Nivel 2 y preguntas de análisis*

**Evidencia:** `14_costo_rendimiento.txt`

**Resultado:** 100 solicitudes por camino: directo 3.53 ms, por el Broker 4.45 ms de promedio → **+0.92 ms por solicitud**

```text
# Evidencia adicional: costo del Broker en rendimiento (pregunta 7)
Fecha (hora local del equipo): 2026-11-14 09:18:20
Codigo: rama dev/chris, commit bb03b8c

La misma operacion se envia 100 veces directo al Sistema 1 (puerto 6000, publicado por Docker solo
para esta medicion) y 100 veces a traves del Broker (puerto 5000). La diferencia es el costo del salto
adicional: una conexion TCP mas, marshalling/unmarshalling extra y la busqueda en el registro.

Operacion: gestorTiendas.listarTiendas, 100 solicitudes secuenciales por camino, una conexion TCP nueva por solicitud.

Camino                               promedio    mediana     minimo     maximo
Directo al Sistema 1 (:6000)           3.53 ms     3.19 ms     2.73 ms    11.46 ms
A traves del Broker (:5000)            4.45 ms     4.07 ms     3.35 ms    11.27 ms

Resumen: el Broker agrega en promedio 0.92 ms por solicitud (26.2% mas que la llamada directa).
```
