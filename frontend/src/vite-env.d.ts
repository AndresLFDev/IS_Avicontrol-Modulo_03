/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base del backend, sin barra final. */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
