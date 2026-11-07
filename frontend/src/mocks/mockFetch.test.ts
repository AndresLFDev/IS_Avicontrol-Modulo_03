import { beforeEach, describe, expect, it } from 'vitest'
import { mockFetch, reiniciarDatosSimulados } from './mockFetch.ts'

const BASE = 'http://localhost:8080/api/v1'
const LOTE_NORMAL = 'b0000000-0000-0000-0000-000000000003'
const LOTE_SINIESTRO = 'b0000000-0000-0000-0000-000000000004'
const LOTE_SIN_PRECIO = 'b0000000-0000-0000-0000-000000000006'
const LOTE_QUE_CAMBIA = 'b0000000-0000-0000-0000-000000000007'

function post(ruta: string, cuerpo: unknown) {
  return mockFetch(`${BASE}${ruta}`, { method: 'POST', body: JSON.stringify(cuerpo) })
}

const solicitud = (idLote: string, precioKgCop: number | null = 4500) => ({
  idLote,
  precioKgCop,
  usuario: 'Ana Gómez',
})

describe('mockFetch', () => {
  beforeEach(() => reiniciarDatosSimulados())

  it('pagina la lista de a 6 y ordena por fecha de ingreso', async () => {
    const pagina = await (await mockFetch(`${BASE}/lotes?page=0&size=6`)).json()

    expect(pagina.mensaje).toBe('6 de 8 lotes')
    expect(pagina.totalPages).toBe(2)
    expect(pagina.content[0].nombreLote).toBe('L-2026-03')
  })

  it('encuentra un lote por su UUID exacto', async () => {
    const pagina = await (await mockFetch(`${BASE}/lotes?search=${LOTE_SINIESTRO}`)).json()

    expect(pagina.content).toHaveLength(1)
    expect(pagina.content[0].siniestroTotal).toBe(true)
  })

  it('deshabilita la acción si el lote por liquidar no tiene resultado de M2', async () => {
    const pagina = await (await mockFetch(`${BASE}/lotes?search=L-2026-08`)).json()

    expect(pagina.content[0].etapa).toBe('POR_LIQUIDAR')
    expect(pagina.content[0].accionHabilitada).toBe(false)
  })

  it('calcula el caso normal de CU03 y saca el lote liquidado de la lista', async () => {
    const previa = await (await post('/liquidaciones/previa', solicitud(LOTE_NORMAL))).json()
    expect(previa).toMatchObject({
      ventaBrutaCop: 107_100_000,
      costosOperativosCop: 85_000_000,
      utilidadNetaCop: 22_100_000,
      mortalidadAves: 500,
      porcentajeMortalidad: 5.56,
      pesoPromedioKg: 2.8,
    })

    const creada = await post('/liquidaciones', {
      ...solicitud(LOTE_NORMAL),
      huellaDatos: previa.huellaDatos,
    })
    expect(creada.status).toBe(201)
    const { idLiquidacion } = await creada.json()

    const consulta = await (await mockFetch(`${BASE}/liquidaciones/${idLiquidacion}`)).json()
    expect(consulta.estado).toBe('ACTIVA')

    const pagina = await (await mockFetch(`${BASE}/lotes?search=L-2026-03`)).json()
    expect(pagina.content).toHaveLength(0)
  })

  it('calcula el siniestro total sin venta y rechaza que traiga precio', async () => {
    const previa = await (
      await post('/liquidaciones/previa', solicitud(LOTE_SINIESTRO, null))
    ).json()
    expect(previa).toMatchObject({
      ventaBrutaCop: 0,
      costosOperativosCop: 32_800_000,
      utilidadNetaCop: -32_800_000,
      porcentajeMortalidad: 100,
      pesoPromedioKg: null,
    })

    const conPrecio = await post('/liquidaciones/previa', solicitud(LOTE_SINIESTRO))
    expect(conPrecio.status).toBe(400)
  })

  it('devuelve 422 con las partidas pendientes si falta un precio', async () => {
    const respuesta = await post('/liquidaciones/previa', solicitud(LOTE_SIN_PRECIO))

    expect(respuesta.status).toBe(422)
    expect(respuesta.headers.get('Content-Type')).toBe('application/problem+json')
    expect((await respuesta.json()).partidasPendientes).toEqual([
      { categoria: 'ALIMENTO', concepto: 'Engorde', referenciaOrigen: 'REC-2026-121' },
    ])
  })

  it('devuelve 409 si los datos cambian tras la vista previa y deja confirmar la nueva', async () => {
    const primera = await (await post('/liquidaciones/previa', solicitud(LOTE_QUE_CAMBIA))).json()
    const rechazo = await post('/liquidaciones', {
      ...solicitud(LOTE_QUE_CAMBIA),
      huellaDatos: primera.huellaDatos,
    })
    expect(rechazo.status).toBe(409)
    expect((await rechazo.json()).type).toMatch(/vista-previa-desactualizada$/)

    const segunda = await (await post('/liquidaciones/previa', solicitud(LOTE_QUE_CAMBIA))).json()
    expect(segunda.pesoTotalKg).toBeGreaterThan(primera.pesoTotalKg)

    const creada = await post('/liquidaciones', {
      ...solicitud(LOTE_QUE_CAMBIA),
      huellaDatos: segunda.huellaDatos,
    })
    expect(creada.status).toBe(201)
  })

  it('devuelve 409 con el id de la liquidación activa si se intenta liquidar de nuevo', async () => {
    const previa = await (await post('/liquidaciones/previa', solicitud(LOTE_NORMAL))).json()
    await post('/liquidaciones', { ...solicitud(LOTE_NORMAL), huellaDatos: previa.huellaDatos })

    const repetida = await post('/liquidaciones/previa', solicitud(LOTE_NORMAL))

    expect(repetida.status).toBe(409)
    expect((await repetida.json()).idLiquidacionActiva).toBe(1)
  })

  it('valida el usuario responsable', async () => {
    const respuesta = await post('/liquidaciones/previa', {
      ...solicitud(LOTE_NORMAL),
      usuario: ' ',
    })

    expect(respuesta.status).toBe(400)
    expect((await respuesta.json()).errores).toEqual([
      { campo: 'usuario', mensaje: 'El usuario responsable es obligatorio.' },
    ])
  })
})
