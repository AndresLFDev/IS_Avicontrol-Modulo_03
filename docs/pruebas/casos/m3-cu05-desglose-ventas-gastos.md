### Test Case Specification: Desglose de ventas y gastos
**Created** : 2026-11-13  **Spec/Plan Link** : [spec.md](../../features/m3-cu05-desglose-ventas-gastos/spec.md) · [004-reportes-historial.md](../../planes/004-reportes-historial.md)  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-M3-CU05-DesgloseVentasGastos
**Product Name & Version** : AviControl Módulo 3 (rama `develop`, sin versión entregada)
**Author** : Juan Grau
**Current Owner** : Equipo Módulo 3
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that **el desglose de ventas y gastos de una liquidación** (`GET /api/v1/liquidaciones/{id}/desglose`, la pantalla "Ver desglose" y la exportación `GET /api/v1/liquidaciones/{id}/desglose/excel`) functions successfully under specified operational conditions, y que el sistema:

- muestra los ingresos (pollos vendidos, peso total, peso promedio, precio por kg y venta bruta) y los costos operativos por categoría, con la trazabilidad de cada partida a su origen;
- cuadra: la suma de los subtotales de categoría es igual a los costos operativos de la liquidación;
- muestra el desglose de un siniestro total sin venta;
- exporta a Excel los mismos valores, con los subtotales y el total como fórmulas;
- muestra el desglose de una liquidación anulada en solo lectura y no permite exportarla.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| M3-CU05 Desglose de ventas y gastos | Specification Document | [features/m3-cu05-desglose-ventas-gastos/spec.md](../../features/m3-cu05-desglose-ventas-gastos/spec.md) |
| Plan 004 Reportes e historial | Plan Document | [planes/004-reportes-historial.md](../../planes/004-reportes-historial.md) |
| Guía de pruebas manuales | Process Document | [proceso/guia-pruebas-manuales.md](../../proceso/guia-pruebas-manuales.md) |
| Consultas de apoyo | SQL | Al final de este caso: consultas 4, 6, 7 y 8. También reunidas en [datos/consultas-demo.sql](../datos/consultas-demo.sql) |
| Reinicio de datos | SQL | Al final de este caso. El mismo archivo está en [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El contenido del archivo Excel se comprueba abriéndolo en una hoja de cálculo. La verificación automática de este caso leyó el archivo descargado (una hoja, textos y fórmulas), no su aspecto visual.

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* La Sección 1 del caso [TC-M3-CU03](m3-cu03-generar-liquidacion.md) pasó: existe una liquidación `ACTIVA` de L-2026-03 con precio 4500.

##### Preconditions & Default Setup
* **Initial State** : base con el seed `V1000` y una liquidación `ACTIVA` de L-2026-03 con precio 4500. Si hay otros datos, ejecutar la consulta de reinicio (al final de este caso) y generarla de nuevo.
* **Environment** : local. PostgreSQL en Docker, backend con perfil `dev`, frontend con `npm run dev:backend`.
* **Test Accounts** : no aplica.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

**Última ejecución** : 2026-11-13. Los resultados del backend y de la base se verificaron contra la app real (`bootRun` con perfil `dev` y el seed), llamando a la API con los mismos datos y consultando con `psql`; el Excel descargado se abrió y se leyeron sus textos y fórmulas. Lo que muestra la pantalla se leyó en el código del frontend y no se vio en el navegador: esos pasos quedan en `[~]`.

##### Section 1: Desglose y Excel de una liquidación activa
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir el desglose | Abrir la liquidación activa de L-2026-03 y pulsar "Ver desglose" | N/A | Aparece el desglose con el bloque "Ingresos" (pollos vendidos 8.500, peso total 23.800 kg, peso promedio 2,8 kg, precio por kg $4.500, venta bruta $107.100.000) y el bloque "Costos operativos" | [~] | API: esos valores. Etiquetas tomadas del código |
| 2 | Revisar las categorías | Mirar "Costos operativos" | N/A | Alimento $64.800.000 (Pre-inicio $15.600.000, Inicio $30.000.000, Engorde $19.200.000). Medicina $2.200.000 (Amoxicilina $600.000 de REC-2026-090 y $420.000 de REC-2026-101, Vacuna Newcastle $1.180.000 de REC-2026-085). Población $18.000.000, "Costo de adquisición del lote (9000 aves)", origen Módulo 1. Costos operativos totales $85.000.000 | [~] | API: esas partidas, en ese orden, con referencias de origen. Con el seed el orden es siempre el mismo |
| 3 | Revisar la utilidad y la exportación | Mirar la utilidad neta y el botón "Exportar a Excel" | N/A | Utilidad neta $22.100.000 y el botón "Exportar a Excel" habilitado | [~] | API: `utilidadNetaCop` 22.100.000 y `exportacionPermitida` `true` |
| 4 | Comparar con la base | Ejecutar las consultas 7 y 8 (al final de este caso) | N/A | Los mismos números que la pantalla: subtotales 64.800.000, 2.200.000 y 18.000.000 | [✔] | Verificado con la base |
| 5 | Exportar a Excel | Pulsar "Exportar a Excel" | N/A | Se descarga el archivo `Liquidacion_Lote_<id>.xlsx` (con el id de la liquidación) | [~] | API: 200, tipo `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `attachment`, 4,7 KB |
| 6 | Abrir el archivo | Abrir el Excel en una hoja de cálculo | N/A | Una hoja con "Liquidación de lote", la Matriz de Venta Final (pollos vendidos, peso total, peso promedio, precio por kg, venta bruta, mortalidad, costos operativos, utilidad neta) y el "Desglose de costos" con concepto, cantidad, unidad, precio unitario, origen y subtotal. Los subtotales por categoría y el total de costos son fórmulas `SUM` | [~] | Verificado leyendo el archivo: una hoja, los textos de esos encabezados y las fórmulas `SUM(F21:F23)`, `SUM(F26:F28)`, `SUM(F31:F31)` y `SUM(F24,F29,F32)`. El aspecto en pantalla no se vio |

--------------------------------------------------------------------------------

##### Section 2: Siniestro total y liquidación anulada
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Desglose de un siniestro total | Generar la liquidación de L-2026-04 ([TC-M3-CU03](m3-cu03-generar-liquidacion.md), Sección 2) y pulsar "Ver desglose" | N/A | La pantalla muestra "Este lote no registró venta." Costos operativos $32.800.000 (Alimento $21.000.000, Medicina $1.800.000, Población $10.000.000) y utilidad neta -$32.800.000 | [~] | API: ventas en 0, peso promedio y precio vacíos, esos costos y utilidad. Texto de pantalla tomado del código |
| 2 | Excel de un siniestro total | Pulsar "Exportar a Excel" | N/A | Se descarga el archivo; un siniestro total sí se exporta | [~] | API: 200 con `attachment` |
| 3 | Anular la liquidación de L-2026-03 | Seguir [TC-M3-CU04](m3-cu04-anular-liquidacion.md), Sección 1 | Motivo `Precio por kg mal digitado` | Queda `ANULADA` | [~] | Verificado con la API |
| 4 | Abrir el desglose de la anulada | Abrir "Ver desglose" de la liquidación anulada | N/A | Aparece el rótulo "LIQUIDACIÓN ANULADA" con el mismo desglose y los mismos valores, en solo lectura | [~] | API: estado `ANULADA`, costos 85.000.000 y el registro de anulación. Rótulo tomado del código |
| 5 | Intentar exportar | Mirar el botón "Exportar a Excel" | N/A | El botón está deshabilitado. Al pasar el cursor dice "Una liquidación anulada no se puede exportar" | [~] | API: `exportacionPermitida` es `false`. Texto tomado del código |
| 6 | Exportar por la URL | En el navegador, abrir `http://localhost:8080/api/v1/liquidaciones/<id>/desglose/excel` con el id de la anulada | Id de la consulta 6 | 409 `exportacion-no-permitida`: "La Liquidación <id> está anulada: su desglose es de solo lectura y no se puede exportar." | [✔] | Verificado con la API |
| 7 | Desglose de una liquidación que no existe | `GET /api/v1/liquidaciones/999999/desglose` | N/A | 404 `liquidacion-no-encontrada` | [✔] | Verificado con la API |
| 8 | Dejar la base limpia | Ejecutar la consulta de reinicio (al final de este caso) | N/A | Sin liquidaciones | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Consultas SQL de este caso
Las consultas de apoyo y el reinicio están aquí mismo, listos para copiar; no hace falta abrir ningún otro archivo. La numeración es la de [`datos/consultas-demo.sql`](../datos/consultas-demo.sql), que las reúne todas. Conexión de desarrollo: host `localhost`, puerto `DB_PORT` del `.env` (5433), base `avicontrol`, usuario y contraseña `avicontrol`. En una consola SQL, pon el cursor dentro de la consulta y pulsa Ctrl+Enter. Las consultas son de solo lectura; solo la de reinicio borra datos.

**Consulta 4**

```sql
-- 4. Partidas de alimento y de medicina de cada lote, con su subtotal
-- Cada partida de alimento cuesta cantidad_kg por precio_unitario_kg_cop. Cada tramo de
-- medicina cuesta cantidad por precio. Una partida con precio NULL o estado PENDIENTE_PRECIO
-- impide liquidar (error 422 PARTIDAS_SIN_PRECIO). Eso pasa con L-2026-06 (REC-2026-121).
SELECT l.nombre AS lote, 'ALIMENTO' AS categoria, p.tipo_alimento AS concepto,
       p.referencia_origen AS referencia, p.cantidad_kg AS cantidad,
       p.precio_unitario_kg_cop AS precio, p.estado_valorizacion,
       ROUND(p.cantidad_kg * p.precio_unitario_kg_cop) AS subtotal_cop
  FROM partida_alimento_lote p
  JOIN lote l ON l.id_lote = p.id_lote
UNION ALL
SELECT l.nombre, 'MEDICINA', c.medicamento,
       t.referencia_recepcion, t.cantidad_unidad_base,
       t.precio_unitario_base_cop, t.estado_valorizacion,
       ROUND(t.cantidad_unidad_base * t.precio_unitario_base_cop)
  FROM consumo_medicamento_lote c
  JOIN tramo_recepcion_consumo t ON t.id_consumo_origen = c.id_consumo_origen
  JOIN lote l ON l.id_lote = c.id_lote
 ORDER BY 1, 2, 3;
```

**Consulta 6**

```sql
-- 6. Liquidaciones generadas
-- Una fila por liquidación. Mira estado (ACTIVA o ANULADA), el precio, la venta bruta, la
-- mortalidad, los costos y la utilidad. En siniestro total, precio_kg_cop y peso_promedio_kg
-- son NULL, pollos_vendidos es 0 y id_resultado_sacrificio es NULL.
-- Esperado para L-2026-03 con precio 4500: venta 107.100.000, costos 85.000.000,
-- utilidad 22.100.000, mortalidad 500 aves (5,56 %).
-- Esperado para L-2026-04: venta 0, costos 32.800.000, utilidad -32.800.000, mortalidad 100 %.
SELECT liq.id_liquidacion, l.nombre AS lote, liq.estado, liq.precio_kg_cop,
       liq.pollos_vendidos, liq.peso_total_kg, liq.venta_bruta_cop,
       liq.mortalidad_aves, liq.porcentaje_mortalidad,
       liq.costos_operativos_cop, liq.utilidad_neta_cop,
       liq.usuario_responsable, liq.fecha_hora_generacion
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
 ORDER BY liq.id_liquidacion;
```

**Consulta 7**

```sql
-- 7. Partidas congeladas de cada liquidación
-- Al generar, la app copia cada partida a partida_costo_lote. Así el desglose no cambia aunque
-- M1 o M2 envíen datos nuevos después. Debe haber partidas de ALIMENTO, MEDICINA y POBLACION.
-- Esperado para L-2026-03: Pre-inicio 15.600.000, Inicio 30.000.000, Engorde 19.200.000,
-- Amoxicilina 600.000 y 420.000 (dos recepciones), Vacuna Newcastle 1.180.000, población 18.000.000.
-- Con el seed el orden es siempre el mismo: las partidas se copian ordenadas por su id de origen.
SELECT p.id_liquidacion, p.categoria, p.concepto, p.cantidad, p.unidad_medida,
       p.precio_unitario_cop, p.subtotal_cop, p.fuente_origen, p.referencia_origen
  FROM partida_costo_lote p
 ORDER BY p.id_liquidacion, p.categoria, p.id_partida;
```

**Consulta 8**

```sql
-- 8. Subtotales por categoría de cada liquidación
-- Debe coincidir con las tres tarjetas del desglose y sumar los costos operativos.
-- Esperado para L-2026-03: ALIMENTO 64.800.000, MEDICINA 2.200.000, POBLACION 18.000.000.
SELECT p.id_liquidacion, p.categoria, SUM(p.subtotal_cop) AS subtotal_cop
  FROM partida_costo_lote p
 GROUP BY p.id_liquidacion, p.categoria
 ORDER BY p.id_liquidacion, p.categoria;
```

**Consulta de reinicio** (borra datos)

```sql
-- Consulta de reinicio. ATENCIÓN: BORRA datos. Solo para la base de desarrollo local (perfil dev).
-- Borra lo que generó la app (liquidaciones, partidas, snapshot, anulaciones y avisos) y devuelve los
-- resultados de sacrificio a NO_UTILIZADO. No borra el seed. Selecciona las seis sentencias y ejecútalas juntas.
DELETE FROM registro_anulacion;
DELETE FROM snapshot_datos_origen;
DELETE FROM partida_costo_lote;
DELETE FROM aviso_utilizacion_resultado;
DELETE FROM liquidacion;
UPDATE resultado_final_sacrificio SET estado_utilizacion = 'NO_UTILIZADO';

-- Opcional: para que los ids de liquidación vuelvan a empezar en 1.
-- ALTER SEQUENCE liquidacion_id_liquidacion_seq RESTART WITH 1;
```

--------------------------------------------------------------------------------
##### Edge Cases & Boundary Conditions
* **EC-001** : Los datos de origen cambian después de generar la liquidación. El desglose no cambia, porque lee las partidas congeladas de la liquidación (`partida_costo_lote`) y no las de Módulo 1 o Módulo 2. Verificado con la API: se cambió el precio de una partida de `partida_alimento_lote` (de 2.400 a 2.450) y el desglose quedó idéntico, con costos de $85.000.000. Después se devolvió el precio a 2.400.
* **EC-002** : El orden de las partidas dentro de una categoría no está definido en la spec. Las partidas se copian a la liquidación ordenadas por su id de origen. Con el seed, que tiene ids fijos, el orden es siempre el mismo; con otros ids de origen el orden puede ser otro. Los totales no cambian.
* **EC-003** : Descargar el Excel de una liquidación grande o con muchas partidas. No aplica con el seed.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
