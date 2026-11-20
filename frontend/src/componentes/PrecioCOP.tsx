import { formatearCOP } from '../lib/formato'

export function PrecioCOP({ valor }: { valor: number }) {
  return <span>{formatearCOP(valor)}</span>
}
