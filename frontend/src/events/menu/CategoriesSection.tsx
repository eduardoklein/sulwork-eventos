import { useState } from 'react'
import { deleteCategory } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import type { Category } from '../../types/api'
import { EmptyState } from '../../ui/EmptyState'
import { Section } from '../../ui/Section'
import { buttonClass } from '../../ui/styles'
import { CategoryDialog } from './CategoryDialog'
import { DeleteDialog } from './DeleteDialog'

type Dialog = { kind: 'form'; category?: Category } | { kind: 'delete'; category: Category } | null

export function CategoriesSection({
  eventId,
  categories,
  editable,
  onChanged,
}: {
  eventId: number
  categories: Category[]
  editable: boolean
  onChanged: () => void
}) {
  const options = useDomainOptions()
  const [dialog, setDialog] = useState<Dialog>(null)
  const close = () => setDialog(null)
  const addButton = editable ? (
    <button type="button" onClick={() => setDialog({ kind: 'form' })} className={buttonClass('primary')}>
      Adicionar categoria
    </button>
  ) : undefined

  return (
    <Section title="Categorias e metas" description="Cada categoria tem uma unidade e uma meta de quantidade." action={categories.length > 0 ? addButton : undefined}>
      {categories.length === 0 ? (
        <EmptyState
          title="Nenhuma categoria ainda"
          text={editable ? 'Crie uma categoria, como Pratos ou Bebidas, para depois adicionar os itens.' : 'Este evento não tem categorias.'}
          action={addButton}
        />
      ) : (
        <ul className="divide-y divide-line border-y border-line bg-surface">
          {categories.map((category) => (
            <li key={category.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <div>
                <h3 className="font-display text-lg font-bold tracking-tight">{category.name}</h3>
                <p className="text-sm text-ink-soft">
                  {labelFor(options?.measureUnits, category.unit)} · meta {category.goalQuantity} · planejado {category.plannedQuantity} · contribuído{' '}
                  {category.contributedQuantity}
                </p>
                <p className="text-sm text-ink-soft">{category.totalItems === 1 ? '1 item' : `${category.totalItems} itens`}</p>
              </div>
              {editable && (
                <div className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    aria-label={`Editar ${category.name}`}
                    onClick={() => setDialog({ kind: 'form', category })}
                    className={buttonClass('secondary')}
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    aria-label={`Excluir ${category.name}`}
                    disabled={category.totalItems > 0}
                    aria-describedby={category.totalItems > 0 ? `category-${category.id}-locked` : undefined}
                    onClick={() => setDialog({ kind: 'delete', category })}
                    className={buttonClass('danger')}
                  >
                    Excluir
                  </button>
                  {category.totalItems > 0 && (
                    <span id={`category-${category.id}-locked`} className="basis-full text-sm text-ink-soft">
                      Só dá para excluir uma categoria sem itens.
                    </span>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {dialog?.kind === 'form' && <CategoryDialog eventId={eventId} category={dialog.category} onSaved={onChanged} onClose={close} />}
      {dialog?.kind === 'delete' && (
        <DeleteDialog
          title="Excluir esta categoria?"
          text={`A categoria ${dialog.category.name} será apagada. Isso não pode ser desfeito.`}
          done="Categoria excluída."
          remove={() => deleteCategory(eventId, dialog.category.id)}
          onDeleted={onChanged}
          onClose={close}
        />
      )}
    </Section>
  )
}
