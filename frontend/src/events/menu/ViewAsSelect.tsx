import type { Participant } from '../../types/api'
import { Field } from '../../ui/Field'
import { fieldClass } from '../../ui/styles'

export function ViewAsSelect({
  participants,
  value,
  onChange,
}: {
  participants: Participant[]
  value: number | null
  onChange: (participantId: number | null) => void
}) {
  return (
    <div className="mt-6 max-w-sm">
      <Field label="Ver o cardápio como" hint="Cada item mostra se é seguro para essa pessoa.">
        {(control) => (
          <select {...control} value={value ?? ''} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)} className={fieldClass}>
            <option value="">Ninguém</option>
            {participants.map((participant) => (
              <option key={participant.id} value={participant.id}>
                {participant.name}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  )
}
