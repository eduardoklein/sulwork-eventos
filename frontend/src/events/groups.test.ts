import { describe, expect, it } from 'vitest'
import { makeEvent } from '../test/fakeApi'
import { groupEvents, hasCoverage } from './groups'

describe('groupEvents', () => {
  const events = [
    makeEvent({ id: 1, name: 'Aberto tarde', status: 'ABERTO', changeDeadline: '2026-12-20T18:00:00' }),
    makeEvent({ id: 2, name: 'Aberto cedo', status: 'ABERTO', changeDeadline: '2026-11-10T18:00:00' }),
    makeEvent({ id: 3, name: 'Rascunho longe', status: 'RASCUNHO', dateTime: '2027-01-30T12:00:00' }),
    makeEvent({ id: 4, name: 'Rascunho perto', status: 'RASCUNHO', dateTime: '2026-12-01T12:00:00' }),
    makeEvent({ id: 5, name: 'Realizado antigo', status: 'REALIZADO', dateTime: '2026-09-01T12:00:00' }),
    makeEvent({ id: 6, name: 'Encerrado recente', status: 'ENCERRADO', dateTime: '2026-10-25T12:00:00' }),
    makeEvent({ id: 7, name: 'Cancelado antigo', status: 'CANCELADO', dateTime: '2026-08-01T12:00:00' }),
    makeEvent({ id: 8, name: 'Cancelado recente', status: 'CANCELADO', dateTime: '2026-11-01T12:00:00' }),
  ]

  it('makes the four groups, in the order of the question "where can I still contribute?"', () => {
    expect(groupEvents(events).map((group) => group.title)).toEqual([
      'Abertos para contribuição',
      'Rascunhos',
      'Encerrados e realizados',
      'Cancelados',
    ])
  })

  it('orders the open events by the closest deadline, the most urgent first', () => {
    const open = groupEvents(events)[0]

    expect(open.events.map((event) => event.name)).toEqual(['Aberto cedo', 'Aberto tarde'])
  })

  it('orders the drafts by the closest date', () => {
    expect(groupEvents(events)[1].events.map((event) => event.name)).toEqual(['Rascunho perto', 'Rascunho longe'])
  })

  it('puts closed and held events together, the most recent first', () => {
    expect(groupEvents(events)[2].events.map((event) => event.name)).toEqual(['Encerrado recente', 'Realizado antigo'])
  })

  it('orders the cancelled events with the most recent first', () => {
    expect(groupEvents(events)[3].events.map((event) => event.name)).toEqual(['Cancelado recente', 'Cancelado antigo'])
  })

  it('leaves out the groups that have no event', () => {
    const groups = groupEvents([makeEvent({ id: 1, name: 'Só este', status: 'RASCUNHO' })])

    expect(groups.map((group) => group.key)).toEqual(['drafts'])
  })

  it('returns nothing for no events', () => {
    expect(groupEvents([])).toEqual([])
  })

  it('orders deadlines by the real instant when the events are in different time zones', () => {
    const saoPaulo = makeEvent({ id: 1, name: 'São Paulo 12h', changeDeadline: '2026-12-13T12:00:00', timeZone: 'America/Sao_Paulo' })
    const lisbon = makeEvent({ id: 2, name: 'Lisboa 12h', changeDeadline: '2026-12-13T12:00:00', timeZone: 'Europe/Lisbon' })

    expect(groupEvents([saoPaulo, lisbon])[0].events.map((event) => event.name)).toEqual(['Lisboa 12h', 'São Paulo 12h'])
  })

  it('breaks a tie by the oldest event', () => {
    const first = makeEvent({ id: 1, name: 'Primeiro' })
    const second = makeEvent({ id: 2, name: 'Segundo' })

    expect(groupEvents([second, first])[0].events.map((event) => event.id)).toEqual([1, 2])
  })

  it('does not change the list it receives', () => {
    const input = [events[0], events[1]]

    groupEvents(input)

    expect(input.map((event) => event.id)).toEqual([1, 2])
  })
})

describe('hasCoverage', () => {
  it.each([
    ['ABERTO', true],
    ['ENCERRADO', true],
    ['REALIZADO', true],
    ['RASCUNHO', false],
    ['CANCELADO', false],
  ] as const)('%s: %s', (status, expected) => {
    expect(hasCoverage(status)).toBe(expected)
  })
})
