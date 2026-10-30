export const EVENT_TABS = ['summary', 'menu', 'participants', 'contributions'] as const
export type EventTab = (typeof EVENT_TABS)[number]

export type Route =
  | { name: 'events' }
  | { name: 'new' }
  | { name: 'edit'; id: number }
  | { name: 'event'; id: number; tab: EventTab }

export const DEFAULT_TAB: EventTab = 'summary'
export const EVENTS_ROUTE: Route = { name: 'events' }

const EVENTS = /^\/events\/?$/
const NEW = /^\/events\/new\/?$/
const EDIT = /^\/events\/(\d+)\/edit\/?$/
const EVENT = /^\/events\/(\d+)(?:\/(summary|menu|participants|contributions))?\/?$/

/** Turns an address (`#/events/5/menu`) into a route, or `null` when no screen has that address. */
export function parseRoute(hash: string): Route | null {
  const path = hash.replace(/^#/, '')
  if (EVENTS.test(path)) return EVENTS_ROUTE
  if (NEW.test(path)) return { name: 'new' }
  const edit = EDIT.exec(path)
  if (edit) {
    const id = toId(edit[1])
    return id === null ? null : { name: 'edit', id }
  }
  const event = EVENT.exec(path)
  if (event) {
    const id = toId(event[1])
    return id === null ? null : { name: 'event', id, tab: (event[2] as EventTab | undefined) ?? DEFAULT_TAB }
  }
  return null
}

/** The address of a route, the inverse of `parseRoute`. */
export function routeHash(route: Route): string {
  switch (route.name) {
    case 'events':
      return '#/events'
    case 'new':
      return '#/events/new'
    case 'edit':
      return `#/events/${route.id}/edit`
    case 'event':
      return `#/events/${route.id}/${route.tab}`
  }
}

function toId(digits: string): number | null {
  const id = Number(digits)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}
