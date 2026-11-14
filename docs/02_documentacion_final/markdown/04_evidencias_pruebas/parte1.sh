#!/usr/bin/env bash
# Parte 1: evidencias con el stack sano (01-06, 11, 13 y 14).
source "$(dirname "$0")/comun.sh"

# 01 Registro de proveedores
encabezado 01_registro_proveedores.txt "Prueba 1: Registro correcto de proveedores" \
"Al arrancar, el Sistema 1 (proveedor) se registra dinamicamente en el Broker enviando la operacion
broker.registrarProveedor por socket, una vez por cada servicio que ofrece. El Broker asocia cada
nombre de servicio con el host y puerto del proveedor."
{
  echo "## Consola del Sistema 1 (docker logs uady-sistema-centralizado)"
  docker logs uady-sistema-centralizado 2>&1 | grep -E "Servicios:|Proveedor registrado|Escuchando"
  echo
  echo "## Bitacora del Broker (docker logs uady-broker)"
  docker logs uady-broker 2>&1 | grep -E "^Broker iniciado|registrarProveedor|\[REGISTRO\] .* registrar "
  echo
  echo "Resumen: los 3 servicios (gestorTiendas, inventario, gestorPrecios) quedan registrados hacia"
  echo "sistema-centralizado:6000. Prueba automatizada relacionada: RegistroServiciosTest (registrarYBuscar)."
} >> "$EV/01_registro_proveedores.txt"

# 02 Localizacion
encabezado 02_localizacion_servicio.txt "Prueba 2: Localizacion de un servicio" \
"El cliente solo conoce la direccion del Broker (localhost:5000). El Broker busca en su registro el
proveedor del servicio 'inventario' y le reenvia la solicitud. La bitacora muestra el proveedor localizado."
paso 02_localizacion_servicio.txt "inventario.listarInventario" ev-02 inventario listarInventario '{}'

# 03 Una operacion por servicio
encabezado 03_ejecucion_operacion.txt "Prueba 3: Ejecucion de una operacion (una por cada servicio registrado)" \
"Se ejecuta una operacion de cada uno de los 3 servicios registrados. Todas viajan por el Broker
como JSON por socket y llegan al proveedor localizado."
paso 03_ejecucion_operacion.txt "Servicio gestorTiendas: listarTiendas" ev-03a gestorTiendas listarTiendas '{}'
paso 03_ejecucion_operacion.txt "Servicio inventario: listarInventario" ev-03b inventario listarInventario '{}'
paso 03_ejecucion_operacion.txt "Servicio gestorPrecios: listarPreciosProducto" ev-03c gestorPrecios listarPreciosProducto '{"id_producto":1}'

# 04 Respuesta exitosa
encabezado 04_respuesta_exitosa.txt "Prueba 4: Respuesta exitosa" \
"El cliente registra la venta de una unidad (CU2). El Sistema 1 la procesa en una transaccion y la
respuesta incluye id de la solicitud, resultado, estado y fecha y hora."
paso 04_respuesta_exitosa.txt "gestorTiendas.actualizarTiendaVenta" ev-04 gestorTiendas actualizarTiendaVenta \
  '{"numero_serie":"SN-UADY-0003","id_tienda":1,"id_usuario":2}'
{
  echo "Estado en la base de datos:"
  sql "SELECT m.numero_serie, m.estado, v.id_tienda, v.id_usuario, v.fecha_venta FROM mercancia m JOIN ventas v ON v.numero_serie = m.numero_serie WHERE m.numero_serie = 'SN-UADY-0003'"
} >> "$EV/04_respuesta_exitosa.txt"

# 05 Servicio desconocido
encabezado 05_servicio_desconocido.txt "Prueba 5: Servicio desconocido" \
"El cliente pide un servicio que ningun proveedor registro. El Broker lo rechaza sin contactar a
ningun proveedor y sin caerse."
paso 05_servicio_desconocido.txt "servicioFantasma.listar" ev-05 servicioFantasma listar '{}'

