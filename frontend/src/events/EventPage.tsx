import { useState } from 'react'
import { routeHash, type EventTab } from '../router/route'
import { EmptyState } from '../ui/EmptyState'
import { ErrorPanel } from '../ui/ErrorPanel'
import { buttonClass } from '../ui/styles'
import { EventHeader } from './EventHeader'
import { EventTabs } from './EventTabs'
import { ContributionsTab } from './contributions/ContributionsTab'
import { MenuTab } from './menu/MenuTab'
import { ParticipantsTab } from './participants/ParticipantsTab'
import { SummaryTab } from './summary/SummaryTab'
import { useEventData } from './useEventData'

export function EventPage({ id, tab }: { id: number; tab: EventTab }) {
  const { state, reload, retry } = useEventData(id)
  // The person whose view of the menu is on screen. It lives here, so it holds when the tab changes.
  const [viewAs, setViewAs] = useState<number | null>(null)

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
      <ErrorPanel message={state.message} onRetry={retry} />
    )
  }
  const { event, categories, items, participants, contributions, analysis } = state.data
  return (
    <>
      <EventHeader event={event} onChanged={reload} />
      <EventTabs
        eventId={id}
        current={tab}
        counts={{ menu: items.length, participants: participants.length, contributions: contributions.length }}
      />
      {tab === 'menu' && (
        <MenuTab
          event={event}
          categories={categories}
          items={items}
          participants={participants}
          analysis={analysis}
          viewAs={viewAs}
          onViewAsChange={setViewAs}
          onChanged={reload}
        />
      )}
      {tab === 'participants' && (
        <ParticipantsTab
          event={event}
          participants={participants}
          contributions={contributions}
          analysis={analysis}
          onViewMenu={(participantId) => {
            setViewAs(participantId)
            window.location.hash = routeHash({ name: 'event', id, tab: 'menu' })
          }}
          onChanged={reload}
        />
      )}
      {tab === 'contributions' && (
        <ContributionsTab event={event} items={items} participants={participants} contributions={contributions} onChanged={reload} />
      )}
      {tab === 'summary' && <SummaryTab event={event} items={items} participants={participants} onChanged={reload} />}
    </>
  )
}
