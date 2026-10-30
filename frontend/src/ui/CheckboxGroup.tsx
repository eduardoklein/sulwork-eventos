import type { Option } from '../types/api'

/** A group of checkboxes for a closed list. The text of each option comes from the domain options. */
export function CheckboxGroup<Code extends string>({
  legend,
  options,
  selected,
  onChange,
}: {
  legend: string
  options: Option<Code>[]
  selected: Code[]
  onChange: (selected: Code[]) => void
}) {
  const toggle = (code: Code) => onChange(selected.includes(code) ? selected.filter((item) => item !== code) : [...selected, code])
  return (
    <fieldset>
      <legend className="mb-1 text-sm font-semibold text-ink">{legend}</legend>
      <div className="flex flex-wrap gap-x-5 gap-y-2">
        {options.map((option) => (
          <label key={option.code} className="flex items-center gap-2">
            <input type="checkbox" checked={selected.includes(option.code)} onChange={() => toggle(option.code)} />
            {option.description}
          </label>
        ))}
      </div>
    </fieldset>
  )
}
