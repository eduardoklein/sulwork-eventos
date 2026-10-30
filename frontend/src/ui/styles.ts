export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'

const BUTTON_BASE =
  'inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-semibold transition-colors ' +
  'disabled:cursor-not-allowed disabled:opacity-60'

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-pen text-white hover:bg-pen-dark',
  secondary: 'border border-line-strong bg-surface text-ink hover:bg-paper',
  danger: 'border border-danger bg-surface text-danger-ink hover:bg-danger-soft',
  ghost: 'text-pen hover:bg-pen-soft',
}

/** Use one primary button per view: it is the main action of the screen. */
export function buttonClass(variant: ButtonVariant = 'secondary'): string {
  return `${BUTTON_BASE} ${BUTTON_VARIANTS[variant]}`
}

export const labelClass = 'mb-1 block text-sm font-semibold text-ink'

export const fieldClass =
  'w-full rounded-md border border-line-strong bg-surface px-3 py-2 text-base text-ink placeholder:text-ink-soft ' +
  'aria-[invalid=true]:border-danger'

export const fieldErrorClass = 'mt-1 text-sm text-danger-ink'
