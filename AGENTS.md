# AGENTS.md — Guía para trabajar con IA en AVICONTROL Módulo 3

Este archivo define cómo trabajar en este repositorio. Léelo completo antes de cualquier tarea.

## 1. Contexto: AVICONTROL y los tres módulos

AVICONTROL es un sistema de gestión avícola dividido en tres módulos, desarrollados por **tres equipos distintos en tres repositorios distintos**. El documento raíz del profesor está en `docs/enunciado/AVICONTROL.md`.

| Módulo | Responsabilidad | Repositorio |
| :--- | :--- | :--- |
| M1 — Gestión de Galpones e Infraestructura | Galpones, lotes, población, estados del galpón, costo del lote | https://github.com/andr4dev/AVICONTROL |
| M2 — Nutrición, Sanidad e Inventario Vivo | Dieta por etapa, mortalidad, medicamentos, inventario de bodega, sacrificio | https://github.com/OscarHernandez123/AviControlMod2 |
| **M3 — Liquidación de Lote y Análisis de Rentabilidad** | **Este repositorio.** Liquidación (con su Matriz de Venta Final), anulación, desglose, historial | https://github.com/AndresLFDev/IS_Avicontrol-Modulo_03 |

**Universidad del Magdalena — Ingeniería de Software.** Proyecto académico; el objetivo es que los cálculos sean auditables y que cada línea de código se trace a un spec.

## 2. Jerarquía de fuentes de verdad

Cuando dos documentos se contradicen, manda el de más arriba:

1. `docs/requisitos/especificacion-general.md` — índice **normativo** del módulo: glosario, RT-01..RT-08, modelo de integración, CA-G, SC, decisiones D-01..D-07.
2. `docs/features/m3-cuNN-*/spec.md` — spec autocontenido de cada caso de uso.
3. `docs/arquitectura/general.md` y `docs/planes/00N-*.md` — **planes técnicos y arquitectura** del módulo (Clean Architecture, DDL Flyway, contratos REST/Kafka, tareas T0NN por CU con sus tests). Definen el *cómo*; nunca contradicen a un spec.
4. `docs/proceso/control-cambios.md` — registro de solicitudes de cambio (SC-<n>) y sus motivos.
5. `docs/enunciado/AVICONTROL.md` — documento raíz del profesor. Define el alcance global de los tres módulos, **pero el spec de M3 se aparta de él deliberadamente** en los puntos siguientes. No "corrijas" el código hacia el documento raíz.

| Tema | Documento raíz | Spec M3 (manda) | Motivo |
| :--- | :--- | :--- | :--- |
| Venta Bruta | `Pollos finales × Peso promedio × Precio/kg` | `pesoTotalKg × precioKg` | El peso promedio es un cociente con error de redondeo; el peso total es el hecho físico |
| Mortalidad | "Pérdida por Mortalidad" = `Población inicial − Pollos finales` | "Mortalidad del Lote" = `poblaciónInicial − poblaciónActual` (ambas de M1) | Restar vendidos mezcla muertos con descartes; no es pérdida monetaria |
| Costos Operativos | `Alimento + Medicina + Indirectos` | `Costo de Población + Alimento + Medicina`; indirectos **fuera de alcance** (RT-08) | El documento raíz lista el Costo de Población en 3.1 pero lo omite en 3.2 |
| Nombre del resultado | "reporte de rentabilidad" | **Liquidación** | Término "reporte" eliminado del glosario |
| Estados del galpón | 4 estados | 6 estados con la grafía de M1 | M1 es la fuente oficial (D-07) |

## 3. Stack Tecnológico

**Java 21 (LTS) + Spring Boot 3.3.x + Gradle + PostgreSQL + Flyway + Kafka + Apache POI + JUnit 5 / Mockito**.

- **Backend**: Arquitectura Limpia por Capas (`domain/`, `application/`, `infrastructure/`).
- **Persistencia**: PostgreSQL con migraciones Flyway (`db/migration/V1__...sql`).
- **Mensajería**: Apache Kafka para recepción de eventos de M1 y M2.

## 4. Estructura del Repositorio

