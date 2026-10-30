import { vi } from 'vitest'
import type { DomainOptions, FoodEvent } from '../types/api'

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

/**
 * Replaces `fetch` with a table of answers by address (`/api/events`, `/api/events/3/coverage`...). A value can be
 * a function, to build a failure or an answer that changes. An address that is not in the table answers 404.
 */
export function stubApi(routes: Record<string, unknown>) {
  const fetchMock = vi.fn(async (input: string) => {
    if (!(input in routes)) {
      return json(404, { status: 404, detail: 'Recurso não encontrado.' })
    }
    const answer = routes[input]
    return typeof answer === 'function' ? (answer as () => Response)() : json(200, answer)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

export function failure(status: number, detail: string): () => Response {
  return () => json(status, { status, detail })
}

export const domainOptions: DomainOptions = {
  eventTypes: [
    { code: 'BREAKFAST', description: 'Café da Manhã' },
    { code: 'LUNCH', description: 'Almoço' },
    { code: 'SNACK', description: 'Lanche' },
    { code: 'GET_TOGETHER', description: 'Confraternização' },
    { code: 'OTHER', description: 'Outro' },
  ],
  eventStatuses: [
    { code: 'RASCUNHO', description: 'Rascunho' },
    { code: 'ABERTO', description: 'Aberto' },
    { code: 'ENCERRADO', description: 'Encerrado' },
    { code: 'REALIZADO', description: 'Realizado' },
    { code: 'CANCELADO', description: 'Cancelado' },
  ],
  measureUnits: [],
  allergens: [],
  foodCharacteristics: [],
  dietaryRestrictions: [],
  eventReadiness: [
    { code: 'NO_ITEMS', description: 'Sem itens' },
    { code: 'CRITICAL', description: 'Crítico' },
    { code: 'IN_PROGRESS', description: 'Em andamento' },
    { code: 'MANDATORY_SECURED', description: 'Obrigatórios garantidos' },
    { code: 'COMPLETE', description: 'Completo' },
  ],
}

export function makeEvent(overrides: Partial<FoodEvent> & Pick<FoodEvent, 'id' | 'name'>): FoodEvent {
  return {
    description: null,
    type: 'LUNCH',
    dateTime: '2026-12-18T12:00:00',
    changeDeadline: '2026-12-13T18:00:00',
    timeZone: 'America/Sao_Paulo',
    status: 'ABERTO',
    deadlineExpired: false,
    acceptsContributions: true,
    allowedTransitions: ['ENCERRADO', 'CANCELADO'],
    createdAt: '2026-10-29T10:00:00',
    updatedAt: '2026-10-29T10:00:00',
    ...overrides,
  }
}

export function makeCoverage(overallPercentage: number | null, readiness: string = 'IN_PROGRESS') {
  return {
    readiness,
    overallPercentage,
    mandatoryPercentage: 100,
    optionalPercentage: 0,
    totalItems: overallPercentage === null ? 0 : 3,
    categories: [],
  }
}
