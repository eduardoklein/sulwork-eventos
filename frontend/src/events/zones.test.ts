import { afterEach, describe, expect, it, vi } from 'vitest'
import { browserTimeZone, zoneChoices, zoneLabel } from './zones'

afterEach(() => vi.restoreAllMocks())

describe('zoneChoices', () => {
  it('offers the four Brazilian zones, with clear names', () => {
    expect(zoneChoices('America/Sao_Paulo').map((zone) => zone.label)).toEqual([
      'Fernando de Noronha (UTC-2)',
      'Brasília (UTC-3)',
      'Manaus (UTC-4)',
      'Rio Branco (UTC-5)',
    ])
  })

  it('adds a chosen zone that is not in the list, so an event in Lisbon keeps its zone', () => {
    const choices = zoneChoices('Europe/Lisbon')

    expect(choices).toHaveLength(5)
    expect(choices.at(-1)).toEqual({ id: 'Europe/Lisbon', label: 'Europe/Lisbon' })
  })
})

describe('zoneLabel', () => {
  it('uses the clear name of a known zone and the id of an unknown one', () => {
    expect(zoneLabel('America/Manaus')).toBe('Manaus (UTC-4)')
    expect(zoneLabel('Asia/Tokyo')).toBe('Asia/Tokyo')
  })
})

describe('browserTimeZone', () => {
  it('reads the zone of the browser', () => {
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: 'America/Manaus' } as Intl.ResolvedDateTimeFormatOptions)

    expect(browserTimeZone()).toBe('America/Manaus')
  })

  it.each([
    ['America/Recife', 'America/Sao_Paulo'],
    ['America/Fortaleza', 'America/Sao_Paulo'],
    ['America/Cuiaba', 'America/Manaus'],
    ['America/Eirunepe', 'America/Rio_Branco'],
  ])('maps %s to %s, the zone of the list with the same offset', (browser, expected) => {
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: browser } as Intl.ResolvedDateTimeFormatOptions)

    expect(browserTimeZone()).toBe(expected)
  })

  it('falls back to Brasília when no zone of the list has the offset of the browser', () => {
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: 'Asia/Tokyo' } as Intl.ResolvedDateTimeFormatOptions)

    expect(browserTimeZone()).toBe('America/Sao_Paulo')
  })

  it('falls back to Brasília when the browser gives no zone', () => {
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: '' } as Intl.ResolvedDateTimeFormatOptions)

    expect(browserTimeZone()).toBe('America/Sao_Paulo')
  })
})
