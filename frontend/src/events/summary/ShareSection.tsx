import { getSummary } from '../../api/client'
import { useNotify } from '../../notifications/context'
import { useRemote } from '../../lib/useRemote'
import { ErrorPanel } from '../../ui/ErrorPanel'
import { Section } from '../../ui/Section'
import { buttonClass } from '../../ui/styles'

/** The text for the team group, with no CPF and no dietary restriction in it. */
export function ShareSection({ eventId, version }: { eventId: number; version: number }) {
  const notify = useNotify()
  const { state, retry } = useRemote(() => getSummary(eventId), 'summary', version, 'Não foi possível carregar o resumo.')

  const copy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text)
      notify('Texto copiado.', 'success')
    } catch {
      notify('Não foi possível copiar. Selecione o texto e copie.', 'error')
    }
  }

  return (
    <Section title="Compartilhar com a equipe" description="Um texto pronto para colar no grupo da equipe.">
      {state.status === 'loading' && (
        <div role="status" aria-busy="true">
          <span className="sr-only">Carregando o resumo</span>
          <div aria-hidden="true" className="h-24 animate-pulse rounded bg-line" />
        </div>
      )}
      {state.status === 'error' && <ErrorPanel message={state.message} onRetry={retry} />}
      {state.status === 'ready' && (
        <>
          <pre aria-label="Texto do resumo" tabIndex={0} className="max-h-96 overflow-auto whitespace-pre-wrap rounded-lg border border-line bg-surface p-4 font-sans text-sm">
            {state.data.text}
          </pre>
          <button type="button" onClick={() => copy(state.data.text)} className={`${buttonClass('secondary')} mt-3`}>
            Copiar texto
          </button>
        </>
      )}
    </Section>
  )
}
