import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DomainOptionsProvider } from '../../domain/DomainOptionsProvider'
import { NotificationsProvider } from '../../notifications/NotificationsProvider'
import { domainOptions, failure, makeCategory, makeEvent, makeItem, stubApi } from '../../test/fakeApi'
import type { Category, FoodEvent, Item } from '../../types/api'
import { MenuTab } from './MenuTab'

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status })

function renderTab({ categories = [], items = [], event = makeEvent({ id: 1, name: 'Almoço' }) }: { categories?: Category[]; items?: Item[]; event?: FoodEvent } = {}) {
  const onChanged = vi.fn()
  render(
    <NotificationsProvider>
      <DomainOptionsProvider>
        <MenuTab event={event} categories={categories} items={items} onChanged={onChanged} />
      </DomainOptionsProvider>
    </NotificationsProvider>,
  )
  return { onChanged }
}

const sent = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) => {
  const call = fetchMock.mock.calls.find(([input, init]) => input === address && init?.method === method)
  return call ? JSON.parse((call[1]!.body as string | undefined) ?? 'null') : undefined
}
const count = (fetchMock: ReturnType<typeof stubApi>, method: string, address: string) =>
  fetchMock.mock.calls.filter(([input, init]) => input === address && init?.method === method).length

const dishes = makeCategory({ id: 1, name: 'Pratos', unit: 'PORTION', goalQuantity: 30, totalItems: 1, plannedQuantity: 10, contributedQuantity: 4 })
const drinks = makeCategory({ id: 2, name: 'Bebidas', unit: 'MILLILITER', goalQuantity: 0, totalItems: 0 })
const lasagna = makeItem({
  id: 7,
  name: 'Lasanha',
  mandatory: true,
  allergens: ['GLUTEN', 'LACTOSE'],
  characteristics: ['ANIMAL_MEAT'],
  contributedQuantity: 4,
  pendingQuantity: 6,
  coveredPercentage: 40,
})

