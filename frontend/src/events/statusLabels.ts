import type { EventStatus } from '../types/api'

export interface StatusAction {
  /** The name of the button: what the person does, not the name of the new situation. */
  label: string
  /** The notice after the change. */
  done: string
  /** The question before a change that has no way back. */
  confirm?: string
  danger?: boolean
}

/** By the situation the event goes to. A new event never goes back to RASCUNHO, so it has no action. */
export const STATUS_ACTIONS: Partial<Record<EventStatus, StatusAction>> = {
  ABERTO: { label: 'Abrir para contribuições', done: 'Evento aberto para contribuições.' },
  ENCERRADO: { label: 'Encerrar contribuições', done: 'Contribuições encerradas.' },
  REALIZADO: {
    label: 'Marcar como realizado',
    done: 'Evento marcado como realizado.',
    confirm: 'Depois de realizado, o evento não muda mais.',
  },
  CANCELADO: {
    label: 'Cancelar evento',
    done: 'Evento cancelado.',
    confirm: 'Um evento cancelado não pode ser reaberto.',
    danger: true,
  },
}
