import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { DomainOptionsProvider } from '../domain/DomainOptionsProvider'
import { NotificationsProvider } from '../notifications/NotificationsProvider'
import type { EventTab } from '../router/route'
import { domainOptions, failure, makeEvent, stubApi } from '../test/fakeApi'
import type { FoodEvent } from '../types/api'
import { EventPage } from './EventPage'

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status })

function renderPage(tab: EventTab = 'summary') {
  return render(
    <NotificationsProvider>
      <DomainOptionsProvider>
        <EventPage id={1} tab={tab} />
      </DomainOptionsProvider>
    </NotificationsProvider>,
  )
}

/** The routes of an event. A function in `event` answers by method, to change what GET returns after a change. */
function stubEvent(event: FoodEvent, extra: Record<string, unknown> = {}) {
  return stubApi({
    '/api/domain/options': domainOptions,
    '/api/events/1': event,
    '/api/events/1/categories': [],
    '/api/events/1/items': [{ id: 1 }, { id: 2 }, { id: 3 }],
    '/api/events/1/participants': [{ id: 1 }, { id: 2 }],
    '/api/events/1/contributions': [{ id: 1 }],
    ...extra,
  })
}

const calls = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) =>
  fetchMock.mock.calls.filter(([input, init]) => input === address && init?.method === method)

const open = makeEvent({ id: 1, name: 'Almoço de fim de ano', description: 'No salão da empresa', status: 'ABERTO' })

beforeEach(() => {
  window.location.hash = '#/events/1'
})

describe('EventPage: header and tabs', () => {
  it('shows a loading state, then the name of the event as the heading', async () => {
    stubEvent(open)

    renderPage()

    expect(screen.getByText('Carregando o evento').closest('[role="status"]')).toHaveAttribute('aria-busy', 'true')
    expect(await screen.findByRole('heading', { level: 1, name: 'Almoço de fim de ano' })).toBeInTheDocument()
  })

  it('shows the status, the type, the date with the zone, the deadline and the description', async () => {
    stubEvent(open)

    renderPage()

    await screen.findByRole('heading', { level: 1 })
    expect(await screen.findByText('Aberto')).toBeInTheDocument()
    expect(screen.getByText('Almoço · 18/12/2026 às 12:00 · Brasília (UTC-3)')).toBeInTheDocument()
    expect(screen.getByText('Contribuições até 13/12/2026 às 18:00')).toBeInTheDocument()
    expect(screen.getByText('No salão da empresa')).toBeInTheDocument()
  })

  it('has one link for each tab, with the counters of the lists', async () => {
    stubEvent(open)

    renderPage()

    const nav = await screen.findByRole('navigation', { name: 'Seções do evento' })
    const links = within(nav).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Resumo', 'Cardápio(3)', 'Participantes(2)', 'Contribuições(1)'])
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '#/events/1/summary',
      '#/events/1/menu',
      '#/events/1/participants',
      '#/events/1/contributions',
    ])
  })

  it.each([
    ['summary', 'Resumo'],
    ['menu', 'Cardápio'],
    ['participants', 'Participantes'],
    ['contributions', 'Contribuições'],
  ] as const)('marks only the tab %s as the current one', async (tab, title) => {
    stubEvent(open)

    renderPage(tab)

    const nav = await screen.findByRole('navigation', { name: 'Seções do evento' })
    const current = within(nav).getAllByRole('link', { current: 'page' })
    expect(current).toHaveLength(1)
    expect(current[0]).toHaveTextContent(title)
    expect(screen.getByRole('region', { name: title })).toBeInTheDocument()
  })

  it('changes the section when the tab changes', async () => {
    stubEvent(open)
    const view = renderPage('summary')
    await screen.findByRole('heading', { level: 1 })

    view.rerender(
      <NotificationsProvider>
        <DomainOptionsProvider>
          <EventPage id={1} tab="menu" />
        </DomainOptionsProvider>
      </NotificationsProvider>,
    )

    expect(screen.getByRole('region', { name: 'Cardápio' })).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Resumo' })).not.toBeInTheDocument()
  })
})

