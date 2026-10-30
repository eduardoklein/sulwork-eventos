// CPF helpers for the forms. The backend checks the CPF again: these only help the person while typing.

const FORMAT = /^(\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/

export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '')
}

/** Masks the CPF while the person types (390.533.447-05), keeping at most 11 digits. */
export function formatCpf(value: string): string {
  const digits = onlyDigits(value).slice(0, 11)
  if (digits.length <= 3) return digits
  if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`
  if (digits.length <= 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`
}

/** The same rules as the backend: 11 digits or the full mask, not all equal, and both check digits right. */
export function isValidCpf(value: string): boolean {
  const trimmed = value.trim()
  if (!FORMAT.test(trimmed)) return false
  const digits = onlyDigits(trimmed)
  if (new Set(digits).size === 1) return false
  return checkDigit(digits, 9) === Number(digits[9]) && checkDigit(digits, 10) === Number(digits[10])
}

/** The digit that comes after the first `length` positions (modulo 11). */
export function checkDigit(digits: string, length: number): number {
  let sum = 0
  for (let i = 0; i < length; i++) {
    sum += Number(digits[i]) * (length + 1 - i)
  }
  const remainder = (sum * 10) % 11
  return remainder === 10 ? 0 : remainder
}
