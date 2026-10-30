import type { ReactNode } from 'react'

/** An empty list is an invitation to act: say what goes here and offer the action that fills it. */
export function EmptyState({ title, text, action }: { title: string; text: string; action?: ReactNode }) {
  return (
    <div className="rounded-lg border border-dashed border-line-strong bg-surface px-6 py-8 text-center">
      <p className="font-display text-lg font-bold">{title}</p>
      <p className="mx-auto mt-1 max-w-prose text-sm text-ink-soft">{text}</p>
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}
