import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DomainOptionsProvider } from '../../domain/DomainOptionsProvider'
import { NotificationsProvider } from '../../notifications/NotificationsProvider'
import { domainOptions, failure, makeAnalysis, makeEvent, makeParticipant, stubApi } from '../../test/fakeApi'
import type { Contribution, DietaryAnalysis, FoodEvent, Participant } from '../../types/api'
import { ParticipantsTab } from './ParticipantsTab'

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status })

function renderTab({
  event = makeEvent({ id: 1, name: 'Almoço' }),
  participants = [],
  contributions = [],
  analysis = makeAnalysis(),
}: { event?: FoodEvent; participants?: Participant[]; contributions?: Contribution[]; analysis?: DietaryAnalysis } = {}) {
  const onChanged = vi.fn()
  const onViewMenu = vi.fn()
  render(
    <NotificationsProvider>
      <DomainOptionsProvider>
        <ParticipantsTab event={event} participants={participants} contributions={contributions} analysis={analysis} onViewMenu={onViewMenu} onChanged={onChanged} />
      </DomainOptionsProvider>
    </NotificationsProvider>,
  )
  return { onChanged, onViewMenu }
}

const sent = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) => {
  const call = fetchMock.mock.calls.find(([input, init]) => input === address && init?.method === method)
  return call ? JSON.parse((call[1]!.body as string | undefined) ?? 'null') : undefined
}
const count = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) =>
  fetchMock.mock.calls.filter(([input, init]) => input === address && init?.method === method).length

const CPF = '39053344705'
const LOOKUP = `/api/collaborators?cpf=${CPF}`
const notFound = failure(404, 'Colaborador não encontrado.')

const ana = makeParticipant({ id: 1, name: 'Ana Souza', restrictions: ['CELIAC_GLUTEN_FREE', 'VEGAN'] })
const bia = makeParticipant({ id: 2, name: 'Bia Lima' })
const analysis = makeAnalysis({
  totalItems: 5,
  participants: [
    { participantId: 1, name: 'Ana Souza', restrictions: ['CELIAC_GLUTEN_FREE', 'VEGAN'], totalItems: 5, safeItems: 3, unsafeItems: [] },
    { participantId: 2, name: 'Bia Lima', restrictions: [], totalItems: 1, safeItems: 1, unsafeItems: [] },
  ],
  restrictions: [{ restriction: 'VEGAN', description: 'Vegano', peopleCount: 1, compatibleItems: 3, totalItems: 5 }],
  alerts: [
    { level: 'CRITICAL', message: 'Só 1 item serve para quem é vegano.', participantId: 1 },
    { level: 'INFO', message: 'Nenhum participante tem alergia a ovo.', participantId: null },
  ],
})

async function openEnroll(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getAllByRole('button', { name: 'Inscrever participante' })[0])
  return screen.getByRole('dialog', { name: 'Inscrever participante' })
}

