import type { Category, FoodEvent, Item } from '../../types/api'
import { CategoriesSection } from './CategoriesSection'
import { ItemsSection } from './ItemsSection'
import { menuEditable } from './menuRules'

export function MenuTab({ event, categories, items, onChanged }: { event: FoodEvent; categories: Category[]; items: Item[]; onChanged: () => void }) {
  const editable = menuEditable(event.status)
  return (
    <section aria-label="Cardápio">
      {!editable && (
        <p className="mt-6 max-w-prose rounded-lg border border-line-strong bg-surface px-4 py-3 text-sm">
          O cardápio não muda mais, porque o evento não aceita alterações nele.
        </p>
      )}
      <CategoriesSection eventId={event.id} categories={categories} editable={editable} onChanged={onChanged} />
      <ItemsSection eventId={event.id} categories={categories} items={items} editable={editable} onChanged={onChanged} />
    </section>
  )
}
