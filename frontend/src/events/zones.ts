export interface ZoneChoice {
  id: string
  label: string
}

const BRAZIL_ZONES: ZoneChoice[] = [
  { id: 'America/Noronha', label: 'Fernando de Noronha (UTC-2)' },
  { id: 'America/Sao_Paulo', label: 'Brasília (UTC-3)' },
  { id: 'America/Manaus', label: 'Manaus (UTC-4)' },
  { id: 'America/Rio_Branco', label: 'Rio Branco (UTC-5)' },
]

const FALLBACK_ZONE = 'America/Sao_Paulo'

function offsetOf(zone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: zone, timeZoneName: 'longOffset' }).formatToParts(new Date())
  return parts.find((part) => part.type === 'timeZoneName')?.value ?? ''
}

/**
 * The zone of the list that fits the browser, which is where the person most likely is. A browser zone that is
 * not in the list (Recife, Cuiabá) takes the zone of the list with the same offset, and Brasília when none has it.
 */
export function browserTimeZone(): string {
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || FALLBACK_ZONE
  if (BRAZIL_ZONES.some((choice) => choice.id === zone)) return zone
  return BRAZIL_ZONES.find((choice) => offsetOf(choice.id) === offsetOf(zone))?.id ?? FALLBACK_ZONE
}

/** A clear name for a zone. A zone outside the short list shows its own id. */
export function zoneLabel(id: string): string {
  return BRAZIL_ZONES.find((zone) => zone.id === id)?.label ?? id
}

/** The short list. A zone that is already chosen but is not in it (an event in Lisbon) is added, so it is not lost. */
export function zoneChoices(current: string): ZoneChoice[] {
  return BRAZIL_ZONES.some((zone) => zone.id === current) ? BRAZIL_ZONES : [...BRAZIL_ZONES, { id: current, label: current }]
}