describe('ParticipantsTab: the list', () => {
  it('invites the person to enroll the first participant', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab()

    expect(screen.getByText('Nenhum participante ainda')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Inscrever participante' })).toHaveLength(1)
  })

  it('shows the name, the masked CPF, the restrictions and how many items are safe', async () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ participants: [ana, bia], analysis })

    const row = (await screen.findByRole('heading', { name: 'Ana Souza' })).closest('li')!
    expect(within(row).getByText('CPF ***.533.447-**')).toBeInTheDocument()
    expect(within(row).getByText('Restrições: Celíaco, sem glúten, Vegano')).toBeInTheDocument()
    expect(within(row).getByText('3 de 5 itens são seguros')).toBeInTheDocument()
    const other = screen.getByRole('heading', { name: 'Bia Lima' }).closest('li')!
    expect(within(other).getByText('Restrições: nenhuma')).toBeInTheDocument()
    expect(within(other).getByText('1 de 1 item é seguro')).toBeInTheDocument()
  })

  it('says nothing about safe items when the menu has no items', () => {
    stubApi({ '/api/domain/options': domainOptions })
    const empty = makeAnalysis({ participants: [{ participantId: 1, name: 'Ana Souza', restrictions: [], totalItems: 0, safeItems: 0, unsafeItems: [] }] })

    renderTab({ participants: [ana], analysis: empty })

    expect(screen.queryByText(/seguro/)).not.toBeInTheDocument()
  })

  it('shows the alerts and the summary by restriction of the event', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ participants: [ana], analysis })

    const panel = screen.getByRole('region', { name: 'Restrições alimentares do evento' })
    expect(within(panel).getByText('Crítico')).toBeInTheDocument()
    expect(within(panel).getByText('Só 1 item serve para quem é vegano.')).toBeInTheDocument()
    expect(within(panel).getByText('Aviso')).toBeInTheDocument()
    expect(within(panel).getByText('Vegano')).toBeInTheDocument()
    expect(within(panel).getByText('1 pessoa · 3 de 5 itens servem')).toBeInTheDocument()
  })

  it('has no panel when there is nothing to say', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ participants: [bia], analysis: makeAnalysis() })

    expect(screen.queryByRole('region', { name: 'Restrições alimentares do evento' })).not.toBeInTheDocument()
  })

  it('opens the menu as the participant', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    const { onViewMenu } = renderTab({ participants: [ana, bia], analysis })

    await user.click(screen.getByRole('button', { name: 'Ver cardápio de Bia Lima' }))

    expect(onViewMenu).toHaveBeenCalledWith(2)
  })

  it.each(['RASCUNHO', 'ENCERRADO', 'REALIZADO', 'CANCELADO'] as const)('offers no enroll or remove when the event is %s, but still the menu and the registration', (status) => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ event: makeEvent({ id: 1, name: 'Almoço', status }), participants: [ana], analysis })

    expect(screen.getByText('Só um evento aberto aceita mudanças nos participantes.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Inscrever participante' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Remover/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver cardápio de Ana Souza' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Editar cadastro de Ana Souza' })).toBeInTheDocument()
  })
})

