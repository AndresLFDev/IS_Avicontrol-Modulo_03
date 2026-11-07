import { useEffect, useId, useRef, type ReactNode } from 'react'

const ANCHOS = { normal: 'max-w-lg', amplio: 'max-w-4xl' }

interface Props {
  titulo: string
  onCerrar: () => void
  children: ReactNode
  ancho?: keyof typeof ANCHOS
  /** Mientras se guarda no se puede cerrar: la operación ya está en curso. */
  bloqueado?: boolean
}

/**
 * Ventana sobre la página. Usa `<dialog>` para que el navegador encierre el foco y bloquee el
 * fondo. Un clic afuera no cierra: se perdería lo que el usuario lleva escrito.
 */
export default function Modal({
  titulo,
  onCerrar,
  children,
  ancho = 'normal',
  bloqueado = false,
}: Props) {
  const ventana = useRef<HTMLDialogElement>(null)
  const idTitulo = useId()

  useEffect(() => {
    const dialogo = ventana.current
    if (dialogo && !dialogo.open) dialogo.showModal()
    return () => dialogo?.close()
  }, [])

  return (
    <dialog
      ref={ventana}
      aria-labelledby={idTitulo}
      onCancel={(evento) => {
        evento.preventDefault()
        if (!bloqueado) onCerrar()
      }}
      className={`m-auto max-h-[90vh] w-[calc(100%-2rem)] overflow-y-auto rounded-2xl bg-white p-6 shadow-xl backdrop:bg-black/50 ${ANCHOS[ancho]}`}
    >
      <h2 id={idTitulo} className="pr-8 text-xl font-bold">
        {titulo}
      </h2>
      <div className="mt-3 space-y-4">{children}</div>
      {/* Va al final para que el foco inicial caiga en el contenido y no en la X */}
      <button
        type="button"
        aria-label="Cerrar"
        disabled={bloqueado}
        onClick={onCerrar}
        className="absolute top-4 right-4 size-8 rounded-full text-xl leading-none text-gray-500 hover:bg-gray-100 disabled:opacity-40"
      >
        ×
      </button>
    </dialog>
  )
}
