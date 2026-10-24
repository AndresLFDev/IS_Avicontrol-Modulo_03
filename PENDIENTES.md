# PENDIENTES.md — Lista de Pendientes del Módulo 03

Lista de tareas pendientes y acuerdos del equipo para las siguientes fases.

## Arquitectura y Frontend

- [ ] Diseñar las pantallas P1 a P5 en el frontend (React + TypeScript + Vite), incluido el indicador de sincronización (RT-09) y el campo de usuario responsable en generar y anular.
- [ ] Exportar a Excel (CU05): implementación técnica con Apache POI (`poi-ooxml`).

## Coordinación con Módulo 1 y Módulo 2

- [ ] Decisiones D-01 a D-08 con los equipos de M1 y M2 (ver `docs/requisitos/especificacion-general.md`).
- [ ] D-08: acordar que M1 y M2 publiquen sus eventos en Kafka con los tópicos y campos del plan 001 (`docs/planes/001-sincronizacion-m1-m2.md`), o ajustar el contrato a lo que publiquen.
- [ ] D-03: acordar con M2 el formato y la confirmación del aviso de utilización (`avicontrol.m3.avisos-utilizacion`), y que no corrija un resultado ya avisado (es definitivo para M3).

## Construcción del Backend

- [ ] Crear `backend/build.gradle` y `settings.gradle` con Spring Boot 4.1.x, Spring Kafka, Flyway, PostgreSQL, Apache POI, springdoc, Testcontainers y ArchUnit.
- [ ] Crear `compose.yaml` con PostgreSQL y Kafka para desarrollo local.
- [ ] Crear las migraciones Flyway en `backend/src/main/resources/db/migration/` (`V1__create_sync_tables.sql`, `V2__create_partidas_tables.sql`, `V3__create_liquidacion_tables.sql`) y los datos de demostración en `db/seed/`.
- [ ] Implementar la suite de pruebas unitarias (JUnit 5 / Mockito), de integración (Testcontainers) y de arquitectura (ArchUnit).
