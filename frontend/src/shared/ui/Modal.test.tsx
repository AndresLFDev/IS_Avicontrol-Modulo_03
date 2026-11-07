import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Modal from './Modal.tsx'

describe('Modal', () => {
  it('se abre como diálogo con su título', () => {
    render(
      <Modal titulo="Generar liquidación" onCerrar={() => {}}>
        <p>Contenido</p>
      </Modal>,
    )

    expect(screen.getByRole('dialog', { name: 'Generar liquidación' })).toBeInTheDocument()
    expect(screen.getByText('Contenido')).toBeInTheDocument()
  })

  it('se cierra con la X y con Escape', async () => {
    const onCerrar = vi.fn<() => void>()
    render(
      <Modal titulo="Título" onCerrar={onCerrar}>
        <p>Contenido</p>
      </Modal>,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }))

    expect(onCerrar).toHaveBeenCalledTimes(2)
  })

  it('no se cierra mientras está bloqueado', () => {
    const onCerrar = vi.fn<() => void>()
    render(
      <Modal titulo="Título" onCerrar={onCerrar} bloqueado>
        <p>Contenido</p>
      </Modal>,
    )

    fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }))

    expect(screen.getByRole('button', { name: 'Cerrar' })).toBeDisabled()
    expect(onCerrar).not.toHaveBeenCalled()
  })
})
