import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DomainOptionsProvider } from '../../domain/DomainOptionsProvider'
import { NotificationsProvider } from '../../notifications/NotificationsProvider'
import { domainOptions, failure, makeContribution, makeEvent, makeItem, makeParticipant, stubApi } from '../../test/fakeApi'
import type { Contribution, FoodEvent, Item, Participant } from '../../types/api'
import { ContributionsTab } from './ContributionsTab'

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status })

const ana = makeParticipant({ id: 1, name: 'Ana Souza' })
const bia = makeParticipant({ id: 2, name: 'Bia Lima' })
const lasagna = makeItem({ id: 7, name: 'Lasanha', requiredQuantity: 10, contributedQuantity: 4, pendingQuantity: 6 })
const farofa = makeItem({ id: 8, name: 'Farofa', requiredQuantity: 5, contributedQuantity: 0, pendingQuantity: 5 })
const done = makeItem({ id: 9, name: 'Pão', requiredQuantity: 3, contributedQuantity: 3, pendingQuantity: 0 })

function renderTab({
  event = makeEvent({ id: 1, name: 'Almoço' }),
  items = [lasagna, farofa, done],
  participants = [ana, bia],
  contributions = [],
}: { event?: FoodEvent; items?: Item[]; participants?: Participant[]; contributions?: Contribution[] } = {}) {
  const onChanged = vi.fn()
  render(
    <NotificationsProvider>
      <DomainOptionsProvider>
        <ContributionsTab event={event} items={items} participants={participants} contributions={contributions} onChanged={onChanged} />
      </DomainOptionsProvider>
    </NotificationsProvider>,
  )
  return { onChanged }
}

const sent = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) => {
  const call = fetchMock.mock.calls.find(([input, init]) => input === address && init?.method === method)
  return call ? JSON.parse((call[1]!.body as string | undefined) ?? 'null') : undefined
}
const count = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) =>
  fetchMock.mock.calls.filter(([input, init]) => input === address && init?.method === method).length

const contribution = makeContribution({ id: 3, participantId: 1, participantName: 'Ana Souza', itemId: 7, itemName: 'Lasanha', quantity: 4, note: 'Sem queijo' })

describe('ContributionsTab: the table', () => {
  it('invites the person to register the first contribution', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab()

    expect(screen.getByText('Nenhuma contribuição ainda')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Registrar contribuição' })).toHaveLength(1)
  })

  it('shows the participant, the item with its note, the quantity with the unit and the date', async () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ contributions: [contribution, makeContribution({ id: 4, participantName: 'Bia Lima', itemName: 'Farofa', quantity: 2, unit: 'GRAM' })] })

    const row = (await screen.findByRole('rowheader', { name: 'Ana Souza' })).closest('tr')!
    expect(within(row).getByText('Lasanha')).toBeInTheDocument()
    expect(within(row).getByText('Sem queijo')).toBeInTheDocument()
    expect(within(row).getByText('4 (porção)')).toBeInTheDocument()
    expect(within(row).getByText('29/10/2026 às 10:30')).toBeInTheDocument()
    const other = screen.getByRole('rowheader', { name: 'Bia Lima' }).closest('tr')!
    expect(within(other).getByText('2 (grama)')).toBeInTheDocument()
    expect(within(other).queryByText('Sem queijo')).not.toBeInTheDocument()
  })

  it('rolls inside its own box, so a narrow screen does not roll the whole page', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ contributions: [contribution] })

    expect(screen.getByRole('table').parentElement).toHaveClass('overflow-x-auto')
  })
})

