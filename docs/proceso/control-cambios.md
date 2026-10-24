# Control de cambios — Avicontrol Módulo 03

Registro interno de las solicitudes de cambio (SC) sobre lo acordado para el Módulo 03: fórmulas, funcionalidades, alcance, fechas y datos esperados de otros módulos. El proceso se describe en [`CONTRIBUTING.md`](../../CONTRIBUTING.md#5-control-de-cambios).

## Estados

| Estado | Significado |
|---|---|
| **Propuesto** | Registrada en un PR de `docs/`, pendiente de aprobación |
| **Aprobado** | PR de `docs/` aprobado por el líder; plan actualizado |
| **Rechazado** | No se aplicará; se deja registrado el motivo |
| **Implementado** | El código del cambio está en `develop` |
| **Verificado** | El cambio está incluido en una versión entregada |

## Registro

Los ID de solicitud llevan dos dígitos (`SC-03`) y no deben confundirse con los criterios de éxito de las specs, que llevan tres (`SC-003`). Fuera de este archivo se citan como "solicitud SC-03" o con `Refs: SC-03`.

| ID | Fecha | Solicitante | Descripción | Motivo | Impacto (alcances / esfuerzo) | Estado | Aprobó | Issue | Versión |
|---|---|---|---|---|---|---|---|---|---|
| SC-01 | 2026-10-05 | Equipo M3 | La lista de lotes deja de mostrar los lotes con Liquidación `ACTIVA`; se consultan en el historial | Evitar duplicar en la lista lo que ya muestra el historial | CU01, CU03, CU04, diccionario, planes 002 y 004 · Figma lista de lotes · 0,5 días | Propuesto | | — | |
| SC-02 | 2026-10-05 | Docente | Definir los contratos REST completos: query params, cuerpos de respuesta y errores | Observación del docente: query params y API REST sin definir | `api` · arquitectura general, planes 002, 003 y 004 · 0,5 días | Propuesto | | — | |
| SC-03 | 2026-10-23 | Equipo M3 | La integración con M1 y M2 pasa a ser solo por eventos Kafka: M1 y M2 publican, M3 consume; el aviso de utilización se publica en un tópico de M3 | Unificar el modelo de integración, que se contradecía entre la especificación general (consulta) y la arquitectura (Kafka) | `integracion` · especificación general, CU07–CU10, arquitectura general, plan 001 · Depende de que M1 y M2 publiquen (D-08) · 1 día | Propuesto | | #9 | |
| SC-04 | 2026-10-23 | Equipo M3 | Sin autenticación: se quitan Spring Security, JWT, roles y los errores 401/403; el usuario responsable se digita en los formularios de generar y anular | La plataforma no tiene login | `api`, `liquidacion` · especificación general, CU03, CU04, arquitectura general, plan 003 · 0,5 días | Propuesto | | #9 | |
| SC-05 | 2026-10-24 | Equipo M3 | El resultado de sacrificio es definitivo desde que una Liquidación lo usa: M3 rechaza las correcciones de M2 aunque la Liquidación se anule, y la anulación ya no sirve para corregir la cantidad ni el peso | CU08.FR-005 exigía anular para corregir el resultado, pero al anular el resultado seguía `UTILIZADO` y la corrección se rechazaba (revisión del PR #10) | `integracion`, `liquidacion` · CU04, CU08, diccionario, especificación general (D-03), plan 001 · 0,25 días | Propuesto | | #9 | |

<!--
Ejemplos de filas:

| SC-01 | 2026-10-05 | Docente | Incluir costos indirectos en la utilidad neta | Requisito del cliente | `costos`, `liquidacion` · 2 días | Implementado | Líder | #27 | |
| SC-02 | 2026-10-12 | Módulo 2 (aviso) | El consumo de alimento se reporta por semana, no por día | Cambio en Módulo 2 | `integracion` · 1 día | Aprobado | Líder | #31 | |
-->

## Detalle de solicitudes

Opcional: para cambios que requieren más explicación que una fila de la tabla.

### SC-01 — <título>

- **Situación actual:** cómo funciona hoy.
- **Cambio propuesto:** cómo debe funcionar.
- **Motivo:** por qué se pide.
- **Impacto:** alcances, archivos o fórmulas afectados, esfuerzo estimado y riesgos.
- **Decisión:** aprobado o rechazado, quién decidió y cuándo.
