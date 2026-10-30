import { useState } from 'react'
import { updateCollaborator } from '../../api/client'
import { useDomainOptions } from '../../domain/context'
import { useAction } from '../../lib/useAction'
import type { DietaryRestriction, Participant } from '../../types/api'
import { CheckboxGroup } from '../../ui/CheckboxGroup'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { TextField } from '../../ui/TextField'

/** Edits the registration of a person. It is the only place where a person changes, and the CPF never does. */
export function EditCollaboratorDialog({ participant, onSaved, onClose }: { participant: Participant; onSaved: () => void; onClose: () => void }) {
  const options = useDomainOptions()
  const { busy, message, fieldErrors, reject, run } = useAction<'name'>()
  const [name, setName] = useState(participant.name)
  const [restrictions, setRestrictions] = useState<DietaryRestriction[]>(participant.restrictions)

  const save = () => {
    if (!name.trim()) {
      reject(null, { name: 'Informe o nome da pessoa.' })
      return
    }
    void run(() => updateCollaborator(participant.collaboratorId, { name: name.trim(), restrictions }), {
      done: 'Cadastro salvo.',
      failed: 'Não foi possível salvar o cadastro.',
      onDone: () => {
        onSaved()
        onClose()
      },
    })
  }

  return (
    <ConfirmDialog open title="Editar cadastro" confirmLabel="Salvar" busy={busy} error={message} onConfirm={save} onCancel={onClose}>
      <p>
        CPF: <span className="font-semibold">{participant.cpf}</span>. O CPF não muda.
      </p>
      <p className="text-sm text-ink-soft">Esta mudança vale para a pessoa em todos os eventos.</p>
      <TextField label="Nome" error={fieldErrors.name} maxLength={120} value={name} onChange={setName} />
      {options && (
        <CheckboxGroup legend="Restrições alimentares" options={options.dietaryRestrictions} selected={restrictions} onChange={setRestrictions} />
      )}
    </ConfirmDialog>
  )
}
