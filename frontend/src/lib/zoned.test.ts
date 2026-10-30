import { describe, expect, it } from 'vitest'
import { toInstant } from './zoned'

const iso = (instant: number) => new Date(instant).toISOString()

describe('toInstant', () => {
  it('reads 12:00 in São Paulo (UTC-3) as 15:00 UTC', () => {
    expect(iso(toInstant('2026-12-18T12:00:00', 'America/Sao_Paulo'))).toBe('2026-12-18T15:00:00.000Z')
  })

  it('reads 12:00 in Lisbon as 12:00 UTC in winter and 11:00 UTC in summer', () => {
    expect(iso(toInstant('2026-12-18T12:00:00', 'Europe/Lisbon'))).toBe('2026-12-18T12:00:00.000Z')
    expect(iso(toInstant('2026-07-01T12:00:00', 'Europe/Lisbon'))).toBe('2026-07-01T11:00:00.000Z')
  })

  it('handles a zone ahead of UTC and a time without seconds', () => {
    expect(iso(toInstant('2026-12-18T09:30', 'Asia/Tokyo'))).toBe('2026-12-18T00:30:00.000Z')
  })

  it('does not depend on the time zone of the machine', () => {
    const sameWallClock = toInstant('2026-06-15T10:00:00', 'America/New_York')

    expect(iso(sameWallClock)).toBe('2026-06-15T14:00:00.000Z')
  })

  it('puts 12:00 in Lisbon before 12:00 in São Paulo on the same day', () => {
    expect(toInstant('2026-12-18T12:00:00', 'Europe/Lisbon')).toBeLessThan(
      toInstant('2026-12-18T12:00:00', 'America/Sao_Paulo'),
    )
  })

  it('keeps the right offset right after daylight saving starts in New York', () => {
    expect(iso(toInstant('2026-03-08T12:00:00', 'America/New_York'))).toBe('2026-03-08T16:00:00.000Z')
    expect(iso(toInstant('2026-03-07T12:00:00', 'America/New_York'))).toBe('2026-03-07T17:00:00.000Z')
  })
})
