import { useState } from 'react'
import { enrollParticipant } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import { formatCpf, isValidCpf, onlyDigits } from '../../lib/cpf'
import { useAction } from '../../lib/useAction'
import type { DietaryRestriction, Participant } from '../../types/api'
import { CheckboxGroup } from '../../ui/CheckboxGroup'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { Field } from '../../ui/Field'
import { TextField } from '../../ui/TextField'
import { fieldClass } from '../../ui/styles'
import { useCollaboratorLookup } from './useCollaboratorLookup'

/**
 * Enrolls a person in two steps. The CPF comes first and alone, because it tells if the person is already
 * registered. A registered person keeps the data of the registration, and a new one fills in name and restrictions.
 * It is mounted only while open, so it starts clean each time.
 */
export function EnrollDialog({
  eventId,
  participants,
  onSaved,
  onClose,
}: {
  eventId: number
  participants: Participant[]
  onSaved: () => void
  onClose: () => void
}) {
  const options = useDomainOptions()
  const { busy, message, fieldErrors, reject, run } = useAction<'name'>()
  const [cpf, setCpf] = useState('')
  const [name, setName] = useState('')
  const [restrictions, setRestrictions] = useState<DietaryRestriction[]>([])
  const lookup = useCollaboratorLookup(cpf)

  const alreadyEnrolled = lookup.status === 'found' && participants.some((participant) => participant.collaboratorId === lookup.collaborator.id)
  const complete = onlyDigits(cpf).length === 11
  const cpfError = complete && !isValidCpf(cpf) ? 'CPF inválido. Confira os números.' : undefined
  const ready = (lookup.status === 'found' && !alreadyEnrolled) || lookup.status === 'new'

  const enroll = () => {
    if (lookup.status === 'new' && !name.trim()) {
      reject(null, { name: 'Informe o nome da pessoa.' })
      return
    }
    const digits = onlyDigits(cpf)
    void run(() => enrollParticipant(eventId, lookup.status === 'new' ? { cpf: digits, name: name.trim(), restrictions } : { cpf: digits }), {
      done: 'Participante inscrito.',
      failed: 'Não foi possível inscrever o participante.',
      onDone: () => {
        onSaved()
        onClose()
      },
    })
  }

  return (
    <ConfirmDialog
      open
      title="Inscrever participante"
      confirmLabel="Inscrever"
      confirmDisabled={!ready}
      busy={busy}
      error={message}
      onConfirm={enroll}
      onCancel={onClose}
    >
      <Field label="CPF" hint={cpfError ? undefined : 'Digite o CPF. Com ele, o sistema vê se a pessoa já tem cadastro.'} error={cpfError}>
        {(control) => (
          <input
            {...control}
            type="text"
            inputMode="numeric"
            autoComplete="off"
            value={cpf}
            onChange={(e) => setCpf(formatCpf(e.target.value))}
            className={fieldClass}
          />
        )}
      </Field>

      {lookup.status === 'searching' && <p role="status">Procurando o cadastro…</p>}
      {lookup.status === 'error' && (
        <p role="alert" className="rounded-md border border-danger bg-danger-soft px-3 py-2 text-danger-ink">
          {lookup.message}
        </p>
      )}
      {lookup.status === 'found' && (
        <div className="space-y-2 rounded-md border border-line-strong bg-paper px-4 py-3">
          <p className="font-semibold">Pessoa já cadastrada. Os dados do cadastro serão mantidos.</p>
          <p>
            Nome: <span className="font-semibold">{lookup.collaborator.name}</span>
          </p>
          <p>
            Restrições:{' '}
            {lookup.collaborator.restrictions.length > 0
              ? lookup.collaborator.restrictions.map((code) => labelFor(options?.dietaryRestrictions, code)).join(', ')
              : 'nenhuma'}
          </p>
          {alreadyEnrolled && (
            <p role="alert" className="font-semibold text-danger-ink">
              Esta pessoa já está inscrita neste evento.
            </p>
          )}
        </div>
      )}
      {lookup.status === 'new' && (
        <>
          <p className="font-semibold">CPF novo. Preencha o cadastro da pessoa.</p>
          <TextField label="Nome" error={fieldErrors.name} maxLength={120} value={name} onChange={setName} />
          {options && (
            <CheckboxGroup legend="Restrições alimentares" options={options.dietaryRestrictions} selected={restrictions} onChange={setRestrictions} />
          )}
        </>
      )}
    </ConfirmDialog>
  )
}
