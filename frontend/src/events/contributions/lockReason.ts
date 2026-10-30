import { labelFor } from '../../domain/context'
import type { DomainOptions, FoodEvent } from '../../types/api'

/**
 * Why the event does not take contributions now, or `null` when it does. The API already says if it takes them
 * (`acceptsContributions`, with the status and the deadline together), so the screen only explains the reason.
 */
export function lockReason(event: FoodEvent, options: DomainOptions | null): string | null {
  if (event.acceptsContributions) return null
  if (event.status !== 'ABERTO') {
    return `Só um evento aberto aceita contribuições. Este evento está ${labelFor(options?.eventStatuses, event.status).toLowerCase()}.`
  }
  return 'O prazo para alterações já passou.'
}
