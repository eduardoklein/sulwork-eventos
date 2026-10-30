import { useEffect, useState } from 'react'
import { ApiError, findCollaboratorByCpf } from '../../api/client'
import { isValidCpf, onlyDigits } from '../../lib/cpf'
import type { Collaborator } from '../../types/api'

export type Lookup =
  | { status: 'idle' }
  | { status: 'searching' }
  | { status: 'found'; collaborator: Collaborator }
  | { status: 'new' }
  | { status: 'error'; message: string }

/**
 * Looks for the person of a CPF as soon as the CPF is valid. A 404 means a new person. The answer is kept with
 * the CPF it belongs to, so a CPF that changed never shows the answer of the old one.
 */
export function useCollaboratorLookup(cpf: string): Lookup {
  const [answer, setAnswer] = useState<{ digits: string; lookup: Lookup } | null>(null)
  const valid = isValidCpf(cpf)
  const digits = onlyDigits(cpf)

  useEffect(() => {
    if (!valid) return
    let cancelled = false
    findCollaboratorByCpf(digits).then(
      (collaborator) => !cancelled && setAnswer({ digits, lookup: { status: 'found', collaborator } }),
      (error: unknown) => {
        if (cancelled) return
        if (error instanceof ApiError && error.status === 404) {
          setAnswer({ digits, lookup: { status: 'new' } })
        } else {
          const message = error instanceof ApiError ? error.message : 'Não foi possível procurar o cadastro.'
          setAnswer({ digits, lookup: { status: 'error', message } })
        }
      },
    )
    return () => {
      cancelled = true
    }
  }, [valid, digits])

  if (!valid) return { status: 'idle' }
  return answer?.digits === digits ? answer.lookup : { status: 'searching' }
}
