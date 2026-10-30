import { useCallback, useEffect, useState } from 'react'
import { ApiError, getCoverage, listEvents } from '../api/client'
import type { Coverage, FoodEvent } from '../types/api'
import { hasCoverage } from './groups'

export type CoverageState =
  | { status: 'loading' }
  | { status: 'ready'; coverage: Coverage }
  | { status: 'error' }

export type EventListState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; events: FoodEvent[] }

/**
 * The events, and then the coverage of each one that has a coverage, in parallel. A coverage that fails only
 * breaks its own bar: the list stays whole.
 */
export function useEventList() {
  const [list, setList] = useState<EventListState>({ status: 'loading' })
  const [coverages, setCoverages] = useState<Record<number, CoverageState>>({})
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    listEvents()
      .then((events) => {
        if (cancelled) return
        setList({ status: 'ready', events })
        const withCoverage = events.filter((event) => hasCoverage(event.status))
        setCoverages(Object.fromEntries(withCoverage.map((event) => [event.id, { status: 'loading' } as const])))
        for (const event of withCoverage) {
          getCoverage(event.id).then(
            (coverage) => !cancelled && setCoverages((current) => ({ ...current, [event.id]: { status: 'ready', coverage } })),
            () => !cancelled && setCoverages((current) => ({ ...current, [event.id]: { status: 'error' } })),
          )
        }
      })
      .catch((error: unknown) => {
        if (cancelled) return
        setList({ status: 'error', message: error instanceof ApiError ? error.message : 'Não foi possível carregar os eventos.' })
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  // The state starts as loading. Trying again goes back to it here, in the click, and not inside the effect.
  const retry = useCallback(() => {
    setList({ status: 'loading' })
    setCoverages({})
    setAttempt((current) => current + 1)
  }, [])
  return { list, coverages, retry }
}
