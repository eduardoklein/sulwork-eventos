import type { DietaryAnalysis, DomainOptions, Participant, UnsafeItem } from '../../types/api'
import { labelFor } from '../../domain/context'

/** The person whose view of the menu is on screen, and the items that are not safe for them. */
export interface Viewer {
  name: string
  unsafeByItem: Map<number, UnsafeItem>
}

/** `null` when nobody is chosen, or the chosen person left the event. An item that is not in the map is safe. */
export function viewerOf(participant: Participant | undefined, analysis: DietaryAnalysis): Viewer | null {
  if (!participant) return null
  const diet = analysis.participants.find((entry) => entry.participantId === participant.id)
  return { name: participant.name, unsafeByItem: new Map((diet?.unsafeItems ?? []).map((unsafe) => [unsafe.itemId, unsafe])) }
}

/** "Não é seguro: contém glúten, lactose; carne de origem animal". */
export function unsafeText(unsafe: UnsafeItem, options: DomainOptions | null): string {
  const allergens = unsafe.conflictingAllergens.map((code) => labelFor(options?.allergens, code).toLowerCase())
  const characteristics = unsafe.conflictingCharacteristics.map((code) => labelFor(options?.foodCharacteristics, code).toLowerCase())
  const parts = [allergens.length > 0 && `contém ${allergens.join(', ')}`, characteristics.length > 0 && characteristics.join(', ')]
  return `Não é seguro: ${parts.filter(Boolean).join('; ')}`
}
