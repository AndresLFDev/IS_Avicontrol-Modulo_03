# Guía de contribución — Avicontrol Módulo 03

Este documento define el flujo de trabajo con Git del proyecto: ramas, nombres, commits, pull requests, entregas y control de cambios. Todo integrante del equipo debe seguirlo.

El Módulo 03 corresponde a **Liquidación de Lote y Análisis de Rentabilidad** (ver [`AVICONTROL.md`](docs/enunciado/AVICONTROL.md)). La organización de carpetas y las reglas de nombres de archivos están en [`docs/proceso/estructura-repositorio.md`](docs/proceso/estructura-repositorio.md).

## Índice

1. [Ramas](#1-ramas)
2. [Nombres de rama](#2-nombres-de-rama)
3. [Commits](#3-commits)
4. [Qué rama y qué tipo usar](#4-qué-rama-y-qué-tipo-usar)
5. [Control de cambios](#5-control-de-cambios)
6. [Flujo diario](#6-flujo-diario)
7. [Pull requests](#7-pull-requests)
8. [Entregas y hotfix](#8-entregas-y-hotfix)
9. [Versionado](#9-versionado)
10. [Reglas de oro](#10-reglas-de-oro)

---

## 1. Ramas

```
main ─────●─────────────────────────●──────────────●─────►  versiones entregadas (tags)
           \                       / \            /
release     \                ●──●─┘   \          /
             \              /          \        /
develop ──────●──●──●──●──●──●──────────●──●───●─────────►  integración
                \  /  \  /                \   /
feature/*        ●●    \/                  hotfix/* (sale de main)
docs/*                 ●●
```

| Rama | Sale de | Se une a | Tipo de merge | Propósito |
|---|---|---|---|---|
| `main` | — | — | — | Código entregado y estable. Cada merge lleva un tag `vX.Y.Z` |
| `develop` | `main` | `release/*` | — | Integración del trabajo terminado |
| `feature/*` | `develop` | `develop` | **Squash** | Implementar una funcionalidad |
| `docs/*` | `develop` | `develop` | **Squash** | Cambios **solo** de documentación (plan, requisitos, diseño, control de cambios) |
| `release/*` | `develop` | `main` y `develop` | **Merge commit** | Preparar una entrega |
| `hotfix/*` | `main` | `main` y `develop` | **Merge commit** | Arreglo urgente sobre lo ya entregado |

`main` y `develop` están **protegidas**: no se permite push directo y todo entra por pull request con al menos 1 aprobación y la CI en verde.

---

## 2. Nombres de rama

```
feature/AV3-<id>-<descripcion>     feature/AV3-12-costo-alimento
docs/AV3-<id>-<descripcion>        docs/AV3-27-costos-indirectos
release/v<X.Y.Z>                   release/v1.1.0
hotfix/v<X.Y.Z>-<descripcion>      hotfix/v1.1.1-calculo-utilidad
```

| Regla | ✅ Correcto | ❌ Incorrecto |
|---|---|---|
| Usar el prefijo correspondiente | `feature/AV3-12-...` | `AV3-12-...`, `feat-...`, `Feature/...` |
| Incluir el ID de la tarea o issue | `feature/AV3-12-costo-alimento` | `feature/costo-alimento` |
| Todo en minúsculas | `costo-alimento` | `Costo-Alimento` |
| Separar palabras con guiones | `venta-bruta` | `venta_bruta`, `ventaBruta` |
| Sin tildes, ñ ni caracteres especiales | `calculo-utilidad` | `cálculo-utilidad`, `lotes#2` |
| Descripción de 2 a 5 palabras | `reporte-rentabilidad` | `generar-reporte-de-rentabilidad-comparando-costos-e-ingresos` |
| Describir qué se hace, no quién lo hace | `liquidacion-lote` | `cambios-andres` |
| Una rama por tarea | `feature/AV3-12-...` | `feature/AV3-12-13-14-varios` |
| Nada genérico | `filtro-fecha-reportes` | `fix`, `cambios`, `prueba` |

Si una tarea no tiene ID, primero se crea el issue y luego la rama.

Expresión regular de validación:

```
^(feature|docs)/AV3-[0-9]+-[a-z0-9]+(-[a-z0-9]+){0,4}$|^release/v[0-9]+\.[0-9]+\.[0-9]+$|^hotfix/v[0-9]+\.[0-9]+\.[0-9]+-[a-z0-9-]+$
```

---

## 3. Commits

Se usa la convención [Conventional Commits](https://www.conventionalcommits.org/es/).

### Formato

```
<tipo>(<alcance>): <descripción>

[cuerpo opcional: por qué se hizo el cambio]

[pie opcional: Closes #n · Refs #n · Refs: SC-n · BREAKING CHANGE: ...]
```

### Tipos

| Tipo | Uso | Efecto en la versión |
|---|---|---|
| `feat` | Funcionalidad nueva o cambio de comportamiento | MENOR |
| `fix` | Corrección de un bug | PARCHE |
| `docs` | Solo documentación | — |
| `style` | Formato, sin cambios de lógica | — |
| `refactor` | Reestructura sin cambiar el comportamiento | — |
| `perf` | Mejora de rendimiento | PARCHE |
| `test` | Pruebas | — |
| `build` | Dependencias o empaquetado | — |
| `ci` | Pipelines de integración continua | — |
| `chore` | Mantenimiento general | — |
| `revert` | Revierte un commit anterior | Según lo revertido |

Para indicar un cambio incompatible se agrega `!` después del alcance (`feat(api)!: ...`) y un pie `BREAKING CHANGE: <detalle>`. Esto sube la versión MAYOR.

### Alcances

**Funcionales (Módulo 03):**

| Alcance | Área |
|---|---|
| `costos` | Costos de crianza: alimento, insumos médicos y población inicial (3.1) |
| `venta` | Venta bruta: pollos finales × peso promedio × precio/kg (3.2) |
| `liquidacion` | Cierre del lote: mortalidad, costos operativos y utilidad neta (3.2) |
| `reportes` | Reporte de rentabilidad: costos frente a ingresos, comparación por galpón |
| `integracion` | Lectura de datos de los módulos 1 y 2 |

Un alcance funcional cubre backend y frontend: `feat(liquidacion)` puede tocar el paquete `liquidacion` del backend y `frontend/src/features/liquidacion/` en el mismo commit.

**Técnicos:**

| Alcance | Área |
|---|---|
| `api` | Capa de endpoints (`backend/.../adapter/rest/`) y cliente HTTP del frontend (`frontend/src/shared/api/`) |
| `db` | Esquema, migraciones y seeds (`backend/src/main/resources/db/`) |
| `ui` | Componentes, layout y estilos compartidos del frontend (`frontend/src/app/`, `shared/`, `styles/`) |
| `config` | Configuración del proyecto (Maven, Vite, Spring, compose, CI) |

**Documentación:**

| Alcance | Documento |
|---|---|
| `plan` | Plan técnico de un caso de uso: `docs/features/*/plan.md` |
| `requisitos` | Requisitos del módulo (`docs/requisitos/`) y de cada caso de uso (`docs/features/*/spec.md`) |
| `diseno` | Arquitectura y diagramas: `docs/arquitectura/` |
| `cambios` | [`docs/proceso/control-cambios.md`](docs/proceso/control-cambios.md) |

Los alcances van en minúsculas, son una sola palabra y no llevan tildes. No se agregan alcances nuevos sin acordarlo con el equipo. Si el cambio afecta muchas áreas, se omite el alcance.

### Descripción

| Regla | ✅ Correcto | ❌ Incorrecto |
|---|---|---|
| Verbo en presente, 3.ª persona | `agrega`, `corrige`, `elimina` | `agregué`, `agregar`, `agregando` |
| Empieza en minúscula | `agrega filtro` | `Agrega filtro` |
| Sin punto final | `agrega filtro` | `agrega filtro.` |
| Máximo 72 caracteres en la primera línea | — | — |
| En español y específica | `corrige cálculo de utilidad neta` | `fix`, `cambios`, `ya funciona` |
| Dice qué cambia, no cómo | `valida precio por kg positivo` | `agrega un if en la línea 40` |

Verbos sugeridos:

- `feat`: agrega, implementa, calcula, permite, incorpora
- `fix`: corrige, soluciona, evita, valida
- `refactor`: extrae, separa, simplifica, renombra, mueve
- `docs`: documenta, actualiza, registra, describe, modifica
- `test`: agrega pruebas de, cubre
- `chore` / `build`: actualiza, configura, elimina

### Pies

| Pie | Uso |
|---|---|
| `Closes #12` | Cierra el issue al hacer merge |
| `Refs #12` | Menciona el issue sin cerrarlo |
| `Refs: SC-03` | Referencia una solicitud de cambio (ver [sección 5](#5-control-de-cambios)) |
| `BREAKING CHANGE: <detalle>` | Cambio incompatible |
| `Co-authored-by: Nombre <email>` | Trabajo en pareja |

### Ejemplos

```
feat(costos): calcula costo de alimento por lote
feat(venta): agrega cálculo de venta bruta
fix(liquidacion): corrige porcentaje de mortalidad
feat(reportes): agrega comparación de rentabilidad por galpón
feat(integracion): obtiene consumo de alimento del módulo 2
docs(plan): modifica fórmula de utilidad neta
docs(cambios): registra SC-04 por aviso del módulo 2
test(costos): cubre costo de insumos médicos
chore: configura eslint y prettier
```

```
fix(liquidacion): corrige porcentaje de mortalidad

Se dividía por los pollos finales en lugar de la población inicial,
lo que inflaba el indicador en lotes con muchas bajas.

Closes #18
```

### Buenas prácticas

- Un cambio lógico por commit.
- El proyecto debe compilar en cada commit.
- Commits pequeños y frecuentes.
- Revisar con `git diff --staged` antes de hacer commit.
- Nunca commitear `.env`, credenciales ni `node_modules`.

---

## 4. Qué rama y qué tipo usar

| Situación | Rama | Tipo de commit |
|---|---|---|
| Se agrega una funcionalidad | `feature/` | `feat` |
| Cambia el comportamiento porque cambió el requisito | `feature/` | `feat` |
| Cambio incompatible (API, base de datos, flujo) | `feature/` | `feat!` + `BREAKING CHANGE` |
| El código no cumplía lo ya definido | `feature/` | `fix` |
| Bug urgente en una versión entregada | `hotfix/` | `fix` |
| Se modifica el plan pero aún no se implementa | `docs/` | `docs(plan)` + `docs(cambios)` |
| Otro módulo avisa de un cambio que nos afecta | `docs/` y luego `feature/` | `docs(cambios)`, luego `feat(integracion)` o `fix(integracion)` |
| Reestructura sin cambiar el comportamiento | `feature/` | `refactor` |

Si el requisito **cambió**, es `feat`. Si el requisito **era el mismo** y el código no lo cumplía, es `fix`.

---

## 5. Control de cambios

El control de cambios es **interno del equipo**: registra las decisiones que modifican lo acordado para el Módulo 03 y los avisos de otros módulos que nos afectan. No es un proceso coordinado con los demás módulos.

El registro está en [`docs/proceso/control-cambios.md`](docs/proceso/control-cambios.md). Cada solicitud tiene un ID `SC-<n>`.

### Cuándo se necesita una solicitud de cambio

| Necesita SC | No necesita SC |
|---|---|
| Cambiar una fórmula (costos, venta bruta, mortalidad, utilidad) | Corregir un bug (`fix`) |
| Agregar o quitar una funcionalidad | Refactorizar código |
| Cambiar qué datos se esperan de otros módulos | Mejorar estilos o textos de la interfaz |
| Un aviso de otro módulo que afecta al nuestro | Corregir errores de redacción |
| Cambiar el alcance o una fecha de entrega | |

### Flujo

```
1. docs/AV3-27-...    →  docs(cambios) + docs(plan)   Refs: SC-03 · Refs #27    → PR aprobado por el líder
2. feature/AV3-27-... →  feat(liquidacion)!: ...      Refs: SC-03 · Closes #27  → PR revisado como implementación
```

**Paso 1: registrar y documentar el cambio**

1. Crear el issue y la rama `docs/AV3-<id>-<descripcion>`.
2. Agregar la fila `SC-<n>` en `docs/proceso/control-cambios.md` con estado **Propuesto** y su análisis de impacto.
3. Actualizar el plan o los requisitos afectados.
4. Abrir el PR. Al aprobarlo, el estado de la SC pasa a **Aprobado** (o **Rechazado**, y el PR se cierra sin tocar el plan).

```
docs(plan): incluye costos indirectos en la utilidad neta

Se actualiza el plan para que la utilidad neta descuente costos
indirectos (energía, mano de obra) además de alimento y medicina.
Pendiente de implementación.

Refs: SC-03
Refs #27
```

- Sin `!` ni `BREAKING CHANGE`, porque el documento no rompe el código.
- Con `Refs` en lugar de `Closes`, porque el issue sigue abierto.
- El PR de `docs/` es la **aprobación formal** de la solicitud y lo debe aprobar el líder del equipo.

**Paso 2: implementar el cambio**

```
feat(liquidacion)!: incluye costos indirectos en la utilidad neta

BREAKING CHANGE: POST /liquidaciones requiere el campo costosIndirectos.
Refs: SC-03
Closes #27
```

Después del merge, la SC pasa a **Implementado**, y a **Verificado** cuando se confirme en la siguiente release.

Si hay una `release/*` abierta y el cambio de documentación debe ir en esa entrega, se hace directamente en esa `release/*`.

### Avisos de otros módulos

Cuando otro módulo informe un cambio que nos afecta:

1. Registrar una SC con ese módulo como solicitante, por ejemplo **Módulo 2 (aviso)**.
2. Implementar el ajuste en la capa `integracion` con una rama `feature/`.

---

## 6. Flujo diario

```bash
# 1. Partir de develop actualizado
git switch develop
git pull
git switch -c feature/AV3-12-costo-alimento

# 2. Trabajar con commits pequeños
git add -p
git commit -m "feat(costos): calcula costo de alimento por lote"

# 3. Mantener la rama al día con develop
git fetch origin
git rebase origin/develop

# 4. Subir la rama y abrir un PR hacia develop
git push -u origin feature/AV3-12-costo-alimento
```

Para cambios de documentación el flujo es igual, con una rama `docs/`.

### Corregir commits

| Situación | Comando | Condición |
|---|---|---|
| Corregir el mensaje del último commit | `git commit --amend` | Solo si no se ha subido |
| Agregar un archivo olvidado al último commit | `git add <archivo> && git commit --amend --no-edit` | Solo si no se ha subido |
| Deshacer el último commit conservando los cambios | `git reset --soft HEAD~1` | Solo si no se ha subido |
| Deshacer un commit ya subido | `git revert <hash>` | Siempre seguro |

---

## 7. Pull requests

- **Título:** con el mismo formato de un commit (`<tipo>(<alcance>): <descripción>`), por ejemplo `feat(costos): calcula costo de alimento por lote`. Al hacer squash, GitHub usa el título del PR como mensaje del commit en `develop`. El ID de la tarea ya va en el nombre de la rama.
- **Destino:** `develop` para `feature/` y `docs/`; `main` para `release/` y `hotfix/`.
- **Requisitos:** al menos 1 aprobación de otro integrante y la CI en verde. Los PR de control de cambios los aprueba el líder.
- **Descripción:** qué cambia, cómo probarlo, el issue relacionado (`Closes #12` o `Refs #12`) y la SC si aplica (`Refs: SC-03`).
- **Merge:**
  - `feature/` y `docs/` → **Squash and merge**. GitHub toma el título del PR y le agrega el número: `feat(costos): calcula costo de alimento por lote (#12)`.
  - `release/` y `hotfix/` → **Create a merge commit**.
- **Después del merge:** borrar la rama. Si hace falta más trabajo, crear una rama nueva desde `develop`.

---

## 8. Entregas y hotfix

### Release

Al cierre de un sprint o antes de una entrega:

1. Crear `release/vX.Y.0` desde `develop`.
2. En esa rama solo se permiten correcciones, la actualización de la versión, el CHANGELOG y la documentación. No entran funcionalidades nuevas.
3. Abrir un PR hacia `main` y unir con **merge commit**.
4. Crear el tag `vX.Y.0` sobre `main`.
5. Unir `main` de vuelta a `develop`.
6. Marcar como **Verificado** en `docs/proceso/control-cambios.md` las SC incluidas en la entrega.

### Hotfix

1. Crear `hotfix/vX.Y.Z-<descripcion>` desde `main`.
2. Corregir y abrir un PR hacia `main`; unir con **merge commit**.
3. Crear el tag `vX.Y.Z`.
4. Unir también a `develop` (o a la `release/*` abierta, si existe).

---

## 9. Versionado

Se usa [Versionado Semántico](https://semver.org/lang/es/) `MAYOR.MENOR.PARCHE`.

| Cambio | Versión |
|---|---|
| `BREAKING CHANGE` / `!` | MAYOR (`1.x.x` → `2.0.0`) |
| `feat` | MENOR (`1.1.x` → `1.2.0`) |
| `fix` / `perf` | PARCHE (`1.1.0` → `1.1.1`) |
| `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci` | No cambia la versión |

Durante el desarrollo se usa `v0.x`. La primera entrega formal es `v1.0.0`.

---

## 10. Reglas de oro

1. Nunca hacer push directo a `main` ni a `develop`.
2. Una tarea = una rama = un PR = un commit con squash en `develop`.
3. Squash en ramas cortas (`feature/`, `docs/`); merge commit en `release/` y `hotfix/`.
4. No reescribir (`amend`, `reset`, `rebase`) commits que otros ya descargaron. En ramas compartidas se usa `git revert`.
5. Borrar la rama después del merge.
6. Todo cambio debe poder rastrearse: rama → ID de tarea, commit → `Closes`/`Refs`, cambio de lo acordado → `SC-n`.
