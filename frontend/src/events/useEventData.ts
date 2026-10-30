import { useCallback, useEffect, useState } from 'react'
import { ApiError, getEvent, listCategories, listContributions, listItems, listParticipants } from '../api/client'
import type { Category, Contribution, FoodEvent, Item, Participant } from '../types/api'

/** What several tabs share: the event, and the lists that the counters of the tabs count. */
export interface EventData {
  event: FoodEvent
  categories: Category[]
  items: Item[]
  participants: Participant[]
  contributions: Contribution[]
}

export type EventDataState =
  | { status: 'loading' }
  | { status: 'error'; message: string; notFound: boolean }
  | { status: 'ready'; data: EventData }

async function fetchEventData(id: number): Promise<EventData> {
  const [event, categories, items, participants, contributions] = await Promise.all([
    getEvent(id),
    listCategories(id),
    listItems(id),
    listParticipants(id),
    listContributions(id),
  ])
  return { event, categories, items, participants, contributions }
}

function failed(error: unknown): EventDataState {
  if (error instanceof ApiError) {
    return { status: 'error', message: error.message, notFound: error.status === 404 }
  }
  return { status: 'error', message: 'Não foi possível carregar o evento.', notFound: false }
}

export function useEventData(id: number) {
  const [state, setState] = useState<EventDataState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    fetchEventData(id).then(
      (data) => !cancelled && setState({ status: 'ready', data }),
      (error: unknown) => !cancelled && setState(failed(error)),
    )
    return () => {
      cancelled = true
    }
  }, [id, attempt])

  /** Loads again and keeps the data on screen until the new data arrives, so the tabs do not blink. */
  const reload = useCallback(() => setAttempt((current) => current + 1), [])

  const retry = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((current) => current + 1)
  }, [])

  return { state, reload, retry }
}