describe('EventPage: loading failures', () => {
  it('says the event was not found, with a way back to the list', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1': failure(404, 'Evento não encontrado.'),
      '/api/events/1/categories': [],
      '/api/events/1/items': [],
      '/api/events/1/participants': [],
      '/api/events/1/contributions': [],
    })

    renderPage()

    expect(await screen.findByText('Evento não encontrado')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver os eventos' })).toHaveAttribute('href', '#/events')
    expect(screen.queryByRole('button', { name: 'Tentar de novo' })).not.toBeInTheDocument()
  })

  it('shows another failure with a button to try again', async () => {
    let attempts = 0
    stubEvent(open, { '/api/events/1': () => (++attempts === 1 ? failure(500, 'Erro interno do servidor.')() : json(200, open)) })
    const user = userEvent.setup()

    renderPage()

    expect(await screen.findByText('Erro interno do servidor.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Almoço de fim de ano' })).toBeInTheDocument()
  })

  it('fails the whole screen when one of the lists fails', async () => {
    stubEvent(open, { '/api/events/1/items': failure(500, 'Erro ao carregar os itens.') })

    renderPage()

    expect(await screen.findByText('Erro ao carregar os itens.')).toBeInTheDocument()
  })
})

describe('EventPage: edit and delete buttons', () => {
  it('links to the edit screen', async () => {
    stubEvent(open)

    renderPage()

    expect(await screen.findByRole('link', { name: 'Editar evento' })).toHaveAttribute('href', '#/events/1/edit')
  })

  it.each(['REALIZADO', 'CANCELADO'] as const)('hides "Editar evento" when the event is %s', async (status) => {
    stubEvent({ ...open, status, allowedTransitions: [] })

    renderPage()

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('link', { name: 'Editar evento' })).not.toBeInTheDocument()
  })

  it.each(['ABERTO', 'ENCERRADO', 'REALIZADO', 'CANCELADO'] as const)('does not offer "Excluir evento" when the event is %s', async (status) => {
    stubEvent({ ...open, status })

    renderPage()

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('button', { name: 'Excluir evento' })).not.toBeInTheDocument()
  })

  it('asks before it deletes a draft, and goes to the list after', async () => {
    const draft = { ...open, status: 'RASCUNHO' as const, allowedTransitions: ['ABERTO' as const, 'CANCELADO' as const] }
    const fetchMock = stubEvent(draft, { '/api/events/1': (init?: RequestInit) => (init?.method === 'DELETE' ? new Response(null, { status: 204 }) : json(200, draft)) })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Excluir evento' }))
    const dialog = screen.getByRole('dialog', { name: 'Excluir este evento?' })
    expect(calls(fetchMock, 'DELETE', '/api/events/1')).toHaveLength(0)
    await user.click(within(dialog).getByRole('button', { name: 'Excluir evento' }))

    await waitFor(() => expect(window.location.hash).toBe('#/events'))
    expect(calls(fetchMock, 'DELETE', '/api/events/1')).toHaveLength(1)
    expect(await screen.findByText('Evento excluído.')).toBeInTheDocument()
  })

  it('does not delete when the person goes back', async () => {
    const draft = { ...open, status: 'RASCUNHO' as const, allowedTransitions: ['ABERTO' as const] }
    const fetchMock = stubEvent(draft)
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Excluir evento' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Voltar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(calls(fetchMock, 'DELETE', '/api/events/1')).toHaveLength(0)
    expect(window.location.hash).toBe('#/events/1')
  })

  it('keeps the dialog open and shows the reason when the API refuses to delete', async () => {
    const draft = { ...open, status: 'RASCUNHO' as const, allowedTransitions: ['ABERTO' as const] }
    stubEvent(draft, {
      '/api/events/1': (init?: RequestInit) =>
        init?.method === 'DELETE' ? failure(422, 'Só eventos em rascunho podem ser excluídos.')() : json(200, draft),
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Excluir evento' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir evento' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('Só eventos em rascunho podem ser excluídos.')
    expect(window.location.hash).toBe('#/events/1')
  })
})

