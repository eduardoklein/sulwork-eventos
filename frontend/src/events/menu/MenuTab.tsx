import type { Category, DietaryAnalysis, FoodEvent, Item, Participant } from '../../types/api'
import { CategoriesSection } from './CategoriesSection'
import { ItemsSection } from './ItemsSection'
import { menuEditable } from './menuRules'
import { viewerOf } from './safety'
import { ViewAsSelect } from './ViewAsSelect'

export function MenuTab({
  event,
  categories,
  items,
  participants,
  analysis,
  viewAs,
  onViewAsChange,
  onChanged,
}: {
  event: FoodEvent
  categories: Category[]
  items: Item[]
  participants: Participant[]
  analysis: DietaryAnalysis
  /** The participant whose view of the menu is on screen. A person who left the event counts as nobody. */
  viewAs: number | null
  onViewAsChange: (participantId: number | null) => void
  onChanged: () => void
}) {
  const editable = menuEditable(event.status)
  const viewer = viewerOf(
    participants.find((participant) => participant.id === viewAs),
    analysis,
  )
  return (
    <section aria-label="Cardápio">
      {!editable && (
        <p className="mt-6 max-w-prose rounded-lg border border-line-strong bg-surface px-4 py-3 text-sm">
          O cardápio não muda mais, porque o evento não aceita alterações nele.
        </p>
      )}
      {participants.length > 0 && <ViewAsSelect participants={participants} value={viewer ? viewAs : null} onChange={onViewAsChange} />}
      <CategoriesSection eventId={event.id} categories={categories} editable={editable} onChanged={onChanged} />
      <ItemsSection eventId={event.id} categories={categories} items={items} editable={editable} viewer={viewer} onChanged={onChanged} />
    </section>
  )
}
