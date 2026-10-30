import { useCallback, useEffect, useState } from 'react'
import { ApiError, getEvent } from '../api/client'
import type { FoodEvent } from '../types/api'

export type EventState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; event: FoodEvent }

export function useEvent(id: number) {
  const [state, setState] = useState<EventState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    getEvent(id).then(
      (event) => !cancelled && setState({ status: 'ready', event }),
      (error: unknown) =>
        !cancelled &&
        setState({ status: 'error', message: error instanceof ApiError ? error.message : 'Não foi possível carregar o evento.' }),
    )
    return () => {
      cancelled = true
    }
  }, [id, attempt])

  const retry = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((current) => current + 1)
  }, [])
  return { state, retry }
}
