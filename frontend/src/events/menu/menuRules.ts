import type { EventStatus } from '../../types/api'

/** Only a draft or an open event takes changes to its menu. The backend has the same rule and refuses the rest. */
export function menuEditable(status: EventStatus): boolean {
  return status === 'RASCUNHO' || status === 'ABERTO'
}
