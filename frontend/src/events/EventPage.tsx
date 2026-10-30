import type { EventTab } from '../router/route'
import { EmptyState } from '../ui/EmptyState'
import { buttonClass } from '../ui/styles'
import { EventHeader } from './EventHeader'
import { EventTabs } from './EventTabs'
import { TAB_TITLES } from './tabTitles'
import { useEventData } from './useEventData'

export function EventPage({ id, tab }: { id: number; tab: EventTab }) {
  const { state, reload, retry } = useEventData(id)

  if (state.status === 'loading') {
    return (
      <div role="status" aria-busy="true">
        <span className="sr-only">Carregando o evento</span>
        <div aria-hidden="true" className="h-9 w-2/3 animate-pulse rounded bg-line" />
      </div>
    )
  }
  if (state.status === 'error') {
    return state.notFound ? (
      <EmptyState
        title="Evento não encontrado"
        text="Ele pode ter sido excluído, ou o endereço está errado."
        action={<a href="#/events" className={buttonClass('primary')}>Ver os eventos</a>}
      />
    ) : (
      <div role="alert" className="rounded-lg border border-danger bg-danger-soft px-6 py-5 text-danger-ink">
        <p className="font-semibold">{state.message}</p>
        <button type="button" onClick={retry} className={`${buttonClass('secondary')} mt-3`}>
          Tentar de novo
        </button>
      </div>
    )
  }
  const { event, items, participants, contributions } = state.data
  return (
    <>
      <EventHeader event={event} onChanged={reload} />
      <EventTabs
        eventId={id}
        current={tab}
        counts={{ menu: items.length, participants: participants.length, contributions: contributions.length }}
      />
      {/* The content of each tab comes with steps 31 to 35. */}
      <section aria-label={TAB_TITLES[tab]} className="py-6 text-ink-soft">
        <p>Esta aba chega nos próximos passos.</p>
      </section>
    </>
  )
}
