import { useState } from 'react'
import { ApiError } from '../../api/client'
import { useNotify } from '../../notifications/context'
import { ConfirmDialog } from '../../ui/ConfirmDialog'

/** Asks before deleting a category or an item. `remove` calls the API. It is mounted only while open. */
export function DeleteDialog({
  title,
  text,
  done,
  confirmLabel = 'Excluir',
  remove,
  onDeleted,
  onClose,
}: {
  title: string
  text: string
  done: string
  confirmLabel?: string
  remove: () => Promise<void>
  onDeleted: () => void
  onClose: () => void
}) {
  const notify = useNotify()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const confirm = async () => {
    setBusy(true)
    try {
      await remove()
      notify(done, 'success')
      onDeleted()
      onClose()
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'Não foi possível excluir.')
      setBusy(false)
    }
  }

  return (
    <ConfirmDialog open title={title} confirmLabel={confirmLabel} danger busy={busy} error={error} onConfirm={confirm} onCancel={onClose}>
      <p>{text}</p>
    </ConfirmDialog>
  )
}
