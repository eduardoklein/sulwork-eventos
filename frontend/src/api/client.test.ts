import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, createEvent, deleteEvent, findCollaboratorByCpf, listEvents } from './client'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
  fetchMock.mockReset()
})

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

async function caught(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise
  } catch (error) {
    return error as ApiError
  }
  throw new Error('The call should have failed.')
}

describe('API client', () => {
  it('calls the API under /api and sends only the filters that have a value', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, [{ id: 1, name: 'Almoço' }]))

    const events = await listEvents({ status: 'ABERTO', from: '2026-11-01', type: undefined })

    expect(events).toEqual([{ id: 1, name: 'Almoço' }])
    expect(fetchMock).toHaveBeenCalledWith('/api/events?status=ABERTO&from=2026-11-01', expect.anything())
  })

  it('does not send a content type on a request without body', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []))

    await listEvents()

    expect(fetchMock).toHaveBeenCalledWith('/api/events', { headers: undefined })
  })

  it('sends the body as JSON with the method of the route', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 7 }))
    const body = { name: 'Almoço', type: 'LUNCH' as const, dateTime: '2026-12-01T12:00:00', changeDeadline: '2026-11-30T12:00:00' }

    await createEvent(body)

    expect(fetchMock).toHaveBeenCalledWith('/api/events', {
      method: 'POST',
      body: JSON.stringify(body),
      headers: { 'Content-Type': 'application/json' },
    })
  })

  it('returns nothing for a 204 response', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(deleteEvent(3)).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledWith('/api/events/3', { method: 'DELETE', headers: undefined })
  })

  it('keeps the masked CPF readable in the query', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 1 }))

    await findCollaboratorByCpf('390.533.447-05')

    expect(fetchMock).toHaveBeenCalledWith('/api/collaborators?cpf=390.533.447-05', expect.anything())
  })

  it('turns a validation error into an ApiError with the message of each field', async () => {
    fetchMock.mockResolvedValue(jsonResponse(400, {
      status: 400,
      detail: 'Dados inválidos.',
      errors: [
        { field: 'name', message: 'não deve estar em branco' },
        { field: 'type', message: 'não deve ser nulo' },
      ],
    }))

    const error = await caught(listEvents())

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toBeInstanceOf(Error)
    expect(error.message).toBe('Dados inválidos.')
    expect(error.status).toBe(400)
    expect(error.fieldErrors).toEqual({ name: 'não deve estar em branco', type: 'não deve ser nulo' })
  })

  it('uses the message of a business rule error and has no field errors', async () => {
    fetchMock.mockResolvedValue(jsonResponse(422, { status: 422, detail: 'Este item já está completo.' }))

    const error = await caught(listEvents())

    expect(error.message).toBe('Este item já está completo.')
    expect(error.status).toBe(422)
    expect(error.fieldErrors).toEqual({})
  })

  it('falls back to a generic message when the error body is not JSON', async () => {
    fetchMock.mockResolvedValue(new Response('<html>Bad Gateway</html>', { status: 502 }))

    const error = await caught(listEvents())

    expect(error.message).toBe('Erro inesperado (502).')
    expect(error.status).toBe(502)
  })

  it('turns a network failure into an ApiError with status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await caught(listEvents())

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(0)
    expect(error.message).toBe('Não foi possível falar com o servidor. Tente de novo.')
  })
})
