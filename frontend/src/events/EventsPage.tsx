import { labelFor, useDomainOptions } from '../domain/context'
import { formatDateTime } from '../lib/format'
import type { DomainOptions, FoodEvent } from '../types/api'
import { EmptyState } from '../ui/EmptyState'
import { Meter } from '../ui/Meter'
import { Section } from '../ui/Section'
import { StatusPill } from '../ui/StatusPill'
import { buttonClass } from '../ui/styles'
import { groupEvents } from './groups'
import { useEventList, type CoverageState } from './useEventList'

export function EventsPage() {
  const { list, coverages, retry } = useEventList()
  const options = useDomainOptions()

  if (list.status === 'loading') {
    return <EventsSkeleton />
  }
  if (list.status === 'error') {
    return (
      <div role="alert" className="rounded-lg border border-danger bg-danger-soft px-6 py-5 text-danger-ink">
        <p className="font-semibold">{list.message}</p>
        <button type="button" onClick={retry} className={`${buttonClass('secondary')} mt-3`}>
          Tentar de novo
        </button>
      </div>
    )
  }
  if (list.events.length === 0) {
    return (
      <EmptyState
        title="Nenhum evento ainda"
        text="Crie o primeiro evento para montar o cardápio e combinar quem leva o quê."
        action={<a href="#/events/new" className={buttonClass('primary')}>Criar evento</a>}
      />
    )
  }
  return (
    <>
      {groupEvents(list.events).map((group) => (
        <Section key={group.key} title={group.title}>
          <ul className="divide-y divide-line border-y border-line bg-surface">
            {group.events.map((event) => (
              <EventRow key={event.id} event={event} options={options} coverage={coverages[event.id]} />
            ))}
          </ul>
        </Section>
      ))}
    </>
  )
}

function EventRow({ event, options, coverage }: { event: FoodEvent; options: DomainOptions | null; coverage?: CoverageState }) {
  return (
    <li className="relative px-4 py-4 hover:bg-paper">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <h3 className="font-display text-lg font-bold tracking-tight">
          {/* The link covers the whole row, so the row is one big target and still has one clear name. */}
          <a href={`#/events/${event.id}`} className="after:absolute after:inset-0">
            {event.name}
          </a>
        </h3>
        <StatusPill status={event.status} label={labelFor(options?.eventStatuses, event.status)} />
      </div>
      <p className="mt-1 text-sm text-ink-soft">
        {labelFor(options?.eventTypes, event.type)} · {formatDateTime(event.dateTime)}
      </p>
      <p className="text-sm text-ink-soft">{deadlineText(event)}</p>
      {coverage && <CoverageCell event={event} options={options} state={coverage} />}
    </li>
  )
}

function deadlineText(event: FoodEvent): string {
  const when = formatDateTime(event.changeDeadline)
  return event.status === 'ABERTO' ? `Contribuições até ${when}` : `Prazo: ${when}`
}

function CoverageCell({ event, options, state }: { event: FoodEvent; options: DomainOptions | null; state: CoverageState }) {
  if (state.status === 'loading') {
    return (
      <div className="mt-3 max-w-sm">
        <div aria-hidden="true" className="h-2 animate-pulse rounded-full bg-line" />
        <span className="sr-only">Carregando a cobertura</span>
      </div>
    )
  }
  if (state.status === 'error') {
    return <p className="mt-3 text-sm text-ink-soft">Cobertura indisponível</p>
  }
  const { overallPercentage, readiness } = state.coverage
  if (overallPercentage === null) {
    return <p className="mt-3 text-sm text-ink-soft">Sem itens no cardápio</p>
  }
  return (
    <div className="mt-3 max-w-sm">
      <Meter value={overallPercentage} label={`Cobertura de ${event.name}`} />
      <p className="mt-1 text-sm font-semibold">
        {overallPercentage}% · {labelFor(options?.eventReadiness, readiness)}
      </p>
    </div>
  )
}

function EventsSkeleton() {
  return (
    <div role="status" aria-busy="true">
      <span className="sr-only">Carregando os eventos</span>
      <div aria-hidden="true" className="space-y-3">
        {[0, 1, 2].map((row) => (
          <div key={row} className="h-24 animate-pulse rounded-lg bg-line" />
        ))}
      </div>
    </div>
  )
}
