import { useEffect, useRef } from 'react'
import { DomainOptionsProvider } from './domain/DomainOptionsProvider'
import { EventsPage } from './events/EventsPage'
import { NotificationsProvider } from './notifications/NotificationsProvider'
import { routeHash, type Route } from './router/route'
import { useRoute } from './router/useRoute'
import { Header } from './shell/Header'

const APP_NAME = 'Sulwork Eventos'

const TAB_TITLES = {
  summary: 'Resumo',
  menu: 'Cardápio',
  participants: 'Participantes',
  contributions: 'Contribuições',
} as const

/** The name of the screen, for the heading and for the page title the screen reader announces. */
function screenTitle(route: Route): string {
  switch (route.name) {
    case 'events':
      return 'Eventos'
    case 'new':
      return 'Novo evento'
    case 'edit':
      return 'Editar evento'
    case 'event':
      return `Evento ${route.id}: ${TAB_TITLES[route.tab].toLowerCase()}`
  }
}

export default function App() {
  const route = useRoute()
  const mainRef = useRef<HTMLElement>(null)
  const title = screenTitle(route)
  const address = routeHash(route)
  const firstScreen = useRef(true)

  useEffect(() => {
    document.title = `${title} · ${APP_NAME}`
  }, [title])

  // A hash change does not reload the page, so a screen reader hears nothing: move the focus to the new screen.
  useEffect(() => {
    if (firstScreen.current) {
      firstScreen.current = false
      return
    }
    mainRef.current?.focus()
  }, [address])

  return (
    <NotificationsProvider>
      <DomainOptionsProvider>
        {/* A button, not a link: a link to "#main" would change the hash and the router would take it as a new screen. */}
        <button
          type="button"
          onClick={() => mainRef.current?.focus()}
          className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded-md focus:bg-surface focus:px-4 focus:py-2 focus:font-semibold"
        >
          Ir para o conteúdo
        </button>
        <Header />
        <main ref={mainRef} tabIndex={-1} className="mx-auto max-w-5xl px-4 py-8 outline-none">
          <h1 className="font-display text-3xl font-bold tracking-tight">{title}</h1>
          {route.name === 'events' && <EventsPage />}
        </main>
      </DomainOptionsProvider>
    </NotificationsProvider>
  )
}
