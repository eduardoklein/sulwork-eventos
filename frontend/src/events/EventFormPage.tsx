import { useEffect, useRef, useState } from 'react'
import { createEvent, updateEvent } from '../api/client'
import { useAction } from '../lib/useAction'
import { routeHash } from '../router/route'
import type { FoodEvent } from '../types/api'
import { ErrorPanel } from '../ui/ErrorPanel'
import { buttonClass } from '../ui/styles'
import { EventForm } from './EventForm'
import {
  editRuleFor,
  type EditRule,
  emptyValues,
  toCreateRequest,
  toUpdateRequest,
  validate,
  valuesOf,
  type EventFormValues,
} from './eventFormValues'
import { useEvent } from './useEvent'

const alertClass = 'rounded-lg border border-danger bg-danger-soft px-6 py-5 text-danger-ink'

/** Creates an event, or edits the one in `eventId`. */
export function EventFormPage({ eventId }: { eventId?: number }) {
  if (eventId === undefined) {
    return <NewEvent />
  }
  return <EditEvent eventId={eventId} />
}

function NewEvent() {
  const [initial] = useState(emptyValues)
  return <SaveEvent initial={initial} creating rule="free" submitLabel="Criar evento" save={(values) => createEvent(toCreateRequest(values))} savedText="Evento criado." />
}

function EditEvent({ eventId }: { eventId: number }) {
  const { state, retry } = useEvent(eventId)

  if (state.status === 'loading') {
    return (
      <p role="status" aria-busy="true">
        Carregando o evento…
      </p>
    )
  }
  if (state.status === 'error') {
    return (
      <ErrorPanel message={state.message} onRetry={retry} />
    )
  }
  const rule = editRuleFor(state.event.status)
  if (rule === 'locked') {
    return (
      <div className="max-w-2xl rounded-lg border border-line-strong bg-surface px-6 py-5">
        <p className="font-semibold">Este evento não pode ser editado.</p>
        <p className="mt-1 text-sm text-ink-soft">Um evento realizado ou cancelado fica como está.</p>
        <a href={routeHash({ name: 'event', id: eventId, tab: 'summary' })} className={`${buttonClass('secondary')} mt-3`}>
          Voltar ao evento
        </a>
      </div>
    )
  }
  const { event } = state
  return (
    <SaveEvent
      initial={valuesOf(event)}
      creating={false}
      rule={rule}
      submitLabel="Salvar"
      save={(values) => updateEvent(event.id, toUpdateRequest(values))}
      savedText="Evento salvo."
    />
  )
}

function SaveEvent({
  initial,
  creating,
  rule,
  submitLabel,
  save,
  savedText,
}: {
  initial: EventFormValues
  creating: boolean
  rule: EditRule
  submitLabel: string
  save: (values: EventFormValues) => Promise<FoodEvent>
  savedText: string
}) {
  const { busy, message, fieldErrors, reject, run } = useAction<keyof EventFormValues>()
  const messageRef = useRef<HTMLDivElement>(null)

  // The message is out of the person's sight when the form is long: move the focus to it.
  useEffect(() => {
    if (message) messageRef.current?.focus()
  }, [message])

  const submit = (values: EventFormValues) => {
    const missing = validate(values)
    if (Object.keys(missing).length > 0) {
      reject('Corrija os campos destacados.', missing)
      return
    }
    void run(() => save(values), {
      done: savedText,
      failed: 'Não foi possível salvar o evento.',
      onDone: (saved) => {
        window.location.hash = routeHash({ name: 'event', id: saved.id, tab: 'summary' })
      },
    })
  }

  return (
    <>
      {message && (
        <div ref={messageRef} tabIndex={-1} role="alert" className={`${alertClass} mb-5 max-w-2xl py-3 outline-none`}>
          {message}
        </div>
      )}
      <EventForm initial={initial} errors={fieldErrors} saving={busy} submitLabel={submitLabel} creating={creating} rule={rule} onSubmit={submit} />
    </>
  )
}
