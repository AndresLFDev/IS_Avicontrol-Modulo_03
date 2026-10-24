import { Link } from 'react-router'

export default function NoEncontradoPage() {
  return (
    <section>
      <h1 className="mb-2 text-2xl font-semibold">Página no encontrada</h1>
      <p>
        La dirección no existe.{' '}
        <Link to="/" className="text-primario underline">
          Volver al inicio
        </Link>
      </p>
    </section>
  )
}
