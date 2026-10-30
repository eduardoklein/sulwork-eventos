import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Chip } from './Chip'
import { EmptyState } from './EmptyState'
import { Section } from './Section'
import { StatusPill } from './StatusPill'
import { buttonClass } from './styles'

describe('shared pieces', () => {
  it('shows the status label that comes from the API', () => {
    render(<StatusPill status="ABERTO" label="Aberto" />)

    expect(screen.getByText('Aberto')).toHaveAttribute('data-status', 'ABERTO')
  })

  it('gives each chip its meaning through the tone', () => {
    render(<Chip tone="danger">Contém glúten</Chip>)

    expect(screen.getByText('Contém glúten')).toHaveAttribute('data-tone', 'danger')
  })

  it('names the section by its title', () => {
    render(<Section title="Cardápio" description="Os itens do evento.">conteúdo</Section>)

    expect(screen.getByRole('region', { name: 'Cardápio' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Cardápio' })).toBeInTheDocument()
  })

  it('invites to act when a list is empty', () => {
    render(<EmptyState title="Monte o cardápio" text="Crie uma categoria para começar." action={<button>Criar categoria</button>} />)

    expect(screen.getByText('Monte o cardápio')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar categoria' })).toBeInTheDocument()
  })

  it('builds a different class for each button variant', () => {
    const variants = (['primary', 'secondary', 'danger', 'ghost'] as const).map(buttonClass)

    expect(new Set(variants).size).toBe(4)
    expect(buttonClass()).toBe(buttonClass('secondary'))
  })
})
