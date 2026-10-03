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
5. **La carpeta coincide con el alcance de commit.** Un `feat(liquidacion)` toca el paquete `liquidacion` del backend y la carpeta `features/liquidacion/` del frontend; un `docs(diseno)` toca `docs/arquitectura/`.
6. **Backend y frontend en el mismo repositorio, cada uno en su carpeta.** Un solo Git Flow, una sola versión, y una funcionalidad completa (endpoint + pantalla) cabe en un solo PR. Ninguno importa archivos del otro: se comunican solo por la API REST.

---

## 2. Vista general

```
IS_Avicontrol-Modulo_03/
├── .github/                    plantillas de PR/issues y CI
├── docs/
│   ├── enunciado/              lo que entregó el docente
│   ├── requisitos/             requisitos de todo el módulo
│   ├── features/               un caso de uso por carpeta
│   ├── arquitectura/           stack, capas y diagramas
│   ├── plantillas/             moldes para specs y planes
│   └── proceso/                forma de trabajo del equipo
├── backend/                    API REST (Java + Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/co/edu/unimagdalena/avicontrol/
│   │   │   │   ├── domain/         reglas de negocio puras
│   │   │   │   ├── service/        casos de uso
│   │   │   │   └── infrastructure/ Spring, REST, JPA, Kafka, clientes M1/M2
│   │   │   └── resources/          configuración y migraciones
│   │   └── test/                   espejo de main
│   ├── .env.example
│   ├── mvnw  mvnw.cmd  .mvn/
│   └── pom.xml
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
| `.github/workflows/ci.yml` | Integración continua: `mvn verify` en `backend/` y `npm run lint`, `npm test`, `npm run build` en `frontend/` |

❌ No van en la raíz: `pom.xml`, `package.json`, `node_modules/`, otros documentos `.md`, diagramas, scripts sueltos, archivos de prueba, exportaciones.

---

## 4. Documentación (`docs/`)

```
docs/
├── enunciado/
│   └── AVICONTROL.md                     enunciado del docente (no se edita)
├── requisitos/                           lo que aplica a TODO el módulo
│   ├── glosario.md
│   ├── reglas-transversales.md
│   └── criterios-aceptacion.md
├── features/                             SOLO carpetas de casos de uso
│   ├── m3-cu01-lista-galpones/
│   │   ├── spec.md                       qué se construye
│   │   └── plan.md                       cómo se construye
│   ├── m3-cu03-generar-liquidacion/
│   │   ├── spec.md
│   │   └── plan.md
│   └── ...
├── arquitectura/
│   ├── general.md                        stack y capas
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
| `requisitos/` | ¿Qué reglas aplican a todo el módulo? (glosario, reglas transversales, criterios globales, supuestos) | `docs(requisitos)` |
| `features/<cu>/spec.md` | ¿Qué debe hacer este caso de uso? | `docs(requisitos)` |
| `features/<cu>/plan.md` | ¿Cómo se implementa este caso de uso? | `docs(plan)` |
| `arquitectura/` | ¿Con qué y cómo se construye todo el sistema? | `docs(diseno)` |
| `plantillas/` | ¿Qué molde copio para un spec o plan nuevo? | `docs` |
| `proceso/` | ¿Cómo trabaja el equipo? | `docs(cambios)` para el registro, `docs` para el resto |

Los nombres de archivo dentro de `requisitos/` son ejemplos: se crean cuando haya contenido para ellos.

Reglas:

