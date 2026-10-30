import type { FormEvent, ReactNode } from 'react'
import { Modal } from './Modal'
import { buttonClass } from './styles'

/** Asks before an action that is hard to undo. An error from the action stays inside, so the person can read it. */
export function ConfirmDialog({
  open,
  title,
  confirmLabel,
  confirmDisabled = false,
  danger = false,
  busy,
  error,
  onConfirm,
  onCancel,
  children,
}: {
  open: boolean
  title: string
  confirmLabel: string
  /** The action is not ready yet, for example while the form still waits for something. */
  confirmDisabled?: boolean
  danger?: boolean
  busy: boolean
  error: string | null
  onConfirm: () => void
  onCancel: () => void
  children: ReactNode
}) {
  const submit = (event: FormEvent) => {
    event.preventDefault()
    onConfirm()
  }
  return (
    <Modal open={open} title={title} onClose={busy ? () => {} : onCancel}>
      <form onSubmit={submit} noValidate className="space-y-4">
        {children}
        {error && (
          <p role="alert" className="rounded-md border border-danger bg-danger-soft px-3 py-2 text-danger-ink">
            {error}
          </p>
        )}
        <div className="flex flex-wrap justify-end gap-3">
          <button type="button" onClick={onCancel} disabled={busy} className={buttonClass('secondary')}>
            Voltar
          </button>
          <button type="submit" disabled={busy || confirmDisabled} className={buttonClass(danger ? 'danger' : 'primary')}>
            {busy ? 'Salvando…' : confirmLabel}
          </button>
        </div>
      </form>
    </Modal>
  )
}
