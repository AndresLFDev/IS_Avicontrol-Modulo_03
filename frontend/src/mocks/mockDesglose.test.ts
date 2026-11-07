import { beforeEach, describe, expect, it } from 'vitest'
import type { CategoriaDesglose, Desglose, HistorialItem } from '../features/reportes/types.ts'
import { mockFetch, reiniciarDatosSimulados } from './mockFetch.ts'

const BASE = 'http://localhost:8080/api/v1/liquidaciones'

async function desglose(id: number): Promise<Desglose> {
  return (await mockFetch(`${BASE}/${id}/desglose`)).json()
}

async function liquidacionDe(lote: string): Promise<HistorialItem> {
  const pagina = await (await mockFetch(`${BASE}?search=${lote}`)).json()
  return pagina.content[0]
}

function sumaCategorias(categorias: CategoriaDesglose[]) {
  return categorias.reduce((total, c) => total + c.subtotalCop, 0)
}

describe('mockFetch: desglose', () => {
  beforeEach(() => reiniciarDatosSimulados())

  it('cuadra al peso en todas las liquidaciones históricas', async () => {
    const pagina = await (await mockFetch(`${BASE}?size=50`)).json()

    for (const { idLiquidacion } of pagina.content as HistorialItem[]) {
      const { categorias, costosOperativosCop } = await desglose(idLiquidacion)
      expect(sumaCategorias(categorias)).toBe(costosOperativosCop)
      for (const categoria of categorias) {
        const suma = categoria.partidas.reduce((total, p) => total + p.subtotalCop, 0)
        expect(suma).toBe(categoria.subtotalCop)
      }
    }
  })

  it('el siniestro total no tiene venta y sí se puede exportar', async () => {
    const { idLiquidacion } = await liquidacionDe('L-2025-04')

    const respuesta = await desglose(idLiquidacion)

    expect(respuesta.siniestroTotal).toBe(true)
    expect(respuesta.ingresos.ventaBrutaCop).toBe(0)
    expect(respuesta.ingresos.precioKgCop).toBeNull()
    expect(respuesta.exportacionPermitida).toBe(true)
  })

  it('una anulada trae su registro y el Excel responde 409', async () => {
    const { idLiquidacion } = await liquidacionDe('L-2026-03')

    const respuesta = await desglose(idLiquidacion)

    expect(respuesta.registroAnulacion?.usuarioResponsable).toBe('Laura Pérez')
    expect(respuesta.exportacionPermitida).toBe(false)
    const excel = await mockFetch(`${BASE}/${idLiquidacion}/desglose/excel`)
    expect(excel.status).toBe(409)
    expect((await excel.json()).type).toContain('exportacion-no-permitida')
  })

  it('no inventa un archivo: el Excel de una activa pide el backend real', async () => {
    const { idLiquidacion } = await liquidacionDe('L-2025-01')

    const excel = await mockFetch(`${BASE}/${idLiquidacion}/desglose/excel`)

    expect(excel.status).toBe(501)
    expect((await excel.json()).detail).toContain('backend real')
  })

  it('responde 404 si la liquidación no existe', async () => {
    expect((await mockFetch(`${BASE}/999/desglose`)).status).toBe(404)
    expect((await mockFetch(`${BASE}/999/desglose/excel`)).status).toBe(404)
  })
})
