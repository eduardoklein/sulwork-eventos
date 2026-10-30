import { useState } from 'react'
import { ApiError, updateCollaborator } from '../../api/client'
import { useDomainOptions } from '../../domain/context'
import { useNotify } from '../../notifications/context'
import type { DietaryRestriction, Participant } from '../../types/api'
import { CheckboxGroup } from '../../ui/CheckboxGroup'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { Field } from '../../ui/Field'
import { fieldClass } from '../../ui/styles'

/** Edits the registration of a person. It is the only place where a person changes, and the CPF never does. */
export function EditCollaboratorDialog({ participant, onSaved, onClose }: { participant: Participant; onSaved: () => void; onClose: () => void }) {
  const options = useDomainOptions()
  const notify = useNotify()
  const [name, setName] = useState(participant.name)
  const [restrictions, setRestrictions] = useState<DietaryRestriction[]>(participant.restrictions)
  const [nameError, setNameError] = useState<string | undefined>()
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const save = async () => {
    if (!name.trim()) {
      setNameError('Informe o nome da pessoa.')
      return
    }
    setNameError(undefined)
    setMessage(null)
    setBusy(true)
    try {
      await updateCollaborator(participant.collaboratorId, { name: name.trim(), restrictions })
      notify('Cadastro salvo.', 'success')
      onSaved()
      onClose()
    } catch (failure) {
      if (failure instanceof ApiError) {
        setNameError(failure.fieldErrors.name)
        setMessage(failure.message)
      } else {
        setMessage('Não foi possível salvar o cadastro.')
      }
      setBusy(false)
    }
  }

  return (
    <ConfirmDialog open title="Editar cadastro" confirmLabel="Salvar" busy={busy} error={message} onConfirm={save} onCancel={onClose}>
      <p>
        CPF: <span className="font-semibold">{participant.cpf}</span>. O CPF não muda.
      </p>
      <p className="text-sm text-ink-soft">Esta mudança vale para a pessoa em todos os eventos.</p>
      <Field label="Nome" error={nameError}>
        {(control) => <input {...control} type="text" maxLength={120} value={name} onChange={(e) => setName(e.target.value)} className={fieldClass} />}
      </Field>
      {options && (
        <CheckboxGroup legend="Restrições alimentares" options={options.dietaryRestrictions} selected={restrictions} onChange={setRestrictions} />
      )}
    </ConfirmDialog>
  )
}
