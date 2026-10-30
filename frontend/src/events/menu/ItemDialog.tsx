import { useState } from 'react'
import { ApiError, createItem, updateItem } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import { useNotify } from '../../notifications/context'
import type { Allergen, Category, FoodCharacteristic, Item } from '../../types/api'
import { CheckboxGroup } from '../../ui/CheckboxGroup'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { Field } from '../../ui/Field'
import { fieldClass } from '../../ui/styles'

interface Values {
  name: string
  categoryId: string
  requiredQuantity: string
  mandatory: boolean
  allergens: Allergen[]
  characteristics: FoodCharacteristic[]
}

type Errors = Partial<Record<keyof Values, string>>

function validate(values: Values, creating: boolean): Errors {
  const errors: Errors = {}
  if (!values.name.trim()) errors.name = 'Informe o nome do item.'
  if (creating && !values.categoryId) errors.categoryId = 'Escolha a categoria.'
  const quantity = Number(values.requiredQuantity)
  if (values.requiredQuantity === '' || !Number.isInteger(quantity) || quantity < 1) {
    errors.requiredQuantity = 'Informe a quantidade necessária, um número inteiro de 1 ou mais.'
  }
  return errors
}

/**
 * Creates an item in a category, or edits the one in `item`. An item never changes category, so the category is
 * a choice only when creating. It is mounted only while open, so it starts clean each time.
 */
export function ItemDialog({
  eventId,
  categories,
  item,
  categoryId,
  onSaved,
  onClose,
}: {
  eventId: number
  categories: Category[]
  item?: Item
  /** The category to start with when creating. */
  categoryId?: number
  onSaved: () => void
  onClose: () => void
}) {
  const options = useDomainOptions()
  const notify = useNotify()
  const [values, setValues] = useState<Values>({
    name: item?.name ?? '',
    categoryId: String(item?.categoryId ?? categoryId ?? ''),
    requiredQuantity: item ? String(item.requiredQuantity) : '',
    mandatory: item?.mandatory ?? false,
    allergens: item?.allergens ?? [],
    characteristics: item?.characteristics ?? [],
  })
  const [errors, setErrors] = useState<Errors>({})
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const creating = !item

  const unit = item?.unit ?? categories.find((category) => String(category.id) === values.categoryId)?.unit
  const set = <Key extends keyof Values>(key: Key, value: Values[Key]) => setValues((current) => ({ ...current, [key]: value }))

  const save = async () => {
    const missing = validate(values, creating)
    setErrors(missing)
    if (Object.keys(missing).length > 0) {
      setMessage('Corrija os campos destacados.')
      return
    }
    const body = {
      name: values.name.trim(),
      requiredQuantity: Number(values.requiredQuantity),
      mandatory: values.mandatory,
      allergens: values.allergens,
      characteristics: values.characteristics,
    }
    setMessage(null)
    setBusy(true)
    try {
      await (item ? updateItem(eventId, item.id, body) : createItem(eventId, { ...body, categoryId: Number(values.categoryId) }))
      notify(item ? 'Item salvo.' : 'Item criado.', 'success')
      onSaved()
      onClose()
    } catch (failure) {
      if (failure instanceof ApiError) {
        setErrors(failure.fieldErrors as Errors)
        setMessage(failure.message)
      } else {
        setMessage('Não foi possível salvar o item.')
      }
      setBusy(false)
    }
  }

  return (
    <ConfirmDialog
      open
      title={item ? 'Editar item' : 'Adicionar item'}
      confirmLabel="Salvar"
      busy={busy}
      error={message}
      onConfirm={save}
      onCancel={onClose}
    >
      <Field label="Nome" error={errors.name}>
        {(control) => <input {...control} type="text" maxLength={120} value={values.name} onChange={(e) => set('name', e.target.value)} className={fieldClass} />}
      </Field>
      {creating ? (
        <Field label="Categoria" error={errors.categoryId}>
          {(control) => (
            <select {...control} value={values.categoryId} onChange={(e) => set('categoryId', e.target.value)} className={fieldClass}>
              <option value="">Escolha a categoria</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          )}
        </Field>
      ) : (
        <p className="text-sm text-ink-soft">
          Categoria: <span className="font-semibold text-ink">{item.categoryName}</span>
        </p>
      )}
      <Field
        label="Quantidade necessária"
        hint={unit ? `Em ${labelFor(options?.measureUnits, unit).toLowerCase()}, a unidade da categoria.` : undefined}
        error={errors.requiredQuantity}
      >
        {(control) => (
          <input {...control} type="number" min={1} step={1} value={values.requiredQuantity} onChange={(e) => set('requiredQuantity', e.target.value)} className={fieldClass} />
        )}
      </Field>
      <label className="flex items-center gap-2">
        <input type="checkbox" checked={values.mandatory} onChange={(e) => set('mandatory', e.target.checked)} />
        Item obrigatório
      </label>
      {options && (
        <>
          <CheckboxGroup legend="Alérgenos" options={options.allergens} selected={values.allergens} onChange={(next) => set('allergens', next)} />
          <CheckboxGroup
            legend="Características"
            options={options.foodCharacteristics}
            selected={values.characteristics}
            onChange={(next) => set('characteristics', next)}
          />
        </>
      )}
    </ConfirmDialog>
  )
}
