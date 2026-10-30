import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { Modal } from './Modal'

function Opener() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button onClick={() => setOpen(true)}>Adicionar item</button>
      <Modal open={open} title="Novo item" onClose={() => setOpen(false)}>
        <input aria-label="Nome" />
        <button>Salvar item</button>
      </Modal>
    </>
  )
}

describe('Modal', () => {
  it('renders nothing while closed', () => {
    render(<Modal open={false} title="Novo item" onClose={() => {}}>conteúdo</Modal>)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('is a dialog named by its title', () => {
    render(<Modal open title="Novo item" onClose={() => {}}>conteúdo</Modal>)

    expect(screen.getByRole('dialog', { name: 'Novo item' })).toHaveAttribute('aria-modal', 'true')
  })

  it('closes with Escape', async () => {
    const onClose = vi.fn()
    render(<Modal open title="Novo item" onClose={onClose}>conteúdo</Modal>)

    await userEvent.keyboard('{Escape}')

    expect(onClose).toHaveBeenCalledOnce()
  })

  it('closes with a click outside but not with a click inside', async () => {
    const onClose = vi.fn()
    render(<Modal open title="Novo item" onClose={onClose}><p>conteúdo</p></Modal>)

    await userEvent.click(screen.getByText('conteúdo'))
    expect(onClose).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole('dialog').parentElement!)
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('moves the focus inside, keeps it there, and gives it back to the opener', async () => {
    const user = userEvent.setup()
    render(<Opener />)
    const opener = screen.getByRole('button', { name: 'Adicionar item' })

    await user.click(opener)
    expect(screen.getByRole('textbox', { name: 'Nome' })).toHaveFocus()

    await user.tab()
    expect(screen.getByRole('button', { name: 'Salvar item' })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole('textbox', { name: 'Nome' })).toHaveFocus()
    await user.tab({ shift: true })
    expect(screen.getByRole('button', { name: 'Salvar item' })).toHaveFocus()

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })
})
