import { getCoverage } from '../../api/client'
import { labelFor, useDomainOptions } from '../../domain/context'
import { useRemote } from '../../lib/useRemote'
import type { CategoryCoverage, Coverage, Item } from '../../types/api'
import { EmptyState } from '../../ui/EmptyState'
import { ErrorPanel } from '../../ui/ErrorPanel'
import { Meter } from '../../ui/Meter'
import { Section } from '../../ui/Section'
import { buttonClass } from '../../ui/styles'
import { ItemStrip } from './ItemStrip'

export function CoverageSection({ eventId, items, version }: { eventId: number; items: Item[]; version: number }) {
  const { state, retry } = useRemote(() => getCoverage(eventId), 'coverage', version, 'Não foi possível carregar a cobertura.')
  return (
    <Section title="Situação do evento" description="Quanto do que o evento precisa já tem quem leve.">
      {state.status === 'loading' && (
        <div role="status" aria-busy="true">
          <span className="sr-only">Carregando a cobertura</span>
          <div aria-hidden="true" className="h-16 animate-pulse rounded bg-line" />
        </div>
      )}
      {state.status === 'error' && <ErrorPanel message={state.message} onRetry={retry} />}
      {state.status === 'ready' &&
        (state.data.totalItems === 0 ? (
          <EmptyState
            title="Ainda não há itens"
            text="Sem itens no cardápio, não há o que cobrir. Adicione os itens para ver a cobertura."
            action={
              <a href={`#/events/${eventId}/menu`} className={buttonClass('primary')}>
                Adicionar itens
              </a>
            }
          />
        ) : (
          <CoverageDetails coverage={state.data} items={items} />
        ))}
    </Section>
  )
}

function CoverageDetails({ coverage, items }: { coverage: Coverage; items: Item[] }) {
  const options = useDomainOptions()
  return (
    <>
      <div className="max-w-md">
        <p className="font-display text-4xl font-bold tracking-tight">{coverage.overallPercentage ?? 0}%</p>
        <Meter value={coverage.overallPercentage} label="Cobertura do evento" />
        <p className="mt-2 font-semibold">{labelFor(options?.eventReadiness, coverage.readiness)}</p>
        <p className="text-sm text-ink-soft">
          Obrigatórios: {coverage.mandatoryPercentage}% · Opcionais: {coverage.optionalPercentage}%
        </p>
      </div>

      <ul className="mt-6 divide-y divide-line border-y border-line bg-surface">
        {coverage.categories
          .filter((category) => category.percentage !== null)
          .map((category) => (
            <CategoryRow key={category.categoryId} category={category} items={items.filter((item) => item.categoryId === category.categoryId)} />
          ))}
      </ul>
      <ul aria-label="Legenda das barras de itens" className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-sm text-ink-soft">
        <li className="flex items-center gap-2">
          <span aria-hidden="true" className="inline-block h-3 w-3 rounded-[2px] bg-ok" /> Item completo
        </li>
        <li className="flex items-center gap-2">
          <span aria-hidden="true" className="inline-block h-3 w-3 rounded-[2px] bg-ink" /> Obrigatório que falta
        </li>
        <li className="flex items-center gap-2">
          <span aria-hidden="true" className="inline-block h-3 w-3 rounded-[2px] bg-line-strong" /> Opcional que falta
        </li>
      </ul>

      <details className="mt-6 max-w-prose">
        <summary className="cursor-pointer font-semibold">Como calculamos a cobertura</summary>
        <div className="mt-2 space-y-2 text-sm text-ink-soft">
          <p>
            A conta é feita por categoria, pois cada uma tem a sua unidade e somar unidades diferentes enganaria. O percentual geral é a
            média das categorias.
          </p>
          <p>
            Dentro de uma categoria, o item obrigatório vale 2 e o opcional vale 1. O que a meta da categoria pede além dos itens também
            entra na conta.
          </p>
          <p>Os percentuais arredondam para baixo, então 100% só aparece quando está tudo completo.</p>
        </div>
      </details>
    </>
  )
}

function CategoryRow({ category, items }: { category: CategoryCoverage; items: Item[] }) {
  const options = useDomainOptions()
  const unit = labelFor(options?.measureUnits, category.unit).toLowerCase()
  return (
    <li className="px-4 py-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="font-display text-lg font-bold tracking-tight">{category.name}</h3>
        <p className="font-semibold">{category.percentage}%</p>
      </div>
      <div className="mt-1 max-w-md">
        <Meter value={category.percentage} label={`Cobertura de ${category.name}`} />
      </div>
      <p className="mt-2 text-sm text-ink-soft">
        {category.contributedQuantity} de {category.effectiveTarget} ({unit})
        {/* `goalDeficit` of the API is the goal minus the items, not what is left to contribute, so this counts it again. */}
        {category.goalQuantity > 0 &&
          (category.goalReached
            ? ` · meta de ${category.goalQuantity} atingida`
            : ` · faltam ${category.goalQuantity - category.contributedQuantity} para a meta de ${category.goalQuantity}`)}
      </p>
      {items.length > 0 && (
        <div className="mt-3 max-w-md">
          <ItemStrip categoryName={category.name} items={items} />
        </div>
      )}
    </li>
  )
}

