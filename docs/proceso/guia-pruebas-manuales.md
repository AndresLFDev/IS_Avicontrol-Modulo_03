# Guía de pruebas manuales — Avicontrol Módulo 03

Esta guía explica cómo preparar el entorno para ejecutar los casos de prueba de [`docs/pruebas/casos/`](../pruebas/casos/) y cómo leer sus resultados. Cada caso de prueba sigue la plantilla [`caso-de-prueba.md`](../plantillas/caso-de-prueba.md); aquí está lo que es común a todos.

## 1. Alcance

Hay un caso de prueba por cada caso de uso implementado:

| Caso de prueba | Caso de uso | Plan |
|---|---|---|
| [`m3-cu01-lista-lotes.md`](../pruebas/casos/m3-cu01-lista-lotes.md) | M3-CU01 Consultar lista de lotes | [002](../planes/002-consulta-lotes.md) |
| [`m3-cu03-generar-liquidacion.md`](../pruebas/casos/m3-cu03-generar-liquidacion.md) | M3-CU03 Generar liquidación | [003](../planes/003-gestion-liquidaciones.md) |
| [`m3-cu04-anular-liquidacion.md`](../pruebas/casos/m3-cu04-anular-liquidacion.md) | M3-CU04 Anular liquidación | [003](../planes/003-gestion-liquidaciones.md) |
| [`m3-cu05-desglose-ventas-gastos.md`](../pruebas/casos/m3-cu05-desglose-ventas-gastos.md) | M3-CU05 Desglose de ventas y gastos | [004](../planes/004-reportes-historial.md) |
| [`m3-cu06-historial-liquidaciones.md`](../pruebas/casos/m3-cu06-historial-liquidaciones.md) | M3-CU06 Historial de liquidaciones | [004](../planes/004-reportes-historial.md) |

**Lo que no se puede probar todavía.** Los casos de uso CU07 a CU10 (consumo de eventos de Módulo 1 y Módulo 2 por Kafka) y la publicación de avisos de utilización a Módulo 2 no están implementados: el backend no tiene consumidores ni publicadores de Kafka. La app trabaja solo con los datos del seed del perfil `dev`, y la tabla `registro_sincronizacion` no recibe filas nuevas al usar la app.

## 2. Preparación del entorno

1. Abre Docker Desktop y espera a que el motor esté corriendo.
2. En la raíz del repositorio levanta la base: `docker compose up -d postgres`. El `compose.yaml` publica PostgreSQL en el puerto `DB_PORT` (5432 por defecto). Si el 5432 ya está ocupado en tu equipo, crea un `.env` en la raíz con `DB_PORT=5433`.
3. Arranca el backend desde `backend/` con el perfil `dev`, que aplica las migraciones y carga el seed (`db/seed/V1000`). Usa el mismo puerto de la base:

   ```
   DB_PORT=5433 ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

   En PowerShell: `$env:DB_PORT='5433'; ./gradlew bootRun --args='--spring.profiles.active=dev'`.
4. Arranca el frontend desde `frontend/` con `npm run dev:backend`. Con `npm run dev` los datos son simulados en el navegador y no se escribe nada en la base.
5. Abre <http://localhost:5173>. La franja "Datos simulados" no debe aparecer.
6. Conecta un cliente SQL (por ejemplo la ventana Database de IntelliJ) a `localhost`, puerto `DB_PORT`, base `avicontrol`, usuario `avicontrol`, contraseña `avicontrol` (valores de desarrollo). Abre [`consultas-demo.sql`](../pruebas/datos/consultas-demo.sql) y ejecuta las secciones 1 y 5: son la referencia de lo que debe verse al inicio.
7. La API también se puede probar en Swagger: <http://localhost:8080/swagger-ui.html>.

No hay inicio de sesión: la aplicación no pide autenticación. El usuario responsable se escribe en los formularios (mínimo 3 caracteres).

## 3. Datos de prueba

Los casos usan los datos del seed. Para repetir las pruebas desde cero se ejecuta [`reset-demo.sql`](../pruebas/datos/reset-demo.sql), que borra lo generado por la app y no toca el seed. Está en un archivo aparte de las consultas porque borra datos: no se ejecuta completo por accidente.

| Lote | Id | Situación en el seed |
|---|---|---|
| L-2026-01 | `b0000000-0000-0000-0000-000000000001` | Productivo, sin alerta de vaciado |
| L-2026-02 | `b0000000-0000-0000-0000-000000000002` | En cosecha |
| L-2026-03 | `b0000000-0000-0000-0000-000000000003` | Caso Dorado: 9.000 a 8.500 pollos, 23.800 kg |
| L-2026-04 | `b0000000-0000-0000-0000-000000000004` | Siniestro total: 8.000 a 0 pollos |
| L-2026-05 | `b0000000-0000-0000-0000-000000000005` | Aislamiento |
| L-2026-06 | `b0000000-0000-0000-0000-000000000006` | Una partida de alimento sin precio |

Los valores esperados de cada caso salen de estos datos. Si el seed cambia, hay que revisar los casos y las consultas.

## 4. Cómo leer los errores

Los errores del backend llegan como `application/problem+json` (RFC 7807) con un campo `type` que termina en el código del error:

| Código (`type`) | HTTP | Cuándo |
|---|---|---|
| `validacion` | 400 | Un campo del formulario no cumple las reglas; trae `errores` por campo |
| `rango-fechas-invalido` | 400 | La fecha inicial del historial es posterior a la final |
| `lote-no-encontrado` | 404 | El lote no existe en la copia local |
| `liquidacion-no-encontrada` | 404 | La liquidación no existe |
| `liquidacion-ya-existente` | 409 | El lote ya tiene una liquidación ACTIVA |
| `liquidacion-ya-anulada` | 409 | La liquidación ya fue anulada |
| `exportacion-no-permitida` | 409 | Se intenta exportar a Excel una liquidación anulada |
| `vista-previa-desactualizada` | 409 | Los datos de origen cambiaron entre la vista previa y la confirmación |
| `lote-no-liquidable` | 422 | El lote no cumple las condiciones; la causa es `SIN_ALERTA_VACIADO`, `SIN_RESULTADO_SACRIFICIO`, `SIN_PARTIDAS_ALIMENTO` o `PARTIDAS_SIN_PRECIO` |

## 5. Cómo registrar el resultado

En la columna `Pass/Fail` de cada tabla se usa:

| Marca | Significa |
|---|---|
| `[✔]` | El paso se ejecutó y el resultado fue el esperado |
| `[✘]` | El paso se ejecutó y falló; se anota el defecto en `Comments` con su severidad (C, M o S) |
| `[~]` | El resultado del backend y de la base se verificó, pero lo que muestra la pantalla no se ha visto en el navegador |
| `[ ]` | Pendiente de ejecutar |

Cada caso de prueba lleva su `Status` (`Draft`, `In Process` o `Approved`) y, en `Test Execution Flow`, la línea **Última ejecución** con la fecha y la forma en que se ejecutó. Un caso pasa a `Approved` cuando todos sus pasos están en `[✔]`.
