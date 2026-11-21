# PENDIENTES.md — Lista de Pendientes del Módulo 03

Lista de tareas pendientes y acuerdos del equipo. Estado revisado el 2026-11-13.

## Pendiente

### Frontend

- [ ] Indicador de sincronización con M1 y M2 (RT-09). El backend ya expone `GET /api/v1/sincronizacion/estado`; falta mostrarlo en la barra de la aplicación (`Layout.tsx`).

### Integración con Módulo 1 y Módulo 2 (M3-CU07 a M3-CU10)

- [x] Sincronización por REST según el plan 001: CU07 con M1 (PR #50) y CU08 a CU10 con M2 más el aviso de utilización por `POST` (este cambio). Desplegada en AWS y probada el 2026-11-21 de punta a punta con la base DEMO de M2 (lote `7331eec8-e447-4868-bdef-8eca3fff8d44`: Liquidación generada y aviso entregado). Falta repetir la prueba con la base principal de M2 y un lote real de M1: M2 registra el ciclo cuando M1 prepare el lote y M3 cambia `M2_BASE_URL` a la ruta sin `/demo/`.
- [ ] Decisiones D-01 a D-08 con los equipos de M1 y M2 (ver `docs/requisitos/especificacion-general.md`).
- [x] D-08: resuelta el 2026-11-21. M2 documentó las rutas y los campos, y autoriza a M3 por IP (sin usuario ni clave).
- [x] D-03: resuelta el 2026-11-21. El aviso va por REST (`POST` a M2), con respuesta idempotente, y M2 no corrige un resultado ya avisado.
- [ ] Quitar Kafka que M3 ya no usa: la dependencia `spring-boot-starter-kafka`, `spring.kafka` en los perfiles, la sección `kafka.topicos` de `application.yml`, el servicio de `compose.yaml` y el contenedor de las pruebas de integración.
- [ ] D-05: decidir si los costos operativos se calculan sobre el valor neto o con impuesto. M2 ya entrega el neto y el impuesto por separado; M3 guarda `valor_impuesto_cop` y no lo suma.

## Hecho

- [x] Backend con Spring Boot, Flyway, PostgreSQL, Apache POI, springdoc, Testcontainers y ArchUnit (`backend/build.gradle`).
- [x] `compose.yaml` con PostgreSQL y Kafka para desarrollo local.
- [x] Migraciones Flyway V1 a V3 y datos de demostración en `backend/src/main/resources/db/seed/`.
- [x] M3-CU01, M3-CU03, M3-CU04, M3-CU05 y M3-CU06 en backend y frontend, incluida la exportación a Excel con Apache POI.
- [x] Suite de pruebas unitarias (JUnit 5 y Mockito), de integración (Testcontainers) y de arquitectura (ArchUnit) en el backend, y pruebas con Vitest en el frontend.