- **Un caso de uso = una carpeta** en `features/`, con `spec.md` y `plan.md` y nada más. Si un caso de uso necesita un diagrama propio, va en `features/<cu>/diagramas/`.
- **Nada suelto en `features/`.** Un documento que hable de varios casos de uso no es de una feature: va en `requisitos/` (si es qué) o en `arquitectura/` (si es cómo).
- **El ID del caso de uso no cambia** aunque cambie su nombre; si cambia el nombre, se renombra la carpeta en un commit `refactor` que solo mueve.
- **Los diagramas se suben solo como fuente editable** (`.drawio`, `.puml`), sin exportaciones `.png` o `.svg`: se ven abriéndolos en draw.io o en la extensión del editor, y así no hay una imagen desactualizada respecto a su fuente.
- **`enunciado/` no se edita.** Los cambios de alcance se registran en `proceso/control-cambios.md`.
- **Las entregas al docente se identifican con tags de Git** (`v1.0.0`), no con carpetas de copias. Si una entrega exige un PDF, se adjunta al release de GitHub.

### Qué cambió respecto a la estructura anterior (`M3-BASE`)

| Antes | Problema | Ahora |
|---|---|---|
| `docs/specs/features/modulo3/spec.md` junto a las carpetas de CU | Un documento de todo el módulo mezclado con las features | Su contenido se reparte en `docs/requisitos/` |
| `docs/specs/features/modulo3/...` | Tres niveles que no aportan: el repo ya es del Módulo 3 | `docs/features/` |
| `docs/specs/features/modulo3/plan/General.md` | La arquitectura de todo el sistema dentro de las features | `docs/arquitectura/general.md` |
| `docs/specs/features/modulo3/CAMBIOS.md` | Registro de cambios dentro de los specs | `docs/proceso/control-cambios.md` |
| `docs/specs/templates/` con `sdd-guide.MD` | Plantillas mezcladas con specs; la guía no es una plantilla; extensión en mayúsculas | Plantillas en `docs/plantillas/`, guía en `docs/proceso/guia-sdd.md` |
| `AVICONTROL.md` en la raíz y en `docs/` | Archivo duplicado | Solo en `docs/enunciado/` |
| `docs/diagramas/Modulo3_v1.drawio` + `.png` de 3,5 MB | Versión en el nombre; imagen pesada que duplica la fuente | Solo `docs/arquitectura/diagramas/casos-uso.drawio` |

---

## 5. Backend: código (`backend/src/main/java`)

Paquete raíz: `co.edu.unimagdalena.avicontrol`.

