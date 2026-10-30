import { EVENT_TABS, routeHash, type EventTab } from '../router/route'
import { TAB_TITLES } from './tabTitles'

/** The sections are addresses, so they are links, and the current one is marked for a screen reader. */
export function EventTabs({ eventId, current, counts }: { eventId: number; current: EventTab; counts: Partial<Record<EventTab, number>> }) {
  return (
    <nav aria-label="Seções do evento" className="mt-6 border-b border-line">
      <ul className="-mb-px flex flex-wrap gap-x-6">
        {EVENT_TABS.map((tab) => {
          const active = tab === current
          return (
            <li key={tab}>
              <a
                href={routeHash({ name: 'event', id: eventId, tab })}
                aria-current={active ? 'page' : undefined}
                className={`inline-block border-b-2 py-3 font-semibold ${active ? 'border-pen text-pen-dark' : 'border-transparent text-ink-soft hover:text-ink'}`}
              >
                {TAB_TITLES[tab]}
                {counts[tab] !== undefined && <span className="ml-1 text-sm font-normal">({counts[tab]})</span>}
              </a>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
