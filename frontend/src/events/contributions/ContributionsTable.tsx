import { labelFor, useDomainOptions } from '../../domain/context'
import { formatDateTime } from '../../lib/format'
import type { Contribution } from '../../types/api'
import { RowActions } from '../../ui/RowActions'

const headerClass = 'px-4 py-2 text-left text-sm font-semibold text-ink-soft'
const cellClass = 'px-4 py-3 align-top'

/** The table rolls inside its own box on a narrow screen, so the page does not roll sideways. */
export function ContributionsTable({
  contributions,
  editable,
  onEdit,
  onRemove,
}: {
  contributions: Contribution[]
  editable: boolean
  onEdit: (contribution: Contribution) => void
  onRemove: (contribution: Contribution) => void
}) {
  const options = useDomainOptions()
  return (
    <div className="overflow-x-auto border-y border-line bg-surface">
      <table className="w-full min-w-[40rem] border-collapse">
        <caption className="sr-only">Contribuições do evento</caption>
        <thead className="border-b border-line">
          <tr>
            <th scope="col" className={headerClass}>
              Participante
            </th>
            <th scope="col" className={headerClass}>
              Item
            </th>
            <th scope="col" className={headerClass}>
              Quantidade
            </th>
            <th scope="col" className={headerClass}>
              Data
            </th>
            {editable && (
              <th scope="col" className={headerClass}>
                Ações
              </th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {contributions.map((contribution) => (
            <tr key={contribution.id}>
              <th scope="row" className={`${cellClass} text-left font-semibold`}>
                {contribution.participantName}
              </th>
              <td className={cellClass}>
                <p>{contribution.itemName}</p>
                {contribution.note && <p className="text-sm text-ink-soft">{contribution.note}</p>}
              </td>
              <td className={cellClass}>
                {contribution.quantity} ({labelFor(options?.measureUnits, contribution.unit).toLowerCase()})
              </td>
              <td className={`${cellClass} whitespace-nowrap text-sm text-ink-soft`}>{formatDateTime(contribution.createdAt)}</td>
              {editable && (
                <td className={cellClass}>
                  <RowActions
                    editName={`Editar a contribuição de ${contribution.participantName} em ${contribution.itemName}`}
                    deleteLabel="Remover"
                    deleteName={`Remover a contribuição de ${contribution.participantName} em ${contribution.itemName}`}
                    onEdit={() => onEdit(contribution)}
                    onDelete={() => onRemove(contribution)}
                  />
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