describe('MenuTab: categories', () => {
  it('invites the person to create the first category', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab()

    expect(screen.getByText('Nenhuma categoria ainda')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Adicionar categoria' })).toHaveLength(1)
    expect(screen.getByText('Crie uma categoria para poder adicionar itens.')).toBeInTheDocument()
  })

  it('lists each category with its unit, goal and quantities', async () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ categories: [dishes, drinks], items: [lasagna] })

    expect(await screen.findByText('Porção · meta 30 · planejado 10 · contribuído 4')).toBeInTheDocument()
    expect(screen.getByText('Mililitro · meta 0 · planejado 0 · contribuído 0')).toBeInTheDocument()
    expect(screen.getByText('1 item')).toBeInTheDocument()
    expect(screen.getByText('0 itens')).toBeInTheDocument()
  })

  it('does not call the API and marks each field that is wrong when saving an empty category', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Adicionar categoria' }))
    const dialog = screen.getByRole('dialog', { name: 'Adicionar categoria' })
    await user.clear(within(dialog).getByLabelText('Meta de quantidade'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    expect(within(dialog).getByText('Informe o nome da categoria.')).toBeInTheDocument()
    expect(within(dialog).getByText('Escolha a unidade.')).toBeInTheDocument()
    expect(within(dialog).getByText('Informe a meta, um número inteiro de 0 ou mais.')).toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/categories')).toBe(0)
  })

  it('creates a category, tells the screen to load again and closes the window', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/categories': () => json(201, drinks) })
    const user = userEvent.setup()
    const { onChanged } = renderTab()

    await user.click(screen.getByRole('button', { name: 'Adicionar categoria' }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Nome'), '  Bebidas ')
    await waitFor(() => expect(within(dialog).getByLabelText('Unidade')).toBeEnabled())
    await user.selectOptions(within(dialog).getByLabelText('Unidade'), 'MILLILITER')
    await user.clear(within(dialog).getByLabelText('Meta de quantidade'))
    await user.type(within(dialog).getByLabelText('Meta de quantidade'), '5000')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1))
    expect(sent(fetchMock, 'POST', '/api/events/1/categories')).toEqual({ name: 'Bebidas', unit: 'MILLILITER', goalQuantity: 5000 })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await screen.findByText('Categoria criada.')).toBeInTheDocument()
  })

  it('edits a category with its data filled in, and sends PUT', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/categories/2': () => json(200, drinks) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [drinks] })

    await user.click(screen.getByRole('button', { name: 'Editar Bebidas' }))
    const dialog = screen.getByRole('dialog', { name: 'Editar categoria' })
    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Bebidas')
    await waitFor(() => expect(within(dialog).getByLabelText('Unidade')).toHaveValue('MILLILITER'))
    await user.clear(within(dialog).getByLabelText('Nome'))
    await user.type(within(dialog).getByLabelText('Nome'), 'Sucos')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(sent(fetchMock, 'PUT', '/api/events/1/categories/2')).toEqual({ name: 'Sucos', unit: 'MILLILITER', goalQuantity: 0 })
    expect(await screen.findByText('Categoria salva.')).toBeInTheDocument()
  })

  it('locks the unit of a category that has items, and says why', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ categories: [dishes], items: [lasagna] })

    await user.click(screen.getByRole('button', { name: 'Editar Pratos' }))

    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByLabelText('Unidade')).toBeDisabled()
    expect(within(dialog).getByText('A unidade não muda enquanto a categoria tem itens.')).toBeInTheDocument()
  })

  it('keeps the window open with what was typed, and marks the field, when the API refuses', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1/categories': () =>
        json(409, { status: 409, detail: 'Já existe uma categoria com este nome.', errors: [{ field: 'name', message: 'Nome já usado.' }] }),
    })
    const user = userEvent.setup()
    const { onChanged } = renderTab()

    await user.click(screen.getByRole('button', { name: 'Adicionar categoria' }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Nome'), 'Pratos')
    await waitFor(() => expect(within(dialog).getByLabelText('Unidade')).toBeEnabled())
    await user.selectOptions(within(dialog).getByLabelText('Unidade'), 'PORTION')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    expect(await within(dialog).findByText('Já existe uma categoria com este nome.')).toBeInTheDocument()
    expect(within(dialog).getByText('Nome já usado.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Pratos')
    expect(within(dialog).getByLabelText('Unidade')).toHaveValue('PORTION')
    expect(onChanged).not.toHaveBeenCalled()
  })

  it('closes with Escape and does not save', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    const { onChanged } = renderTab()

    await user.click(screen.getByRole('button', { name: 'Adicionar categoria' }))
    await user.type(await screen.findByLabelText('Nome'), 'Pratos')
    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/categories')).toBe(0)
    expect(onChanged).not.toHaveBeenCalled()
  })

  it('opens the window clean after a cancel', async () => {
    stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab()

    await user.click(screen.getByRole('button', { name: 'Adicionar categoria' }))
    await user.type(await screen.findByLabelText('Nome'), 'Pratos')
    await user.keyboard('{Escape}')
    await user.click(screen.getByRole('button', { name: 'Adicionar categoria' }))

    expect(screen.getByLabelText('Nome')).toHaveValue('')
  })

  it('deletes an empty category after the confirmation', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/categories/2': () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [drinks] })

    await user.click(screen.getByRole('button', { name: 'Excluir Bebidas' }))
    const dialog = screen.getByRole('dialog', { name: 'Excluir esta categoria?' })
    expect(count(fetchMock, 'DELETE', '/api/events/1/categories/2')).toBe(0)
    await user.click(within(dialog).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(count(fetchMock, 'DELETE', '/api/events/1/categories/2')).toBe(1)
    expect(await screen.findByText('Categoria excluída.')).toBeInTheDocument()
  })

  it('does not offer to delete a category that has items, and says why', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ categories: [dishes], items: [lasagna] })

    expect(screen.getByRole('button', { name: 'Excluir Pratos' })).toBeDisabled()
    expect(screen.getByText('Só dá para excluir uma categoria sem itens.')).toBeInTheDocument()
  })

  it('keeps the window open and shows the reason when a delete fails', async () => {
    stubApi({ '/api/domain/options': domainOptions, '/api/events/1/categories/2': failure(409, 'Não é possível excluir uma categoria que ainda tem itens.') })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [drinks] })

    await user.click(screen.getByRole('button', { name: 'Excluir Bebidas' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('Não é possível excluir uma categoria que ainda tem itens.')
    expect(onChanged).not.toHaveBeenCalled()
  })
})

