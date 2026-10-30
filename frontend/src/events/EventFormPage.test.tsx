import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DomainOptionsProvider } from '../domain/DomainOptionsProvider'
import { NotificationsProvider } from '../notifications/NotificationsProvider'
import { domainOptions, failure, makeEvent, stubApi } from '../test/fakeApi'
import { EventFormPage } from './EventFormPage'

function renderPage(eventId?: number) {
  return render(
    <NotificationsProvider>
      <DomainOptionsProvider>
        <EventFormPage eventId={eventId} />
      </DomainOptionsProvider>
    </NotificationsProvider>,
  )
}

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status })

function bodyOfLastCall(fetchMock: ReturnType<typeof stubApi>, method: string) {
  const call = fetchMock.mock.calls.find(([, init]) => init?.method === method)
  return call ? JSON.parse(call[1]!.body as string) : undefined
}

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('Nome'), 'Almoço de fim de ano')
  await waitFor(() => expect(screen.getByLabelText('Tipo')).toBeEnabled())
  await user.selectOptions(screen.getByLabelText('Tipo'), 'LUNCH')
  fireEvent.change(screen.getByLabelText('Data e hora do evento'), { target: { value: '2026-12-18T12:00' } })
  fireEvent.change(screen.getByLabelText('Prazo para contribuições'), { target: { value: '2026-12-13T18:00' } })
}

beforeEach(() => {
  window.location.hash = '#/events/new'
  vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: 'America/Sao_Paulo' } as Intl.ResolvedDateTimeFormatOptions)
})

afterEach(() => vi.restoreAllMocks())

describe('EventFormPage: creating', () => {
  it('starts as a draft, with the time zone of the browser behind the "Alterar fuso" link', async () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderPage()

    expect(await screen.findByLabelText('Rascunho')).toBeChecked()
    expect(screen.getByLabelText('Aberto')).not.toBeChecked()
    expect(screen.getByText('Brasília (UTC-3)')).toBeInTheDocument()
    expect(screen.queryByLabelText('Fuso horário')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Alterar fuso' })).toBeInTheDocument()
  })

  it('sends the situation that was chosen and goes to the event', async () => {
    const fetchMock = stubApi({
      '/api/domain/options': domainOptions,
      '/api/events': () => json(201, makeEvent({ id: 9, name: 'Almoço de fim de ano' })),
    })
    const user = userEvent.setup()
    renderPage()

    await fillRequiredFields(user)
    await user.click(screen.getByLabelText('Aberto'))
    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    await waitFor(() => expect(window.location.hash).toBe('#/events/9/summary'))
    expect(bodyOfLastCall(fetchMock, 'POST')).toEqual({
      name: 'Almoço de fim de ano',
      description: null,
      type: 'LUNCH',
      dateTime: '2026-12-18T12:00:00',
      changeDeadline: '2026-12-13T18:00:00',
      timeZone: 'America/Sao_Paulo',
      status: 'ABERTO',
    })
    expect(await screen.findByText('Evento criado.')).toBeInTheDocument()
  })

  it('creates a draft when the person does not change the situation', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events': () => json(201, makeEvent({ id: 9, name: 'X' })) })
    const user = userEvent.setup()
    renderPage()

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    await waitFor(() => expect(bodyOfLastCall(fetchMock, 'POST')?.status).toBe('RASCUNHO'))
  })

  it('sends the zone chosen after "Alterar fuso", with the four Brazilian zones to pick from', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events': () => json(201, makeEvent({ id: 9, name: 'X' })) })
    const user = userEvent.setup()
    renderPage()

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Alterar fuso' }))
    const zones = screen.getByLabelText('Fuso horário')
    expect(Array.from((zones as HTMLSelectElement).options).map((option) => option.text)).toEqual([
      'Fernando de Noronha (UTC-2)',
      'Brasília (UTC-3)',
      'Manaus (UTC-4)',
      'Rio Branco (UTC-5)',
    ])
    await user.selectOptions(zones, 'America/Manaus')
    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    await waitFor(() => expect(bodyOfLastCall(fetchMock, 'POST')?.timeZone).toBe('America/Manaus'))
  })

  it('uses the zone of the list with the same offset when the browser zone is not in it', async () => {
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: 'America/Cuiaba' } as Intl.ResolvedDateTimeFormatOptions)
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events': () => json(201, makeEvent({ id: 9, name: 'X' })) })
    const user = userEvent.setup()
    renderPage()

    await fillRequiredFields(user)
    expect(screen.getByText('Manaus (UTC-4)')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    await waitFor(() => expect(bodyOfLastCall(fetchMock, 'POST')?.timeZone).toBe('America/Manaus'))
  })

  it('keeps the zone of an event that is not in the short list when editing', async () => {
    const fetchMock = stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1': (init?: RequestInit) =>
        init?.method === 'PUT' ? json(200, makeEvent({ id: 1, name: 'X' })) : json(200, makeEvent({ id: 1, name: 'X', timeZone: 'Europe/Lisbon' })),
    })
    const user = userEvent.setup()
    renderPage(1)

    expect(await screen.findByText('Europe/Lisbon')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(bodyOfLastCall(fetchMock, 'PUT')?.timeZone).toBe('Europe/Lisbon'))
  })

  it('does not call the API and marks each required field that is empty', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Criar evento' }))

    expect(await screen.findByText('Informe o nome do evento.')).toBeInTheDocument()
    expect(screen.getByText('Escolha o tipo do evento.')).toBeInTheDocument()
    expect(screen.getByText('Informe a data e a hora do evento.')).toBeInTheDocument()
    expect(screen.getByText('Informe o prazo para contribuições.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Corrija os campos destacados.')).toHaveAttribute('role', 'alert')
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })

  it('marks the field the API rejects and stays on the form', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events': () =>
        json(400, { status: 400, detail: 'Dados inválidos.', errors: [{ field: 'name', message: 'O nome é obrigatório.' }] }),
    })
    const user = userEvent.setup()
    renderPage()

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    expect(await screen.findByText('O nome é obrigatório.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Dados inválidos.')).toHaveAttribute('role', 'alert')
    expect(window.location.hash).toBe('#/events/new')
  })

  it('shows a business rule message, keeps what was typed and lets the person try again', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events': failure(422, 'O prazo não pode ser depois da data do evento.'),
    })
    const user = userEvent.setup()
    renderPage()

    await fillRequiredFields(user)
    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    expect(await screen.findByText('O prazo não pode ser depois da data do evento.')).toHaveAttribute('role', 'alert')
    expect(screen.getByLabelText('Nome')).toHaveValue('Almoço de fim de ano')
    expect(screen.getByRole('button', { name: 'Criar evento' })).toBeEnabled()
  })

  it('says it could not save when the network fails', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderPage()
    await fillRequiredFields(user)
    globalThis.fetch = (() => Promise.reject(new TypeError('Failed to fetch'))) as typeof fetch

    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    expect(await screen.findByText('Não foi possível falar com o servidor. Tente de novo.')).toHaveAttribute('role', 'alert')
  })

  it('blocks a second click while it saves', async () => {
    let finish: (response: Response) => void = () => {}
    const fetchMock = stubApi({
      '/api/domain/options': domainOptions,
      '/api/events': () => new Promise<Response>((resolve) => (finish = resolve)) as unknown as Response,
    })
    const user = userEvent.setup()
    renderPage()
    await fillRequiredFields(user)

    await user.click(screen.getByRole('button', { name: 'Criar evento' }))

    const saving = await screen.findByRole('button', { name: 'Salvando…' })
    expect(saving).toBeDisabled()
    await user.click(saving)
    expect(fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST')).toHaveLength(1)
    finish(json(201, makeEvent({ id: 9, name: 'X' })))
    await waitFor(() => expect(window.location.hash).toBe('#/events/9/summary'))
  })
})

