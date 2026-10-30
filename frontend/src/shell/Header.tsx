import { buttonClass } from '../ui/styles'

export function Header() {
  return (
    <header className="border-b border-line bg-surface">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <a href="#/events" className="font-display text-xl font-bold tracking-tight">
          Sulwork Eventos
        </a>
        <nav aria-label="Principal" className="flex flex-wrap items-center gap-2">
          <a href="/swagger-ui.html" target="_blank" rel="noopener noreferrer" className={buttonClass('ghost')}>
            Documentação da API
          </a>
          <a href="#/events/new" className={buttonClass('primary')}>
            Novo evento
          </a>
        </nav>
      </div>
    </header>
  )
}
