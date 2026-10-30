import { useState } from 'react'
import { createContribution, getSuggestions } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import { useAction } from '../../lib/useAction'
import { useRemote } from '../../lib/useRemote'
import type { Item, MeasureUnit, Participant, Suggestion } from '../../types/api'
import { Chip } from '../../ui/Chip'
import { ErrorPanel } from '../../ui/ErrorPanel'
import { Field } from '../../ui/Field'
import { Notice } from '../../ui/Notice'
import { Section } from '../../ui/Section'
import { buttonClass, fieldClass } from '../../ui/styles'

/** What still lacks, and for the chosen person, what to bring first. `lockReason` is set when nothing can be registered. */
export function SuggestionsSection({
  eventId,
  participants,
  items,
  lockReason,
  version,
  onTaken,
}: {
  eventId: number
  participants: Participant[]
  items: Item[]
  lockReason: string | null
  version: number
  onTaken: () => void
}) {
  const [participantId, setParticipantId] = useState<number | null>(null)
  const { busy, message, run } = useAction()
  const participant = participants.find((candidate) => candidate.id === participantId)

  const take = (suggestion: Suggestion) =>
    run(() => createContribution(eventId, { participantId: participantId!, itemId: suggestion.itemId, quantity: suggestion.suggestedQuantity }), {
      done: 'Contribuição registrada.',
      failed: 'Não foi possível registrar a contribuição.',
      onDone: onTaken,
      staysOnScreen: true,
    })

  return (
    <Section title="O que ainda falta levar" description="Escolha quem vai levar e veja o que mais ajuda o evento.">
      {lockReason ? (
        <Notice>{lockReason}</Notice>
      ) : participants.length === 0 ? (
        <p className="text-ink-soft">Inscreva participantes para ver o que cada um pode levar.</p>
      ) : (
        <>
          <div className="max-w-sm">
            <Field label="Quem vai levar?">
              {(control) => (
                <select {...control} value={participantId ?? ''} onChange={(e) => setParticipantId(e.target.value ? Number(e.target.value) : null)} className={fieldClass}>
                  <option value="">Escolha o participante</option>
                  {participants.map((candidate) => (
                    <option key={candidate.id} value={candidate.id}>
                      {candidate.name}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>
          {message && (
            <p role="alert" className="mt-4 rounded-md border border-danger bg-danger-soft px-3 py-2 text-danger-ink">
              {message}
            </p>
          )}
          {participant ? (
            <Suggestions key={participant.id} eventId={eventId} participant={participant} version={version} busy={busy} onTake={take} />
          ) : (
            <MissingItems items={items} />
          )}
        </>
      )}
    </Section>
  )
}

/** Before anybody is chosen: what lacks, with the buttons off. The suggestion depends on the person. */
function MissingItems({ items }: { items: Item[] }) {
  const options = useDomainOptions()
  const missing = items
    .filter((item) => item.pendingQuantity > 0)
    .sort((a, b) => Number(b.mandatory) - Number(a.mandatory) || a.coveredPercentage - b.coveredPercentage)
  if (missing.length === 0) return <p className="mt-4 text-ink-soft">Todos os itens já estão completos.</p>
  return (
    <>
      <p className="mt-4 text-sm text-ink-soft">Escolha um participante para ver a sugestão de quanto levar.</p>
      <ul className="mt-2 divide-y divide-line border-y border-line bg-surface">
        {missing.map((item) => (
          <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
            <div>
              <p className="font-semibold">{item.name}</p>
              <p className="text-sm text-ink-soft">
                Faltam {item.pendingQuantity} ({labelFor(options?.measureUnits, item.unit).toLowerCase()}) · {item.mandatory ? 'obrigatório' : 'opcional'}
              </p>
            </div>
            <button type="button" disabled aria-label={`Levar ${item.name}`} className={buttonClass('secondary')}>
              Levar
            </button>
          </li>
        ))}
      </ul>
    </>
  )
}

function Suggestions({
  eventId,
  participant,
  version,
  busy,
  onTake,
}: {
  eventId: number
  participant: Participant
  version: number
  busy: boolean
  onTake: (suggestion: Suggestion) => void
}) {
  const options = useDomainOptions()
  const { state, retry } = useRemote(() => getSuggestions(eventId, participant.id), 'suggestions', version, 'Não foi possível carregar as sugestões.')

  if (state.status === 'loading') {
    return (
      <div role="status" aria-busy="true" className="mt-4">
        <span className="sr-only">Carregando as sugestões</span>
        <div aria-hidden="true" className="h-16 animate-pulse rounded bg-line" />
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="mt-4">
        <ErrorPanel message={state.message} onRetry={retry} />
      </div>
    )
  }
  if (state.data.length === 0) return <p className="mt-4 text-ink-soft">Nada falta para {participant.name} levar.</p>
  const unitOf = (unit: MeasureUnit) => labelFor(options?.measureUnits, unit).toLowerCase()
  return (
    <ul className="mt-4 divide-y divide-line border-y border-line bg-surface">
      {state.data.map((suggestion) => (
        <li key={suggestion.itemId} className="px-4 py-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="flex flex-wrap items-center gap-2 font-semibold">
                {suggestion.itemName}
                {suggestion.safeForParticipant === false && <Chip tone="danger">Não é seguro para {participant.name}</Chip>}
                {suggestion.safeForParticipant === true && <Chip tone="ok">Seguro para {participant.name}</Chip>}
              </p>
              <p className="text-sm text-ink-soft">
                {suggestion.categoryName} · faltam {suggestion.pendingQuantity} ({unitOf(suggestion.unit)}) · prioridade {suggestion.priority} de 100
              </p>
              {suggestion.reasons.length > 0 && <p className="text-sm text-ink-soft">{suggestion.reasons.join(' ')}</p>}
            </div>
            <button
              type="button"
              disabled={busy}
              aria-label={`Levar ${suggestion.suggestedQuantity} (${unitOf(suggestion.unit)}) de ${suggestion.itemName}`}
              onClick={() => onTake(suggestion)}
              className={buttonClass('primary')}
            >
              Levar {suggestion.suggestedQuantity} ({unitOf(suggestion.unit)})
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
