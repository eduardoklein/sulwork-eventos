import type { ReactNode } from 'react'

/** A fixed note that explains why a screen has no buttons. */
export function Notice({ children }: { children: ReactNode }) {
  return <p className="mt-6 max-w-prose rounded-lg border border-line-strong bg-surface px-4 py-3 text-sm">{children}</p>
}
