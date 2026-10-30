import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Meter } from './Meter'
import { SlotStrip } from './SlotStrip'

describe('Meter', () => {
  it('announces the percentage and turns amber while something is missing', () => {
    render(<Meter value={62} label="Cobertura geral" />)

    const meter = screen.getByRole('meter', { name: 'Cobertura geral' })
    expect(meter).toHaveAttribute('aria-valuenow', '62')
    expect(meter).toHaveAttribute('aria-valuetext', '62%')
    expect(meter).toHaveAttribute('data-tone', 'warn')
  })

  it('turns green only when complete', () => {
    render(<Meter value={100} label="Obrigatórios" />)

    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'ok')
  })

  it('says there are no items when the value is null', () => {
    render(<Meter value={null} label="Cobertura geral" />)

    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuetext', 'Sem itens')
  })
})

describe('SlotStrip', () => {
  function slots() {
    return Array.from(screen.getByRole('meter').children)
  }

  it('has one slot per unit for small quantities and fills what was contributed', () => {
    render(<SlotStrip required={12} contributed={8} unit="porção" />)

    expect(slots()).toHaveLength(12)
    expect(slots().filter((slot) => slot.getAttribute('data-filled') === 'true')).toHaveLength(8)
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuetext', '8 de 12 (porção)')
  })

  it('switches to ten slots of 10% for large quantities', () => {
    render(<SlotStrip required={10000} contributed={6000} unit="mililitro" />)

    expect(screen.getByRole('meter')).toHaveAttribute('data-slots', 'percent')
    expect(slots()).toHaveLength(10)
    expect(slots().filter((slot) => slot.getAttribute('data-filled') === 'true')).toHaveLength(6)
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuetext', '6000 de 10000 (mililitro)')
  })

  it('only fills the last percent slot when that tenth is complete', () => {
    render(<SlotStrip required={2000} contributed={1999} unit="mililitro" />)

    expect(slots().filter((slot) => slot.getAttribute('data-filled') === 'true')).toHaveLength(9)
  })

  it('uses unit slots up to twenty units', () => {
    render(<SlotStrip required={20} contributed={0} unit="unidade" />)

    expect(slots()).toHaveLength(20)
  })
})
