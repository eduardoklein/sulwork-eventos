import { useState } from 'react'
import { removeParticipant } from '../../api/client'
import type { Contribution, DietaryAnalysis, FoodEvent, Participant } from '../../types/api'
import { EmptyState } from '../../ui/EmptyState'
import { Notice } from '../../ui/Notice'
import { Section } from '../../ui/Section'
import { buttonClass } from '../../ui/styles'
import { DeleteDialog } from '../menu/DeleteDialog'
import { DietaryPanel } from './DietaryPanel'
import { EditCollaboratorDialog } from './EditCollaboratorDialog'
import { EnrollDialog } from './EnrollDialog'
import { ParticipantRow } from './ParticipantRow'

type Dialog = { kind: 'enroll' } | { kind: 'edit'; participant: Participant } | { kind: 'remove'; participant: Participant } | null

/** Only an open event takes changes to its participants. The backend has the same rule and refuses the rest. */
const participantsEditable = (event: FoodEvent) => event.status === 'ABERTO'

export function ParticipantsTab({
  event,
  participants,
  contributions,
  analysis,
  onViewMenu,
  onChanged,
}: {
  event: FoodEvent
  participants: Participant[]
  contributions: Contribution[]
  analysis: DietaryAnalysis
  onViewMenu: (participantId: number) => void
  onChanged: () => void
}) {
  const [dialog, setDialog] = useState<Dialog>(null)
  const close = () => setDialog(null)
  const editable = participantsEditable(event)
  const enrollButton = editable ? (
    <button type="button" onClick={() => setDialog({ kind: 'enroll' })} className={buttonClass('primary')}>
      Inscrever participante
    </button>
  ) : undefined
  const removing = dialog?.kind === 'remove' ? dialog.participant : null
  const lostContributions = removing ? contributions.filter((contribution) => contribution.participantId === removing.id).length : 0

  return (
    <div>
      {!editable && (
        <Notice>Só um evento aberto aceita mudanças nos participantes.</Notice>
      )}
      <Section title="Participantes" action={participants.length > 0 ? enrollButton : undefined}>
        {participants.length === 0 ? (
          <EmptyState
            title="Nenhum participante ainda"
            text={editable ? 'Inscreva as pessoas que vão ao evento. Com elas, o sistema confere as restrições alimentares.' : 'Este evento não tem participantes.'}
            action={enrollButton}
          />
        ) : (
          <ul className="divide-y divide-line border-y border-line bg-surface">
            {participants.map((participant) => (
              <ParticipantRow
                key={participant.id}
                participant={participant}
                diet={analysis.participants.find((entry) => entry.participantId === participant.id)}
                editable={editable}
                onViewMenu={() => onViewMenu(participant.id)}
                onEdit={() => setDialog({ kind: 'edit', participant })}
                onRemove={() => setDialog({ kind: 'remove', participant })}
              />
            ))}
          </ul>
        )}
      </Section>
      <DietaryPanel analysis={analysis} />
      {dialog?.kind === 'enroll' && <EnrollDialog eventId={event.id} participants={participants} onSaved={onChanged} onClose={close} />}
      {dialog?.kind === 'edit' && <EditCollaboratorDialog participant={dialog.participant} onSaved={onChanged} onClose={close} />}
      {removing && (
        <DeleteDialog
          title="Remover este participante?"
          text={
            lostContributions > 0
              ? `${removing.name} sai do evento, e ${lostContributions === 1 ? 'a contribuição dessa pessoa também é removida' : `as ${lostContributions} contribuições dessa pessoa também são removidas`}.`
              : `${removing.name} sai do evento.`
          }
          done="Participante removido."
          confirmLabel="Remover"
          remove={() => removeParticipant(event.id, removing.id)}
          onDeleted={onChanged}
          onClose={close}
        />
      )}
    </div>
  )
}
