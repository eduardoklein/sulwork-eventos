import { labelFor, useDomainOptions } from '../domain/context'
import { formatDateTime } from '../lib/format'
import type { FoodEvent } from '../types/api'
import { StatusPill } from '../ui/StatusPill'
import { buttonClass } from '../ui/styles'
import { deadlineText } from './deadlineText'
import { DeleteEventButton } from './DeleteEventButton'
import { editRuleFor } from './eventFormValues'
import { StatusActions } from './StatusActions'
import { zoneLabel } from './zones'

export function EventHeader({ event, onChanged }: { event: FoodEvent; onChanged: () => void }) {
  const options = useDomainOptions()
  return (
    <header>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <h1 className="font-display text-3xl font-bold tracking-tight">{event.name}</h1>
        <StatusPill status={event.status} label={labelFor(options?.eventStatuses, event.status)} />
      </div>
      <p className="mt-2 text-ink-soft">
        {labelFor(options?.eventTypes, event.type)} · {formatDateTime(event.dateTime)} · {zoneLabel(event.timeZone)}
      </p>
      <p className="text-ink-soft">{deadlineText(event)}</p>
      {event.description && <p className="mt-3 max-w-prose">{event.description}</p>}
      <div className="mt-4 flex flex-wrap gap-3">
        {editRuleFor(event.status) !== 'locked' && (
          <a href={`#/events/${event.id}/edit`} className={buttonClass('secondary')}>
            Editar evento
          </a>
        )}
        <StatusActions event={event} onChanged={onChanged} />
        {event.status === 'RASCUNHO' && <DeleteEventButton eventId={event.id} />}
      </div>
    </header>
  )
}
