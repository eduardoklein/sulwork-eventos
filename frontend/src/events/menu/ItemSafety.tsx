import { useDomainOptions } from '../../domain/context'
import type { Item } from '../../types/api'
import { Chip } from '../../ui/Chip'
import { unsafeText, type Viewer } from './safety'

/** Whether the item is safe for the chosen person. Green and red keep their meaning, and the text says it too. */
export function ItemSafety({ item, viewer }: { item: Item; viewer: Viewer }) {
  const options = useDomainOptions()
  const unsafe = viewer.unsafeByItem.get(item.id)
  return unsafe ? <Chip tone="danger">{unsafeText(unsafe, options)}</Chip> : <Chip tone="ok">Seguro para {viewer.name}</Chip>
}
