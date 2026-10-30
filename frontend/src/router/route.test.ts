import { describe, expect, it } from 'vitest'
import { EVENT_TABS, parseRoute, routeHash, type Route } from './route'

describe('parseRoute', () => {
  it.each([
    ['#/events', { name: 'events' }],
    ['#/events/', { name: 'events' }],
    ['#/events/new', { name: 'new' }],
    ['#/events/12/edit', { name: 'edit', id: 12 }],
    ['#/events/12', { name: 'event', id: 12, tab: 'summary' }],
    ['#/events/12/summary', { name: 'event', id: 12, tab: 'summary' }],
    ['#/events/12/menu', { name: 'event', id: 12, tab: 'menu' }],
    ['#/events/12/participants', { name: 'event', id: 12, tab: 'participants' }],
    ['#/events/12/contributions/', { name: 'event', id: 12, tab: 'contributions' }],
  ] as [string, Route][])('reads %s', (hash, route) => {
    expect(parseRoute(hash)).toEqual(route)
  })

  it.each([
    ['an empty address', ''],
    ['the root', '#/'],
    ['an unknown screen', '#/people'],
    ['an unknown tab', '#/events/12/stats'],
    ['an id that is not a number', '#/events/abc'],
    ['an id of zero', '#/events/0'],
    ['a negative id', '#/events/-3'],
    ['a decimal id', '#/events/1.5'],
    ['an id too large to be exact', '#/events/99999999999999999999'],
    ['extra segments', '#/events/12/menu/extra'],
    ['an edit without id', '#/events/edit'],
    ['a different case', '#/Events'],
  ])('has no screen for %s', (_case, hash) => {
    expect(parseRoute(hash)).toBeNull()
  })
})

describe('routeHash', () => {
  it('is the inverse of parseRoute for every screen', () => {
    const routes: Route[] = [
      { name: 'events' },
      { name: 'new' },
      { name: 'edit', id: 7 },
      ...EVENT_TABS.map((tab) => ({ name: 'event', id: 7, tab }) as Route),
    ]

    for (const route of routes) {
      expect(parseRoute(routeHash(route))).toEqual(route)
    }
  })
})
