/** A coverage percentage. Full is green; anything below is amber, because something is still missing. */
export function Meter({ value, label }: { value: number | null; label: string }) {
  const percentage = value === null ? 0 : Math.max(0, Math.min(100, value))
  const tone = value === 100 ? 'ok' : 'warn'
  return (
    <div
      role="meter"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={percentage}
      aria-valuetext={value === null ? 'Sem itens' : `${percentage}%`}
      data-tone={tone}
      className="h-2 w-full overflow-hidden rounded-full bg-line"
    >
      <div className={`h-full ${tone === 'ok' ? 'bg-ok' : 'bg-warn'}`} style={{ width: `${percentage}%` }} />
    </div>
  )
}
