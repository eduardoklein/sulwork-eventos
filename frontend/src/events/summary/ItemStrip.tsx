import { labelFor, useDomainOptions } from '../../domain/context'
import type { Item } from '../../types/api'

type SegmentState = 'complete' | 'mandatory' | 'optional'

const TONES: Record<SegmentState, string> = {
  complete: 'bg-ok',
  mandatory: 'bg-ink',
  optional: 'bg-line-strong',
}

const stateOf = (item: Item): SegmentState => (item.pendingQuantity === 0 ? 'complete' : item.mandatory ? 'mandatory' : 'optional')

/**
 * One segment for each item of a category. The width is the quantity the item needs, so the bar shows where
 * the lack is, and not only how much. It is for one category because the units of two categories do not add up.
 */
export function ItemStrip({ categoryName, items }: { categoryName: string; items: Item[] }) {
  const options = useDomainOptions()
  return (
    <ul aria-label={`Itens de ${categoryName}`} className="flex h-3 gap-[2px]">
      {items.map((item) => {
        const state = stateOf(item)
        const text = `${item.name}: ${item.contributedQuantity} de ${item.requiredQuantity} (${labelFor(options?.measureUnits, item.unit).toLowerCase()}), ${
          item.mandatory ? 'obrigatório' : 'opcional'
        }${state === 'complete' ? ', completo' : ''}`
        return (
          <li
            key={item.id}
            data-state={state}
            title={text}
            style={{ flexGrow: item.requiredQuantity, flexBasis: 0 }}
            className={`min-w-[6px] rounded-[2px] ${TONES[state]}`}
          >
            <span className="sr-only">{text}</span>
          </li>
        )
      })}
    </ul>
  )
}
