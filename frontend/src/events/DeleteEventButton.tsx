import { useState } from 'react'
import { deleteEvent } from '../api/client'
import { useAction } from '../lib/useAction'
import { EVENTS_ROUTE, routeHash } from '../router/route'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { buttonClass } from '../ui/styles'

export function DeleteEventButton({ eventId }: { eventId: number }) {
  const [asking, setAsking] = useState(false)
  const { busy, message, reset, run } = useAction()

  const confirm = () =>
    run(() => deleteEvent(eventId), {
      done: 'Evento excluído.',
      failed: 'Não foi possível excluir o evento.',
      onDone: () => {
        window.location.hash = routeHash(EVENTS_ROUTE)
      },
    })

  return (
    <>
      <button
        type="button"
        onClick={() => {
          reset()
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
        error={message}
        onConfirm={confirm}
        onCancel={() => setAsking(false)}
      >
        <p>O rascunho e o cardápio dele serão apagados. Isso não pode ser desfeito.</p>
      </ConfirmDialog>
    </>
  )
}
