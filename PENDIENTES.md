# PENDIENTES.md — Lista de Pendientes del Módulo 03

Lista de tareas pendientes y acuerdos del equipo para las siguientes fases.

## Arquitectura y Frontend

- [ ] Definir la tecnología y arquitectura oficial del Frontend (React/Vite o alternativa acordada) y diseñar las pantallas P1 a P5.
- [ ] Exportar a Excel (CU05): implementación técnica con Apache POI (`poi-ooxml`).

## Coordinación con Módulo 1 y Módulo 2

- [ ] Decisiones D-01 a D-07 con los equipos de M1 y M2 (ver `docs/requisitos/especificacion-general.md` y `docs/arquitectura/general.md`).
- [ ] Confirmar los nombres de tópicos Kafka (`avicontrol.m2.resultados-sacrificio`) y formato de eventos con M2.
- [ ] Confirmar nombres exactos de campos en DTOs REST con M1 (`idGalpon` vs `galponId`).

## Construcción del Backend

- [ ] Crear el archivo de configuración `build.gradle` con Spring Boot 3.3.x, Flyway, PostgreSQL, Kafka y Apache POI.
- [ ] Crear los scripts de migración Flyway en `backend/src/main/resources/db/migration/` (`V1__init_schema.sql`, `V2__seed_test_data.sql`).
- [ ] Implementar la suite de pruebas unitarias (JUnit 5 / Mockito) y pruebas de arquitectura con ArchUnit.
