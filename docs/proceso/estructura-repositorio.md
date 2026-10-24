# Estructura del repositorio — Avicontrol Módulo 03

Este documento define dónde va cada archivo, cómo se agrupan las carpetas y cómo se nombran. La arquitectura y el stack se describen en [`general.md`](../arquitectura/general.md); aquí solo se aterrizan en carpetas. El flujo de Git está en [`CONTRIBUTING.md`](../../CONTRIBUTING.md).

## Índice

1. [Principios](#1-principios)
2. [Vista general](#2-vista-general)
3. [Raíz del repositorio](#3-raíz-del-repositorio)
4. [Documentación (`docs/`)](#4-documentación-docs)
5. [Backend: código (`backend/src/main/java`)](#5-backend-código-backendsrcmainjava)
6. [Backend: recursos (`backend/src/main/resources`)](#6-backend-recursos-backendsrcmainresources)
7. [Backend: pruebas (`backend/src/test`)](#7-backend-pruebas-backendsrctest)
8. [Frontend (`frontend/`)](#8-frontend-frontend)
9. [Dónde se guarda cada dato](#9-dónde-se-guarda-cada-dato)
10. [Reglas de nombres](#10-reglas-de-nombres)
11. [Reglas de agrupación](#11-reglas-de-agrupación)
12. [Cambiar la estructura](#12-cambiar-la-estructura)

---

## 1. Principios

1. **Cada carpeta responde una sola pregunta.** Si un archivo no responde la pregunta de la carpeta, no va ahí.
2. **Una carpeta contiene archivos del mismo nivel o carpetas del mismo tipo, no ambas mezcladas.** Por ejemplo, `docs/features/` solo contiene carpetas de casos de uso; lo que es de todo el módulo va en otra carpeta.
3. **Cada documento existe en un solo lugar.** No hay copias del mismo archivo en dos rutas.
4. **Sin anidamiento redundante.** El repositorio es solo del Módulo 03, así que no hay carpetas `modulo3/` dentro.
5. **El alcance de commit indica el área funcional.** Un `feat(liquidacion)` toca clases de liquidación del backend (ver [sección 5](#áreas-funcionales-y-alcances-de-commit)) y la carpeta `features/liquidacion/` del frontend; un `docs(diseno)` toca `docs/arquitectura/`.
6. **Backend y frontend en el mismo repositorio, cada uno en su carpeta.** Un solo Git Flow, una sola versión, y una funcionalidad completa (endpoint + pantalla) cabe en un solo PR. Ninguno importa archivos del otro: se comunican solo por la API REST.

---

## 2. Vista general

```
IS_Avicontrol-Modulo_03/
├── .github/                    plantillas de PR/issues y CI
├── docs/
│   ├── enunciado/              lo que entregó el docente
│   ├── requisitos/             requisitos de todo el módulo
│   ├── features/               un caso de uso por carpeta (spec)
│   ├── planes/                 planes de implementación
│   ├── arquitectura/           stack, capas y diagramas
│   ├── plantillas/             moldes para specs y planes
│   └── proceso/                forma de trabajo del equipo
├── backend/                    API REST (Java + Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/co/edu/unimagdalena/avicontrol/
│   │   │   │   ├── domain/         reglas de negocio puras y puertos
│   │   │   │   ├── application/    servicios de casos de uso
│   │   │   │   └── infrastructure/ Spring, REST, JPA, Kafka, POI
│   │   │   └── resources/          configuración y migraciones
│   │   └── test/                   espejo de main
│   ├── .env.example
│   ├── gradlew  gradlew.bat  gradle/
│   ├── settings.gradle
│   └── build.gradle
├── frontend/                   interfaz (React + TypeScript + Vite)
│   ├── public/
│   ├── src/
│   │   ├── app/                arranque, rutas, layout
│   │   ├── features/           una carpeta por área de pantallas
│   │   ├── shared/             lo que usan varias features
│   │   └── styles/             estilos globales
│   ├── .env.example
│   ├── index.html
│   ├── package.json
│   └── vite.config.ts
├── .editorconfig
├── .gitattributes
├── .gitignore
├── compose.yaml
├── CONTRIBUTING.md
└── README.md
```

---

## 3. Raíz del repositorio

En la raíz solo van archivos que aplican a **todo el repositorio** o que GitHub espera encontrar ahí. Lo que es solo del backend va en `backend/` y lo que es solo del frontend va en `frontend/`.

| Archivo / carpeta | Contenido |
|---|---|
| `README.md` | Presentación: integrantes, alcance, cómo ejecutar el proyecto |
| `CONTRIBUTING.md` | Git Flow y forma de trabajo (GitHub lo enlaza desde los PR) |
| `compose.yaml` | Levanta todo en local: PostgreSQL, Kafka, backend y frontend |
| `.gitignore`, `.gitattributes`, `.editorconfig` | Configuración de Git y del editor para ambos proyectos |
| `.github/pull_request_template.md` | Plantilla de PR |
| `.github/ISSUE_TEMPLATE/` | Plantillas de issues |
| `.github/workflows/ci.yml` | Integración continua: `./gradlew check integrationTest` en `backend/` y `npm run lint`, `npm test`, `npm run build` en `frontend/` |

❌ No van en la raíz: `build.gradle`, `package.json`, `node_modules/`, otros documentos `.md`, diagramas, scripts sueltos, archivos de prueba, exportaciones.

---

## 4. Documentación (`docs/`)

```
docs/
├── enunciado/
│   └── AVICONTROL.md                     enunciado del docente (no se edita)
├── requisitos/                           lo que aplica a TODO el módulo
│   ├── especificacion-general.md         épica: glosario, reglas transversales, mapa de CU
│   └── diccionario.md                    diccionario de dominio y de datos
├── features/                             SOLO carpetas de casos de uso
│   ├── m3-cu01-lista-lotes/
│   │   └── spec.md                       qué se construye
│   ├── m3-cu03-generar-liquidacion/
│   │   └── spec.md
│   └── ...
├── planes/                               cómo se construye (uno o más CU por plan)
│   ├── 001-sincronizacion-m1-m2.md       CU07–CU10
│   ├── 002-consulta-lotes.md             CU01
│   ├── 003-gestion-liquidaciones.md      CU03–CU04
│   └── 004-reportes-historial.md         CU05–CU06
├── arquitectura/
│   ├── general.md                        stack, capas, modelo de datos
│   └── diagramas/
│       └── casos-uso.drawio              fuente editable
├── plantillas/
│   ├── spec.md
│   └── plan.md
└── proceso/
    ├── control-cambios.md                registro SC-<n>
    ├── estructura-repositorio.md         este documento
    └── guia-sdd.md                       cómo usar spec y plan (SDD)
```

| Carpeta | Pregunta que responde | Alcance de commit |
|---|---|---|
| `enunciado/` | ¿Qué pidió el docente? | `docs` |
| `requisitos/` | ¿Qué reglas aplican a todo el módulo? (glosario, reglas transversales, criterios globales, diccionario de datos) | `docs(requisitos)` |
| `features/<cu>/spec.md` | ¿Qué debe hacer este caso de uso? | `docs(requisitos)` |
| `planes/` | ¿Cómo se implementa este grupo de casos de uso? | `docs(plan)` |
| `arquitectura/` | ¿Con qué y cómo se construye todo el sistema? | `docs(diseno)` |
| `plantillas/` | ¿Qué molde copio para un spec o plan nuevo? | `docs` |
| `proceso/` | ¿Cómo trabaja el equipo? | `docs(cambios)` para el registro, `docs` para el resto |

Reglas:

- **Un caso de uso = una carpeta** en `features/`, con su `spec.md` y nada más. Si un caso de uso necesita un diagrama propio, va en `features/<cu>/diagramas/`.
- **Un plan cubre uno o más specs.** Los casos de uso que se implementan juntos (mismo modelo, mismos endpoints) comparten plan. Cada plan lista al inicio, en `**Specs**:`, los specs que cubre con enlace, y todo spec debe estar cubierto por exactamente un plan.
- **Nombre de un plan:** `<nnn>-<descripcion>.md`, con número de tres dígitos consecutivo que no se reutiliza (`005-...`).
- **Nada suelto en `features/`.** Un documento que hable de varios casos de uso no es de una feature: va en `requisitos/` (si es qué) o en `arquitectura/` (si es cómo).
- **El ID del caso de uso no cambia** aunque cambie su nombre; si cambia el nombre, se renombra la carpeta en un commit `refactor` que solo mueve.
- **Los diagramas se suben solo como fuente editable** (`.drawio`, `.puml`), sin exportaciones `.png` o `.svg`: se ven abriéndolos en draw.io o en la extensión del editor, y así no hay una imagen desactualizada respecto a su fuente.
- **`enunciado/` no se edita.** Los cambios de alcance se registran en `proceso/control-cambios.md`.
- **Las entregas al docente se identifican con tags de Git** (`v1.0.0`), no con carpetas de copias. Si una entrega exige un PDF, se adjunta al release de GitHub.

### Qué cambió respecto a la estructura anterior (`M3-BASE`)

| Antes | Problema | Ahora |
|---|---|---|
| `docs/specs/features/modulo3/spec.md` junto a las carpetas de CU | Un documento de todo el módulo mezclado con las features | `docs/requisitos/especificacion-general.md` |
| `docs/specs/features/modulo3/...` | Tres niveles que no aportan: el repo ya es del Módulo 3 | `docs/features/` |
| `docs/specs/features/modulo3/plan/General.md` | La arquitectura de todo el sistema dentro de las features | `docs/arquitectura/general.md` |
| `docs/specs/features/modulo3/plan/00N-*.md` | Planes de implementación dentro de las features | `docs/planes/` |
| `docs/specs/features/modulo3/CAMBIOS.md` | Registro de cambios dentro de los specs | `docs/proceso/control-cambios.md` |
| `docs/specs/templates/` con `sdd-guide.MD` | Plantillas mezcladas con specs; la guía no es una plantilla; extensión en mayúsculas | Plantillas en `docs/plantillas/`, guía en `docs/proceso/guia-sdd.md` |
| `AVICONTROL.md` en la raíz y en `docs/` | Archivo duplicado | Solo en `docs/enunciado/` |
| `docs/diagramas/Modulo3_v1.drawio` + `.png` de 3,5 MB | Versión en el nombre; imagen pesada que duplica la fuente | Solo `docs/arquitectura/diagramas/casos-uso.drawio` |

---

## 5. Backend: código (`backend/src/main/java`)

Paquete raíz: `co.edu.unimagdalena.avicontrol`.

La organización es **por capa y, dentro de cada capa, por tipo de pieza** (modelo, puerto, servicio, adaptador), exactamente como la define [`general.md`](../arquitectura/general.md#arquitectura-limpia-por-capas), que es la referencia oficial del árbol completo. No hay subpaquetes por área funcional: el área de una clase se reconoce por su nombre (`GenerarLiquidacionService`, `SyncModulo2Service`).

```
co/edu/unimagdalena/avicontrol/
├── AvicontrolApplication.java
│
├── domain/                               Java puro: sin Spring, JPA, Jackson ni Kafka
│   ├── model/                            Liquidacion, Lote, Galpon, EtapaLote, CategoriaCosto…
│   ├── exception/                        LoteNoLiquidableException, LiquidacionYaAnuladaException…
│   ├── port/
│   │   ├── in/                           *UseCase (casos de uso)
│   │   │   └── dto/                      records de entrada/salida de los casos de uso
│   │   │       └── evento/               datos de eventos entrantes ya validados
│   │   └── out/                          *Repository, *Publisher, *Port
│   └── shared/                           Rounding, Pagina
│
├── application/
│   └── service/                          *Service que implementan los *UseCase, @Transactional
│       └── sync/                         procesamiento de eventos de M1/M2 y aviso a M2
│
└── infrastructure/
    ├── adapter/
    │   ├── rest/                         *Controller, *Request, *Response
    │   ├── messaging/                    *Consumer y *Producer de Kafka, sobres JSON
    │   ├── persistence/                  *Entity, *JpaRepository, *PersistenceAdapter, *EntityMapper
    │   └── export/                       ExcelWorkbookBuilder (Apache POI)
    ├── config/                           KafkaConfig, OpenApiConfig, ClockConfig, SchedulingConfig
    └── exception/                        GlobalExceptionHandler
```

### Qué va en cada capa

| Capa | Puede contener | No puede contener | Puede depender de |
|---|---|---|---|
| `domain` | Entidades, enums, value objects, fórmulas, excepciones de negocio, interfaces de casos de uso (`port/in`) con sus DTOs (`port/in/dto`) e interfaces de repositorios y publicadores (`port/out`) | Anotaciones de Spring, `@Entity`, `@JsonProperty`, tipos de Spring Data (`Page`), `*Request`/`*Response` HTTP, clases de otras capas | Nada |
| `application` | Servicios que implementan los casos de uso, `@Service`, `@Transactional`, `@Scheduled` | Controladores, entidades JPA, clientes Kafka, Apache POI | `domain` |
| `infrastructure` | Controladores, `Request`/`Response`, entidades JPA, mappers, consumidores y productores Kafka, POI, configuración | Reglas de negocio o fórmulas | `application`, `domain` |

La regla de dependencias la verifica ArchUnit en `ArchitectureTest` (ver [sección 7](#7-backend-pruebas-backendsrctest)). Un PR que la rompa no pasa la CI.

### Áreas funcionales y alcances de commit

Las áreas no son paquetes, pero sí deciden el alcance del commit (ver [`CONTRIBUTING.md`](../../CONTRIBUTING.md#alcances)):

| Alcance | Clases típicas |
|---|---|
| `costos` | `PartidaCostoLote`, `CategoriaCosto`, cálculo de costos operativos |
| `venta` | Venta bruta dentro de `CalculoLiquidacion` |
| `liquidacion` | `Liquidacion`, `GenerarLiquidacionService`, `AnularLiquidacionService`, `ListarLotesService`, `LiquidacionController`, `LoteController` |
| `reportes` | `ConsultarHistorialService`, `ConsultarDesgloseService`, `ExportarExcelService`, `ExcelWorkbookBuilder` |
| `integracion` | `application/service/sync/`, `adapter/messaging/`, `SincronizacionController` (estado RT-09), copia local de M1 y M2 |

---

## 6. Backend: recursos (`backend/src/main/resources`)

```
resources/
├── application.yml                       configuración común (tópicos Kafka, umbrales)
├── application-dev.yml                   PostgreSQL y Kafka de compose.yaml; activa db/seed
├── application-test.yml
├── application-prod.yml                  solo referencias a variables de entorno
└── db/
    ├── migration/                        esquema (Flyway, todos los perfiles)
    │   ├── V1__create_sync_tables.sql
    │   ├── V2__create_partidas_tables.sql
    │   └── V3__create_liquidacion_tables.sql
    └── seed/                             datos de demostración (solo perfil dev)
        └── V1000__datos_demo_galpones.sql
```

El contenido de cada migración está en [`general.md`](../arquitectura/general.md#b-scripts-sql-ddl-completos-flyway-migrations). Todos los perfiles usan PostgreSQL; no se usa H2.

---

## 7. Backend: pruebas (`backend/src/test`)

`backend/src/test/java` replica **exactamente** los paquetes de `backend/src/main/java`. La prueba de una clase está en el mismo paquete que la clase.

```
backend/src/test/
├── java/co/edu/unimagdalena/avicontrol/
│   ├── ArchitectureTest.java             reglas de capas (ArchUnit)
│   ├── domain/model/
│   │   └── CalculoLiquidacionTest.java   unitaria
│   ├── application/service/
│   │   └── CU03GenerarLiquidacionTest.java   escenarios de aceptación del spec
│   └── infrastructure/adapter/rest/
│       └── LiquidacionControllerIT.java  integración (Testcontainers)
└── resources/
    ├── application-test.yml
    └── fixtures/
        ├── m1/galpon-actualizado.json     eventos simulados de M1
        └── m2/resultado-sacrificio.json   eventos simulados de M2
```

| Tipo de prueba | Sufijo | Se ejecuta con |
|---|---|---|
| Unitaria (dominio o servicio con Mockito) | `Test` | `./gradlew test` |
| Aceptación de un caso de uso | `CU<nn><Nombre>Test` | `./gradlew test` |
| Integración (Spring Boot Test + Testcontainers con PostgreSQL y Kafka) | `IT` | `./gradlew integrationTest` |

`./gradlew check` ejecuta todo lo anterior más Spotless y ArchUnit.

---

## 8. Frontend (`frontend/`)

React con TypeScript y Vite. Estilos con Tailwind CSS: clases utilitarias en el propio componente y colores del tema en `styles/global.css` (`@theme`); un `.module.css` solo cuando Tailwind no alcance. Pruebas con Vitest y Testing Library; lint con oxlint y formato con Prettier.

```
frontend/
├── public/                               archivos estáticos sin procesar (favicon)
├── src/
│   ├── main.tsx                          punto de entrada
│   ├── app/
│   │   ├── App.tsx
│   │   ├── router.tsx                    todas las rutas en un solo lugar
│   │   └── Layout.tsx                    menú, encabezado, indicador de sincronización (RT-09)
│   ├── features/
│   │   ├── lotes/                        P1 Lista de lotes (CU01)
│   │   │   ├── ListaLotesPage.tsx
│   │   │   ├── LotesTable.tsx
│   │   │   ├── lotesApi.ts
│   │   │   ├── useLotes.ts
│   │   │   ├── types.ts
│   │   │   └── LotesTable.test.tsx
│   │   ├── liquidacion/                  P2 Generar liquidación, P3 Anular (CU03, CU04)
│   │   │   ├── GenerarLiquidacionPage.tsx
│   │   │   ├── MatrizVentaFinal.tsx
│   │   │   ├── VerLiquidacionPage.tsx
│   │   │   ├── AnularLiquidacionDialog.tsx
│   │   │   ├── liquidacionApi.ts
│   │   │   └── types.ts
│   │   └── reportes/                     P4 Desglose, P5 Historial (CU05, CU06)
│   ├── shared/
│   │   ├── api/
│   │   │   └── httpClient.ts             URL base y manejo de errores (ProblemDetail)
│   │   ├── components/                   Button, DataTable, ConfirmDialog
│   │   ├── hooks/
│   │   └── format/                       formatCop.ts, formatFecha.ts
│   └── styles/
│       └── global.css
├── .env.example                          VITE_API_URL=http://localhost:8080
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### Qué va en cada carpeta

| Carpeta | Puede contener | No puede contener |
|---|---|---|
| `app/` | Arranque, rutas, layout general, proveedores de contexto | Pantallas de una feature |
| `features/<area>/` | Páginas, componentes, llamadas a la API, hooks y tipos **de esa área** | Imports de otra feature |
| `shared/` | Lo que usan dos o más features | Lógica de una sola feature |
| `styles/` | Estilos globales y variables de tema | Estilos de un componente (van junto al componente) |

### Reglas del frontend

1. **Una feature por área funcional**, igual que los alcances de commit. `lotes` es la única que no coincide con un alcance: corresponde a CU01, cuyo commit usa `liquidacion` porque la lista de lotes es la entrada al flujo de liquidación.
2. **Las features no se importan entre sí.** Si dos features necesitan lo mismo, se mueve a `shared/`.
3. **Las llamadas HTTP solo van en archivos `<area>Api.ts`**, que usan `shared/api/httpClient.ts`. Los componentes nunca llaman a `fetch` directamente.
4. **Sin cálculos de negocio en el frontend.** Venta bruta, mortalidad, costos y utilidad los calcula el backend; el frontend solo los muestra y les da formato.
5. **Dentro de una feature los archivos van planos**, sin subcarpetas `components/` o `hooks/`; el sufijo indica el rol (igual que en el backend). Si una feature pasa de unos 12 archivos, se discute dividirla.
6. **La prueba va junto al archivo que prueba:** `MatrizVentaFinal.test.tsx` al lado de `MatrizVentaFinal.tsx`.
7. **Los tipos de la API** (`types.ts`) reflejan los `Request`/`Response` del backend con los mismos nombres de campo.

---

## 9. Dónde se guarda cada dato

| Dato | Ruta | ¿Se sube a Git? |
|---|---|---|
| Configuración común del backend | `backend/src/main/resources/application.yml` | Sí |
| Configuración por entorno del backend | `backend/src/main/resources/application-<perfil>.yml` | Sí, sin secretos |
| Secretos (contraseña de la base de datos, credenciales del broker) | Variables de entorno; en local, `backend/.env` | **No** (`.env` está en `.gitignore`) |
| URL de la API para el frontend | `frontend/.env` (`VITE_API_URL`) | **No**; se sube `frontend/.env.example` |
| Lista de variables requeridas | `backend/.env.example`, `frontend/.env.example` | Sí, con valores de ejemplo |
| Esquema de base de datos | `backend/src/main/resources/db/migration/` | Sí |
| Datos de demostración | `backend/src/main/resources/db/seed/` | Sí |
| Datos simulados de M1/M2 para pruebas | `backend/src/test/resources/fixtures/m1/`, `.../m2/` | Sí |
| Imágenes e íconos de la interfaz | `frontend/src/shared/` si se importan desde código; `frontend/public/` si no | Sí |
| Reportes Excel generados | Se generan en memoria y se descargan por la API | **No** |
| Diagramas | `docs/arquitectura/diagramas/` o `docs/features/<cu>/diagramas/` | Sí, solo la fuente (`.drawio`, `.puml`) |
| Compilados (`backend/build/`, `backend/.gradle/`, `frontend/dist/`), `node_modules/`, IDE (`.idea/`, `.vscode/`), logs | — | **No** |
| Dependencias exactas del frontend | `frontend/package-lock.json` | **Sí** |

---

## 10. Reglas de nombres

### Carpetas y archivos de documentación

| Regla | ✅ Correcto | ❌ Incorrecto |
|---|---|---|
| Minúsculas y guiones (`kebab-case`) | `control-cambios.md` | `Control_Cambios.md`, `controlCambios.md` |
| Sin tildes, ñ, espacios ni caracteres especiales | `diseno-base-datos.drawio` | `diseño base datos.drawio` |
| Extensión en minúsculas | `guia.md` | `guia.MD` |
| Carpeta de caso de uso: `m3-cu<nn>-<descripcion>` con dos dígitos | `m3-cu05-desglose-ventas-gastos` | `CU5`, `cu05_desglose` |
| Sin versión ni fecha en el nombre (Git guarda el historial) | `casos-uso.drawio` | `Modulo3_v1.drawio`, `diagrama-final-v2.drawio` |
| Nombres de carpeta en español | `arquitectura`, `plantillas` | `architecture`, `templates` |
| Excepción: archivos que GitHub o el docente nombran así | `README.md`, `CONTRIBUTING.md`, `AVICONTROL.md` | — |

Estas reglas aplican a `docs/`. El código de backend y frontend sigue las convenciones de su lenguaje (abajo).

### Backend (Java)

| Elemento | Regla | Ejemplo |
|---|---|---|
| Paquete | Minúsculas, una palabra, sin tildes | `liquidacion`, `persistence` |
| Capas y paquetes técnicos | Inglés, como en `general.md` | `domain`, `application`, `adapter/rest` |
| Clases de negocio | Español, igual que el glosario | `Liquidacion`, `PartidaCostoLote` |
| Clase | `PascalCase` | `GenerarLiquidacionService` |
| Método y variable | `camelCase` | `calcularUtilidadNeta`, `precioKgCop` |
| Constante | `MAYUSCULAS_CON_GUION_BAJO` | `ESCALA_PORCENTAJE` |

### Sufijos por rol

| Rol | Sufijo | Capa | Ejemplo |
|---|---|---|---|
| Entidad de dominio | sin sufijo | `domain` | `Liquidacion` |
| Puerto de entrada (caso de uso) | `UseCase` | `domain/port/in` | `GenerarLiquidacionUseCase` |
| DTO de caso de uso | `Dto` / `Command` | `domain/port/in/dto` | `LiquidacionDto`, `GenerarLiquidacionCommand` |
| Puerto de salida | `Repository` / `Publisher` / `Port` | `domain/port/out` | `LiquidacionRepository`, `AvisoUtilizacionPublisher`, `GeneradorExcelPort` |
| Excepción de negocio | `Exception` | `domain` | `LiquidacionYaAnuladaException` |
| Implementación de un caso de uso | `Service` | `application` | `AnularLiquidacionService` |
| Controlador REST | `Controller` | `infrastructure` | `LiquidacionController` |
| DTO de entrada / salida | `Request` / `Response` | `infrastructure` | `GenerarLiquidacionRequest` |
| Entidad JPA | `Entity` | `infrastructure` | `LiquidacionEntity` |
| Repositorio Spring Data | `JpaRepository` | `infrastructure` | `LiquidacionJpaRepository` |
| Implementación de un repositorio del dominio | `PersistenceAdapter` | `infrastructure` | `LiquidacionPersistenceAdapter` |
| Conversión entre modelos | `Mapper` | `infrastructure` | `LiquidacionEntityMapper` |
| Evento Kafka (contrato JSON) | `Event` | `infrastructure` | `ResultadoSacrificioPublicadoEvent` |
| Consumidor / productor Kafka | `Consumer` / `Producer` | `infrastructure` | `Modulo2EventConsumer`, `AvisoUtilizacionKafkaProducer` |
| Configuración | `Config` | `infrastructure` | `KafkaConfig` |

### Frontend (React)

| Elemento | Regla | Ejemplo |
|---|---|---|
| Carpeta de feature | Español, minúsculas, una palabra; igual que el área del backend | `liquidacion`, `reportes` |
| Carpetas técnicas | Inglés | `app`, `shared`, `components`, `hooks` |
| Componente | `PascalCase.tsx`, un componente exportado por archivo | `MatrizVentaFinal.tsx` |
| Página (una ruta) | `PascalCase` + `Page` | `GenerarLiquidacionPage.tsx` |
| Diálogo / modal | `PascalCase` + `Dialog` | `AnularLiquidacionDialog.tsx` |
| Hook | `use` + `PascalCase`, `.ts` | `useLotes.ts` |
| Llamadas a la API | `<area>Api.ts` | `liquidacionApi.ts` |
| Otros módulos TypeScript | `camelCase.ts` | `formatCop.ts`, `httpClient.ts` |
| Tipos de una feature | `types.ts` | `features/liquidacion/types.ts` |
| Estilos de un componente | Mismo nombre + `.module.css` | `MatrizVentaFinal.module.css` |
| Prueba | Mismo nombre + `.test.tsx` / `.test.ts` | `formatCop.test.ts` |
| Ruta URL | `kebab-case`, en español | `/liquidaciones/:id/desglose` |
| Variable de entorno | `VITE_` + `MAYUSCULAS` | `VITE_API_URL` |

### Migraciones y recursos (backend)

| Elemento | Regla | Ejemplo |
|---|---|---|
| Migración Flyway | `V<n>__<verbo>_<objeto>.sql` en inglés y `snake_case`, igual que las de `general.md`; numeración consecutiva; una migración aplicada no se edita | `V4__add_index_liquidacion_estado.sql` |
| Datos de demostración | Numeración desde `V1000` para no chocar con el esquema | `V1000__datos_demo_galpones.sql` |
| Tablas y columnas | `snake_case`, en singular | `liquidacion`, `precio_kg_cop` |
| Perfil de configuración | `application-<perfil>.yml` | `application-dev.yml` |
| Fixture de pruebas (evento Kafka) | `kebab-case.json` dentro de la carpeta del módulo origen | `fixtures/m2/resultado-sacrificio.json` |

---

## 11. Reglas de agrupación

Estas reglas son del backend; las del frontend están en la [sección 8](#reglas-del-frontend).

1. **Capa primero, tipo de pieza después.** Una clase nueva se ubica respondiendo: ¿a qué capa pertenece? y ¿qué pieza es (modelo, puerto de entrada o salida, servicio, adaptador)? El árbol oficial está en `general.md`.
2. **No se crean paquetes por área funcional** (`liquidacion/`, `costos/`) ni paquetes de tipo fuera de los definidos (`models/`, `dtos/`, `utils/`). El sufijo y el nombre de la clase indican rol y área.
3. **Excepción:** `application/service/sync/` agrupa el procesamiento de eventos de M1 y M2, y `domain/port/in/dto/evento/` sus datos de entrada.
4. **Lo que usan varias áreas** va en `domain/shared/`. No se crean paquetes `util`, `common` ni `helpers`.
5. **Las pruebas siguen al código.** Si una clase se mueve de paquete, su prueba se mueve en el mismo commit.
6. **Una clase pública por archivo**, con el mismo nombre del archivo.
7. **Profundidad máxima:** la de `general.md` (p. ej. `domain/port/in/dto/evento`). Si hace falta un nivel más, se discute antes.
8. **`.gitkeep` solo mientras la carpeta esté vacía.** El esqueleto inicial los usa para que Git conserve las carpetas; se borran en el mismo commit que agrega el primer archivo real.

---

## 12. Cambiar la estructura

Agregar, renombrar o mover carpetas de primer o segundo nivel, crear un área funcional nueva o cambiar una regla de este documento:

1. Se acuerda con el equipo (igual que un alcance de commit nuevo).
2. Se actualiza este documento en una rama `docs/` con `docs(diseno)`.
3. Los movimientos de archivos se hacen con un commit `refactor` que **solo mueve**, sin cambiar contenido, para que Git conserve el historial.

```
refactor: mueve los servicios de sincronización a application/service/sync
```
