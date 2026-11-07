import { beforeEach, describe, expect, it } from 'vitest'
import type { HistorialItem } from '../features/reportes/types.ts'
import { mockFetch, reiniciarDatosSimulados } from './mockFetch.ts'

const BASE = 'http://localhost:8080/api/v1/liquidaciones'

async function historial(consulta = '') {
  return (await mockFetch(`${BASE}?${consulta}`)).json()
}

function lotes(pagina: { content: HistorialItem[] }) {
  return pagina.content.map((l) => l.nombreLote)
}

describe('mockFetch: historial', () => {
  beforeEach(() => reiniciarDatosSimulados())

  it('ordena de la más reciente a la más antigua y pagina de a 10', async () => {
    const pagina = await historial()

    expect(pagina.content).toHaveLength(10)
    expect(pagina.totalElements).toBe(13)
    expect(pagina.totalPages).toBe(2)
    expect(pagina.mensaje).toBeNull()
    expect(pagina.content[0].nombreLote).toBe('L-2026-03')
    const fechas = pagina.content.map((l: HistorialItem) => l.fechaHoraGeneracion)
    expect(fechas).toEqual(fechas.toSorted().toReversed())
  })

  it('busca por nombre sin distinguir mayúsculas e incluye las anuladas', async () => {
    const pagina = await historial('search=l-2025-08')

    expect(pagina.content.map((l: HistorialItem) => l.estado)).toEqual(['ACTIVA', 'ANULADA'])
  })

  it('busca por nombre de galpón y por UUID exacto', async () => {
    expect((await historial('search=galpón sur')).totalElements).toBe(3)

    const { content } = await historial('search=L-2025-01')
    expect(lotes(await historial(`search=${content[0].idLote}`))).toEqual(['L-2025-01'])
    expect((await historial(`search=${content[0].idGalpon}`)).totalElements).toBe(2)
  })

  it('aplica el rango de fechas en la zona de Colombia, incluyendo sus extremos', async () => {
    // L-2025-09 se generó el 2026-03-01T02:00Z, es decir, el 28 de febrero en Colombia
    expect(lotes(await historial('desde=2026-02-28&hasta=2026-02-28'))).toEqual(['L-2025-09'])
    expect(lotes(await historial('desde=2026-03-01&hasta=2026-05-19'))).toEqual(['L-2025-10'])
  })

  it('responde 400 si el rango está invertido', async () => {
    const respuesta = await mockFetch(`${BASE}?desde=2026-09-30&hasta=2026-09-01`)

    expect(respuesta.status).toBe(400)
    expect(await respuesta.json()).toMatchObject({
      type: 'https://avicontrol.edu.co/errors/rango-fechas-invalido',
      detail: 'La fecha inicial (2026-09-30) debe ser menor o igual a la fecha final (2026-09-01).',
    })
  })

  it('responde 400 si el tamaño de página está fuera de 1 a 50', async () => {
    expect((await mockFetch(`${BASE}?size=51`)).status).toBe(400)
  })

  it('explica cuando el filtro no tiene coincidencias', async () => {
    const pagina = await historial('search=no-existe')

    expect(pagina.content).toEqual([])
    expect(pagina.mensaje).toBe(
      'No se encontraron liquidaciones para los criterios de búsqueda aplicados',
    )
  })

  it('la fila anulada abre su Liquidación con el registro de anulación', async () => {
    const { content } = await historial('search=L-2026-03')

    const detalle = await (await mockFetch(`${BASE}/${content[0].idLiquidacion}`)).json()

    expect(detalle.estado).toBe('ANULADA')
    expect(detalle.registroAnulacion.motivo).toMatch(/precio anterior/)
    expect(detalle.accionesDisponibles).toEqual(['VER_DESGLOSE'])
  })

  it('una Liquidación recién generada aparece de primera', async () => {
    const solicitud = {
      idLote: 'b0000000-0000-0000-0000-000000000003',
      precioKgCop: 4500,
      usuario: 'Ana Gómez',
    }
    const previa = await (
      await mockFetch(`${BASE}/previa`, { method: 'POST', body: JSON.stringify(solicitud) })
    ).json()
    await mockFetch(BASE, {
      method: 'POST',
      body: JSON.stringify({ ...solicitud, huellaDatos: previa.huellaDatos }),
    })

    const { content } = await historial()

    expect(content[0]).toMatchObject({ nombreLote: 'L-2026-03', estado: 'ACTIVA' })
    expect(content[1]).toMatchObject({ nombreLote: 'L-2026-03', estado: 'ANULADA' })
  })
})
