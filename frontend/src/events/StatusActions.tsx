import { useState } from 'react'
import { ApiError, changeEventStatus } from '../api/client'
import { useNotify } from '../notifications/context'
import type { EventStatus, FoodEvent } from '../types/api'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { Field } from '../ui/Field'
import { buttonClass, fieldClass } from '../ui/styles'
import { withSeconds } from './eventFormValues'
import { STATUS_ACTIONS } from './statusLabels'

/**
 * One button for each change the API allows, so the screen never offers a button that fails. A change that has
 * no way back asks first. Opening a draft whose deadline has passed asks for the new deadline.
 */
export function StatusActions({ event, onChanged }: { event: FoodEvent; onChanged: () => void }) {
  const notify = useNotify()
  const [asking, setAsking] = useState<EventStatus | null>(null)
  const [deadline, setDeadline] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const targets = event.allowedTransitions.filter((target) => STATUS_ACTIONS[target])
  const action = asking ? STATUS_ACTIONS[asking] : undefined
  const needsDeadline = asking === 'ABERTO'

  const change = async (target: EventStatus, newChangeDeadline?: string) => {
    setBusy(true)
    try {
      await changeEventStatus(event.id, { newStatus: target, newChangeDeadline })
      notify(STATUS_ACTIONS[target]!.done, 'success')
      setAsking(null)
      onChanged()
    } catch (failure) {
      const message = failure instanceof ApiError ? failure.message : 'Não foi possível mudar a situação do evento.'
      if (asking) setError(message)
      else notify(message, 'error')
    } finally {
      setBusy(false)
    }
  }

  const choose = (target: EventStatus) => {
    const needsQuestion = Boolean(STATUS_ACTIONS[target]!.confirm) || (target === 'ABERTO' && event.deadlineExpired)
    if (!needsQuestion) {
      void change(target)
      return
    }
    setDeadline(event.changeDeadline.slice(0, 16))
    setError(null)
    setAsking(target)
  }

  const confirm = () => {
    if (!asking) return
    if (needsDeadline && !deadline) {
      setError('Informe o novo prazo.')
      return
    }
    void change(asking, needsDeadline ? withSeconds(deadline) : undefined)
  }

  if (targets.length === 0) return null
  return (
    <>
      {targets.map((target) => (
        <button
          key={target}
          type="button"
          disabled={busy}
          onClick={() => choose(target)}
          className={buttonClass(STATUS_ACTIONS[target]!.danger ? 'danger' : 'secondary')}
        >
          {STATUS_ACTIONS[target]!.label}
        </button>
      ))}
      <ConfirmDialog
        open={asking !== null}
        title={action?.label ?? ''}
        confirmLabel={action?.label ?? ''}
        danger={action?.danger}
        busy={busy}
        error={error}
        onConfirm={confirm}
        onCancel={() => setAsking(null)}
      >
        {needsDeadline ? (
          <>
            <p>O prazo para contribuições já passou. Escolha um novo prazo para abrir o evento.</p>
            <Field label="Novo prazo para contribuições">
              {(control) => (
                <input {...control} type="datetime-local" value={deadline} onChange={(e) => setDeadline(e.target.value)} className={fieldClass} />
              )}
            </Field>
          </>
        ) : (
          <p>{action?.confirm}</p>
        )}
      </ConfirmDialog>
    </>
  )
}
