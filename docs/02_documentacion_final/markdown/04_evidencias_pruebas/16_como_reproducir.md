## Cómo reproducirlas

Requisitos: Docker Desktop, JDK 21+, Maven y Git Bash. Los pasos 1 y 2 se ejecutan en la raíz de un
checkout con el código (rama `qa` o `dev/chris`, que incluyen la bitácora del Broker):

1. `mvn -B package -DskipTests` (los clientes usan `sistema-cliente/target/sistema-cliente.jar` como classpath).
2. `docker compose -p evidencias up --build -d mariadb broker sistema-centralizado`
3. Desde la carpeta `docs/02_documentacion_final/markdown/04_evidencias_pruebas`, en Git Bash: `bash parte1.sh`, `bash parte2.sh` y `bash parte3.sh`.
   Los scripts buscan el código cuatro carpetas arriba de esta; si esta carpeta está en otro checkout
   (por ejemplo, en la rama `docs`), indica la ruta del código: `REPO=/ruta/al/checkout bash parte1.sh`.
   - `parte1.sh`: 01–06, 11, 13 y 14 (stack sano).
   - `parte2.sh`: 08, 09 y 12 (detiene y reinicia componentes; levanta un segundo proveedor en el puerto 6001).
   - `parte3.sh`: 07 (excepción del proveedor y base de datos detenida).
4. La 10 se obtiene con `mvn -B test` y las variables `DB_*` apuntando a una MariaDB con el esquema.
5. Al terminar: `docker compose -p evidencias down -v`.

Los scripts regeneran los archivos `.txt`; para que el documento muestre los nuevos logs hay que
copiarlos en la sección de la prueba correspondiente.