describe('MenuTab: items', () => {
  it('groups the items by category, with the details of each item', async () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ categories: [dishes, drinks], items: [lasagna] })

    const group = (await screen.findByRole('heading', { level: 4, name: 'Lasanha' })).closest('li')!
    expect(within(group).getByText('Obrigatório')).toBeInTheDocument()
    expect(within(group).getByText('4 de 10 (porção)')).toBeInTheDocument()
    expect(within(group).getByText('Alérgenos: Glúten, Lactose')).toBeInTheDocument()
    expect(within(group).getByText('Características: Carne de origem animal')).toBeInTheDocument()
    expect(within(group).getByRole('meter', { name: 'Cobertura de Lasanha' })).toHaveAttribute('aria-valuenow', '40')
    expect(screen.getByText('Nenhum item nesta categoria.')).toBeInTheDocument()
  })

  it('writes "Opcional" for an item that is not mandatory', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ categories: [dishes], items: [makeItem({ id: 8, name: 'Farofa' })] })

    expect(screen.getByText('Opcional')).toBeInTheDocument()
  })

  it('adds an item to the category of the button, with the choices of the person', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/items': () => json(201, lasagna) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [dishes, drinks] })

    await user.click(screen.getByRole('button', { name: 'Adicionar item em Bebidas' }))
    const dialog = await screen.findByRole('dialog', { name: 'Adicionar item' })
    expect(within(dialog).getByLabelText('Categoria')).toHaveValue('2')
    await user.type(within(dialog).getByLabelText('Nome'), 'Suco de uva')
    await user.type(within(dialog).getByLabelText('Quantidade necessária'), '2000')
    expect(within(dialog).getByText('Em mililitro, a unidade da categoria.')).toBeInTheDocument()
    await user.click(within(dialog).getByLabelText('Item obrigatório'))
    await user.click(await within(dialog).findByLabelText('Lactose'))
    await user.click(within(dialog).getByLabelText('Açúcar adicionado'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1))
    expect(sent(fetchMock, 'POST', '/api/events/1/items')).toEqual({
      name: 'Suco de uva',
      categoryId: 2,
      requiredQuantity: 2000,
      mandatory: true,
      allergens: ['LACTOSE'],
      characteristics: ['ADDED_SUGAR'],
    })
    expect(await screen.findByText('Item criado.')).toBeInTheDocument()
  })

  it('does not call the API and marks the fields that are wrong when saving an empty item', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions })
    const user = userEvent.setup()
    renderTab({ categories: [dishes, drinks] })

    await user.click(screen.getByRole('button', { name: 'Adicionar item em Pratos' }))
    const dialog = screen.getByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Categoria'), '')
    await user.type(within(dialog).getByLabelText('Quantidade necessária'), '0')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    expect(within(dialog).getByText('Informe o nome do item.')).toBeInTheDocument()
    expect(within(dialog).getByText('Escolha a categoria.')).toBeInTheDocument()
    expect(within(dialog).getByText('Informe a quantidade necessária, um número inteiro de 1 ou mais.')).toBeInTheDocument()
    expect(count(fetchMock, 'POST', '/api/events/1/items')).toBe(0)
  })

  it('edits an item filled in, with no choice of category, and sends PUT without the category', async () => {
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/items/7': () => json(200, lasagna) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [dishes], items: [lasagna] })

    await user.click(screen.getByRole('button', { name: 'Editar Lasanha' }))
    const dialog = screen.getByRole('dialog', { name: 'Editar item' })
    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Lasanha')
    expect(within(dialog).getByLabelText('Quantidade necessária')).toHaveValue(10)
    expect(within(dialog).getByLabelText('Item obrigatório')).toBeChecked()
    expect(within(dialog).queryByLabelText('Categoria')).not.toBeInTheDocument()
    expect(within(dialog).getByText('Pratos')).toBeInTheDocument()
    await waitFor(() => expect(within(dialog).getByLabelText('Glúten')).toBeChecked())
    await user.click(within(dialog).getByLabelText('Glúten'))
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    const body = sent(fetchMock, 'PUT', '/api/events/1/items/7')
    expect(body).toEqual({
      name: 'Lasanha',
      requiredQuantity: 10,
      mandatory: true,
      allergens: ['LACTOSE'],
      characteristics: ['ANIMAL_MEAT'],
    })
    expect(await screen.findByText('Item salvo.')).toBeInTheDocument()
  })

  it('keeps the window open with what was typed when the API refuses the item', async () => {
    stubApi({
      '/api/domain/options': domainOptions,
      '/api/events/1/items/7': failure(422, 'A quantidade necessária não pode ficar abaixo do que já foi contribuído (4).'),
    })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [dishes], items: [lasagna] })

    await user.click(screen.getByRole('button', { name: 'Editar Lasanha' }))
    const dialog = screen.getByRole('dialog')
    await user.clear(within(dialog).getByLabelText('Quantidade necessária'))
    await user.type(within(dialog).getByLabelText('Quantidade necessária'), '2')
    await user.click(within(dialog).getByRole('button', { name: 'Salvar' }))

    expect(await within(dialog).findByText(/não pode ficar abaixo do que já foi contribuído/)).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Quantidade necessária')).toHaveValue(2)
    expect(onChanged).not.toHaveBeenCalled()
  })

  it('deletes an item that has no contributions, after the confirmation', async () => {
    const fresh = makeItem({ id: 8, name: 'Farofa' })
    const fetchMock = stubApi({ '/api/domain/options': domainOptions, '/api/events/1/items/8': () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    const { onChanged } = renderTab({ categories: [dishes], items: [fresh] })

    await user.click(screen.getByRole('button', { name: 'Excluir Farofa' }))
    expect(count(fetchMock, 'DELETE', '/api/events/1/items/8')).toBe(0)
    await user.click(within(screen.getByRole('dialog', { name: 'Excluir este item?' })).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(count(fetchMock, 'DELETE', '/api/events/1/items/8')).toBe(1)
    expect(await screen.findByText('Item excluído.')).toBeInTheDocument()
  })

  it('does not offer to delete an item that has contributions, and says why', () => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ categories: [dishes], items: [lasagna] })

    expect(screen.getByRole('button', { name: 'Excluir Lasanha' })).toBeDisabled()
    expect(screen.getByText('Só dá para excluir um item sem contribuições.')).toBeInTheDocument()
  })
})

describe('MenuTab: an event that does not take changes', () => {
  it.each(['ENCERRADO', 'REALIZADO', 'CANCELADO'] as const)('shows the menu with no buttons when the event is %s', (status) => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ event: makeEvent({ id: 1, name: 'Almoço', status }), categories: [dishes], items: [lasagna] })

    expect(screen.getByText('O cardápio não muda mais, porque o evento não aceita alterações nele.')).toBeInTheDocument()
    expect(screen.getByText('Lasanha')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it.each(['RASCUNHO', 'ABERTO'] as const)('offers the buttons when the event is %s', (status) => {
    stubApi({ '/api/domain/options': domainOptions })

    renderTab({ event: makeEvent({ id: 1, name: 'Almoço', status }), categories: [dishes] })

    expect(screen.getByRole('button', { name: 'Adicionar categoria' })).toBeInTheDocument()
    expect(screen.queryByText(/O cardápio não muda mais/)).not.toBeInTheDocument()
  })
})
