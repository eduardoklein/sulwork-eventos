import type { EventStatus } from '../types/api'

const TONES: Record<EventStatus, string> = {
  RASCUNHO: 'border border-line-strong text-ink-soft',
  ABERTO: 'bg-pen-soft text-pen-dark',
  ENCERRADO: 'bg-line text-ink',
  REALIZADO: 'bg-ok-soft text-ok-ink',
  CANCELADO: 'border border-line-strong text-ink-soft',
}

/** The event status. The label comes from the API domain options, so the frontend never repeats the text. */
export function StatusPill({ status, label }: { status: EventStatus; label: string }) {
  return (
    <span data-status={status} className={`inline-flex rounded-full px-3 py-0.5 text-sm font-semibold ${TONES[status]}`}>
      {label}
    </span>
  )
}
