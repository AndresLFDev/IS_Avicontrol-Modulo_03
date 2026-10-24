import { ApiError, httpClient } from './httpClient.ts'

function respuestaJson(status: number, cuerpo: unknown, contentType = 'application/json') {
  return new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': contentType } })
}

describe('httpClient', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('devuelve el cuerpo JSON de una respuesta exitosa', async () => {
    fetchMock.mockResolvedValue(respuestaJson(200, [{ id: 1 }]))

    await expect(httpClient.get('/api/v1/lotes')).resolves.toEqual([{ id: 1 }])
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/lotes',
      expect.objectContaining({ method: 'GET', body: undefined }),
    )
  })

  it('envía el cuerpo como JSON', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(httpClient.post('/api/v1/x', { a: 1 })).resolves.toBeUndefined()
    const [, init] = fetchMock.mock.calls[0]
    expect(init?.body).toBe('{"a":1}')
    expect(init?.headers).toMatchObject({ 'Content-Type': 'application/json' })
  })

  it('lanza ApiError con el ProblemDetail del backend', async () => {
    const problema = {
      type: 'https://avicontrol.edu.co/errors/validacion',
      title: 'Error de validación',
      status: 400,
      detail: 'La solicitud contiene parámetros inválidos.',
      errores: [{ campo: 'precioKgCop', mensaje: 'debe ser mayor que 0' }],
    }
    fetchMock.mockResolvedValue(respuestaJson(400, problema, 'application/problem+json'))

    const error = await httpClient.get('/api/v1/x').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).problema).toEqual(problema)
    expect((error as ApiError).message).toBe('La solicitud contiene parámetros inválidos.')
  })

  it('lanza ApiError genérico si el error no trae ProblemDetail', async () => {
    fetchMock.mockResolvedValue(new Response('<html>', { status: 502, statusText: 'Bad Gateway' }))

    await expect(httpClient.get('/api/v1/x')).rejects.toMatchObject({
      problema: { status: 502, title: 'Bad Gateway' },
    })
  })

  it('lanza ApiError con estado 0 si no hay conexión', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(httpClient.get('/api/v1/x')).rejects.toMatchObject({
      status: 0,
      problema: { title: 'Sin conexión' },
    })
  })
})
