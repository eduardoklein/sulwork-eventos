import type { AlertLevel, DietaryAnalysis } from '../../types/api'
import { Chip, type ChipTone } from '../../ui/Chip'
import { Section } from '../../ui/Section'

// The alert levels are not in the domain options, so their words live here.
const LEVELS: Record<AlertLevel, { label: string; tone: ChipTone }> = {
  CRITICAL: { label: 'Crítico', tone: 'danger' },
  WARNING: { label: 'Atenção', tone: 'warn' },
  INFO: { label: 'Aviso', tone: 'neutral' },
}

/** The problem as a whole: the alerts of the event, and for each restriction how many people it has and what they can eat. */
export function DietaryPanel({ analysis }: { analysis: DietaryAnalysis }) {
  if (analysis.alerts.length === 0 && analysis.restrictions.length === 0) return null
  return (
    <Section title="Restrições alimentares do evento" description="O que as restrições dos participantes pedem do cardápio.">
      {analysis.alerts.length > 0 && (
        <ul className="mb-4 space-y-2">
          {analysis.alerts.map((alert, index) => (
            <li key={index} className="flex flex-wrap items-center gap-2">
              <Chip tone={LEVELS[alert.level].tone}>{LEVELS[alert.level].label}</Chip>
              <span>{alert.message}</span>
            </li>
          ))}
        </ul>
      )}
      {analysis.restrictions.length > 0 && (
        <ul className="divide-y divide-line border-y border-line bg-surface">
          {analysis.restrictions.map((summary) => (
            <li key={summary.restriction} className="px-4 py-3">
              <p className="font-semibold">{summary.description}</p>
              <p className="text-sm text-ink-soft">
                {summary.peopleCount === 1 ? '1 pessoa' : `${summary.peopleCount} pessoas`} · {summary.compatibleItems} de {summary.totalItems}{' '}
                {summary.totalItems === 1 ? 'item serve' : 'itens servem'}
              </p>
            </li>
          ))}
        </ul>
      )}
    </Section>
  )
}
