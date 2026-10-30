import { useAction } from '../../lib/useAction'
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
  const { busy, message, run } = useAction()

  const confirm = () =>
    run(remove, {
      done,
      failed: 'Não foi possível excluir.',
      onDone: () => {
        onDeleted()
        onClose()
      },
    })

  return (
    <ConfirmDialog open title={title} confirmLabel={confirmLabel} danger busy={busy} error={message} onConfirm={confirm} onCancel={onClose}>
      <p>{text}</p>
    </ConfirmDialog>
  )
}
