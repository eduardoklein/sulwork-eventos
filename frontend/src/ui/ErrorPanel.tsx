import { buttonClass } from './styles'

/** A load that failed: the reason, and a button to try again when trying again can help. */
export function ErrorPanel({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="rounded-lg border border-danger bg-danger-soft px-6 py-5 text-danger-ink">
      <p className="font-semibold">{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className={`${buttonClass('secondary')} mt-3`}>
          Tentar de novo
        </button>
      )}
    </div>
  )
}
