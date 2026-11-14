## Observaciones

- **Hora UTC en los contenedores.** Los contenedores corren en UTC, así que la bitácora y la `fecha_venta`
  muestran 6 horas más que la hora local del equipo (por ejemplo, 15:14 en lugar de 09:14). La fecha en
  el encabezado de cada archivo es la hora local. Es una limitación conocida (zona horaria, error #6).
- **Timeout del cliente frente al Broker** (`07`, caso B). Cuando el proveedor no responde, el Broker
  tarda unos 15 s (3 intentos × 5 s) en contestar `TIMEOUT`; el Sistema 2 espera 5 s, así que el
  usuario vería antes su propio timeout. Es una limitación conocida (error #1).
- **Re-registro tras reiniciar el Broker** (`12`). El Sistema 1 solo se registra al arrancar. Es una
  limitación conocida (error #2); la mejora es un re-registro periódico.
