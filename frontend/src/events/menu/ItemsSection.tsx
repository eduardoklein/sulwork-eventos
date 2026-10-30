import { useState } from 'react'
import { deleteItem } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import type { Category, Item } from '../../types/api'
import { Chip } from '../../ui/Chip'
import { Meter } from '../../ui/Meter'
import { RowActions } from '../../ui/RowActions'
import { Section } from '../../ui/Section'
import { buttonClass } from '../../ui/styles'
import { DeleteDialog } from './DeleteDialog'
import { ItemDialog } from './ItemDialog'
import { ItemSafety } from './ItemSafety'
import type { Viewer } from './safety'

type Dialog = { kind: 'form'; item?: Item; categoryId?: number } | { kind: 'delete'; item: Item } | null

export function ItemsSection({
  eventId,
  categories,
  items,
  editable,
  viewer,
  onChanged,
}: {
  eventId: number
  categories: Category[]
  items: Item[]
  editable: boolean
  viewer: Viewer | null
  onChanged: () => void
}) {
  const [dialog, setDialog] = useState<Dialog>(null)
  const close = () => setDialog(null)

  return (
    <Section title="Itens do cardápio" description="Cada item mostra quanto já foi contribuído do que precisa.">
      {categories.length === 0 ? (
        <p className="text-ink-soft">Crie uma categoria para poder adicionar itens.</p>
      ) : (
        categories.map((category) => (
          <ItemGroup
            key={category.id}
            category={category}
            items={items.filter((item) => item.categoryId === category.id)}
            editable={editable}
            viewer={viewer}
            onAdd={() => setDialog({ kind: 'form', categoryId: category.id })}
            onEdit={(item) => setDialog({ kind: 'form', item })}
            onDelete={(item) => setDialog({ kind: 'delete', item })}
          />
        ))
      )}
      {dialog?.kind === 'form' && (
        <ItemDialog eventId={eventId} categories={categories} item={dialog.item} categoryId={dialog.categoryId} onSaved={onChanged} onClose={close} />
      )}
      {dialog?.kind === 'delete' && (
        <DeleteDialog
          title="Excluir este item?"
          text={`O item ${dialog.item.name} será apagado. Isso não pode ser desfeito.`}
          done="Item excluído."
          confirmLabel="Excluir item"
          remove={() => deleteItem(eventId, dialog.item.id)}
          onDeleted={onChanged}
          onClose={close}
        />
      )}
    </Section>
  )
}

function ItemGroup({
  category,
  items,
  editable,
  viewer,
  onAdd,
  onEdit,
  onDelete,
}: {
  category: Category
  items: Item[]
  editable: boolean
  viewer: Viewer | null
  onAdd: () => void
  onEdit: (item: Item) => void
  onDelete: (item: Item) => void
}) {
  return (
    <div className="mb-6">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-3">
        <h3 className="font-display text-lg font-bold tracking-tight">{category.name}</h3>
        {editable && (
          <button type="button" onClick={onAdd} aria-label={`Adicionar item em ${category.name}`} className={buttonClass('secondary')}>
            Adicionar item
          </button>
        )}
      </div>
      {items.length === 0 ? (
        <p className="text-sm text-ink-soft">Nenhum item nesta categoria.</p>
      ) : (
        <ul className="divide-y divide-line border-y border-line bg-surface">
          {items.map((item) => (
            <ItemRow key={item.id} item={item} editable={editable} viewer={viewer} onEdit={() => onEdit(item)} onDelete={() => onDelete(item)} />
          ))}
        </ul>
      )}
    </div>
  )
}

function ItemRow({
  item,
  editable,
  viewer,
  onEdit,
  onDelete,
}: {
  item: Item
  editable: boolean
  viewer: Viewer | null
  onEdit: () => void
  onDelete: () => void
}) {
  const options = useDomainOptions()
  const hasContributions = item.contributedQuantity > 0
  const allergens = item.allergens.map((code) => labelFor(options?.allergens, code))
  const characteristics = item.characteristics.map((code) => labelFor(options?.foodCharacteristics, code))
  return (
    <li className="px-4 py-3">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <h4 className="font-semibold">{item.name}</h4>
        <Chip tone={item.mandatory ? 'pen' : 'neutral'}>{item.mandatory ? 'Obrigatório' : 'Opcional'}</Chip>
        {viewer && <ItemSafety item={item} viewer={viewer} />}
      </div>
      <p className="text-sm text-ink-soft">
        {item.contributedQuantity} de {item.requiredQuantity} ({labelFor(options?.measureUnits, item.unit).toLowerCase()})
      </p>
      {allergens.length > 0 && <p className="text-sm text-ink-soft">Alérgenos: {allergens.join(', ')}</p>}
      {characteristics.length > 0 && <p className="text-sm text-ink-soft">Características: {characteristics.join(', ')}</p>}
      <div className="mt-2 max-w-sm">
        <Meter value={item.coveredPercentage} label={`Cobertura de ${item.name}`} />
        <p className="mt-1 text-sm font-semibold">{item.coveredPercentage}%</p>
      </div>
      {editable && (
        <div className="mt-2">
          <RowActions
            editName={`Editar ${item.name}`}
            deleteName={`Excluir ${item.name}`}
            lockedReason={hasContributions ? 'Só dá para excluir um item sem contribuições.' : undefined}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>
      )}
    </li>
  )
}
