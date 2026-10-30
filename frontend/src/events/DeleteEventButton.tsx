import { useState } from 'react'
import { ApiError, deleteEvent } from '../api/client'
import { useNotify } from '../notifications/context'
import { EVENTS_ROUTE, routeHash } from '../router/route'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { buttonClass } from '../ui/styles'

export function DeleteEventButton({ eventId }: { eventId: number }) {
  const notify = useNotify()
  const [asking, setAsking] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const confirm = async () => {
    setBusy(true)
    try {
      await deleteEvent(eventId)
      notify('Evento excluído.', 'success')
      window.location.hash = routeHash(EVENTS_ROUTE)
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'Não foi possível excluir o evento.')
      setBusy(false)
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => {
          setError(null)
          setAsking(true)
        }}
        className={buttonClass('danger')}
      >
        Excluir evento
      </button>
      <ConfirmDialog
        open={asking}
        title="Excluir este evento?"
        confirmLabel="Excluir evento"
        danger
        busy={busy}
        error={error}
        onConfirm={confirm}
        onCancel={() => setAsking(false)}
      >
        <p>O rascunho e o cardápio dele serão apagados. Isso não pode ser desfeito.</p>
      </ConfirmDialog>
    </>
  )
}
