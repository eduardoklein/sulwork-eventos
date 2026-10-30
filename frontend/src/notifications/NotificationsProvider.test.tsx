import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useNotify } from './context'
import { NotificationsProvider, SUCCESS_DURATION_MS } from './NotificationsProvider'

function Buttons() {
  const notify = useNotify()
  return (
    <>
      <button onClick={() => notify('Item criado', 'success')}>Criar</button>
      <button onClick={() => notify('Este item já está completo.', 'error')}>Falhar</button>
    </>
  )
}

function setup() {
  render(<NotificationsProvider><Buttons /></NotificationsProvider>)
}

const click = (name: string) => fireEvent.click(screen.getByRole('button', { name }))

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('notifications', () => {
  it('shows a success and removes it after four seconds', () => {
    setup()

    click('Criar')
    expect(screen.getByText('Item criado')).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(SUCCESS_DURATION_MS - 1))
    expect(screen.getByText('Item criado')).toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByText('Item criado')).not.toBeInTheDocument()
  })

  it('keeps an error until the person closes it', () => {
    setup()

    click('Falhar')
    act(() => vi.advanceTimersByTime(60_000))
    expect(screen.getByText('Este item já está completo.')).toBeInTheDocument()

    click('Fechar aviso')
    expect(screen.queryByText('Este item já está completo.')).not.toBeInTheDocument()
  })

  it('lets the person close a success before the time is up', () => {
    setup()

    click('Criar')
    click('Fechar aviso')

    expect(screen.queryByText('Item criado')).not.toBeInTheDocument()
  })

  it('announces a success politely and an error as an alert', () => {
    setup()

    click('Criar')
    click('Falhar')

    expect(screen.getByRole('status')).toHaveTextContent('Item criado')
    expect(screen.getByRole('alert')).toHaveTextContent('Este item já está completo.')
    expect(screen.getByRole('status')).not.toHaveTextContent('Este item já está completo.')
  })

  it('stacks several notifications and removes each one on its own', () => {
    setup()

    click('Criar')
    act(() => vi.advanceTimersByTime(2000))
    click('Criar')
    expect(screen.getAllByText('Item criado')).toHaveLength(2)

    act(() => vi.advanceTimersByTime(2000))
    expect(screen.getAllByText('Item criado')).toHaveLength(1)
    act(() => vi.advanceTimersByTime(2000))
    expect(screen.queryByText('Item criado')).not.toBeInTheDocument()
  })

  it('fails with a clear message when used without the provider', () => {
    const quiet = vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() => render(<Buttons />)).toThrow('useNotify needs a NotificationsProvider above it.')

    quiet.mockRestore()
  })
})
