import type { ReactNode } from 'react'

export type ChipTone = 'neutral' | 'pen' | 'ok' | 'warn' | 'danger'

const TONES: Record<ChipTone, string> = {
  neutral: 'bg-paper text-ink-soft border border-line',
  pen: 'bg-pen-soft text-pen-dark',
  ok: 'bg-ok-soft text-ok-ink',
  warn: 'bg-warn-soft text-warn-ink',
  danger: 'bg-danger-soft text-danger-ink',
}

/** A small label. Green, amber and red keep one meaning: covered, missing, and conflict or danger. */
export function Chip({ tone = 'neutral', children }: { tone?: ChipTone; children: ReactNode }) {
  return (
    <span data-tone={tone} className={`inline-flex items-center gap-1 rounded px-2 py-0.5 text-xs font-semibold ${TONES[tone]}`}>
      {children}
    </span>
  )
}
