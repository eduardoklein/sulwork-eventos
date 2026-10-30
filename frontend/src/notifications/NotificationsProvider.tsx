import { CircleAlert, CircleCheck, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { NotificationsContext, type Notify, type NotificationType } from './context'

/** A success is a confirmation: it goes away. An error has a reason the person must read, so it stays. */
export const SUCCESS_DURATION_MS = 4000

interface Notification {
  id: number
  text: string
  type: NotificationType
}

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const nextId = useRef(1)
  const timers = useRef(new Map<number, ReturnType<typeof setTimeout>>())

  const dismiss = useCallback((id: number) => {
    clearTimeout(timers.current.get(id))
    timers.current.delete(id)
    setNotifications((current) => current.filter((notification) => notification.id !== id))
  }, [])

  const notify = useCallback<Notify>(
    (text, type) => {
      const id = nextId.current++
      setNotifications((current) => [...current, { id, text, type }])
      if (type === 'success') {
        timers.current.set(id, setTimeout(() => dismiss(id), SUCCESS_DURATION_MS))
      }
    },
    [dismiss],
  )

  useEffect(() => {
    const pending = timers.current
    return () => {
      pending.forEach(clearTimeout)
      pending.clear()
    }
  }, [])

  const successes = notifications.filter((notification) => notification.type === 'success')
  const errors = notifications.filter((notification) => notification.type === 'error')

  return (
    <NotificationsContext.Provider value={notify}>
      {children}
      <div className="pointer-events-none fixed inset-x-4 bottom-4 z-[60] flex flex-col items-stretch gap-2 sm:inset-x-auto sm:right-4 sm:top-4 sm:bottom-auto sm:w-96">
        <div role="status" className="flex flex-col gap-2">
          {successes.map((notification) => (
            <Toast key={notification.id} notification={notification} onClose={dismiss} />
          ))}
        </div>
        <div role="alert" className="flex flex-col gap-2">
          {errors.map((notification) => (
            <Toast key={notification.id} notification={notification} onClose={dismiss} />
          ))}
        </div>
      </div>
    </NotificationsContext.Provider>
  )
}

function Toast({ notification, onClose }: { notification: Notification; onClose: (id: number) => void }) {
  const isError = notification.type === 'error'
  const Icon = isError ? CircleAlert : CircleCheck
  return (
    <div
      data-type={notification.type}
      className={`pointer-events-auto flex items-start gap-3 rounded-lg border px-4 py-3 shadow-lg ${
        isError ? 'border-danger bg-danger-soft text-danger-ink' : 'border-ok bg-ok-soft text-ok-ink'
      }`}
    >
      <Icon aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
      <p className="flex-1 text-sm font-semibold">{notification.text}</p>
      <button
        type="button"
        aria-label="Fechar aviso"
        onClick={() => onClose(notification.id)}
        className="-m-1 rounded p-1 hover:bg-black/5"
      >
        <X aria-hidden="true" className="size-4" />
      </button>
    </div>
  )
}
