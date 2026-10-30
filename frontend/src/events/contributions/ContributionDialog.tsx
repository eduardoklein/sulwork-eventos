import { useState } from 'react'
import { createContribution, updateContribution } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import { useAction } from '../../lib/useAction'
import type { Contribution, Item, Participant } from '../../types/api'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { Field } from '../../ui/Field'
import { fieldClass } from '../../ui/styles'

interface Values {
  participantId: string
  itemId: string
  quantity: string
  note: string
}

function validate(values: Values, creating: boolean) {
  const errors: Partial<Record<keyof Values, string>> = {}
  if (creating && !values.participantId) errors.participantId = 'Escolha o participante.'
  if (creating && !values.itemId) errors.itemId = 'Escolha o item.'
  const quantity = Number(values.quantity)
  if (values.quantity === '' || !Number.isInteger(quantity) || quantity < 1) {
    errors.quantity = 'Informe a quantidade, um número inteiro de 1 ou mais.'
  }
  return errors
}

/**
 * Registers a contribution, or edits the one in `contribution`. The person and the item never change, so they are
 * a choice only when registering. The room left in the item is the limit, and the API checks it. It is mounted
 * only while open, so it starts clean each time.
 */
export function ContributionDialog({
  eventId,
  participants,
  items,
  contribution,
  onSaved,
  onClose,
}: {
  eventId: number
  participants: Participant[]
  /** Items that still have room are the ones to choose from, and an edit looks for its own item in all of them. */
  items: Item[]
  contribution?: Contribution
  onSaved: () => void
  onClose: () => void
}) {
  const options = useDomainOptions()
  const { busy, message, fieldErrors: errors, reject, run } = useAction<keyof Values>()
  const [values, setValues] = useState<Values>({
    participantId: '',
    itemId: '',
    quantity: contribution ? String(contribution.quantity) : '',
    note: contribution?.note ?? '',
  })
  const creating = !contribution

  const item = items.find((candidate) => String(candidate.id) === (contribution ? String(contribution.itemId) : values.itemId))
  const room = item && (contribution ? item.pendingQuantity + contribution.quantity : item.pendingQuantity)
  const unit = item && labelFor(options?.measureUnits, item.unit).toLowerCase()
  const choices = items.filter((candidate) => candidate.pendingQuantity > 0)

  const set = (key: keyof Values, value: string) => setValues((current) => ({ ...current, [key]: value }))

  const save = () => {
    const missing = validate(values, creating)
    if (Object.keys(missing).length > 0) {
      reject('Corrija os campos destacados.', missing)
      return
    }
    const quantity = Number(values.quantity)
    const note = values.note.trim() || null
    void run(
      () =>
        contribution
          ? updateContribution(eventId, contribution.id, { quantity, note })
          : createContribution(eventId, { participantId: Number(values.participantId), itemId: Number(values.itemId), quantity, note }),
      {
        done: contribution ? 'Contribuição salva.' : 'Contribuição registrada.',
        failed: 'Não foi possível salvar a contribuição.',
        onDone: () => {
          onSaved()
          onClose()
        },
      },
    )
  }

  return (
    <ConfirmDialog
      open
      title={contribution ? 'Editar contribuição' : 'Registrar contribuição'}
      confirmLabel="Salvar contribuição"
      busy={busy}
      error={message}
      onConfirm={save}
      onCancel={onClose}
    >
      {contribution ? (
        <>
          <p>
            Participante: <span className="font-semibold">{contribution.participantName}</span>
          </p>
          <p>
            Item: <span className="font-semibold">{contribution.itemName}</span>
          </p>
        </>
      ) : (
        <>
          <Field label="Participante" error={errors.participantId}>
            {(control) => (
              <select {...control} value={values.participantId} onChange={(e) => set('participantId', e.target.value)} className={fieldClass}>
                <option value="">Escolha o participante</option>
                {participants.map((participant) => (
                  <option key={participant.id} value={participant.id}>
                    {participant.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Item" error={errors.itemId}>
            {(control) => (
              <select {...control} value={values.itemId} onChange={(e) => set('itemId', e.target.value)} className={fieldClass}>
                <option value="">Escolha o item</option>
                {choices.map((choice) => (
                  <option key={choice.id} value={choice.id}>
                    {choice.name} (faltam {choice.pendingQuantity})
                  </option>
                ))}
              </select>
            )}
          </Field>
        </>
      )}
      <Field
        label="Quantidade"
        hint={room === undefined ? 'Escolha o item para ver quanto cabe.' : `${contribution ? 'O máximo é' : 'Cabem mais'} ${room} (${unit}).`}
        error={errors.quantity}
      >
        {(control) => (
          <input {...control} type="number" min={1} step={1} value={values.quantity} onChange={(e) => set('quantity', e.target.value)} className={fieldClass} />
        )}
      </Field>
      <Field label="Observação (opcional)" error={errors.note}>
        {(control) => <textarea {...control} rows={2} maxLength={255} value={values.note} onChange={(e) => set('note', e.target.value)} className={fieldClass} />}
      </Field>
    </ConfirmDialog>
  )
}
