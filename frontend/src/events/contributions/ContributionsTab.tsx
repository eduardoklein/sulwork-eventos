import { useState } from 'react'
import { removeContribution } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import type { Contribution, FoodEvent, Item, Participant } from '../../types/api'
import { EmptyState } from '../../ui/EmptyState'
import { Notice } from '../../ui/Notice'
import { Section } from '../../ui/Section'
import { buttonClass } from '../../ui/styles'
import { DeleteDialog } from '../menu/DeleteDialog'
import { ContributionDialog } from './ContributionDialog'
import { ContributionsTable } from './ContributionsTable'
import { lockReason } from './lockReason'

type Dialog = { kind: 'form'; contribution?: Contribution } | { kind: 'remove'; contribution: Contribution } | null

export function ContributionsTab({
  event,
  items,
  participants,
  contributions,
  onChanged,
}: {
  event: FoodEvent
  items: Item[]
  participants: Participant[]
  contributions: Contribution[]
  onChanged: () => void
}) {
  const options = useDomainOptions()
  const [dialog, setDialog] = useState<Dialog>(null)
  const close = () => setDialog(null)
  const reason = lockReason(event, options)
  const editable = reason === null

  // What stops a new contribution when the event is open: nobody to contribute, or nothing left to bring.
  const blocker =
    participants.length === 0
      ? 'Inscreva participantes na aba Participantes para registrar contribuições.'
      : !items.some((item) => item.pendingQuantity > 0)
        ? 'Todos os itens do cardápio já estão completos, ou o cardápio não tem itens.'
        : null
  const registerButton =
    editable && !blocker ? (
      <button type="button" onClick={() => setDialog({ kind: 'form' })} className={buttonClass('primary')}>
        Registrar contribuição
      </button>
    ) : undefined

  const removing = dialog?.kind === 'remove' ? dialog.contribution : null

  return (
    <div>
      {reason && <Notice>{reason}</Notice>}
      {editable && blocker && <Notice>{blocker}</Notice>}
      <Section title="Contribuições" description="Quem leva o quê." action={contributions.length > 0 ? registerButton : undefined}>
        {contributions.length === 0 ? (
          <EmptyState
            title="Nenhuma contribuição ainda"
            text={editable ? 'Registre quem vai levar cada item do cardápio.' : 'Este evento não tem contribuições.'}
            action={registerButton}
          />
        ) : (
          <ContributionsTable
            contributions={contributions}
            editable={editable}
            onEdit={(contribution) => setDialog({ kind: 'form', contribution })}
            onRemove={(contribution) => setDialog({ kind: 'remove', contribution })}
          />
        )}
      </Section>
      {dialog?.kind === 'form' && (
        <ContributionDialog eventId={event.id} participants={participants} items={items} contribution={dialog.contribution} onSaved={onChanged} onClose={close} />
      )}
      {removing && (
        <DeleteDialog
          title="Remover esta contribuição?"
          text={`${removing.participantName} deixa de levar ${removing.quantity} (${labelFor(options?.measureUnits, removing.unit).toLowerCase()}) de ${removing.itemName}.`}
          done="Contribuição removida."
          confirmLabel="Remover"
          remove={() => removeContribution(event.id, removing.id)}
          onDeleted={onChanged}
          onClose={close}
        />
      )}
    </div>
  )
}
