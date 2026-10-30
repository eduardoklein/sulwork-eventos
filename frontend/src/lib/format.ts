// Dates come from the API as wall-clock text in the event time zone (`2026-12-18T12:00:00`). They are never turned
// into a `Date`, so the browser time zone cannot move them.

/** 18/12/2026 às 12:00 */
export function formatDateTime(local: string): string {
  const [date, time = '00:00'] = local.split('T')
  return `${formatDate(date)} às ${time.slice(0, 5)}`
}

/** 18/12/2026 */
export function formatDate(local: string): string {
  const [year, month, day] = local.split('T')[0].split('-')
  return `${day}/${month}/${year}`
}
