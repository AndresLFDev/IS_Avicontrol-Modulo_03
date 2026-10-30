import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import LotesTable from './LotesTable.tsx'
import type { LoteResumen } from './types.ts'

function lote(cambios: Partial<LoteResumen>): LoteResumen {
  return {
    idLote: 'b0000000-0000-0000-0000-000000000003',
    nombreLote: 'L-2026-03',
    fechaIngreso: '2026-08-01',
    idGalpon: 'a0000000-0000-0000-0000-000000000003',
    nombreGalpon: 'Galpón Este',
    etapa: 'POR_LIQUIDAR',
    siniestroTotal: false,
    accion: 'GENERAR_LIQUIDACION',
    accionHabilitada: true,
    ...cambios,
  }
}

describe('LotesTable', () => {
  it('muestra la etiqueta de la etapa y el UUID abreviado', () => {
    render(<LotesTable lotes={[lote({})]} onGenerar={() => {}} />)

    const fila = screen.getByRole('row', { name: /L-2026-03/ })
    expect(within(fila).getByText('Por liquidar')).toBeInTheDocument()
    expect(within(fila).getByText('b000…0003')).toHaveAttribute(
      'title',
      'b0000000-0000-0000-0000-000000000003',
    )
    expect(within(fila).getByText('Galpón Este')).toBeInTheDocument()
  })

  it('marca el siniestro total', () => {
    render(<LotesTable lotes={[lote({ siniestroTotal: true })]} onGenerar={() => {}} />)

    expect(screen.getByText('Siniestro total')).toBeInTheDocument()
  })

  it('deshabilita Generar liquidación cuando el backend lo indica', () => {
    render(
      <LotesTable
        lotes={[lote({ etapa: 'PRODUCTIVO', accionHabilitada: false })]}
        onGenerar={() => {}}
      />,
    )

    expect(screen.getByRole('button', { name: 'Generar liquidación' })).toBeDisabled()
    expect(screen.getByText('Productivo')).toBeInTheDocument()
  })

  it('avisa qué lote se quiere liquidar', async () => {
    const onGenerar = vi.fn<(lote: LoteResumen) => void>()
    render(<LotesTable lotes={[lote({})]} onGenerar={onGenerar} />)

    await userEvent.click(screen.getByRole('button', { name: 'Generar liquidación' }))

    expect(onGenerar).toHaveBeenCalledWith(
      expect.objectContaining({ idLote: 'b0000000-0000-0000-0000-000000000003' }),
    )
  })
})