describe('ContributionsTab: registering', () => {
  it('lists only the items that still have room, with how much is missing', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))

    const select = screen.getByLabelText('Item') as HTMLSelectElement
    expect(Array.from(select.options).map((option) => option.text)).toEqual(['Escolha o item', 'Lasanha (faltam 6)', 'Farofa (faltam 5)'])
  })

  it('shows how much fits after the item is chosen', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))
    expect(screen.getByText('Escolha o item para ver quanto cabe.')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Item'), 'Lasanha (faltam 6)')

    expect(screen.getByText('Cabem mais 6 (porção).')).toBeInTheDocument()
  })

  it('registers a contribution, tells the screen to load again and closes the window', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions': () => json(201, contribution) })
    const user = userEvent.setup()
    const { onChanged } = renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))
    const dialog = screen.getByRole('dialog', { name: 'Registrar contribuição' })
    await user.selectOptions(within(dialog).getByLabelText('Participante'), 'Bia Lima')
    await user.selectOptions(within(dialog).getByLabelText('Item'), 'Farofa (faltam 5)')
    await user.type(within(dialog).getByLabelText('Quantidade'), '2')
    await user.type(within(dialog).getByLabelText('Observação (opcional)'), '  Sem cebola ')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1))
    expect(sent(fetchMock, 'POST', '/api/events/1/contributions')).toEqual({ participantId: 2, itemId: 8, quantity: 2, note: 'Sem cebola' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await screen.findByText('Contribuição registrada.')).toBeInTheDocument()
  })

  it('sends no note when the note is empty', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions': () => json(201, contribution) })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))
    const dialog = screen.getByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Participante'), 'Ana Souza')
    await user.selectOptions(within(dialog).getByLabelText('Item'), 'Farofa (faltam 5)')
    await user.type(within(dialog).getByLabelText('Quantidade'), '1')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    await waitFor(() => expect(sent(fetchMock, 'POST', '/api/events/1/contributions')?.note).toBeNull())
  })

  it('does not call the API and marks each field that is wrong', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText('Quantidade'), '0')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    expect(within(dialog).getByText('Escolha o participante.')).toBeInTheDocument()
    expect(within(dialog).getByText('Escolha o item.')).toBeInTheDocument()
    expect(within(dialog).getByText('Informe a quantidade, um número inteiro de 1 ou mais.')).toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/contributions')).toBe(0)
  })

  it('keeps the window open with what was typed when the quantity is more than fits', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions': failure(422, 'Só cabem mais 6 (porção) neste item.') })
    const user = userEvent.setup()
    const { onChanged } = renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))
    const dialog = screen.getByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Participante'), 'Ana Souza')
    await user.selectOptions(within(dialog).getByLabelText('Item'), 'Lasanha (faltam 6)')
    await user.type(within(dialog).getByLabelText('Quantidade'), '9')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    expect(await within(dialog).findByText('Só cabem mais 6 (porção) neste item.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Quantidade')).toHaveValue(9)
    expect(within(dialog).getByLabelText('Participante')).toHaveDisplayValue('Ana Souza')
    expect(within(dialog).getByLabelText('Item')).toHaveDisplayValue('Lasanha (faltam 6)')
    expect(onChanged).not.toHaveBeenCalled()
  })

  it('shows the reason when the person already contributes with the item', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1/contributions': failure(409, 'Esta pessoa já contribui com este item. Edite a contribuição.'),
    })
    const user = userEvent.setup()
    renderTab({ contributions: [contribution] })

    await user.click(screen.getAllByRole('button', { name: 'Registrar contribuição' })[0])
    const dialog = screen.getByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Participante'), 'Ana Souza')
    await user.selectOptions(within(dialog).getByLabelText('Item'), 'Lasanha (faltam 6)')
    await user.type(within(dialog).getByLabelText('Quantidade'), '1')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    expect(await within(dialog).findByText('Esta pessoa já contribui com este item. Edite a contribuição.')).toBeInTheDocument()
  })

  it('closes with Escape without saving', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))
    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/contributions')).toBe(0)
  })

  it('says to enroll participants first when there is nobody, and offers no button', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ participants: [] })

    expect(screen.getByText('Inscreva participantes na aba Participantes para registrar contribuições.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar contribuição' })).not.toBeInTheDocument()
  })

  it('says all the items are complete when none has room left, and offers no button', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ items: [done] })

    expect(screen.getByText('Todos os itens do cardápio já estão completos, ou o cardápio não tem itens.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar contribuição' })).not.toBeInTheDocument()
  })
})

