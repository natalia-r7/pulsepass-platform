import { useEffect, useRef, type ReactNode } from 'react'

interface PropsModal {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children: ReactNode
}

/** Diálogo nativo: atrapa el foco y se cierra con Esc. */
export function Modal({ abierto, titulo, onCerrar, children }: PropsModal) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (abierto && !dialogo.open) {
      if (typeof dialogo.showModal === 'function') dialogo.showModal()
      else dialogo.setAttribute('open', '')
    }
    if (!abierto && dialogo.open) {
      if (typeof dialogo.close === 'function') dialogo.close()
      else dialogo.removeAttribute('open')
    }
  }, [abierto])

  return (
    <dialog
      ref={ref}
      aria-labelledby="modal-titulo"
      onCancel={(e) => {
        e.preventDefault()
        onCerrar()
      }}
      className="m-auto w-[min(32rem,calc(100%-2rem))] rounded-3xl bg-white p-6 text-slate-900 shadow-2xl ring-1 ring-marca-500/20 backdrop:bg-marca-900/60 backdrop:backdrop-blur-sm dark:bg-[#150c2b] dark:text-slate-100"
    >
      {abierto && (
        <>
          <h2 id="modal-titulo" className="text-xl font-bold">
            {titulo}
          </h2>
          <div className="mt-4">{children}</div>
        </>
      )}
    </dialog>
  )
}
