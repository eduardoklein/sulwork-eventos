import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DomainOptionsProvider } from '../../domain/DomainOptionsProvider'
import { NotificationsProvider } from '../../notifications/NotificationsProvider'
import { domainOptions, failure, makeCoverage, makeEvent, makeItem, makeParticipant, stubApi } from '../../test/fakeApi'
import type { CategoryCoverage, FoodEvent, Item, Participant, Suggestion } from '../../types/api'
import { SummaryTab } from './SummaryTab'

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status })

function renderTab({
  event = makeEvent({ id: 1, name: 'Almoço' }),
  items = [],
  participants = [],
}: { event?: FoodEvent; items?: Item[]; participants?: Participant[] } = {}) {
  const onChanged = vi.fn()
  render(
    <NotificationsProvider>
      <DomainOptionsProvider>
        <SummaryTab event={event} items={items} participants={participants} onChanged={onChanged} />
      </DomainOptionsProvider>
    </NotificationsProvider>,
  )
  return { onChanged }
}

const sent = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) => {
  const call = fetchMock.mock.calls.find(([input, init]) => input === address && init?.method === method)
  return call ? JSON.parse((call[1]!.body as string | undefined) ?? 'null') : undefined
}
const calls = (fetchMock: ReturnType<typeof stubApi>, address: string) => fetchMock.mock.calls.filter(([input]) => input === address).length

const ana = makeParticipant({ id: 1, name: 'Ana Souza' })
const bia = makeParticipant({ id: 2, name: 'Bia Lima' })

const lasagna = makeItem({ id: 7, name: 'Lasanha', categoryId: 1, mandatory: true, requiredQuantity: 10, contributedQuantity: 4, pendingQuantity: 6, coveredPercentage: 40 })
const salad = makeItem({ id: 8, name: 'Salada', categoryId: 1, mandatory: false, requiredQuantity: 5, contributedQuantity: 5, pendingQuantity: 0, coveredPercentage: 100 })
const farofa = makeItem({ id: 9, name: 'Farofa', categoryId: 1, mandatory: false, requiredQuantity: 5, contributedQuantity: 1, pendingQuantity: 4, coveredPercentage: 20 })

const category: CategoryCoverage = {
  categoryId: 1,
  name: 'Pratos',
  unit: 'PORTION',
  goalQuantity: 30,
  totalItems: 3,
  // The items need 36 (more than the goal of 30), so the API has no deficit: it is the goal minus the items.
  requiredQuantity: 36,
  contributedQuantity: 21,
  goalDeficit: 0,
  effectiveTarget: 36,
  goalReached: false,
  percentage: 33,
  mandatoryPercentage: 40,
  optionalPercentage: 60,
}
const coverage = {
  readiness: 'IN_PROGRESS',
  overallPercentage: 33,
  mandatoryPercentage: 40,
  optionalPercentage: 60,
  totalItems: 3,
  categories: [category],
}

const suggestion = (overrides: Partial<Suggestion> & Pick<Suggestion, 'itemId' | 'itemName'>): Suggestion => ({
  categoryName: 'Pratos',
  unit: 'PORTION',
  requiredQuantity: 10,
  contributedQuantity: 4,
  pendingQuantity: 6,
  coveragePercentage: 40,
  suggestedQuantity: 3,
  priority: 80,
  safeForParticipant: null,
  reasons: [],
  ...overrides,
})

const SUMMARY = '*Almoço*\nSem CPF aqui.'
const base = { '/api/domain/options': domainOptions, '/api/events/1/summary': { text: SUMMARY } }

