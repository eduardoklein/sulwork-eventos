import { labelFor, useDomainOptions } from '../../domain/context'
import type { ParticipantDiet, Participant } from '../../types/api'
import { RowActions } from '../../ui/RowActions'
import { buttonClass } from '../../ui/styles'

export function ParticipantRow({
  participant,
  diet,
  editable,
  onViewMenu,
  onEdit,
  onRemove,
}: {
  participant: Participant
  /** `undefined` when the analysis has nothing about this person. */
  diet?: ParticipantDiet
  editable: boolean
  onViewMenu: () => void
  onEdit: () => void
  onRemove: () => void
}) {
  const options = useDomainOptions()
  const restrictions = participant.restrictions.map((code) => labelFor(options?.dietaryRestrictions, code))
  const name = participant.name
  return (
    <li className="px-4 py-3">
      <h3 className="font-display text-lg font-bold tracking-tight">{name}</h3>
      <p className="text-sm text-ink-soft">CPF {participant.cpf}</p>
      <p className="text-sm text-ink-soft">Restrições: {restrictions.length > 0 ? restrictions.join(', ') : 'nenhuma'}</p>
      {diet && diet.totalItems > 0 && (
        <p className="text-sm font-semibold">
          {diet.safeItems} de {diet.totalItems} {diet.totalItems === 1 ? 'item é seguro' : 'itens são seguros'}
        </p>
      )}
      <div className="mt-2">
        <RowActions
          editLabel="Editar cadastro"
          editName={`Editar cadastro de ${name}`}
          deleteLabel="Remover"
          deleteName={`Remover ${name}`}
          onEdit={onEdit}
          onDelete={editable ? onRemove : undefined}
        >
          <button type="button" aria-label={`Ver cardápio de ${name}`} onClick={onViewMenu} className={buttonClass('secondary')}>
            Ver cardápio
          </button>
        </RowActions>
      </div>
    </li>
  )
}
