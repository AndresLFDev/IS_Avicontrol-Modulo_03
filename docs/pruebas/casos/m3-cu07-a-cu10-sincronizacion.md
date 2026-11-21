### Test Case Specification: Sincronización con Módulo 1 y Módulo 2 y aviso de utilización
**Created** : 2026-11-21  **Spec/Plan Link** : [CU07](../../features/m3-cu07-consultar-galpon-lote-m1/spec.md) · [CU08](../../features/m3-cu08-consultar-resultado-sacrificio-m2/spec.md) · [CU09](../../features/m3-cu09-consultar-alimento-requerido-m2/spec.md) · [CU10](../../features/m3-cu10-consultar-consumo-medicamento-m2/spec.md) · [001-sincronizacion-m1-m2.md](../../planes/001-sincronizacion-m1-m2.md)  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-M3-CU07-CU10-Sincronizacion
**Product Name & Version** : AviControl Módulo 3 (rama `develop`, sin versión entregada)
**Author** : Juan Grau
**Current Owner** : Equipo Módulo 3
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that **la sincronización con Módulo 1 y Módulo 2** (tareas programadas del backend, sin pantalla propia) functions successfully under specified operational conditions, y que el sistema:

- consulta por REST el REST de M1 y llena la copia local de galpones, lotes y alertas de vaciado (CU07);
- consulta a M2, solo por los lotes con alerta de vaciado y sin Liquidación activa, el resultado final, el alimento y el medicamento, y los guarda con la precisión de M2: 4 decimales en cantidades y precios (CU08, CU09, CU10);
- no duplica datos al repetir la consulta y no renueva `fecha_hora_sync` si nada cambió;
- conserva la copia local y marca la ejecución `FALLIDA` cuando el otro módulo no responde, y lo advierte en RT-09;
- genera la Liquidación con los datos de M2 y le avisa a M2, una sola vez, que usó el resultado (el aviso queda `ENTREGADO` solo con la confirmación de M2).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| M3-CU07 a M3-CU10 | Specification Document | [features/](../../features/) (carpetas `m3-cu07` a `m3-cu10`) |
| Plan 001 Sincronización | Plan Document | [planes/001-sincronizacion-m1-m2.md](../../planes/001-sincronizacion-m1-m2.md) |
| Integración de M2 con M3 | External Document | [integracion-modulo3-rest-kafka.md](https://github.com/OscarHernandez123/AviControlMod2/blob/develop/docs/integracion-modulo3-rest-kafka.md) (repositorio de M2) |
| Guía de pruebas manuales | Process Document | [proceso/guia-pruebas-manuales.md](../../proceso/guia-pruebas-manuales.md) |
| Consultas de apoyo | SQL | Al final de este caso: consultas 13b, 13c, 14, 15 y 16. También reunidas en [datos/consultas-demo.sql](../datos/consultas-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* **Solo se puede probar contra los módulos reales desde el servidor de AWS.** M2 solo acepta peticiones de la IP `52.15.95.91` (la de la EC2 de M3): desde un equipo local responde 403. En local, con `M2_BASE_URL` vacío, M3 no consulta a M2.
* Las secciones 2 y 3 necesitan que **M2 tenga registrado un ciclo completo** (resultado final, suministros de alimento y consumos de medicamento) para un lote que exista en M1 y ya tenga alerta de vaciado. Al 2026-11-21 la base principal de M2 no tiene ningún ciclo completo; el de `7331eec8-e447-4868-bdef-8eca3fff8d44` existe solo en su base DEMO (`/demo/api/integracion/modulo3`), que M2 mantiene hasta la presentación.
* No hay pantalla de la sincronización. Se comprueba con la base, con `GET /api/v1/sincronizacion/estado` y con la lista de lotes.
* El ciclo es de 5 minutos (`SYNC_M1_INTERVALO`, `SYNC_M2_INTERVALO`) y los avisos se reintentan cada minuto. Los pasos indican cuánto esperar.

##### Dependencies & Assumptions
* Despliegue en AWS según [`desplegar-aws.ps1`](../../proceso/guia-pruebas-manuales.md) con `M1_BASE_URL` y `M2_BASE_URL` fijados; el servidor ejecuta Flyway hasta V4.
* El caso [TC-M3-CU03](m3-cu03-generar-liquidacion.md) pasó: se puede generar una Liquidación.
* Conexión a la base del servidor: `ssh` a la instancia y `docker exec -it avicontrol-m3-prod-postgres-1 psql -U avicontrol avicontrol`.

##### Preconditions & Default Setup
* **Initial State** : base con las migraciones V1 a V4, sin datos de demostración (la copia local la llena la sincronización).
* **Environment** : AWS (`http://52.15.95.91`), backend con perfil `prod`.
* **Test Accounts** : no aplica.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

**Última ejecución** : 2026-11-21, en AWS, contra la **base DEMO de M2** (`https://3.21.155.197/demo/api/integracion/modulo3`) con datos simulados por M2 para el lote `7331eec8-e447-4868-bdef-8eca3fff8d44` (1.000 pollos y 2.500 kg; alimento de 40 kg a 2.500 y 20 kg a 3.000 con 5 % de impuesto; medicamento de 100 ml a 50). Resultado: pasos 1 a 3, 5 a 8, 10, 11, 13 y 15 correctos. Se generó la Liquidación 1 con precio `7000` (venta 17.500.000, costos 5.165.000, utilidad 12.335.000); el aviso quedó `ENTREGADO` al primer intento y M2 marca el resultado `UTILIZADO`. **Pendiente**: repetir contra la base principal de M2 (ruta sin `/demo/`) con un lote real de M1, y los pasos 4, 9, 12, 14 y 16 a 19 (repeticiones, bloqueo de corrección en M2 y fallos del otro módulo). La lógica de todos los pasos está cubierta por pruebas automáticas contra PostgreSQL real y un M1 y un M2 simulados con los JSON que ellos documentan: `SincronizacionM1IT` (8 pruebas), `SincronizacionM2IT` (15 pruebas), `ClienteM1RestTest`, `ClienteM2RestTest`, `AplicarLoteM2ServiceTest`, `SyncModulo2ServiceTest` y `PublicarAvisosM2ServiceTest`. La V4 se aplicó además sobre una copia de la base de AWS con 34 lotes y 13 liquidaciones: las sumas de verificación de cantidades y precios no cambiaron.

##### Section 1: Módulo 1 (CU07)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Esperar la primera sincronización | Esperar 1 minuto tras arrancar el backend y ejecutar la consulta 13b | N/A | Hay una fila `MODULO_1` con resultado `EXITOSA`, `registros_actualizados` mayor que 0 y `descripcion_error` vacía (o con los datos omitidos) | [x] | Automatizado: `SincronizacionM1IT.llenaLaCopiaLocal...` |
| 2 | Ver los lotes de M1 en la app | Abrir <http://52.15.95.91>, lista de lotes | N/A | Aparecen los lotes que trae M1: los vaciados como "Por liquidar" y los de galpones en aislamiento, productivos o en cosecha, en su etapa | [x] | Automatizado: `elLoteDesvinculadoApareceComoPorLiquidar...` |
| 3 | Revisar RT-09 | Abrir <http://52.15.95.91/api/v1/sincronizacion/estado> | N/A | `modulo1.desactualizada` es `false` y trae la fecha de la última sincronización | [x] | Consulta 13c devuelve la misma fecha |
| 4 | Comprobar que repetir no duplica | Esperar un ciclo (5 min) y ejecutar las consultas de conteo de la consulta 13b | N/A | Sube el número de filas de la bitácora, pero no el de lotes ni el de alertas | [ ] | Automatizado: `repetirLaSincronizacionNoDuplica...` |

##### Section 2: Módulo 2 (CU08, CU09, CU10)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 5 | Preparar los datos en M2 | En M2, con sus pantallas: registrar para un lote de M1 con alerta de vaciado el resultado final, al menos un suministro de alimento (con dos recepciones de precio distinto, si se puede) y un consumo de medicamento | Lote de M1 que esté "Por liquidar" | M2 devuelve los datos en sus tres consultas | [x] | No insertar datos directo en la base de M2. Ejecutado con la base DEMO de M2 (2026-11-21) |
| 6 | Esperar la sincronización con M2 | Esperar hasta 5 minutos y ejecutar la consulta 13b | N/A | Hay una fila `MODULO_2` `EXITOSA` con `registros_actualizados` igual a 1 resultado + partidas + consumos + tramos | [x] | Automatizado: `llenaLaCopiaLocalConLosDatosDeM2` |
| 7 | Comprobar lo guardado | Ejecutar la consulta 14 | `<id del lote>` | Un resultado, una partida de alimento por cada suministro y recepción (no consolidadas) y los tramos de medicamento con su recepción | [x] | Automatizado: ídem |
| 8 | Comprobar la precisión | Ejecutar la consulta 15 | N/A | Los precios y las cantidades conservan los 4 decimales de M2 (por ejemplo `0.0333`, no `0.03`) | [x] | Consulta 15: las siete columnas dicen 18,4; la consulta 14 mostró `40.0000` y `2500.0000` | Automatizado: `guardaLosPreciosYLasCantidadesConLos4Decimales...` |
| 9 | Repetir sin cambios | Esperar otro ciclo y ejecutar la consulta 14 | N/A | Las mismas filas y la misma `fecha_hora_sync` | [ ] | Automatizado: `repetirNoDuplicaNiRenuevaLaFechaDeSincronizacion...` |
| 10 | Revisar RT-09 | Abrir `/api/v1/sincronizacion/estado` | N/A | `modulo2.desactualizada` es `false` | [x] | |

##### Section 3: Liquidación con datos de M2 y aviso de utilización
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 11 | Generar la Liquidación | Con el caso TC-M3-CU03, generar la Liquidación del lote con un precio por kg | Precio `7000` | La vista previa y la Liquidación usan las cantidades y precios de M2 (costos de alimento y medicina con los 4 decimales de M2) | [x] | Lote `7331eec8`: alimento 160.000, medicamento 5.000; venta 17.500.000 y utilidad 12.335.000 | Automatizado: `laLiquidacionSeCalculaConLosPreciosCompletosDeM2` |
| 12 | Revisar el aviso pendiente | Ejecutar la consulta 16 inmediatamente | N/A | El resultado está `UTILIZADO` y el aviso `PENDIENTE` o ya `ENTREGADO` | [ ] | |
| 13 | Esperar el envío | Esperar 1 minuto y ejecutar la consulta 16 | N/A | El aviso queda `ENTREGADO`, con `intentos` igual a 1 | [x] | M2 DEMO devuelve el resultado `UTILIZADO` | Automatizado: `despuesDeLiquidarElAvisoSaleUnaSolaVez...` |
| 14 | Comprobar que M2 bloqueó la corrección | En M2, intentar corregir ese resultado final | N/A | M2 rechaza la corrección | [ ] | Es una comprobación del lado de M2 |
| 15 | Comprobar que no se vuelve a consultar | Ejecutar la consulta 13b tras otro ciclo | N/A | La sincronización con M2 sigue `EXITOSA` y no cambia el resultado ni las partidas del lote ya liquidado | [x] | Tras liquidar (05:55) hubo 4 ciclos con M2 `EXITOSA` y 0 registros; `fecha_hora_sync` del resultado y las partidas sigue en 05:51:58 | Automatizado: ídem |

##### Section 4: Fallos de los otros módulos
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 16 | Simular que M2 no responde | En el servidor, cambiar `M2_BASE_URL` en `.env.prod` por una dirección que no responda y reiniciar con `desplegar-aws.ps1 -SoloConfig` | `https://203.0.113.1/api/integracion/modulo3` | Tras un ciclo hay una fila `MODULO_2` `FALLIDA` con la causa en `descripcion_error`; los datos de M2 que ya había no cambian | [ ] | Automatizado: `siM2CaeLaCopiaSeConserva...` |
| 17 | Ver el aviso en RT-09 | Abrir `/api/v1/sincronizacion/estado` | N/A | `modulo2.desactualizada` es `true` y sigue mostrando la fecha de la última sincronización exitosa | [ ] | |
| 18 | Aviso con M2 caído | Si hay una Liquidación recién generada, ejecutar la consulta 16 | N/A | El aviso sigue `PENDIENTE` y `intentos` aumenta cada minuto; la Liquidación no se ve afectada | [ ] | Automatizado: `siM2NoConfirmaElAvisoQuedaPendiente...` |
| 19 | Restaurar | Volver a poner el `M2_BASE_URL` correcto y reiniciar | `https://3.21.155.197/api/integracion/modulo3` | La siguiente sincronización es `EXITOSA` y el aviso pendiente pasa a `ENTREGADO` con el mismo `eventId` | [ ] | |

--------------------------------------------------------------------------------

##### Consultas SQL de este caso
Las consultas están aquí mismo, listas para copiar. La numeración es la de [`datos/consultas-demo.sql`](../datos/consultas-demo.sql), que las reúne todas.

**Consulta 13b**

```sql
-- 13b. Bitácora de sincronización con M1 y M2: una fila por ejecución. resultado EXITOSA o FALLIDA;
--      descripcion_error lleva la causa del fallo o los datos que se omitieron por inválidos.
SELECT id, fuente, topico, fecha_hora_inicio, fecha_hora_fin, resultado, registros_actualizados,
       descripcion_error
  FROM registro_sincronizacion
 ORDER BY id DESC
 LIMIT 20;
```

**Consulta 13c**

```sql
-- 13c. Última sincronización exitosa por fuente: es lo que devuelve GET /api/v1/sincronizacion/estado.
SELECT fuente, MAX(fecha_hora_fin) AS ultima_exitosa
  FROM registro_sincronizacion
 WHERE resultado = 'EXITOSA' AND fecha_hora_fin IS NOT NULL
 GROUP BY fuente
 ORDER BY fuente;
```

**Consulta 14**

```sql
-- 14. Lo que M3 guardó de M2 para un lote. Sustituye el UUID por el del lote que probaste.
SELECT 'resultado' AS dato, r.id_resultado::text AS id, r.cantidad_final_pollos::text AS cantidad,
       r.peso_total_kg::text AS valor, r.estado_utilizacion AS estado
  FROM resultado_final_sacrificio r WHERE r.id_lote = '00000000-0000-0000-0000-000000000000'
UNION ALL
SELECT 'alimento', p.id_partida_origen::text, p.cantidad_kg::text, p.precio_unitario_kg_cop::text,
       p.estado_valorizacion
  FROM partida_alimento_lote p WHERE p.id_lote = '00000000-0000-0000-0000-000000000000'
UNION ALL
SELECT 'medicamento ' || c.medicamento, t.referencia_recepcion, t.cantidad_unidad_base::text,
       t.precio_unitario_base_cop::text, t.estado_valorizacion
  FROM tramo_recepcion_consumo t
  JOIN consumo_medicamento_lote c ON c.id_consumo_origen = t.id_consumo_origen
 WHERE c.id_lote = '00000000-0000-0000-0000-000000000000';
```

**Consulta 15**

```sql
-- 15. Precisión de cantidades y precios: debe decir 18,4 en las siete columnas (migración V4).
SELECT table_name, column_name, numeric_precision, numeric_scale
  FROM information_schema.columns
 WHERE table_schema = 'public'
   AND (table_name, column_name) IN (
       ('partida_alimento_lote', 'cantidad_kg'), ('partida_alimento_lote', 'precio_unitario_kg_cop'),
       ('consumo_medicamento_lote', 'cantidad_unidad_base'),
       ('tramo_recepcion_consumo', 'cantidad_unidad_base'),
       ('tramo_recepcion_consumo', 'precio_unitario_base_cop'),
       ('partida_costo_lote', 'cantidad'), ('partida_costo_lote', 'precio_unitario_cop'))
 ORDER BY table_name, column_name;
```

**Consulta 16**

```sql
-- 16. Aviso de utilización de cada resultado: PENDIENTE hasta que M2 responde 200 con UTILIZADO.
--     intentos cuenta los envíos (los fallidos y el exitoso).
SELECT a.id_aviso, a.id_resultado, r.id_lote, r.estado_utilizacion, a.estado_entrega, a.intentos,
       a.fecha_hora_emision
  FROM aviso_utilizacion_resultado a
  JOIN resultado_final_sacrificio r ON r.id_resultado = a.id_resultado
 ORDER BY a.id_aviso;
```

--------------------------------------------------------------------------------
##### Edge Cases & Boundary Conditions
* **EC-001** : El lote no tiene resultado, alimento ni medicamento en M2. La ejecución es `EXITOSA` con 0 registros y no se escribe nada; la Liquidación queda bloqueada por falta de datos. Automatizado: `unLoteSinResultadoNiPartidasNoEscribeNada...`.
* **EC-002** : M2 entrega un resultado con otro `id` para un lote que ya tiene uno. Se conserva el primero y la ejecución anota una incidencia (CU08.FR-010). Automatizado: `unSegundoResultadoDistintoParaElLote...`.
* **EC-003** : El resultado ya está `UTILIZADO` y M2 lo corrige. No se actualiza (CU08.FR-004). Automatizado: `unResultadoYaUtilizadoNoSePisa...`.
* **EC-004** : Un consumo de medicamento con un tramo inválido (cantidad cero, recepción repetida, unidades distintas) se omite completo y se informa, para no cobrar de menos. Automatizado: `unConsumoInvalidoSeOmite...`.
* **EC-005** : Una partida sin precio queda `PENDIENTE_PRECIO` y bloquea la Liquidación (CU09.FR-005). Automatizado: `unaPartidaSinPrecioSeGuarda...`.
* **EC-006** : M2 responde 409 o algo distinto de 200 `UTILIZADO` al aviso. No se marca `ENTREGADO`. Automatizado: `ClienteM2RestTest`.
* **EC-007** : El certificado de M2 (de IP, vence el 2026-11-27 y se renueva solo) falla. Las consultas dan `FALLIDA` por TLS y no se desactiva la verificación. No automatizado.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