```text
CONTRIBUTING.md                            # Guía de Git, ramas y commits
README.md                                  # Contexto académico, integrantes, enlace Figma
AGENTS.md                                  # Este archivo
PENDIENTES.md                              # Lista de pendientes del equipo
docs/
├── enunciado/
│   └── AVICONTROL.md                      # Documento raíz del profesor (3 módulos)
├── proceso/
│   ├── estructura-repositorio.md          # Estructura de carpetas
│   ├── control-cambios.md                 # Registro de cambios (SC-n)
│   └── guia-sdd.md                        # Guía metodológica SDD
├── plantillas/
│   ├── spec.md                            # Plantilla para casos de uso
│   └── plan.md                            # Plantilla para planes técnicos
├── requisitos/
│   ├── especificacion-general.md          # ÍNDICE NORMATIVO: glosario, RT, CA-G, SC, D-01..D-07
│   └── diccionario.md                     # Diccionario de 13 tablas relacionales
├── features/                              # Specs atómicos por Caso de Uso
│   ├── m3-cu01-lista-lotes/spec.md
│   ├── m3-cu03-generar-liquidacion/spec.md
│   ├── m3-cu04-anular-liquidacion/spec.md
│   ├── m3-cu05-desglose-ventas-gastos/spec.md
│   ├── m3-cu06-historial-liquidaciones/spec.md
│   ├── m3-cu07-consultar-galpon-lote-m1/spec.md
│   ├── m3-cu08-consultar-resultado-sacrificio-m2/spec.md
│   ├── m3-cu09-consultar-alimento-requerido-m2/spec.md
│   └── m3-cu10-consultar-consumo-medicamento-m2/spec.md
├── arquitectura/
│   ├── general.md                         # PLAN TÉCNICO GENERAL (arquitectura, DDL, Kafka, REST)
│   └── diagramas/
│       └── casos-uso.drawio               # Diagrama de casos de uso (solo editable)
└── planes/
    ├── 001-sincronizacion-m1-m2.md
    ├── 002-consulta-galpones.md
    ├── 003-gestion-liquidaciones.md
    └── 004-reportes-historial.md
backend/                                   # Código y recursos del backend
frontend/                                  # Recursos y componentes del frontend
```

Cada CU tiene su carpeta y su `spec.md` es autocontenido. **No existe M3-CU02**: se unificó en CU03; el ID queda vacante a propósito.

## 5. Flujo SDD (Specification-Driven Development)

**No se escribe código sin spec y sin plan.** Las tres fases son secuenciales:
1. **SPEC** — leer `docs/features/m3-cuNN-*/spec.md` completo, más `docs/requisitos/especificacion-general.md`.
2. **PLAN** — leer el plan en `docs/planes/` y `docs/arquitectura/general.md`.
3. **Implementación** — Java 21 + Spring Boot 3.3.x + pruebas JUnit 5 que ejecutan los acceptance scenarios.

**Orden de implementación:** `CU07 → CU10` (sincronización y copia local) → `CU01` → `CU03` → `CU04` → `CU05` y `CU06` en paralelo.

## 6. Reglas Normativas

**Glosario:**
- **Liquidación**: **único Documento Financiero** del módulo. Se *genera* en CU03 a partir del resultado final de sacrificio (M2), el precio/kg ingresado y los costos de M1 y M2.
- **Matriz de Venta Final** (o Matriz de Ventas): la **tabla de presentación** de una Liquidación. **No es entidad ni documento**; no se registra aparte ni tiene estado.
- **Desglose**: proyección de solo lectura sobre una Liquidación. **No es entidad**, no se persiste.
- **Documento Financiero**: término reservado para la Liquidación. Ciclo `ACTIVA → ANULADA`.
- **Anulación**: único mecanismo de corrección (CU04). Nunca edita ni borra.
- Siniestro total (población actual 0): se liquida sin resultado de sacrificio ni precio/kg, con Venta Bruta = 0.

## 7. Decisiones Pendientes con Módulos 1 y 2 (D-01..D-07)

- **M1**: Galpón (`UUID`, `Nombre`, `Aforo máximo`, `Estado`). Lote (`UUID`, `Nombre`, `Población inicial`, `Población actual`, `Fecha ingreso`, `Costo total COP`). Estados del galpón: `Disponible`, `Productivo`, `En cosecha`, `Vaciado sanitario`, `Mantenimiento`, `Aislamiento`.
- **M2**: Resultado de sacrificio (`cantidad final`, `peso total kg`, `estado utilización`). Alimento (`Σ kg consumidos × precio neto histórico`). Medicamentos (`Σ cantidad aplicada × precio neto histórico`).
- Cuando una tarea toque una decisión abierta: implementar lo que no depende de ella y aislar el punto de decisión con un supuesto explícito en el plan.

## 8. Convenciones de Código

- **Idioma**: Código (paquetes, clases, métodos, variables, tests) en **inglés**. Documentación, specs, planes, commits y mensajes visibles en **español**.
- **Dinero y números**: Todo valor monetario es `BigDecimal` (COP sin decimales `setScale(0, HALF_UP)`; porcentajes `setScale(2, HALF_UP)`). Cantidades enteras `int`/`long`.
- **Arquitectura Limpia Estricta**: Los DTOs de casos de uso residen en `domain.port.in.dto` para mantener desacoplada la capa de aplicación.

## 9. Git y Commits (CONTRIBUTING.md)

- Ramas: `feature/AV3-<id>-<descripcion>`, `docs/AV3-<id>-<descripcion>`.
- Commits según Conventional Commits: `<tipo>(<alcance>): <descripción>`.
- Unir a `develop` mediante **Squash and merge**.
