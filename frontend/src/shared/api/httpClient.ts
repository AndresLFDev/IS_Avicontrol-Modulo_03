// Única puerta de salida HTTP del frontend: las features la usan desde sus archivos <area>Api.ts.

const BASE_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')

/** Error de un campo en una respuesta 400 `validacion`. */
export interface ErrorCampo {
  campo: string
  mensaje: string
}

/** Cuerpo `application/problem+json` (RFC 7807) que devuelve el GlobalExceptionHandler. */
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail?: string
  instance?: string
  timestamp?: string
  errores?: ErrorCampo[]
}

export class ApiError extends Error {
  readonly problema: ProblemDetail

  constructor(problema: ProblemDetail) {
    super(problema.detail ?? problema.title)
    this.name = 'ApiError'
    this.problema = problema
  }

  get status() {
    return this.problema.status
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: {
        Accept: 'application/json, application/problem+json',
        ...(body !== undefined && { 'Content-Type': 'application/json' }),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError({
      type: 'about:blank',
      title: 'Sin conexión',
      status: 0,
      detail: 'No se pudo contactar al servidor. Verifique su conexión e intente de nuevo.',
    })
  }

  if (!respuesta.ok) {
    throw new ApiError(await leerProblema(respuesta))
  }
  if (respuesta.status === 204) {
    return undefined as T
  }
  return (await respuesta.json()) as T
}

async function leerProblema(respuesta: Response): Promise<ProblemDetail> {
  if (respuesta.headers.get('Content-Type')?.includes('json')) {
    try {
      return (await respuesta.json()) as ProblemDetail
    } catch {
      // cuerpo inválido: se usa el genérico de abajo
    }
  }
  return { type: 'about:blank', title: respuesta.statusText || 'Error', status: respuesta.status }
}

export const httpClient = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T = void>(path: string) => request<T>('DELETE', path),
}
