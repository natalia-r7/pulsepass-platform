import { Aviso } from '../../componentes/Aviso'
import { Boton } from '../../componentes/Boton'
import { Modal } from '../../componentes/Modal'
import { mensajeDeError } from '../../lib/http'
import { useCancelarTicket } from './hooks'

interface Props {
  ticketCode: string | null
  onCerrar: () => void
}

/** Confirmación de cancelación; un 409 se muestra y el modal sigue abierto (TEST-FE-TKT-008). */
export function ModalCancelar({ ticketCode, onCerrar }: Props) {
  const cancelar = useCancelarTicket()
  const cerrar = () => {
    cancelar.reset()
    onCerrar()
  }
  return (
    <Modal abierto={ticketCode !== null} titulo="Cancelar ticket" onCerrar={cerrar}>
      <p>¿Seguro que quieres cancelar el ticket {ticketCode}? Esta acción no se puede deshacer.</p>
      {cancelar.isError && <div className="mt-3"><Aviso tipo="error" mensaje={mensajeDeError(cancelar.error)} /></div>}
      <div className="mt-4 flex justify-end gap-2">
        <Boton variante="secundario" onClick={cerrar}>Volver</Boton>
        <Boton variante="peligro" cargando={cancelar.isPending} onClick={() => ticketCode && cancelar.mutate(ticketCode, { onSuccess: cerrar })}>
          Sí, cancelar
        </Boton>
      </div>
    </Modal>
  )
}
