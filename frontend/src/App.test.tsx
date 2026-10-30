import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import App from './App'
import { domainOptions, stubApi } from './test/fakeApi'

// jsdom fires `hashchange` in a later task, like a browser, so the tests wait for the screen to change.
async function goTo(hash: string) {
  await act(async () => {
    window.location.hash = hash
    await new Promise((resolve) => setTimeout(resolve))
  })
}

beforeEach(() => {
  stubApi({ '/api/events': [], '/api/domain/options': domainOptions })
  window.location.hash = '#/events'
  document.title = ''
})

describe('App', () => {
  it('shows the application name in the header', () => {
    render(<App />)

    expect(screen.getByRole('link', { name: 'Sulwork Eventos' })).toHaveAttribute('href', '#/events')
  })

  it('links to the API documentation in a new tab and to the new event screen', () => {
    render(<App />)

    const docs = screen.getByRole('link', { name: 'Documentação da API' })
    expect(docs).toHaveAttribute('href', '/swagger-ui.html')
    expect(docs).toHaveAttribute('target', '_blank')
    expect(docs).toHaveAttribute('rel', expect.stringContaining('noopener'))
    expect(screen.getByRole('link', { name: 'Novo evento' })).toHaveAttribute('href', '#/events/new')
  })

  it.each([
    ['#/events', 'Eventos'],
    ['#/events/new', 'Novo evento'],
    ['#/events/5/edit', 'Editar evento'],
    ['#/events/5', 'Evento 5: resumo'],
    ['#/events/5/menu', 'Evento 5: cardápio'],
    ['#/events/5/participants', 'Evento 5: participantes'],
    ['#/events/5/contributions', 'Evento 5: contribuições'],
  ])('shows the screen of %s', (hash, heading) => {
    window.location.hash = hash

    render(<App />)

    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
  })

  it('follows the address when it changes', async () => {
    render(<App />)

    await goTo('#/events/9/menu')

    expect(screen.getByRole('heading', { level: 1, name: 'Evento 9: cardápio' })).toBeInTheDocument()
  })

  it('takes an unknown address to the events list and replaces the address', async () => {
    window.location.hash = '#/nothing-here'

    render(<App />)

    expect(screen.getByRole('heading', { level: 1, name: 'Eventos' })).toBeInTheDocument()
    await waitFor(() => expect(window.location.hash).toBe('#/events'))
  })

  it('takes an empty address and a broken id to the events list too', async () => {
    window.location.hash = ''
    const first = render(<App />)
    await waitFor(() => expect(window.location.hash).toBe('#/events'))
    first.unmount()

    window.location.hash = '#/events/abc'
    render(<App />)
    await waitFor(() => expect(window.location.hash).toBe('#/events'))
  })

  it('puts the screen name in the page title', async () => {
    render(<App />)

    expect(document.title).toBe('Eventos · Sulwork Eventos')
    await goTo('#/events/new')
    expect(document.title).toBe('Novo evento · Sulwork Eventos')
  })

  it('moves the focus to the new screen when the address changes, but not on the first load', async () => {
    render(<App />)
    const main = screen.getByRole('main')
    expect(main).not.toHaveFocus()

    await goTo('#/events/new')

    expect(main).toHaveFocus()
  })

  it('has a skip link that sends the focus to the content without changing the address', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.tab()
    expect(screen.getByRole('button', { name: 'Ir para o conteúdo' })).toHaveFocus()
    await user.keyboard('{Enter}')

    expect(screen.getByRole('main')).toHaveFocus()
    expect(window.location.hash).toBe('#/events')
  })
})
