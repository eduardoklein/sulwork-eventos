import { formatDateTime } from '../lib/format'
import type { FoodEvent } from '../types/api'

/** "Contribuições até …" while the event takes contributions, and a plain "Prazo: …" otherwise. */
export function deadlineText(event: FoodEvent): string {
  const when = formatDateTime(event.changeDeadline)
  return event.status === 'ABERTO' ? `Contribuições até ${when}` : `Prazo: ${when}`
}