describe('SummaryTab: coverage', () => {
  it('shows the overall percentage, the readiness, the mandatory and optional percentages', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })

    renderTab({ items: [lasagna, salad, farofa] })

    expect(await screen.findByText('33%', { selector: 'p.font-display' })).toBeInTheDocument()
    expect(screen.getByRole('meter', { name: 'Cobertura do evento' })).toHaveAttribute('aria-valuenow', '33')
    expect(screen.getByText('Em andamento')).toBeInTheDocument()
    expect(screen.getByText('Obrigatórios: 40% · Opcionais: 60%')).toBeInTheDocument()
  })

  it('shows each category with its percentage, quantities and goal', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })

    renderTab({ items: [lasagna, salad, farofa] })

    const row = (await screen.findByRole('heading', { name: 'Pratos' })).closest('li')!
    expect(within(row).getByRole('meter', { name: 'Cobertura de Pratos' })).toHaveAttribute('aria-valuenow', '33')
    expect(within(row).getByText('21 de 36 (porção) · faltam 9 para a meta de 30')).toBeInTheDocument()
  })

  it('says the goal was reached', async () => {
    stubApi({ ...base, '/api/events/1/coverage': { ...coverage, categories: [{ ...category, goalReached: true, contributedQuantity: 33 }] } })

    renderTab({ items: [lasagna] })

    expect(await screen.findByText('33 de 36 (porção) · meta de 30 atingida')).toBeInTheDocument()
  })

  it('counts what lacks for the goal from what was contributed, even when the items need more than the goal', async () => {
    stubApi({ ...base, '/api/events/1/coverage': { ...coverage, categories: [{ ...category, goalQuantity: 30, contributedQuantity: 25, requiredQuantity: 36, effectiveTarget: 36, goalDeficit: 0 }] } })

    renderTab({ items: [lasagna] })

    expect(await screen.findByText('25 de 36 (porção) · faltam 5 para a meta de 30')).toBeInTheDocument()
  })

  it('says nothing about a goal when the category has none', async () => {
    stubApi({ ...base, '/api/events/1/coverage': { ...coverage, categories: [{ ...category, goalQuantity: 0 }] } })

    renderTab({ items: [lasagna] })

    expect(await screen.findByText('21 de 36 (porção)')).toBeInTheDocument()
  })

  it('leaves out a category with no items and no goal', async () => {
    stubApi({ ...base, '/api/events/1/coverage': { ...coverage, categories: [category, { ...category, categoryId: 2, name: 'Vazia', percentage: null }] } })

    renderTab({ items: [lasagna] })

    await screen.findByRole('heading', { name: 'Pratos' })
    expect(screen.queryByRole('heading', { name: 'Vazia' })).not.toBeInTheDocument()
  })

  it('has one segment for each item of the category, by state, and a legend', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })

    renderTab({ items: [lasagna, salad, farofa] })

    const strip = await screen.findByRole('list', { name: 'Itens de Pratos' })
    const segments = within(strip).getAllByRole('listitem')
    expect(segments.map((segment) => segment.getAttribute('data-state'))).toEqual(['mandatory', 'complete', 'optional'])
    expect(segments.map((segment) => segment.style.flexGrow)).toEqual(['10', '5', '5'])
    expect(within(strip).getByText('Lasanha: 4 de 10 (porção), obrigatório')).toBeInTheDocument()
    expect(within(strip).getByText('Salada: 5 de 5 (porção), opcional, completo')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Legenda das barras de itens' })).toHaveTextContent('Item completo')
  })

  it('explains how the coverage is calculated', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })

    renderTab({ items: [lasagna] })

    expect(await screen.findByText('Como calculamos a cobertura')).toBeInTheDocument()
    expect(screen.getByText(/o item obrigatório vale 2 e o opcional vale 1/)).toBeInTheDocument()
  })

  it('shows an empty state with a link to the menu, and no percentage, when the event has no items', async () => {
    stubApi({ ...base, '/api/events/1/coverage': { ...makeCoverage(100, 'NO_ITEMS'), totalItems: 0 } })

    renderTab()

    expect(await screen.findByText('Ainda não há itens')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Adicionar itens' })).toHaveAttribute('href', '#/events/1/menu')
    expect(screen.queryByRole('meter', { name: 'Cobertura do evento' })).not.toBeInTheDocument()
    expect(screen.queryByText('100%')).not.toBeInTheDocument()
  })

  it('shows the failure of the coverage with a way to try again, and the other sections stay', async () => {
    let attempts = 0
    stubApi({ ...base, '/api/events/1/coverage': () => (++attempts === 1 ? failure(500, 'Erro interno do servidor.')() : json(200, coverage)) })
    const user = userEvent.setup()
    renderTab({ items: [lasagna] })

    expect(await screen.findByText('Erro interno do servidor.')).toBeInTheDocument()
    expect(await screen.findByLabelText('Texto do resumo')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByText('Em andamento')).toBeInTheDocument()
  })
})

