## Prueba 10: Pruebas automatizadas

*Requisitos del Nivel 2 y preguntas de análisis*

**Evidencia:** `10_pruebas_automatizadas.txt`

**Resultado:** `mvn test` con MariaDB: **195 pruebas, 0 fallos, 0 omitidas** (Common 10, Broker 47, Sistema 1 129, Sistema 2 9)

```text
# Evidencia adicional: pruebas automatizadas
Fecha (hora local del equipo): 2026-11-14 09:21:33
Codigo: rama dev/chris, commit bb03b8c

Comando: mvn -B test, con la MariaDB de pruebas (DB_HOST=localhost, DB_PORT=3307) para que se ejecuten tambien
las pruebas de integracion con la base de datos.

## Resultado por clase de prueba
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in mx.uady.inventario.common.contrato.SolicitudRespuestaTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.035 s -- in mx.uady.inventario.common.modelo.ModelosGsonTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.113 s -- in mx.uady.inventario.broker.network.BrokerServerTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.019 s -- in mx.uady.inventario.broker.registry.RegistroServiciosTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.845 s -- in mx.uady.inventario.broker.routing.EnrutadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.uady.inventario.broker.util.BitacoraBrokerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.uady.inventario.broker.util.ConfiguracionBrokerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in mx.uady.inventario.broker.util.JsonBrokerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.867 s -- in mx.uady.inventario.central.controller.MercanciaControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.uady.inventario.central.controller.ProductoControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.108 s -- in mx.uady.inventario.central.controller.TareaAsyncTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.467 s -- in mx.uady.inventario.central.controller.VentaControllerTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.uady.inventario.central.network.DespachadorSolicitudesTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.096 s -- in mx.uady.inventario.central.network.IntegracionFase5Test
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in mx.uady.inventario.central.network.JsonServicioTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.078 s -- in mx.uady.inventario.central.network.RegistradorBrokerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in mx.uady.inventario.central.network.ServidorServicioTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in mx.uady.inventario.central.network.VentaServicioTcpTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in mx.uady.inventario.central.repository.MercanciaRepositoryTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.081 s -- in mx.uady.inventario.central.repository.VentaRepositoryIntegracionTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in mx.uady.inventario.central.service.jdbc.ConsultasVentaJdbcIntegracionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.185 s -- in mx.uady.inventario.central.service.jdbc.MercanciaServiceJdbcIntegracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.102 s -- in mx.uady.inventario.central.service.jdbc.ProductoServiceJdbcIntegracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in mx.uady.inventario.central.service.jdbc.TransaccionesTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.081 s -- in mx.uady.inventario.central.service.memoria.MercanciaServiceMemoriaTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.uady.inventario.central.service.memoria.ProductoServiceMemoriaTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in mx.uady.inventario.central.service.memoria.VentasMemoriaTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in mx.uady.inventario.central.service.ProductoValidatorTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in mx.uady.inventario.central.service.QrServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in mx.uady.inventario.central.service.VentaServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.011 s -- in mx.uady.inventario.central.util.ConexionBDIntegracionTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in mx.uady.inventario.central.util.ConfiguracionBDTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in mx.uady.inventario.central.util.ConfiguracionServicioTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in mx.uady.inventario.central.view.FiltroTextoTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.002 s -- in mx.uady.inventario.central.view.MensajesTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in mx.uady.inventario.central.view.MercanciaPanelTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in mx.uady.inventario.central.view.MercanciaTableModelTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in mx.uady.inventario.central.view.ProductoDialogTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.076 s -- in mx.uady.inventario.central.view.ProductosPanelTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in mx.uady.inventario.central.view.VentaHistorialPanelTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in mx.uady.inventario.central.view.VentaPanelTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.286 s -- in mx.uady.inventario.cliente.controller.RegistroVentaControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.uady.inventario.cliente.util.ConfiguracionNetworkTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.054 s -- in mx.uady.inventario.cliente.view.RegistroVentaFrameTest

## Resultado por modulo
[INFO] Reactor Build Order:
[INFO] Building Sistema de Trazabilidad e Inventarios UADY 1.0.0-SNAPSHOT [1/5]
[INFO] Building Common - Modulo comun 1.0.0-SNAPSHOT                      [2/5]
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building Broker - Intermediario 1.0.0-SNAPSHOT                     [3/5]
[INFO] Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building Sistema 1 - Centralizado / Proveedor 1.0.0-SNAPSHOT       [4/5]
[INFO] Tests run: 129, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building Sistema 2 - Cliente del Proveedor 1.0.0-SNAPSHOT          [5/5]
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