describe('ContributionsTab: editing', () => {
  const editName = 'Editar a contribuição de Ana Souza em Lasanha'

  it('locks the person and the item as text, and shows the most the quantity can be', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ contributions: [contribution] })

    await user.click(screen.getByRole('button', { name: editName }))

    const dialog = screen.getByRole('dialog', { name: 'Editar contribuição' })
    expect(within(dialog).queryByLabelText('Participante')).not.toBeInTheDocument()
    expect(within(dialog).queryByLabelText('Item')).not.toBeInTheDocument()
    expect(within(dialog).getByText('Ana Souza')).toBeInTheDocument()
    expect(within(dialog).getByText('Lasanha')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Quantidade')).toHaveValue(4)
    expect(within(dialog).getByLabelText('Observação (opcional)')).toHaveValue('Sem queijo')
    expect(within(dialog).getByText('O máximo é 10 (porção).')).toBeInTheDocument()
  })

  it('saves with PUT and does not create another contribution', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions/3': () => json(200, contribution) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ contributions: [contribution] })

    await user.click(screen.getByRole('button', { name: editName }))
    const dialog = screen.getByRole('dialog')
    await user.clear(within(dialog).getByLabelText('Quantidade'))
    await user.type(within(dialog).getByLabelText('Quantidade'), '6')
    await user.clear(within(dialog).getByLabelText('Observação (opcional)'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(sent(fetchMock, 'PUT', '/api/events/1/contributions/3')).toEqual({ quantity: 6, note: null })
    expect(count(fetchMock, 'POST', '/api/events/1/contributions')).toBe(0)
    expect(await screen.findByText('Contribuição salva.')).toBeInTheDocument()
  })

  it('keeps the window open with the reason when the quantity is too big', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions/3': failure(422, 'O máximo para esta contribuição é 10 (porção).') })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ contributions: [contribution] })

    await user.click(screen.getByRole('button', { name: editName }))
    const dialog = screen.getByRole('dialog')
    await user.clear(within(dialog).getByLabelText('Quantidade'))
    await user.type(within(dialog).getByLabelText('Quantidade'), '11')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar contribuição' }))

    expect(await within(dialog).findByText('O máximo para esta contribuição é 10 (porção).')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Quantidade')).toHaveValue(11)
    expect(onChanged).not.toHaveBeenCalled()
  })
})

describe('ContributionsTab: removing', () => {
  const removeName = 'Remover a contribuição de Ana Souza em Lasanha'

  it('asks first, and removes after the confirmation', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions/3': () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ contributions: [contribution] })

    await user.click(screen.getByRole('button', { name: removeName }))
    const dialog = screen.getByRole('dialog', { name: 'Remover esta contribuição?' })
    expect(within(dialog).getByText('Ana Souza deixa de levar 4 (porção) de Lasanha.')).toBeInTheDocument()
    expect(count(fetchMock, 'DELETE', '/api/events/1/contributions/3')).toBe(0)
    await user.click(within(dialog).getByRole('button', { name: 'Remover contribuição' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(count(fetchMock, 'DELETE', '/api/events/1/contributions/3')).toBe(1)
    expect(await screen.findByText('Contribuição removida.')).toBeInTheDocument()
  })

  it('does not remove when the person goes back', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ contributions: [contribution] })

    await user.click(screen.getByRole('button', { name: removeName }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Voltar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(count(fetchMock, 'DELETE', '/api/events/1/contributions/3')).toBe(0)
  })

  it('keeps the window open with the reason when the API refuses', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1/contributions/3': failure(422, 'O prazo para alterações já passou.') })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ contributions: [contribution] })

    await user.click(screen.getByRole('button', { name: removeName }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Remover contribuição' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('O prazo para alterações já passou.')
    expect(onChanged).not.toHaveBeenCalled()
  })
})

describe('ContributionsTab: an event that does not take contributions', () => {
  it.each([
    ['RASCUNHO', 'rascunho'],
    ['ENCERRADO', 'encerrado'],
    ['REALIZADO', 'realizado'],
    ['CANCELADO', 'cancelado'],
  ] as const)('explains the status and hides the actions when the event is %s', async (status, word) => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ event: makeEvent({ id: 1, name: 'Almoço', status, acceptsContributions: false }), contributions: [contribution] })

    expect(await screen.findByText(`Só um evento aberto aceita contribuições. Este evento está ${word}.`)).toBeInTheDocument()
    expect(screen.getByRole('rowheader', { name: 'Ana Souza' })).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Ações' })).not.toBeInTheDocument()
  })

  it('explains the deadline and hides the actions when the event is open but the deadline passed', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({
      event: makeEvent({ id: 1, name: 'Almoço', status: 'ABERTO', deadlineExpired: true, acceptsContributions: false }),
      contributions: [contribution],
    })

    expect(screen.getByText('O prazo para alterações já passou.')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('shows no reason and has the actions when the event takes contributions', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ contributions: [contribution] })

    expect(screen.queryByText(/Só um evento aberto|O prazo para alterações/)).not.toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Ações' })).toBeInTheDocument()
  })

  it('says there are no contributions, without an invitation, when the event is locked and the table is empty', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ event: makeEvent({ id: 1, name: 'Almoço', status: 'CANCELADO', acceptsContributions: false }) })

    expect(screen.getByText('Este evento não tem contribuições.')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
