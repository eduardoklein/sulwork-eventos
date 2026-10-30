import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { DomainOptionsProvider } from '../domain/DomainOptionsProvider'
import { domainOptions, failure, makeCoverage, makeEvent, stubApi } from '../test/fakeApi'
import { EventsPage } from './EventsPage'

function renderPage() {
  return render(
    <DomainOptionsProvider>
      <EventsPage />
    </DomainOptionsProvider>,
  )
}

const lunch = makeEvent({ id: 1, name: 'Almoço de fim de ano', changeDeadline: '2026-12-13T18:00:00' })
const breakfast = makeEvent({ id: 2, name: 'Café de integração', type: 'BREAKFAST', changeDeadline: '2026-11-13T18:00:00' })
const draft = makeEvent({ id: 3, name: 'Churrasco', status: 'RASCUNHO', type: 'GET_TOGETHER' })
const held = makeEvent({ id: 4, name: 'Lanche do time', status: 'REALIZADO', type: 'SNACK', dateTime: '2026-10-19T15:30:00' })
const cancelled = makeEvent({ id: 5, name: 'Happy hour', status: 'CANCELADO', type: 'OTHER' })

describe('EventsPage', () => {
  it('shows a loading state while the events come', async () => {
    stubApi({ '/api/events': [lunch], '/api/events/1/coverage': makeCoverage(62), '/api/domain/options': domainOptions })

    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Carregando os eventos')
    await screen.findByText('Almoço de fim de ano')
  })

  it('groups the events by situation, each group with a heading', async () => {
    stubApi({
      '/api/events': [lunch, breakfast, draft, held, cancelled],
      '/api/events/1/coverage': makeCoverage(62),
      '/api/events/2/coverage': makeCoverage(100, 'COMPLETE'),
      '/api/events/4/coverage': makeCoverage(100, 'COMPLETE'),
      '/api/domain/options': domainOptions,
    })

    renderPage()

    const headings = await screen.findAllByRole('heading', { level: 2 })
    expect(headings.map((heading) => heading.textContent)).toEqual([
      'Abertos para contribuição',
      'Rascunhos',
      'Encerrados e realizados',
      'Cancelados',
    ])
    const open = screen.getByRole('region', { name: 'Abertos para contribuição' })
    expect(within(open).getAllByRole('heading', { level: 3 }).map((h) => h.textContent)).toEqual([
      'Café de integração',
      'Almoço de fim de ano',
    ])
  })

  it('makes each event a link to its screen', async () => {
    stubApi({ '/api/events': [lunch], '/api/events/1/coverage': makeCoverage(62), '/api/domain/options': domainOptions })

    renderPage()

    expect(await screen.findByRole('link', { name: 'Almoço de fim de ano' })).toHaveAttribute('href', '#/events/1')
  })

  it('shows the status, the type, the date and the deadline of an open event', async () => {
    stubApi({ '/api/events': [lunch], '/api/events/1/coverage': makeCoverage(62), '/api/domain/options': domainOptions })

    renderPage()

    const row = (await screen.findByRole('link', { name: 'Almoço de fim de ano' })).closest('li')!
    await waitFor(() => expect(within(row).getByText('Aberto')).toBeInTheDocument())
    expect(row).toHaveTextContent('Almoço · 18/12/2026 às 12:00')
    expect(row).toHaveTextContent('Contribuições até 13/12/2026 às 18:00')
  })

  it('writes "Prazo" instead of "Contribuições até" for events that are not open', async () => {
    stubApi({ '/api/events': [draft], '/api/domain/options': domainOptions })

    renderPage()

    expect(await screen.findByText('Prazo: 13/12/2026 às 18:00')).toBeInTheDocument()
  })

  it('shows the coverage as a meter with the percentage and the readiness', async () => {
    stubApi({ '/api/events': [lunch], '/api/events/1/coverage': makeCoverage(62, 'IN_PROGRESS'), '/api/domain/options': domainOptions })

    renderPage()

    const meter = await screen.findByRole('meter', { name: 'Cobertura de Almoço de fim de ano' })
    expect(meter).toHaveAttribute('aria-valuenow', '62')
    expect(await screen.findByText('62% · Em andamento')).toBeInTheDocument()
  })

  it('asks for the coverage only of open, closed and held events', async () => {
    const fetchMock = stubApi({
      '/api/events': [lunch, draft, held, cancelled],
      '/api/events/1/coverage': makeCoverage(62),
      '/api/events/4/coverage': makeCoverage(100, 'COMPLETE'),
      '/api/domain/options': domainOptions,
    })

    renderPage()
    await screen.findAllByRole('meter')

    const calls = fetchMock.mock.calls.map(([address]) => address)
    expect(calls).toContain('/api/events/1/coverage')
    expect(calls).toContain('/api/events/4/coverage')
    expect(calls).not.toContain('/api/events/3/coverage')
    expect(calls).not.toContain('/api/events/5/coverage')
  })

  it('says there are no items when the coverage has no percentage', async () => {
    stubApi({ '/api/events': [lunch], '/api/events/1/coverage': makeCoverage(null, 'NO_ITEMS'), '/api/domain/options': domainOptions })

    renderPage()

    expect(await screen.findByText('Sem itens no cardápio')).toBeInTheDocument()
    expect(screen.queryByRole('meter')).not.toBeInTheDocument()
  })

  it('breaks only one bar when one coverage fails, and keeps the rest of the list', async () => {
    stubApi({
      '/api/events': [lunch, breakfast],
      '/api/events/1/coverage': failure(500, 'Erro interno do servidor.'),
      '/api/events/2/coverage': makeCoverage(100, 'COMPLETE'),
      '/api/domain/options': domainOptions,
    })

    renderPage()

    expect(await screen.findByText('Cobertura indisponível')).toBeInTheDocument()
    expect(await screen.findByText('100% · Completo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Almoço de fim de ano' })).toBeInTheDocument()
    expect(screen.getAllByRole('meter')).toHaveLength(1)
  })

  it('shows the code instead of the text when the domain options fail to load', async () => {
    stubApi({ '/api/events': [lunch], '/api/events/1/coverage': makeCoverage(62), '/api/domain/options': failure(500, 'Erro') })

    renderPage()

    const row = (await screen.findByRole('link', { name: 'Almoço de fim de ano' })).closest('li')!
    expect(within(row).getByText('ABERTO')).toBeInTheDocument()
    expect(row).toHaveTextContent('LUNCH')
  })

  it('invites the person to create the first event when there is none', async () => {
    stubApi({ '/api/events': [], '/api/domain/options': domainOptions })

    renderPage()

    expect(await screen.findByText('Nenhum evento ainda')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Criar evento' })).toHaveAttribute('href', '#/events/new')
  })

  it('shows the reason when the list fails, and tries again on request', async () => {
    let attempts = 0
    stubApi({
      '/api/events': () => {
        attempts++
        return attempts === 1
          ? new Response(JSON.stringify({ status: 500, detail: 'Erro interno do servidor.' }), { status: 500 })
          : new Response(JSON.stringify([lunch]), { status: 200 })
      },
      '/api/events/1/coverage': makeCoverage(62),
      '/api/domain/options': domainOptions,
    })
    const user = userEvent.setup()

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Erro interno do servidor.')
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    expect(await screen.findByRole('link', { name: 'Almoço de fim de ano' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('explains a network failure in plain words', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    globalThis.fetch = (() => Promise.reject(new TypeError('Failed to fetch'))) as typeof fetch

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor. Tente de novo.')
  })
})
