import type {
  Category,
  CategoryRequest,
  ChangeStatusRequest,
  Collaborator,
  Contribution,
  ContributionFilters,
  Coverage,
  CreateContributionRequest,
  CreateEventRequest,
  CreateItemRequest,
  DietaryAnalysis,
  DomainOptions,
  EnrollParticipantRequest,
  EventFilters,
  FoodEvent,
  Item,
  Participant,
  ProblemDetail,
  Suggestion,
  Summary,
  UpdateCollaboratorRequest,
  UpdateContributionRequest,
  UpdateEventRequest,
  UpdateItemRequest,
} from '../types/api'

const NETWORK_ERROR = 'Não foi possível falar com o servidor. Tente de novo.'

/**
 * Every failed call throws this. `message` is the text to show to the person, and `fieldErrors` maps each invalid
 * field to its message, so a form can mark the field.
 */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type Query = Record<string, string | number | undefined>

async function request<T>(path: string, init: RequestInit = {}, query: Query = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(withQuery(`/api${path}`, query), {
      ...init,
      headers: init.body ? { 'Content-Type': 'application/json' } : undefined,
    })
  } catch {
    throw new ApiError(NETWORK_ERROR, 0)
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: Partial<ProblemDetail> = {}
  try {
    problem = (await response.json()) as ProblemDetail
  } catch {
    // Not a Problem Details body (for example, an HTML page from a proxy): fall back to a generic message.
  }
  const fieldErrors = Object.fromEntries((problem.errors ?? []).map((error) => [error.field, error.message]))
  return new ApiError(problem.detail ?? `Erro inesperado (${response.status}).`, response.status, fieldErrors)
}

function withQuery(path: string, query: Query): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  }
  const search = params.toString()
  return search ? `${path}?${search}` : path
}

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) })

// Events

export const listEvents = (filters: EventFilters = {}) => request<FoodEvent[]>('/events', {}, { ...filters })
export const getEvent = (id: number) => request<FoodEvent>(`/events/${id}`)
export const createEvent = (body: CreateEventRequest) => request<FoodEvent>('/events', json('POST', body))
export const updateEvent = (id: number, body: UpdateEventRequest) =>
  request<FoodEvent>(`/events/${id}`, json('PUT', body))
export const changeEventStatus = (id: number, body: ChangeStatusRequest) =>
  request<FoodEvent>(`/events/${id}/status`, json('PATCH', body))
export const deleteEvent = (id: number) => request<void>(`/events/${id}`, { method: 'DELETE' })

// Categories

export const listCategories = (eventId: number) => request<Category[]>(`/events/${eventId}/categories`)
export const createCategory = (eventId: number, body: CategoryRequest) =>
  request<Category>(`/events/${eventId}/categories`, json('POST', body))
export const updateCategory = (eventId: number, id: number, body: CategoryRequest) =>
  request<Category>(`/events/${eventId}/categories/${id}`, json('PUT', body))
export const deleteCategory = (eventId: number, id: number) =>
  request<void>(`/events/${eventId}/categories/${id}`, { method: 'DELETE' })

// Items

export const listItems = (eventId: number, categoryId?: number) =>
  request<Item[]>(`/events/${eventId}/items`, {}, { categoryId })
export const createItem = (eventId: number, body: CreateItemRequest) =>
  request<Item>(`/events/${eventId}/items`, json('POST', body))
export const updateItem = (eventId: number, id: number, body: UpdateItemRequest) =>
  request<Item>(`/events/${eventId}/items/${id}`, json('PUT', body))
export const deleteItem = (eventId: number, id: number) =>
  request<void>(`/events/${eventId}/items/${id}`, { method: 'DELETE' })

// Participants and collaborators

export const listParticipants = (eventId: number) => request<Participant[]>(`/events/${eventId}/participants`)
export const enrollParticipant = (eventId: number, body: EnrollParticipantRequest) =>
  request<Participant>(`/events/${eventId}/participants`, json('POST', body))
export const removeParticipant = (eventId: number, id: number) =>
  request<void>(`/events/${eventId}/participants/${id}`, { method: 'DELETE' })
export const findCollaboratorByCpf = (cpf: string) => request<Collaborator>('/collaborators', {}, { cpf })
export const updateCollaborator = (id: number, body: UpdateCollaboratorRequest) =>
  request<Collaborator>(`/collaborators/${id}`, json('PUT', body))

// Contributions

export const listContributions = (eventId: number, filters: ContributionFilters = {}) =>
  request<Contribution[]>(`/events/${eventId}/contributions`, {}, { ...filters })
export const createContribution = (eventId: number, body: CreateContributionRequest) =>
  request<Contribution>(`/events/${eventId}/contributions`, json('POST', body))
export const updateContribution = (eventId: number, id: number, body: UpdateContributionRequest) =>
  request<Contribution>(`/events/${eventId}/contributions/${id}`, json('PUT', body))
export const removeContribution = (eventId: number, id: number) =>
  request<void>(`/events/${eventId}/contributions/${id}`, { method: 'DELETE' })

// Coverage, dietary analysis and the assistant

export const getCoverage = (eventId: number) => request<Coverage>(`/events/${eventId}/coverage`)
export const getDietaryAnalysis = (eventId: number) => request<DietaryAnalysis>(`/events/${eventId}/dietary-analysis`)
export const getSuggestions = (eventId: number, participantId: number) =>
  request<Suggestion[]>(`/events/${eventId}/suggestions`, {}, { participantId })
export const getSummary = (eventId: number) => request<Summary>(`/events/${eventId}/summary`)

// Domain options

export const getDomainOptions = () => request<DomainOptions>('/domain/options')
