const FECHA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
const FECHA_HORA = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'medium',
  timeStyle: 'short',
  timeZone: 'America/Bogota',
})

/** Fecha ISO sin hora (`2026-08-01`) → "1 ago 2026". Se arma como fecha local para no correr el día. */
export function formatFecha(fechaIso: string) {
  const [anio, mes, dia] = fechaIso.split('-').map(Number)
  return FECHA.format(new Date(anio, mes - 1, dia))
}

/** Instante ISO (`2026-10-20T13:07:01Z`) → fecha y hora de Colombia. */
export function formatFechaHora(instanteIso: string) {
  return FECHA_HORA.format(new Date(instanteIso))
}
