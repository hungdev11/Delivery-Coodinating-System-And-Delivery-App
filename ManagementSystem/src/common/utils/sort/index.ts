import type { Column, RowData } from '@tanstack/table-core'
import { resolveComponent, h } from 'vue'

const UButton = resolveComponent('UButton')

export const sortSet = <TData extends RowData>({
  column,
  config,
}: {
  column: Column<TData>
  config: {
    variant: 'ghost' | 'solid' | 'outline' | 'soft' | 'link'
    label: string
    class: string
    activeColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
    inactiveColor?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'
  }
}) => {
  const currentSortOrder = column.getNextSortingOrder()
  const isSorted = column.getIsSorted()
  const getSortIcon = () => {
    if (currentSortOrder === 'asc' && isSorted) return 'i-lucide-arrow-up-narrow-wide'
    if (currentSortOrder === 'desc' && isSorted) return 'i-lucide-arrow-down-wide-narrow'
    return 'i-lucide-arrow-up-down'
  }

  let nextBehavior
  if (isSorted && currentSortOrder === 'asc') {
    nextBehavior = () => column.toggleSorting(true, true)
  } else if (isSorted && currentSortOrder === 'desc') {
    nextBehavior = () => column.clearSorting()
  } else {
    nextBehavior = () => column.toggleSorting(false, true)
  }

  return h(UButton, {
    variant: config.variant,
    label: config.label,
    class: config.class,
    color: isSorted ? config.activeColor : config.inactiveColor,
    icon: getSortIcon(),
    onClick: nextBehavior,
  })
}
