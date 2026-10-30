/** Up to this many units, each unit is one slot. Above it, the strip has ten slots of 10% each. */
const MAX_UNIT_SLOTS = 20
const PERCENT_SLOTS = 10

/**
 * The sign-up sheet line of an item: every slot is a place someone can fill. Filled slots are in pen blue, and the
 * empty ones show at a glance what is still missing. The slots stay in one line and shrink on narrow screens.
 */
export function SlotStrip({ required, contributed, unit }: { required: number; contributed: number; unit: string }) {
  const covered = Math.max(0, Math.min(contributed, required))
  const perUnit = required <= MAX_UNIT_SLOTS
  const total = perUnit ? required : PERCENT_SLOTS
  const filled = perUnit ? covered : Math.floor((covered / required) * PERCENT_SLOTS)
  return (
    <div
      role="meter"
      aria-label={`Quantidade combinada (${unit})`}
      aria-valuemin={0}
      aria-valuemax={required}
      aria-valuenow={covered}
      aria-valuetext={`${covered} de ${required} (${unit})`}
      data-slots={perUnit ? 'unit' : 'percent'}
      className="grid gap-[3px]"
      style={{ gridTemplateColumns: `repeat(${total}, minmax(0, 14px))` }}
    >
      {Array.from({ length: total }, (_, index) => (
        <span
          key={index}
          aria-hidden="true"
          data-filled={index < filled}
          className={`aspect-square w-full rounded-[3px] ${index < filled ? 'bg-pen' : 'border border-dashed border-line-strong'}`}
        />
      ))}
    </div>
  )
}
