const COP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})
const COP_DECIMALES = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})
const NUMERO = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })

/** Monto entero en pesos: 107100000 → "$ 107.100.000". */
export function formatCop(valor: number) {
  return COP.format(valor)
}

/** Precio que puede traer centavos: 4500.5 → "$ 4.500,5". */
export function formatPrecio(valor: number) {
  return COP_DECIMALES.format(valor)
}

/** Cantidad con separador de miles y hasta 2 decimales: 23800 → "23.800". */
export function formatNumero(valor: number) {
  return NUMERO.format(valor)
}
