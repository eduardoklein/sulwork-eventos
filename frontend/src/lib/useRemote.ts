import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/client'

export type Remote<T> = { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready'; data: T }

/**
 * Loads something once, and again when `key` changes (that is another thing, so it shows as loading) or when
 * `version` goes up (the same thing after a change, so the old data stays on screen until the new data comes).
 * `load` may be a new function on each render: only the last one is used.
 */
export function useRemote<T>(load: () => Promise<T>, key: string, version = 0, failed = 'Não foi possível carregar.') {
  const loadRef = useRef(load)
  useEffect(() => {
    loadRef.current = load
  })
  const [answer, setAnswer] = useState<{ key: string; state: Remote<T> } | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    loadRef.current().then(
      (data) => !cancelled && setAnswer({ key, state: { status: 'ready', data } }),
      (error: unknown) =>
        !cancelled && setAnswer({ key, state: { status: 'error', message: error instanceof ApiError ? error.message : failed } }),
    )
    return () => {
      cancelled = true
    }
  }, [key, version, attempt, failed])

  const retry = useCallback(() => {
    setAnswer(null)
    setAttempt((current) => current + 1)
  }, [])

  const state: Remote<T> = answer?.key === key ? answer.state : { status: 'loading' }
  return { state, retry }
}
