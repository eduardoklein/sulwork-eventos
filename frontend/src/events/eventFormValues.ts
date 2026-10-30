import type { CreateEventRequest, EventStatus, EventType, FoodEvent, UpdateEventRequest } from '../types/api'
import { browserTimeZone } from './zones'

/** What the form holds. The dates are in the format of the `datetime-local` field: `YYYY-MM-DDTHH:mm`. */
export interface EventFormValues {
  name: string
  description: string
  type: EventType | ''
  status: Extract<EventStatus, 'RASCUNHO' | 'ABERTO'>
  dateTime: string
  changeDeadline: string
  timeZone: string
}

export type FormErrors = Partial<Record<keyof EventFormValues, string>>

export function emptyValues(): EventFormValues {
  return { name: '', description: '', type: '', status: 'RASCUNHO', dateTime: '', changeDeadline: '', timeZone: browserTimeZone() }
}

export function valuesOf(event: FoodEvent): EventFormValues {
  return {
    name: event.name,
    description: event.description ?? '',
    type: event.type,
    status: 'RASCUNHO',
    dateTime: event.dateTime.slice(0, 16),
    changeDeadline: event.changeDeadline.slice(0, 16),
    timeZone: event.timeZone,
  }
}

/** What the person can still change in an event, by its situation. */
export type EditRule = 'free' | 'deadline-frozen' | 'locked'

export function editRuleFor(status: EventStatus): EditRule {
  if (status === 'REALIZADO' || status === 'CANCELADO') return 'locked'
  return status === 'ENCERRADO' ? 'deadline-frozen' : 'free'
}

/** The checks that need no server: a required field that is empty. The rest of the rules come from the API. */
export function validate(values: EventFormValues): FormErrors {
  const errors: FormErrors = {}
  if (!values.name.trim()) errors.name = 'Informe o nome do evento.'
  if (!values.type) errors.type = 'Escolha o tipo do evento.'
  if (!values.dateTime) errors.dateTime = 'Informe a data e a hora do evento.'
  if (!values.changeDeadline) errors.changeDeadline = 'Informe o prazo para contribuições.'
  return errors
}

export const withSeconds = (local: string) => (local.length === 16 ? `${local}:00` : local)

function bodyOf(values: EventFormValues): UpdateEventRequest {
  return {
    name: values.name.trim(),
    description: values.description.trim() || null,
    type: values.type as EventType,
    dateTime: withSeconds(values.dateTime),
    changeDeadline: withSeconds(values.changeDeadline),
    timeZone: values.timeZone,
  }
}

export const toCreateRequest = (values: EventFormValues): CreateEventRequest => ({ ...bodyOf(values), status: values.status })
export const toUpdateRequest = (values: EventFormValues): UpdateEventRequest => bodyOf(values)