describe('EventFormPage: editing', () => {
  const stored = makeEvent({ id: 1, name: 'Almoço de fim de ano', description: 'No salão', status: 'ABERTO', timeZone: 'America/Manaus' })

  it('comes filled with the event, and has no situation field', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1': stored })

    renderPage(1)

    expect(await screen.findByLabelText('Nome')).toHaveValue('Almoço de fim de ano')
    expect(screen.getByLabelText('Descrição (opcional)')).toHaveValue('No salão')
    await waitFor(() => expect(screen.getByLabelText('Tipo')).toHaveValue('LUNCH'))
    expect(screen.getByLabelText('Data e hora do evento')).toHaveValue('2026-12-18T12:00')
    expect(screen.getByLabelText('Prazo para contribuições')).toHaveValue('2026-12-13T18:00')
    expect(screen.getByText('Manaus (UTC-4)')).toBeInTheDocument()
    expect(screen.queryByText('Situação inicial')).not.toBeInTheDocument()
  })

  it('saves the change with PUT, without a situation, and goes to the event', async () => {
    const fetchMock = stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1': (init?: RequestInit) => (init?.method === 'PUT' ? json(200, { ...stored, name: 'Almoço' }) : json(200, stored)),
    })
    const user = userEvent.setup()
    renderPage(1)

    const name = await screen.findByLabelText('Nome')
    await user.clear(name)
    await user.type(name, 'Almoço')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(window.location.hash).toBe('#/events/1/summary'))
    const body = bodyOfLastCall(fetchMock, 'PUT')
    expect(body).toMatchObject({ name: 'Almoço', description: 'No salão', timeZone: 'America/Manaus' })
    expect(body).not.toHaveProperty('status')
    expect(await screen.findByText('Evento salvo.')).toBeInTheDocument()
  })

  it('shows the error of the API and stays on the form', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1': (init?: RequestInit) =>
        init?.method === 'PUT' ? failure(422, 'O prazo de um evento aberto precisa estar no futuro.')() : json(200, stored),
    })
    const user = userEvent.setup()
    renderPage(1)

    await user.click(await screen.findByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText('O prazo de um evento aberto precisa estar no futuro.')).toHaveAttribute('role', 'alert')
    expect(screen.getByLabelText('Nome')).toHaveValue('Almoço de fim de ano')
  })

  it('freezes the deadline and the zone of a closed event, and says why', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1': { ...stored, status: 'ENCERRADO' } })

    renderPage(1)

    expect(await screen.findByLabelText('Prazo para contribuições')).toBeDisabled()
    expect(screen.getByText('Em um evento encerrado, o prazo não pode mudar.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Alterar fuso' })).not.toBeInTheDocument()
    expect(screen.getByText(/o fuso não pode mudar/)).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toBeEnabled()
  })

  it.each(['REALIZADO', 'CANCELADO'] as const)('does not show the form of an event that is %s', async (status) => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1': { ...stored, status } })

    renderPage(1)

    expect(await screen.findByText('Este evento não pode ser editado.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voltar ao evento' })).toHaveAttribute('href', '#/events/1/summary')
  })

  it('says the event does not exist, and tries again on request', async () => {
    let attempts = 0
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1': () => (++attempts === 1 ? failure(404, 'Evento não encontrado.')() : json(200, stored)),
    })
    const user = userEvent.setup()
    renderPage(1)

    expect((await screen.findByText('Evento não encontrado.')).closest('[role="alert"]')).not.toBeNull()
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByLabelText('Nome')).toHaveValue('Almoço de fim de ano')
  })
})
