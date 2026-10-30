import { createContext, useContext } from 'react'
import type { DomainOptions, Option } from '../types/api'

export interface DomainOptionsState {
  /** `null` while loading, or when the options could not be loaded. */
  options: DomainOptions | null
}

export const DomainOptionsContext = createContext<DomainOptionsState>({ options: null })

/** The closed lists of the system, with the text of each option. The labels live only in the backend. */
export function useDomainOptions(): DomainOptions | null {
  return useContext(DomainOptionsContext).options
}

/** The text of an option. Without the options (still loading, or failed), the code itself is better than nothing. */
export function labelFor<Code extends string>(list: Option<Code>[] | undefined, code: Code): string {
  return list?.find((option) => option.code === code)?.description ?? code
}
