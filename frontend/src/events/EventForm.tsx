import { useState, type FormEvent } from 'react'
import { labelFor, useDomainOptions } from '../domain/context'
import type { EventStatus } from '../types/api'
import { Field } from '../ui/Field'
import { buttonClass, fieldClass } from '../ui/styles'
import { type EditRule, type EventFormValues, type FormErrors } from './eventFormValues'
import { zoneChoices, zoneLabel } from './zones'

const NEW_EVENT_STATUSES: EventFormValues['status'][] = ['RASCUNHO', 'ABERTO']

/** The fields of an event. It holds only what is typed. Saving, and what happens next, belong to the page. */
export function EventForm({
  initial,
  errors,
  saving,
  submitLabel,
  creating,
  rule,
  onSubmit,
}: {
  initial: EventFormValues
  errors: FormErrors
  saving: boolean
  submitLabel: string
  creating: boolean
  rule: EditRule
  onSubmit: (values: EventFormValues) => void
}) {
  const options = useDomainOptions()
  const [values, setValues] = useState(initial)
  const [changingZone, setChangingZone] = useState(false)
  const frozen = rule === 'deadline-frozen'

  const set = <Key extends keyof EventFormValues>(key: Key, value: EventFormValues[Key]) =>
    setValues((current) => ({ ...current, [key]: value }))

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit(values)
  }

  return (
    <form onSubmit={submit} noValidate className="max-w-2xl space-y-5">
      <Field label="Nome" error={errors.name}>
        {(control) => (
          <input {...control} type="text" maxLength={120} value={values.name} onChange={(e) => set('name', e.target.value)} className={fieldClass} />
        )}
      </Field>

      <Field label="Descrição (opcional)" error={errors.description}>
        {(control) => (
          <textarea {...control} rows={3} maxLength={1000} value={values.description} onChange={(e) => set('description', e.target.value)} className={fieldClass} />
        )}
      </Field>

      <Field label="Tipo" error={errors.type}>
        {(control) => (
          <select {...control} value={values.type} disabled={!options} onChange={(e) => set('type', e.target.value as EventFormValues['type'])} className={fieldClass}>
            <option value="">{options ? 'Escolha o tipo' : 'Carregando os tipos…'}</option>
            {options?.eventTypes.map((type) => (
              <option key={type.code} value={type.code}>
                {type.description}
              </option>
            ))}
          </select>
        )}
      </Field>

      {creating && (
        <fieldset>
          <legend className="mb-1 text-sm font-semibold text-ink">Situação inicial</legend>
          <div className="flex flex-wrap gap-x-6 gap-y-2">
            {NEW_EVENT_STATUSES.map((status: EventStatus) => (
              <label key={status} className="flex items-center gap-2">
                <input type="radio" name="status" value={status} checked={values.status === status} onChange={() => set('status', status as EventFormValues['status'])} />
                {labelFor(options?.eventStatuses, status)}
              </label>
            ))}
          </div>
          <p className="mt-1 text-sm text-ink-soft">
            Aberto já recebe contribuições e exige um prazo no futuro. Depois, a mudança de situação passa pelos botões do evento.
          </p>
          {errors.status && <p className="mt-1 text-sm text-danger-ink">{errors.status}</p>}
        </fieldset>
      )}

      <Field label="Data e hora do evento" error={errors.dateTime}>
        {(control) => (
          <input {...control} type="datetime-local" value={values.dateTime} onChange={(e) => set('dateTime', e.target.value)} className={fieldClass} />
        )}
      </Field>

      <Field
        label="Prazo para contribuições"
        hint={frozen ? 'Em um evento encerrado, o prazo não pode mudar.' : 'Até quando as pessoas podem mudar o que vão levar.'}
        error={errors.changeDeadline}
      >
        {(control) => (
          <input
            {...control}
            type="datetime-local"
            value={values.changeDeadline}
            disabled={frozen}
            onChange={(e) => set('changeDeadline', e.target.value)}
            className={fieldClass}
          />
        )}
      </Field>

      <div>
        {changingZone && !frozen ? (
          <Field label="Fuso horário" hint="A data e o prazo valem neste fuso." error={errors.timeZone}>
            {(control) => (
              <select {...control} value={values.timeZone} onChange={(e) => set('timeZone', e.target.value)} className={fieldClass}>
                {zoneChoices(values.timeZone).map((zone) => (
                  <option key={zone.id} value={zone.id}>
                    {zone.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
        ) : (
          <p className="text-sm text-ink-soft">
            Fuso horário: <span className="font-semibold text-ink">{zoneLabel(values.timeZone)}</span>
            {frozen ? ' (em um evento encerrado, o fuso não pode mudar)' : null}
            {!frozen && (
              <>
                {' · '}
                <button type="button" onClick={() => setChangingZone(true)} className="font-semibold text-pen underline">
                  Alterar fuso
                </button>
              </>
            )}
            {errors.timeZone && <span className="ml-2 text-danger-ink">{errors.timeZone}</span>}
          </p>
        )}
      </div>

      <div className="flex flex-wrap gap-3 pt-2">
        <button type="submit" disabled={saving} className={buttonClass('primary')}>
          {saving ? 'Salvando…' : submitLabel}
        </button>
        <a href="#/events" className={buttonClass('secondary')}>
          Cancelar
        </a>
      </div>
    </form>
  )
}