describe('ParticipantsTab: enrolling', () => {
  it('asks only for the CPF at first, with the confirm button off', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    const dialog = await openEnroll(user)

    expect(within(dialog).getByLabelText('CPF')).toBeInTheDocument()
    expect(within(dialog).queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Inscrever participante' })).toBeDisabled()
  })

  it('masks the CPF while the person types', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), '3905')

    expect(within(dialog).getByLabelText('CPF')).toHaveValue('390.5')
  })

  it('does not look for the person while the CPF is incomplete or invalid, and says when it is invalid', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), '390533447')
    expect(fetchMock.mock.calls.some(([input]) => String(input).startsWith('/api/collaborators'))).toBe(false)
    await user.type(within(dialog).getByLabelText('CPF'), '00')

    expect(within(dialog).getByText('CPF inválido. Confira os números.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('CPF')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchMock.mock.calls.some(([input]) => String(input).startsWith('/api/collaborators'))).toBe(false)
    expect(within(dialog).getByRole('button', { name: 'Inscrever participante' })).toBeDisabled()
  })

  it('asks for name and restrictions when the CPF is new, and enrolls with them', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: notFound, '/api/events/1/participants': () => json(201, ana) })
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)
    expect(await within(dialog).findByText('CPF novo. Preencha o cadastro da pessoa.')).toBeInTheDocument()
    await user.type(within(dialog).getByLabelText('Nome'), '  Ana Souza ')
    await user.click(within(dialog).getByLabelText('Vegano'))
    await user.click(within(dialog).getByRole('button', { name: 'Inscrever participante' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1))
    expect(sent(fetchMock, 'POST', '/api/events/1/participants')).toEqual({ cpf: CPF, name: 'Ana Souza', restrictions: ['VEGAN'] })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await screen.findByText('Participante inscrito.')).toBeInTheDocument()
  })

  it('does not enroll a new person without a name', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: notFound })
    const user = userEvent.setup()
    renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)
    await within(dialog).findByLabelText('Nome')
    await user.click(within(dialog).getByRole('button', { name: 'Inscrever participante' }))

    expect(within(dialog).getByText('Informe o nome da pessoa.')).toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/participants')).toBe(0)
  })

  it('shows the stored data of a registered person, locked, and enrolls with only the CPF', async () => {
    const registered = { id: 70, name: 'Carla Dias', cpf: '***.533.447-**', restrictions: ['LACTOSE_INTOLERANCE'] }
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: registered, '/api/events/1/participants': () => json(201, ana) })
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)

    expect(await within(dialog).findByText('Pessoa já cadastrada. Os dados do cadastro serão mantidos.')).toBeInTheDocument()
    expect(within(dialog).getByText('Carla Dias')).toBeInTheDocument()
    expect(within(dialog).getByText('Restrições: Intolerância à lactose')).toBeInTheDocument()
    expect(within(dialog).queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(within(dialog).queryByRole('checkbox')).not.toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Inscrever participante' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(sent(fetchMock, 'POST', '/api/events/1/participants')).toEqual({ cpf: CPF })
  })

  it('says a registered person with no restrictions has none', async () => {
    stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: { id: 70, name: 'Carla Dias', cpf: '***', restrictions: [] } })
    const user = userEvent.setup()
    renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)

    expect(await within(dialog).findByText('Restrições: nenhuma')).toBeInTheDocument()
  })

  it('does not enroll a person who is already in the event', async () => {
    const enrolled = makeParticipant({ id: 1, name: 'Ana Souza', collaboratorId: 70 })
    stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: { id: 70, name: 'Ana Souza', cpf: '***', restrictions: [] } })
    const user = userEvent.setup()
    renderTab({ participants: [enrolled] })
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)

    expect(await within(dialog).findByText('Esta pessoa já está inscrita neste evento.')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Inscrever participante' })).toBeDisabled()
  })

  it('goes back to the first step when the CPF changes after the lookup', async () => {
    stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: notFound })
    const user = userEvent.setup()
    renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)
    await within(dialog).findByLabelText('Nome')
    await user.type(within(dialog).getByLabelText('CPF'), '{Backspace}')

    expect(within(dialog).queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Inscrever participante' })).toBeDisabled()
  })

  it('shows the reason, and keeps the button off, when the lookup fails', async () => {
    stubApi({ '/api/domain/options': domainOptions, [LOOKUP]: failure(500, 'Erro interno do servidor.') })
    const user = userEvent.setup()
    renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)

    expect(await within(dialog).findByText('Erro interno do servidor.')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Inscrever participante' })).toBeDisabled()
  })

  it('keeps the window open with the reason when the API refuses the enrollment', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      [LOOKUP]: notFound,
      '/api/events/1/participants': failure(422, 'Só um evento aberto aceita mudanças nos participantes.'),
    })
    const user = userEvent.setup()
    const { onChanged } = renderTab()
    const dialog = await openEnroll(user)

    await user.type(within(dialog).getByLabelText('CPF'), CPF)
    await user.type(await within(dialog).findByLabelText('Nome'), 'Ana')
    await user.click(within(dialog).getByRole('button', { name: 'Inscrever participante' }))

    expect(await within(dialog).findByText('Só um evento aberto aceita mudanças nos participantes.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Ana')
    expect(onChanged).not.toHaveBeenCalled()
  })

  it('closes with Escape without enrolling', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()
    await openEnroll(user)

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/participants')).toBe(0)
  })
})

