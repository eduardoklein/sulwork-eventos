import { Field } from './Field'
import { fieldClass } from './styles'

/** A line of text with its label and error. */
export function TextField({
  label,
  value,
  onChange,
  error,
  maxLength,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  maxLength?: number
}) {
  return (
    <Field label={label} error={error}>
      {(control) => <input {...control} type="text" maxLength={maxLength} value={value} onChange={(e) => onChange(e.target.value)} className={fieldClass} />}
    </Field>
  )
}
