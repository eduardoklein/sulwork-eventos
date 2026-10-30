import { useId, type ReactNode } from 'react'
import { buttonClass } from './styles'

/**
 * The edit and delete buttons of a row. A list has many of them, so each one has a full name for a screen reader.
 * When the row cannot be deleted, the delete button is off and `lockedReason` says why, in sight and in the name.
 */
export function RowActions({
  editLabel = 'Editar',
  editName,
  deleteLabel = 'Excluir',
  deleteName,
  lockedReason,
  onEdit,
  onDelete,
  children,
}: {
  editLabel?: string
  /** The accessible name of the edit button, with the row it belongs to. */
  editName: string
  deleteLabel?: string
  deleteName: string
  lockedReason?: string
  onEdit: () => void
  /** Without it, the row has no delete button, for a row that the event no longer lets go. */
  onDelete?: () => void
  /** More buttons, before the edit button. */
  children?: ReactNode
}) {
  const reasonId = useId()
  return (
    <div className="flex flex-wrap items-center gap-2">
      {children}
      <button type="button" aria-label={editName} onClick={onEdit} className={buttonClass('secondary')}>
        {editLabel}
      </button>
      {onDelete && (
        <button
          type="button"
          aria-label={deleteName}
          disabled={Boolean(lockedReason)}
          aria-describedby={lockedReason ? reasonId : undefined}
          onClick={onDelete}
          className={buttonClass('danger')}
        >
          {deleteLabel}
        </button>
      )}
      {onDelete && lockedReason && (
        <span id={reasonId} className="basis-full text-sm text-ink-soft">
          {lockedReason}
        </span>
      )}
    </div>
  )
}
