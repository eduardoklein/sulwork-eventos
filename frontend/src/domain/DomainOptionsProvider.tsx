import { useEffect, useState, type ReactNode } from 'react'
import { getDomainOptions } from '../api/client'
import type { DomainOptions } from '../types/api'
import { DomainOptionsContext } from './context'

/** Loads the options once for the whole app. If the call fails, the screens show the codes instead of the texts. */
export function DomainOptionsProvider({ children }: { children: ReactNode }) {
  const [options, setOptions] = useState<DomainOptions | null>(null)

  useEffect(() => {
    let cancelled = false
    getDomainOptions()
      .then((loaded) => {
        if (!cancelled) setOptions(loaded)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  return <DomainOptionsContext.Provider value={{ options }}>{children}</DomainOptionsContext.Provider>
}