# 06 Parametro invalido
encabezado 06_parametro_invalido.txt "Prueba 6: Parametro invalido" \
"Se envian ventas con parametros faltantes o invalidos. El proveedor los valida y responde
PARAMETROS_INVALIDOS con un mensaje claro, conservando el id de la solicitud."
paso 06_parametro_invalido.txt "Falta numero_serie" ev-06a gestorTiendas actualizarTiendaVenta '{"id_tienda":1,"id_usuario":2}'
paso 06_parametro_invalido.txt "id_tienda negativo" ev-06b gestorTiendas actualizarTiendaVenta '{"numero_serie":"SN-UADY-0001","id_tienda":-5,"id_usuario":2}'
paso 06_parametro_invalido.txt "Producto inexistente en gestorPrecios" ev-06c gestorPrecios listarPreciosProducto '{"id_producto":999}'

# 13 Solicitud duplicada (pregunta 9)
encabezado 13_solicitud_duplicada.txt "Evidencia adicional: solicitud duplicada (pregunta 9)" \
"La misma venta se envia dos veces. La primera se registra; la segunda se rechaza porque la unidad ya
esta VENDIDO. En la base de datos queda una sola venta: la transaccion con bloqueo pesimista
(SELECT ... FOR UPDATE) evita que una solicitud repetida registre la venta dos veces."
paso 13_solicitud_duplicada.txt "Primer envio" ev-13a gestorTiendas actualizarTiendaVenta '{"numero_serie":"SN-UADY-0005","id_tienda":2,"id_usuario":4}'
paso 13_solicitud_duplicada.txt "Segundo envio (misma venta, id de solicitud distinto)" ev-13b gestorTiendas actualizarTiendaVenta '{"numero_serie":"SN-UADY-0005","id_tienda":2,"id_usuario":4}'
{
  echo "Ventas registradas para SN-UADY-0005 en la base de datos:"
  sql "SELECT COUNT(*) AS ventas FROM ventas WHERE numero_serie = 'SN-UADY-0005'"
} >> "$EV/13_solicitud_duplicada.txt"

# 11 Concurrencia
encabezado 11_concurrencia_hilos.txt "Evidencia adicional: atencion concurrente con hilos" \
"20 clientes abren cada uno su propio socket y envian una solicitud al mismo tiempo. El Broker atiende
cada conexion en un hilo de su pool (16 hilos) y cada cliente recibe la respuesta con el id de SU
solicitud. La bitacora muestra los distintos hilos del pool que atendieron."
{
  echo "## Clientes"
  $JAVA -cp "$CP" "$EV/EvidenciaConcurrencia.java" 20 ev-11 2>&1 | grep -v '^WARNING'
  echo
  echo "## Bitacora del Broker"
  docker logs uady-broker 2>&1 | grep -F "id=ev-11-"
  echo
  echo "Hilos distintos del pool que atendieron: $(docker logs uady-broker 2>&1 | grep -F 'id=ev-11-' | grep -o 'pool-1-thread-[0-9]*' | sort -u | wc -l)"
  echo "Prueba automatizada relacionada: IntegracionFase5Test (veinteClientesSimultaneosATravesDelBrokerTerminanSinErrores)."
} >> "$EV/11_concurrencia_hilos.txt"

# 14 Rendimiento (pregunta 7)
encabezado 14_costo_rendimiento.txt "Evidencia adicional: costo del Broker en rendimiento (pregunta 7)" \
"La misma operacion se envia 100 veces directo al Sistema 1 (puerto 6000, publicado por Docker solo
para esta medicion) y 100 veces a traves del Broker (puerto 5000). La diferencia es el costo del salto
adicional: una conexion TCP mas, marshalling/unmarshalling extra y la busqueda en el registro."
$JAVA -cp "$CP" "$EV/EvidenciaRendimiento.java" 100 6000 5000 2>&1 | grep -v '^WARNING' >> "$EV/14_costo_rendimiento.txt"

echo "parte 1 terminada"