describe('SummaryTab: what still lacks', () => {
  const stub = (extra: Record<string, unknown> = {}) => stubApi({ ...base, '/api/events/1/coverage': coverage, ...extra })

  it('lists what lacks with the buttons off until somebody is chosen', async () => {
    stub()

    renderTab({ items: [farofa, lasagna, salad], participants: [ana, bia] })

    expect(await screen.findByText('Escolha um participante para ver a sugestão de quanto levar.')).toBeInTheDocument()
    const rows = screen.getAllByRole('listitem').filter((row) => row.textContent?.includes('Faltam'))
    expect(rows.map((row) => row.textContent)).toEqual([
      expect.stringContaining('Lasanha'),
      expect.stringContaining('Farofa'),
    ])
    expect(screen.getByRole('button', { name: 'Levar Lasanha' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Levar Farofa' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Levar Salada' })).not.toBeInTheDocument()
  })

  it('says all the items are complete when nothing lacks', async () => {
    stub()

    renderTab({ items: [salad], participants: [ana] })

    expect(await screen.findByText('Todos os itens já estão completos.')).toBeInTheDocument()
  })

  it('asks to enroll participants first when there are none', async () => {
    stub()

    renderTab({ items: [lasagna] })

    expect(await screen.findByText('Inscreva participantes para ver o que cada um pode levar.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Quem vai levar?')).not.toBeInTheDocument()
  })

  it('asks for the suggestions of the chosen person and shows them with the buttons on', async () => {
    const fetchMock = stub({
      '/api/events/1/suggestions?participantId=1': [
        suggestion({ itemId: 7, itemName: 'Lasanha', suggestedQuantity: 3, priority: 85, reasons: ['Item obrigatório.', 'A cobertura está abaixo da média do evento.'], safeForParticipant: true }),
        suggestion({ itemId: 9, itemName: 'Farofa', suggestedQuantity: 2, safeForParticipant: false }),
      ],
    })
    const user = userEvent.setup()
    renderTab({ items: [lasagna, farofa], participants: [ana, bia] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')

    expect(await screen.findByRole('button', { name: 'Levar 3 (porção) de Lasanha' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Levar 2 (porção) de Farofa' })).toBeEnabled()
    expect(calls(fetchMock, '/api/events/1/suggestions?participantId=1')).toBe(1)
    expect(screen.getByText('Seguro para Ana Souza')).toBeInTheDocument()
    expect(screen.getByText('Não é seguro para Ana Souza')).toBeInTheDocument()
    expect(screen.getByText(/prioridade 85 de 100/)).toBeInTheDocument()
    expect(screen.getByText('Item obrigatório. A cobertura está abaixo da média do evento.')).toBeInTheDocument()
  })

  it('shows no safety mark for a person without restrictions', async () => {
    stub({ '/api/events/1/suggestions?participantId=2': [suggestion({ itemId: 7, itemName: 'Lasanha', safeForParticipant: null })] })
    const user = userEvent.setup()
    renderTab({ items: [lasagna], participants: [ana, bia] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Bia Lima')

    await screen.findByRole('button', { name: /Levar 3/ })
    expect(screen.queryByText(/Seguro para|Não é seguro para/)).not.toBeInTheDocument()
  })

  it('says nothing lacks for the person when the list is empty', async () => {
    stub({ '/api/events/1/suggestions?participantId=1': [] })
    const user = userEvent.setup()
    renderTab({ items: [lasagna], participants: [ana] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')

    expect(await screen.findByText('Nada falta para Ana Souza levar.')).toBeInTheDocument()
  })

  it('goes back to the list with the buttons off when the person is cleared', async () => {
    stub({ '/api/events/1/suggestions?participantId=1': [suggestion({ itemId: 7, itemName: 'Lasanha' })] })
    const user = userEvent.setup()
    renderTab({ items: [lasagna], participants: [ana] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')
    await screen.findByRole('button', { name: /Levar 3/ })
    await user.selectOptions(screen.getByLabelText('Quem vai levar?'), 'Escolha o participante')

    expect(screen.getByRole('button', { name: 'Levar Lasanha' })).toBeDisabled()
  })

  it('shows the failure of the suggestions with a way to try again', async () => {
    let attempts = 0
    stub({
      '/api/events/1/suggestions?participantId=1': () =>
        ++attempts === 1 ? failure(500, 'Erro interno do servidor.')() : json(200, [suggestion({ itemId: 7, itemName: 'Lasanha' })]),
    })
    const user = userEvent.setup()
    renderTab({ items: [lasagna], participants: [ana] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')
    const panel = (await screen.findByText('Erro interno do servidor.')).closest('[role="alert"]') as HTMLElement
    await user.click(within(panel).getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('button', { name: /Levar 3/ })).toBeInTheDocument()
  })

  it('registers the suggested quantity, then loads everything again and tells the screen', async () => {
    const fetchMock = stub({
      '/api/events/1/suggestions?participantId=1': [suggestion({ itemId: 7, itemName: 'Lasanha', suggestedQuantity: 3 })],
      '/api/events/1/contributions': () => json(201, {}),
    })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ items: [lasagna], participants: [ana] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')
    await user.click(await screen.findByRole('button', { name: 'Levar 3 (porção) de Lasanha' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1))
    expect(sent(fetchMock, 'POST', '/api/events/1/contributions')).toEqual({ participantId: 1, itemId: 7, quantity: 3 })
    expect(await screen.findByText('Contribuição registrada.')).toBeInTheDocument()
    await waitFor(() => expect(calls(fetchMock, '/api/events/1/coverage')).toBe(2))
    expect(calls(fetchMock, '/api/events/1/summary')).toBe(2)
    expect(calls(fetchMock, '/api/events/1/suggestions?participantId=1')).toBe(2)
  })

  it('lets the person take another item right after one was taken', async () => {
    stub({
      '/api/events/1/suggestions?participantId=1': [
        suggestion({ itemId: 7, itemName: 'Lasanha', suggestedQuantity: 3 }),
        suggestion({ itemId: 9, itemName: 'Farofa', suggestedQuantity: 2 }),
      ],
      '/api/events/1/contributions': () => json(201, {}),
    })
    const user = userEvent.setup()
    renderTab({ items: [lasagna, farofa], participants: [ana] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')
    await user.click(await screen.findByRole('button', { name: 'Levar 3 (porção) de Lasanha' }))

    await screen.findByText('Contribuição registrada.')
    await waitFor(() => expect(screen.getByRole('button', { name: 'Levar 2 (porção) de Farofa' })).toBeEnabled())
  })

  it('shows the reason, and keeps the suggestions, when the API refuses', async () => {
    stub({
      '/api/events/1/suggestions?participantId=1': [suggestion({ itemId: 7, itemName: 'Lasanha' })],
      '/api/events/1/contributions': failure(422, 'Só cabem mais 2 (porção) neste item.'),
    })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ items: [lasagna], participants: [ana] })

    await user.selectOptions(await screen.findByLabelText('Quem vai levar?'), 'Ana Souza')
    await user.click(await screen.findByRole('button', { name: /Levar 3/ }))

    expect(await screen.findByText('Só cabem mais 2 (porção) neste item.')).toHaveAttribute('role', 'alert')
    expect(screen.getByRole('button', { name: /Levar 3/ })).toBeEnabled()
    expect(onChanged).not.toHaveBeenCalled()
  })

  it.each([
    [makeEvent({ id: 1, name: 'Almoço', status: 'ENCERRADO', acceptsContributions: false }), 'Só um evento aberto aceita contribuições. Este evento está encerrado.'],
    [makeEvent({ id: 1, name: 'Almoço', status: 'ABERTO', deadlineExpired: true, acceptsContributions: false }), 'O prazo para alterações já passou.'],
  ])('shows the reason, and no way to choose or take, when the event does not take contributions', async (event, reason) => {
    stub()

    renderTab({ event, items: [lasagna], participants: [ana] })

    expect(await screen.findByText(reason)).toBeInTheDocument()
    expect(screen.queryByLabelText('Quem vai levar?')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Levar/ })).not.toBeInTheDocument()
  })
})

describe('SummaryTab: share with the team', () => {
  it('shows the text of the summary', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })

    renderTab({ items: [lasagna] })

    const text = await screen.findByLabelText('Texto do resumo')
    expect(text).toHaveTextContent('*Almoço*')
    expect(text).toHaveTextContent('Sem CPF aqui.')
  })

  it('copies the text and says so', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })
    const user = userEvent.setup()
    const writeText = vi.spyOn(navigator.clipboard, 'writeText')
    renderTab({ items: [lasagna] })

    await user.click(await screen.findByRole('button', { name: 'Copiar texto' }))

    expect(writeText).toHaveBeenCalledWith(SUMMARY)
    expect(await screen.findByText('Texto copiado.')).toBeInTheDocument()
  })

  it('says it could not copy when the browser refuses', async () => {
    stubApi({ ...base, '/api/events/1/coverage': coverage })
    const user = userEvent.setup()
    vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValue(new Error('denied'))
    renderTab({ items: [lasagna] })

    await user.click(await screen.findByRole('button', { name: 'Copiar texto' }))

    expect(await screen.findByText('Não foi possível copiar. Selecione o texto e copie.')).toBeInTheDocument()
  })

  it('shows the failure of the text with a way to try again', async () => {
    let attempts = 0
    stubApi({
      ...base,
      '/api/events/1/coverage': coverage,
      '/api/events/1/summary': () => (++attempts === 1 ? failure(500, 'Erro ao montar o resumo.')() : json(200, { text: SUMMARY })),
    })
    const user = userEvent.setup()
    renderTab({ items: [lasagna] })

    const panel = (await screen.findByText('Erro ao montar o resumo.')).closest('[role="alert"]') as HTMLElement
    await user.click(within(panel).getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByLabelText('Texto do resumo')).toBeInTheDocument()
  })
})
