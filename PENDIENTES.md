# PENDIENTES.md — Lista de Pendientes del Módulo 03

Lista de tareas pendientes y acuerdos del equipo. Estado revisado el 2026-11-13.

## Pendiente

### Frontend

- [ ] Indicador de sincronización con M1 y M2 (RT-09). El backend ya expone `GET /api/v1/sincronizacion/estado`; falta mostrarlo en la barra de la aplicación (`Layout.tsx`).

### Integración con Módulo 1 y Módulo 2 (M3-CU07 a M3-CU10)

- [ ] Implementar la sincronización por REST según el plan 001 (`docs/planes/001-sincronizacion-m1-m2.md`): primero CU07 con M1 (issue #48), después CU08 a CU10 con M2. El backend aún no tiene clientes REST ni tarea de sincronización; el aviso de utilización solo se guarda en la tabla.
- [ ] Decisiones D-01 a D-08 con los equipos de M1 y M2 (ver `docs/requisitos/especificacion-general.md`).
- [ ] D-08: acordar con M2 las rutas, la autenticación y los campos de sus consultas (para M1 ya están en su acuerdo de contratos).
- [ ] D-03: acordar con M2 el canal (REST o Kafka alojado por M2), el formato y la confirmación del aviso de utilización, y que no corrija un resultado ya avisado (es definitivo para M3). Hoy el aviso solo se guarda en la tabla `aviso_utilizacion_resultado` con estado `PENDIENTE`; no se publica.

## Hecho

- [x] Backend con Spring Boot, Flyway, PostgreSQL, Apache POI, springdoc, Testcontainers y ArchUnit (`backend/build.gradle`).
- [x] `compose.yaml` con PostgreSQL y Kafka para desarrollo local.
- [x] Migraciones Flyway V1 a V3 y datos de demostración en `backend/src/main/resources/db/seed/`.
- [x] M3-CU01, M3-CU03, M3-CU04, M3-CU05 y M3-CU06 en backend y frontend, incluida la exportación a Excel con Apache POI.
- [x] Suite de pruebas unitarias (JUnit 5 y Mockito), de integración (Testcontainers) y de arquitectura (ArchUnit) en el backend, y pruebas con Vitest en el frontend.
