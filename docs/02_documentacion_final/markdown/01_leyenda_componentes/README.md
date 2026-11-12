# Leyenda del diagrama de componentes (general)

Diagrama base: `Componentes.mmd` (carpeta `diagramas`).

| Componente | Acciones principales |
|---|---|
| Cliente (Sistema 2) | Construye solicitudes JSON, las envía al Broker por socket y presenta la respuesta. |
| Broker | Registra servicios, identifica la operación, enruta la solicitud al proveedor y devuelve la respuesta. |
| Proveedor (Sistema 1) | Ejecuta la lógica de negocio, genera/vincula QR, persiste/consulta en BD y responde al Broker. |
| MariaDB | Almacena productos, mercancía, tiendas, ventas y demás datos del inventario. |