La organización es **primero por capa y dentro de cada capa por área funcional**. Las capas son las de [`general.md`](../arquitectura/general.md) y las áreas coinciden con los alcances de commit de [`CONTRIBUTING.md`](../../CONTRIBUTING.md#alcances).

```
co/edu/unimagdalena/avicontrol/
├── AvicontrolApplication.java
│
├── domain/                               Java puro: sin Spring, JPA, Jackson ni Kafka
│   ├── costos/                           PartidaCosto, TipoCosto
│   ├── venta/                            Venta
│   ├── liquidacion/                      Liquidacion, EstadoLiquidacion, LiquidacionRepository,
│   │                                     LiquidacionYaAnuladaException
│   ├── integracion/                      Galpon, Lote, ResultadoSacrificio, Modulo1Port, Modulo2Port
│   └── shared/                           Redondeo, Dinero
│
├── service/                              casos de uso y límite transaccional
│   ├── costos/
│   ├── venta/
│   ├── liquidacion/                      GenerarLiquidacionService, AnularLiquidacionService
│   ├── reportes/                         DesgloseService, HistorialLiquidacionesService
│   └── integracion/                      SincronizacionService
│
└── infrastructure/
    ├── adapter/
    │   ├── rest/                         entrada HTTP, por área
    │   │   └── liquidacion/              LiquidacionController, *Request, *Response
    │   ├── persistence/                  base de datos, por área
    │   │   └── liquidacion/              LiquidacionEntity, LiquidacionJpaRepository,
    │   │                                 LiquidacionPersistenceAdapter, LiquidacionEntityMapper
    │   ├── client/                       otros módulos, por módulo de origen
    │   │   ├── m1/                       Modulo1Client
    │   │   └── m2/                       Modulo2Client, ResultadoSacrificioConsumer
    │   └── export/                       ExcelExporter (Apache POI)
    ├── config/                           SecurityConfig, OpenApiConfig, KafkaConfig, ClockConfig
    └── exception/                        GlobalExceptionHandler
```

### Qué va en cada capa

| Capa | Puede contener | No puede contener | Puede depender de |
|---|---|---|---|
| `domain` | Entidades, enums, value objects, fórmulas, excepciones de negocio, interfaces de repositorio y de puertos | Anotaciones de Spring, `@Entity`, `@JsonProperty`, DTOs, clases de otras capas | Nada |
| `service` | Servicios de casos de uso, comandos y vistas de solo lectura, `@Transactional` | Controladores, entidades JPA, clientes HTTP o Kafka | `domain` |
| `infrastructure` | Controladores, DTOs, entidades JPA, mappers, clientes, configuración | Reglas de negocio o fórmulas | `service`, `domain` |

La regla de dependencias la verifica ArchUnit en `ArchitectureTest` (ver [sección 7](#7-backend-pruebas-backendsrctest)). Un PR que la rompa no pasa la CI.

### Áreas funcionales

| Área (paquete) | Alcance de commit | Contenido | ¿Tiene paquete en `domain`? |
|---|---|---|---|
| `costos` | `costos` | Alimento, insumos médicos, población inicial | Sí |
| `venta` | `venta` | Venta bruta | Sí |
| `liquidacion` | `liquidacion` | Generar, anular, mortalidad, utilidad neta | Sí |
| `reportes` | `reportes` | Desglose, historial, exportación | **No**: los reportes son vistas de `Liquidacion`, no entidades nuevas |
| `integracion` | `integracion` | Copia local de datos de M1 y M2, sincronización | Sí |
| `shared` | sin alcance | Redondeo y value objects comunes | Solo en `domain` |

En `infrastructure/adapter/rest/` y `persistence/` las carpetas de área se crean con la primera clase de esa área; no todas las áreas tendrán controlador o tabla propia.

---

## 6. Backend: recursos (`backend/src/main/resources`)

```
resources/
├── application.yml                       configuración común
├── application-dev.yml                   H2 o PostgreSQL local
├── application-test.yml
├── application-prod.yml                  solo referencias a variables de entorno
└── db/
    ├── migration/                        esquema (Flyway, todos los perfiles)
    │   ├── V1__crear_tabla_liquidacion.sql
    │   └── V2__agregar_estado_liquidacion.sql
    └── seed/                             datos de demostración (solo perfil dev)
        └── V1000__datos_demo_galpones.sql
```

---

## 7. Backend: pruebas (`backend/src/test`)

`backend/src/test/java` replica **exactamente** los paquetes de `backend/src/main/java`. La prueba de una clase está en el mismo paquete que la clase.

```
backend/src/test/
├── java/co/edu/unimagdalena/avicontrol/
│   ├── ArchitectureTest.java             reglas de capas (ArchUnit)
│   ├── domain/liquidacion/
│   │   └── LiquidacionTest.java          unitaria
│   ├── service/liquidacion/
│   │   └── CU03GenerarLiquidacionTest.java   escenarios de aceptación del spec
│   └── infrastructure/adapter/rest/liquidacion/
│       └── LiquidacionControllerIT.java  integración (Testcontainers)
└── resources/
    ├── application-test.yml
    └── fixtures/
        ├── m1/galpones-activos.json       respuestas simuladas de M1
        └── m2/resultado-sacrificio.json   respuestas y eventos simulados de M2
```

| Tipo de prueba | Sufijo | Se ejecuta con |
|---|---|---|
| Unitaria (dominio o servicio con Mockito) | `Test` | `mvn test` |
| Aceptación de un caso de uso | `CU<nn><Nombre>Test` | `mvn test` |
| Integración (Spring Boot Test + Testcontainers) | `IT` | `mvn verify` |

---

## 8. Frontend (`frontend/`)

React con TypeScript y Vite. Pruebas con Vitest y Testing Library.

```
frontend/
├── public/                               archivos estáticos sin procesar (favicon)
├── src/
│   ├── main.tsx                          punto de entrada
│   ├── app/
│   │   ├── App.tsx
│   │   ├── router.tsx                    todas las rutas en un solo lugar
│   │   └── Layout.tsx                    menú, encabezado, indicador de sincronización
│   ├── features/
│   │   ├── galpones/                     P1 Lista de galpones (CU01)
│   │   │   ├── GalponesPage.tsx
│   │   │   ├── GalponesTable.tsx
│   │   │   ├── galponesApi.ts
│   │   │   ├── useGalpones.ts
│   │   │   ├── types.ts
│   │   │   └── GalponesTable.test.tsx
│   │   ├── liquidacion/                  P2 Generar liquidación, P3 Anular (CU03, CU04)
│   │   │   ├── GenerarLiquidacionPage.tsx
│   │   │   ├── MatrizVentaFinal.tsx
│   │   │   ├── AnularLiquidacionDialog.tsx
│   │   │   ├── liquidacionApi.ts
│   │   │   └── types.ts
│   │   └── reportes/                     P4 Desglose, P5 Historial (CU05, CU06)
│   ├── shared/
│   │   ├── api/
│   │   │   └── httpClient.ts             URL base, token JWT, manejo de errores
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

1. **Una feature por área funcional**, igual que en el backend. `galpones` es la única que no coincide con un alcance de commit: corresponde a CU01, que en el backend vive en `integracion`, pero en pantalla el usuario ve galpones.
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
| Secretos (contraseñas, clave JWT) | Variables de entorno; en local, `backend/.env` | **No** (`.env` está en `.gitignore`) |
| URL de la API para el frontend | `frontend/.env` (`VITE_API_URL`) | **No**; se sube `frontend/.env.example` |
| Lista de variables requeridas | `backend/.env.example`, `frontend/.env.example` | Sí, con valores de ejemplo |
| Esquema de base de datos | `backend/src/main/resources/db/migration/` | Sí |
| Datos de demostración | `backend/src/main/resources/db/seed/` | Sí |
| Datos simulados de M1/M2 para pruebas | `backend/src/test/resources/fixtures/m1/`, `.../m2/` | Sí |
| Imágenes e íconos de la interfaz | `frontend/src/shared/` si se importan desde código; `frontend/public/` si no | Sí |
| Reportes Excel generados | Se generan en memoria y se descargan por la API | **No** |
| Diagramas | `docs/arquitectura/diagramas/` o `docs/features/<cu>/diagramas/` | Sí, solo la fuente (`.drawio`, `.puml`) |
| Compilados (`backend/target/`, `frontend/dist/`), `node_modules/`, IDE (`.idea/`, `.vscode/`), logs | — | **No** |
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
| Capas y paquetes técnicos | Inglés, como en `general.md` | `domain`, `service`, `adapter/rest` |
| Áreas y clases de negocio | Español, igual que el glosario | `liquidacion`, `PartidaCosto` |
| Clase | `PascalCase` | `GenerarLiquidacionService` |
| Método y variable | `camelCase` | `calcularUtilidadNeta`, `precioKgCop` |
| Constante | `MAYUSCULAS_CON_GUION_BAJO` | `ESCALA_PORCENTAJE` |

### Sufijos por rol

| Rol | Sufijo | Capa | Ejemplo |
|---|---|---|---|
| Entidad de dominio | sin sufijo | `domain` | `Liquidacion` |
| Interfaz de repositorio o puerto | `Repository` / `Port` | `domain` | `LiquidacionRepository`, `Modulo2Port` |
| Excepción de negocio | `Exception` | `domain` | `LiquidacionYaAnuladaException` |
| Caso de uso | `Service` | `service` | `AnularLiquidacionService` |
| Controlador REST | `Controller` | `infrastructure` | `LiquidacionController` |
| DTO de entrada / salida | `Request` / `Response` | `infrastructure` | `GenerarLiquidacionRequest` |
| Entidad JPA | `Entity` | `infrastructure` | `LiquidacionEntity` |
| Repositorio Spring Data | `JpaRepository` | `infrastructure` | `LiquidacionJpaRepository` |
| Implementación de un repositorio del dominio | `PersistenceAdapter` | `infrastructure` | `LiquidacionPersistenceAdapter` |
| Conversión entre modelos | `Mapper` | `infrastructure` | `LiquidacionEntityMapper` |
| Cliente de otro módulo | `Client` | `infrastructure` | `Modulo1Client` |
| Evento Kafka | `Event` | `infrastructure` | `LiquidacionGeneradaEvent` |
| Consumidor / productor Kafka | `Consumer` / `Producer` | `infrastructure` | `ResultadoSacrificioConsumer` |
| Configuración | `Config` | `infrastructure` | `SecurityConfig` |

### Frontend (React)

| Elemento | Regla | Ejemplo |
|---|---|---|
| Carpeta de feature | Español, minúsculas, una palabra; igual que el área del backend | `liquidacion`, `reportes` |
| Carpetas técnicas | Inglés | `app`, `shared`, `components`, `hooks` |
| Componente | `PascalCase.tsx`, un componente exportado por archivo | `MatrizVentaFinal.tsx` |
| Página (una ruta) | `PascalCase` + `Page` | `GenerarLiquidacionPage.tsx` |
| Diálogo / modal | `PascalCase` + `Dialog` | `AnularLiquidacionDialog.tsx` |
| Hook | `use` + `PascalCase`, `.ts` | `useGalpones.ts` |
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
| Migración Flyway | `V<n>__<descripcion_en_snake_case>.sql`, numeración consecutiva; una migración aplicada no se edita | `V3__crear_tabla_partida_costo.sql` |
| Datos de demostración | Numeración desde `V1000` para no chocar con el esquema | `V1000__datos_demo_galpones.sql` |
| Tablas y columnas | `snake_case`, en singular | `liquidacion`, `precio_kg_cop` |
| Perfil de configuración | `application-<perfil>.yml` | `application-dev.yml` |
| Fixture de pruebas | `kebab-case.json` dentro de la carpeta del módulo origen | `fixtures/m2/resultado-sacrificio.json` |

---

## 11. Reglas de agrupación

Estas reglas son del backend; las del frontend están en la [sección 8](#reglas-del-frontend).

1. **Capa primero, área después.** Una clase nueva se ubica respondiendo dos preguntas: ¿a qué capa pertenece? y ¿de qué área funcional es?
2. **No se crean paquetes por tipo dentro de un área** (`models/`, `dtos/`, `exceptions/`, `utils/`). Las clases de un área viven juntas y su sufijo indica el rol. Las excepciones de negocio van en el área a la que pertenecen.
3. **Excepción:** `infrastructure/adapter/client/` se divide por módulo de origen (`m1/`, `m2/`), no por área.
4. **Lo que usan varias áreas** va en `domain/shared/`. No se crean paquetes `util`, `common` ni `helpers`.
5. **Las pruebas siguen al código.** Si una clase se mueve de paquete, su prueba se mueve en el mismo commit.
6. **Una clase pública por archivo**, con el mismo nombre del archivo.
7. **Profundidad máxima:** capa → (adaptador) → área. Si hace falta un nivel más, se discute antes.
8. **`.gitkeep` solo mientras la carpeta esté vacía.** El esqueleto inicial los usa para que Git conserve las carpetas; se borran en el mismo commit que agrega el primer archivo real.

---

## 12. Cambiar la estructura

Agregar, renombrar o mover carpetas de primer o segundo nivel, crear un área funcional nueva o cambiar una regla de este documento:

1. Se acuerda con el equipo (igual que un alcance de commit nuevo).
2. Se actualiza este documento en una rama `docs/` con `docs(diseno)`.
3. Los movimientos de archivos se hacen con un commit `refactor` que **solo mueve**, sin cambiar contenido, para que Git conserve el historial.

```
refactor: mueve clases de sincronización al área integracion
```
