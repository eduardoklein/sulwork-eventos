import { checkDigit, formatCpf } from '../lib/cpf'

/**
 * Builds a CPF that passes the check digits, so tests can use many different people. It lives only in the tests:
 * the product never offers to make up a CPF.
 */
export function generateValidCpf(seed: number): string {
  const base = String(100_000_000 + seed * 7).padStart(9, '0')
  const withFirst = base + checkDigit(base, 9)
  return withFirst + checkDigit(withFirst, 10)
}

export function generateValidFormattedCpf(seed: number): string {
  return formatCpf(generateValidCpf(seed))
}
