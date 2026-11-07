/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base del backend, sin barra final. */
  readonly VITE_API_URL?: string
  /** 'true' reemplaza las llamadas HTTP por el backend simulado de src/mocks. */
  readonly VITE_USE_MOCKS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
