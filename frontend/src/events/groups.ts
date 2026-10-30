import { toInstant } from '../lib/zoned'
import type { EventStatus, FoodEvent } from '../types/api'

export type GroupKey = 'open' | 'drafts' | 'done' | 'cancelled'

export interface EventGroup {
  key: GroupKey
  title: string
  events: FoodEvent[]
}

const GROUPS: { key: GroupKey; title: string; statuses: EventStatus[]; order: (event: FoodEvent) => number }[] = [
  // Where can I still contribute? The closest deadline comes first, because it is the most urgent.
  { key: 'open', title: 'Abertos para contribuição', statuses: ['ABERTO'], order: (e) => toInstant(e.changeDeadline, e.timeZone) },
  { key: 'drafts', title: 'Rascunhos', statuses: ['RASCUNHO'], order: (e) => toInstant(e.dateTime, e.timeZone) },
  { key: 'done', title: 'Encerrados e realizados', statuses: ['ENCERRADO', 'REALIZADO'], order: (e) => -toInstant(e.dateTime, e.timeZone) },
  { key: 'cancelled', title: 'Cancelados', statuses: ['CANCELADO'], order: (e) => -toInstant(e.dateTime, e.timeZone) },
]

/** The events by situation, each group in its own order, without the groups that have no event. */
export function groupEvents(events: FoodEvent[]): EventGroup[] {
  return GROUPS.map((group) => ({
    key: group.key,
    title: group.title,
    events: events
      .filter((event) => group.statuses.includes(event.status))
      .sort((a, b) => group.order(a) - group.order(b) || a.id - b.id),
  })).filter((group) => group.events.length > 0)
}

/** A draft has no contributions and a cancelled event no longer matters: only the others show a coverage. */
export function hasCoverage(status: EventStatus): boolean {
  return status === 'ABERTO' || status === 'ENCERRADO' || status === 'REALIZADO'
}
