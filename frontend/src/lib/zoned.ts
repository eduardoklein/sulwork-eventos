/**
 * The instant (in milliseconds) of a wall-clock date and time in a time zone. Two events in different zones can only
 * be put in order by their real instants: 12:00 in Lisbon comes before 12:00 in São Paulo on the same day.
 */
export function toInstant(local: string, timeZone: string): number {
  const [date, time = '00:00:00'] = local.split('T')
  const [year, month, day] = date.split('-').map(Number)
  const [hour, minute, second = 0] = time.split(':').map(Number)
  const asUtc = Date.UTC(year, month - 1, day, hour, minute, second)
  // The offset depends on the instant, so a second pass fixes the days when the offset changes (daylight saving).
  const firstGuess = asUtc - offsetAt(asUtc, timeZone)
  return asUtc - offsetAt(firstGuess, timeZone)
}

/** How many milliseconds the wall clock of the zone is ahead of UTC at that instant. */
function offsetAt(instant: number, timeZone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
  }).formatToParts(new Date(instant))
  const value = (type: string) => Number(parts.find((part) => part.type === type)?.value)
  const wallClockAsUtc = Date.UTC(value('year'), value('month') - 1, value('day'), value('hour'), value('minute'), value('second'))
  return wallClockAsUtc - Math.floor(instant / 1000) * 1000
}
