import { useState } from 'react'
import { useDomainOptions } from '../../domain/context'
import type { FoodEvent, Item, Participant } from '../../types/api'
import { lockReason } from '../contributions/lockReason'
import { CoverageSection } from './CoverageSection'
import { ShareSection } from './ShareSection'
import { SuggestionsSection } from './SuggestionsSection'

export function SummaryTab({
  event,
  items,
  participants,
  onChanged,
}: {
  event: FoodEvent
  items: Item[]
  participants: Participant[]
  onChanged: () => void
}) {
  const options = useDomainOptions()
  // Goes up after a contribution is registered here, so the coverage, the suggestions and the text load again.
  const [version, setVersion] = useState(0)
  const taken = () => {
    setVersion((current) => current + 1)
    onChanged()
  }
  return (
    <section aria-label="Resumo">
      <CoverageSection eventId={event.id} items={items} version={version} />
      <SuggestionsSection
        eventId={event.id}
        participants={participants}
        items={items}
        lockReason={lockReason(event, options)}
        version={version}
        onTaken={taken}
      />
      <ShareSection eventId={event.id} version={version} />
    </section>
  )
}
