import { describe, expect, it } from 'vitest'
import { makeEvent } from '../test/fakeApi'
import { editRuleFor, emptyValues, toCreateRequest, toUpdateRequest, validate, valuesOf, type EventFormValues } from './eventFormValues'

const filled: EventFormValues = {
  name: '  Almoço de fim de ano ',
  description: '',
  type: 'LUNCH',
  status: 'ABERTO',
  dateTime: '2026-12-18T12:00',
  changeDeadline: '2026-12-13T18:00',
  timeZone: 'America/Manaus',
}

describe('validate', () => {
  it('accepts a form with every required field', () => {
    expect(validate(filled)).toEqual({})
  })

  it('names each required field that is empty', () => {
    expect(validate({ ...emptyValues(), name: '   ' })).toEqual({
      name: 'Informe o nome do evento.',
      type: 'Escolha o tipo do evento.',
      dateTime: 'Informe a data e a hora do evento.',
      changeDeadline: 'Informe o prazo para contribuições.',
    })
  })
})

describe('toCreateRequest', () => {
  it('trims the name, adds the seconds, and sends an empty description as null', () => {
    expect(toCreateRequest(filled)).toEqual({
      name: 'Almoço de fim de ano',
      description: null,
      type: 'LUNCH',
      dateTime: '2026-12-18T12:00:00',
      changeDeadline: '2026-12-13T18:00:00',
      timeZone: 'America/Manaus',
      status: 'ABERTO',
    })
  })

  it('keeps seconds that are already there', () => {
    expect(toCreateRequest({ ...filled, dateTime: '2026-12-18T12:00:30' }).dateTime).toBe('2026-12-18T12:00:30')
  })
})

describe('toUpdateRequest', () => {
  it('has no status, because a change of situation goes through its own route', () => {
    expect(toUpdateRequest(filled)).not.toHaveProperty('status')
  })
})

describe('valuesOf', () => {
  it('fills the form from an event, without the seconds the field does not show', () => {
    const values = valuesOf(makeEvent({ id: 1, name: 'Café', description: 'Com pão de queijo', type: 'BREAKFAST' }))

    expect(values).toMatchObject({
      name: 'Café',
      description: 'Com pão de queijo',
      type: 'BREAKFAST',
      dateTime: '2026-12-18T12:00',
      changeDeadline: '2026-12-13T18:00',
      timeZone: 'America/Sao_Paulo',
    })
  })

  it('shows a missing description as empty text', () => {
    expect(valuesOf(makeEvent({ id: 1, name: 'Café', description: null })).description).toBe('')
  })
})

describe('editRuleFor', () => {
  it.each([
    ['RASCUNHO', 'free'],
    ['ABERTO', 'free'],
    ['ENCERRADO', 'deadline-frozen'],
    ['REALIZADO', 'locked'],
    ['CANCELADO', 'locked'],
  ] as const)('%s: %s', (status, rule) => {
    expect(editRuleFor(status)).toBe(rule)
  })
})