describe('ParticipantsTab: editing the registration', () => {
  it('shows the data of the person, with the CPF that does not change, and saves with PUT', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/collaborators/10': () => json(200, ana) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ participants: [ana], analysis })

    await user.click(screen.getByRole('button', { name: 'Editar cadastro de Ana Souza' }))
    const dialog = screen.getByRole('dialog', { name: 'Editar cadastro' })
    expect(within(dialog).getByText('***.533.447-**')).toBeInTheDocument()
    expect(within(dialog).queryByLabelText('CPF')).not.toBeInTheDocument()
    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Ana Souza')
    expect(within(dialog).getByLabelText('Vegano')).toBeChecked()
    await user.clear(within(dialog).getByLabelText('Nome'))
    await user.type(within(dialog).getByLabelText('Nome'), 'Ana Souza Lima')
    await user.click(within(dialog).getByLabelText('Vegano'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar cadastro' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(sent(fetchMock, 'PUT', '/api/collaborators/10')).toEqual({ name: 'Ana Souza Lima', restrictions: ['CELIAC_GLUTEN_FREE'] })
    expect(await screen.findByText('Cadastro salvo.')).toBeInTheDocument()
  })

  it('can clear all the restrictions', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/collaborators/10': () => json(200, ana) })
    const user = userEvent.setup()
    renderTab({ participants: [ana], analysis })

    await user.click(screen.getByRole('button', { name: 'Editar cadastro de Ana Souza' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByLabelText('Vegano'))
    await user.click(within(dialog).getByLabelText('Celíaco, sem glúten'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar cadastro' }))

    await waitFor(() => expect(sent(fetchMock, 'PUT', '/api/collaborators/10')).toEqual({ name: 'Ana Souza', restrictions: [] }))
  })

  it('does not call the API when the name is empty', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ participants: [ana], analysis })

    await user.click(screen.getByRole('button', { name: 'Editar cadastro de Ana Souza' }))
    const dialog = screen.getByRole('dialog')
    await user.clear(within(dialog).getByLabelText('Nome'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar cadastro' }))

    expect(within(dialog).getByText('Informe o nome da pessoa.')).toBeInTheDocument()
    expect(count(fetchMock, 'PUT', '/api/collaborators/10')).toBe(0)
  })

  it('keeps the window open with what was typed when the API refuses', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/collaborators/10': failure(422, 'Dados inválidos.') })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ participants: [ana], analysis })

    await user.click(screen.getByRole('button', { name: 'Editar cadastro de Ana Souza' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText('Nome'), ' Lima')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar cadastro' }))

    expect(await within(dialog).findByText('Dados inválidos.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Ana Souza Lima')
    expect(onChanged).not.toHaveBeenCalled()
  })
})

describe('ParticipantsTab: removing', () => {
  const contribution = (id: number, participantId: number) => ({ id, participantId }) as Contribution

  it('asks first, says how many contributions leave with the person, and removes', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/participants/1': () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ participants: [ana, bia], contributions: [contribution(1, 1), contribution(2, 1), contribution(3, 2)], analysis })

    await user.click(screen.getByRole('button', { name: 'Remover Ana Souza' }))
    const dialog = screen.getByRole('dialog', { name: 'Remover este participante?' })
    expect(within(dialog).getByText('Ana Souza sai do evento, e as 2 contribuições dessa pessoa também são removidas.')).toBeInTheDocument()
    expect(count(fetchMock, 'DELETE', '/api/events/1/participants/1')).toBe(0)
    await user.click(within(dialog).getByRole('button', { name: 'Remover participante' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(count(fetchMock, 'DELETE', '/api/events/1/participants/1')).toBe(1)
    expect(await screen.findByText('Participante removido.')).toBeInTheDocument()
  })

  it('uses the singular for one contribution, and says nothing about contributions when there is none', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ participants: [ana, bia], contributions: [contribution(3, 2)], analysis })

    await user.click(screen.getByRole('button', { name: 'Remover Bia Lima' }))
    expect(screen.getByText('Bia Lima sai do evento, e a contribuição dessa pessoa também é removida.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Voltar' }))
    await user.click(screen.getByRole('button', { name: 'Remover Ana Souza' }))

    expect(screen.getByText('Ana Souza sai do evento.')).toBeInTheDocument()
  })

  it('does not remove when the person goes back', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ participants: [ana], analysis })

    await user.click(screen.getByRole('button', { name: 'Remover Ana Souza' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Voltar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(count(fetchMock, 'DELETE', '/api/events/1/participants/1')).toBe(0)
  })

  it('keeps the window open with the reason when the API refuses', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1/participants/1': failure(422, 'Só um evento aberto aceita mudanças nos participantes.') })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ participants: [ana], analysis })

    await user.click(screen.getByRole('button', { name: 'Remover Ana Souza' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Remover participante' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('Só um evento aberto aceita mudanças nos participantes.')
    expect(onChanged).not.toHaveBeenCalled()
  })
})
