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
      className="m-auto w-[min(32rem,calc(100%-2rem))] rounded-xl bg-white p-6 text-slate-900 shadow-xl backdrop:bg-slate-950/60 dark:bg-slate-900 dark:text-slate-100"
    >
      {abierto && (
        <>
          <h2 id="modal-titulo" className="text-lg font-semibold">
            {titulo}
          </h2>
          <div className="mt-4">{children}</div>
        </>
      )}
    </dialog>
  )
}
