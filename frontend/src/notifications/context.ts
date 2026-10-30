import { createContext, useContext } from 'react'

export type NotificationType = 'success' | 'error'

export type Notify = (text: string, type: NotificationType) => void

export const NotificationsContext = createContext<Notify | null>(null)

/** `notify(text, type)`: a success goes away by itself, and an error stays until the person closes it. */
export function useNotify(): Notify {
  const notify = useContext(NotificationsContext)
  if (!notify) {
    throw new Error('useNotify needs a NotificationsProvider above it.')
  }
  return notify
}
