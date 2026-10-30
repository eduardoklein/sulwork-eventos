import { describe, expect, it } from 'vitest'
import { generateValidCpf, generateValidFormattedCpf } from '../test/cpf'
import { formatCpf, isValidCpf, onlyDigits } from './cpf'

describe('formatCpf', () => {
  it.each([
    ['', ''],
    ['3', '3'],
    ['390', '390'],
    ['3905', '390.5'],
    ['390533', '390.533'],
    ['3905334', '390.533.4'],
    ['390533447', '390.533.447'],
    ['3905334470', '390.533.447-0'],
    ['39053344705', '390.533.447-05'],
  ])('masks %s as %s while typing', (typed, masked) => {
    expect(formatCpf(typed)).toBe(masked)
  })

  it('drops letters and keeps at most eleven digits', () => {
    expect(formatCpf('a390.533b447-05999')).toBe('390.533.447-05')
  })

  it('keeps the mask stable when it receives an already masked value', () => {
    expect(formatCpf('390.533.447-05')).toBe('390.533.447-05')
  })
})

describe('isValidCpf', () => {
  it.each(['39053344705', '390.533.447-05', '52998224725', ' 529.982.247-25 '])('accepts %s', (cpf) => {
    expect(isValidCpf(cpf)).toBe(true)
  })

  it.each([
    ['a wrong check digit', '39053344706'],
    ['too few digits', '3905334470'],
    ['too many digits', '390533447051'],
    ['repeated digits', '111.111.111-11'],
    ['a partial mask', '390.53344705'],
    ['letters', '390.533.447-0x'],
    ['an empty value', ''],
  ])('refuses %s', (_case, cpf) => {
    expect(isValidCpf(cpf)).toBe(false)
  })
})

describe('onlyDigits', () => {
  it('removes everything that is not a digit', () => {
    expect(onlyDigits('390.533.447-05')).toBe('39053344705')
  })
})

describe('test CPF generator', () => {
  it('makes valid and distinct CPFs, with or without mask', () => {
    const cpfs = [0, 1, 2, 3, 4].map(generateValidCpf)

    expect(cpfs.every(isValidCpf)).toBe(true)
    expect(new Set(cpfs).size).toBe(5)
    expect(isValidCpf(generateValidFormattedCpf(7))).toBe(true)
  })
})
