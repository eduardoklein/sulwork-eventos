import { useState } from 'react'
import { createCategory, updateCategory } from '../../api/client'
import { useDomainOptions } from '../../domain/context'
import { useAction } from '../../lib/useAction'
import type { Category, MeasureUnit } from '../../types/api'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { Field } from '../../ui/Field'
import { TextField } from '../../ui/TextField'
import { fieldClass } from '../../ui/styles'

interface Values {
  name: string
  unit: MeasureUnit | ''
  goalQuantity: string
}

function validate(values: Values) {
  const errors: Partial<Record<keyof Values, string>> = {}
  if (!values.name.trim()) errors.name = 'Informe o nome da categoria.'
  if (!values.unit) errors.unit = 'Escolha a unidade.'
  const goal = Number(values.goalQuantity)
  if (values.goalQuantity === '' || !Number.isInteger(goal) || goal < 0) errors.goalQuantity = 'Informe a meta, um número inteiro de 0 ou mais.'
  return errors
}

/** Creates a category, or edits the one in `category`. It is mounted only while open, so it starts clean each time. */
export function CategoryDialog({
  eventId,
  category,
  onSaved,
  onClose,
}: {
  eventId: number
  category?: Category
  onSaved: () => void
  onClose: () => void
}) {
  const options = useDomainOptions()
  const { busy, message, fieldErrors: errors, reject, run } = useAction<keyof Values>()
  const [values, setValues] = useState<Values>({
    name: category?.name ?? '',
    unit: category?.unit ?? '',
    goalQuantity: category ? String(category.goalQuantity) : '0',
  })
  const unitLocked = Boolean(category && category.totalItems > 0)

  const set = (key: keyof Values, value: string) => setValues((current) => ({ ...current, [key]: value }))

  const save = () => {
    const missing = validate(values)
    if (Object.keys(missing).length > 0) {
      reject('Corrija os campos destacados.', missing)
      return
    }
    const body = { name: values.name.trim(), unit: values.unit as MeasureUnit, goalQuantity: Number(values.goalQuantity) }
    void run(() => (category ? updateCategory(eventId, category.id, body) : createCategory(eventId, body)), {
      done: category ? 'Categoria salva.' : 'Categoria criada.',
      failed: 'Não foi possível salvar a categoria.',
      onDone: () => {
        onSaved()
        onClose()
      },
    })
  }

  return (
    <ConfirmDialog
      open
      title={category ? 'Editar categoria' : 'Adicionar categoria'}
      confirmLabel="Salvar categoria"
      busy={busy}
      error={message}
      onConfirm={save}
      onCancel={onClose}
    >
      <TextField label="Nome" error={errors.name} maxLength={80} value={values.name} onChange={(value) => set('name', value)} />
      <Field
        label="Unidade"
        hint={unitLocked ? 'A unidade não muda enquanto a categoria tem itens.' : 'Os itens da categoria usam esta unidade.'}
        error={errors.unit}
      >
        {(control) => (
          <select {...control} value={values.unit} disabled={unitLocked || !options} onChange={(e) => set('unit', e.target.value)} className={fieldClass}>
            <option value="">{options ? 'Escolha a unidade' : 'Carregando as unidades…'}</option>
            {options?.measureUnits.map((unit) => (
              <option key={unit.code} value={unit.code}>
                {unit.description}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Meta de quantidade" hint="Quanto o evento precisa desta categoria no total. Use 0 para não ter meta." error={errors.goalQuantity}>
        {(control) => (
          <input {...control} type="number" min={0} step={1} value={values.goalQuantity} onChange={(e) => set('goalQuantity', e.target.value)} className={fieldClass} />
        )}
      </Field>
    </ConfirmDialog>
  )
}