describe('EventPage: status actions', () => {
  it('offers one button for each transition the API allows, and no other', async () => {
    stubEvent(open)

    renderPage()

    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('button', { name: 'Encerrar contribuições' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar evento' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Abrir para contribuições' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como realizado' })).not.toBeInTheDocument()
  })

  it('offers no status button when the event is in a final situation', async () => {
    stubEvent({ ...open, status: 'REALIZADO', allowedTransitions: [] })

    renderPage()

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('button', { name: /Encerrar|Cancelar|Abrir|Marcar/ })).not.toBeInTheDocument()
  })

  it('changes the situation at once, without a question, and loads the event again', async () => {
    let current: FoodEvent = open
    const fetchMock = stubEvent(open, {
      '/api/events/1': () => json(200, current),
      '/api/events/1/status': () => {
        current = { ...open, status: 'ENCERRADO', allowedTransitions: ['REALIZADO', 'CANCELADO'] }
        return json(200, current)
      },
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Encerrar contribuições' }))

    expect(await screen.findByText('Contribuições encerradas.')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Marcar como realizado' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Encerrar contribuições' })).not.toBeInTheDocument()
    expect(JSON.parse(calls(fetchMock, 'PATCH', '/api/events/1/status')[0][1]!.body as string)).toEqual({ newStatus: 'ENCERRADO' })
  })

  it('shows the reason in a notice when a change without a question fails', async () => {
    stubEvent(open, { '/api/events/1/status': failure(422, 'Mudança de situação inválida.') })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Encerrar contribuições' }))

    expect(await screen.findByText('Mudança de situação inválida.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Encerrar contribuições' })).toBeEnabled()
  })

  it('asks before it cancels, and cancels only after the confirmation', async () => {
    const fetchMock = stubEvent(open, { '/api/events/1/status': { ...open, status: 'CANCELADO', allowedTransitions: [] } })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancelar evento' }))
    const dialog = screen.getByRole('dialog', { name: 'Cancelar evento' })
    expect(within(dialog).getByText('Um evento cancelado não pode ser reaberto.')).toBeInTheDocument()
    expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(0)
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar evento' }))

    await waitFor(() => expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(1))
    expect(JSON.parse(calls(fetchMock, 'PATCH', '/api/events/1/status')[0][1]!.body as string)).toEqual({ newStatus: 'CANCELADO' })
    expect(await screen.findByText('Evento cancelado.')).toBeInTheDocument()
  })

  it('does not change anything when the person goes back from the question', async () => {
    const fetchMock = stubEvent(open)
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancelar evento' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Voltar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(0)
  })

  it('asks before it marks an event as held', async () => {
    const closed = { ...open, status: 'ENCERRADO' as const, allowedTransitions: ['REALIZADO' as const, 'CANCELADO' as const] }
    stubEvent(closed)
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Marcar como realizado' }))

    expect(within(screen.getByRole('dialog')).getByText('Depois de realizado, o evento não muda mais.')).toBeInTheDocument()
  })

  it('keeps the question open and shows the reason when the API refuses', async () => {
    stubEvent(open, { '/api/events/1/status': failure(422, 'Não é possível cancelar agora.') })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Cancelar evento' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar evento' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('Não é possível cancelar agora.')
  })

  describe('opening a draft', () => {
    const draft = { ...open, status: 'RASCUNHO' as const, allowedTransitions: ['ABERTO' as const, 'CANCELADO' as const] }

    it('opens at once when the deadline has not passed, with no new deadline', async () => {
      const fetchMock = stubEvent({ ...draft, deadlineExpired: false }, { '/api/events/1/status': { ...open } })
      const user = userEvent.setup()
      renderPage()

      await user.click(await screen.findByRole('button', { name: 'Abrir para contribuições' }))

      await waitFor(() => expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(1))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(JSON.parse(calls(fetchMock, 'PATCH', '/api/events/1/status')[0][1]!.body as string)).toEqual({ newStatus: 'ABERTO' })
    })

    it('asks for a new deadline when the deadline has passed, and sends it', async () => {
      const fetchMock = stubEvent({ ...draft, deadlineExpired: true }, { '/api/events/1/status': { ...open } })
      const user = userEvent.setup()
      renderPage()

      await user.click(await screen.findByRole('button', { name: 'Abrir para contribuições' }))
      const dialog = screen.getByRole('dialog', { name: 'Abrir para contribuições' })
      expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(0)
      const field = within(dialog).getByLabelText('Novo prazo para contribuições')
      await user.clear(field)
      await user.type(field, '2026-12-16T18:00')
      await user.click(within(dialog).getByRole('button', { name: 'Abrir para contribuições' }))

      await waitFor(() => expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(1))
      expect(JSON.parse(calls(fetchMock, 'PATCH', '/api/events/1/status')[0][1]!.body as string)).toEqual({
        newStatus: 'ABERTO',
        newChangeDeadline: '2026-12-16T18:00:00',
      })
    })

    it('does not call the API when the new deadline is empty', async () => {
      const fetchMock = stubEvent({ ...draft, deadlineExpired: true })
      const user = userEvent.setup()
      renderPage()

      await user.click(await screen.findByRole('button', { name: 'Abrir para contribuições' }))
      const dialog = screen.getByRole('dialog')
      await user.clear(within(dialog).getByLabelText('Novo prazo para contribuições'))
      await user.click(within(dialog).getByRole('button', { name: 'Abrir para contribuições' }))

      expect(await within(dialog).findByRole('alert')).toHaveTextContent('Informe o novo prazo.')
      expect(calls(fetchMock, 'PATCH', '/api/events/1/status')).toHaveLength(0)
    })
  })
})
