import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from './format'

describe('formatDateTime', () => {
  it('writes the wall-clock date and time as it came, with no time zone shift', () => {
    expect(formatDateTime('2026-12-18T12:00:00')).toBe('18/12/2026 às 12:00')
    expect(formatDateTime('2026-01-05T08:30:00')).toBe('05/01/2026 às 08:30')
    expect(formatDateTime('2026-12-31T23:59:00')).toBe('31/12/2026 às 23:59')
  })

  it('accepts the time without seconds', () => {
    expect(formatDateTime('2026-12-18T12:00')).toBe('18/12/2026 às 12:00')
  })
})

describe('formatDate', () => {
  it('writes only the date', () => {
    expect(formatDate('2026-12-18T12:00:00')).toBe('18/12/2026')
    expect(formatDate('2026-12-18')).toBe('18/12/2026')
  })
})
