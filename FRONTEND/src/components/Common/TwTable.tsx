import { type ReactNode } from 'react'
import { cn } from '../../lib/utils'

export interface Column<T> {
  key: string
  label: string
  sortable?: boolean
  render: (row: T) => ReactNode
  className?: string
}

interface TwTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  keyExtractor: (row: T) => string | number
  loading?: boolean
  emptyMessage?: string
  maxHeight?: string
}

export default function TwTable<T>({
  columns,
  rows,
  keyExtractor,
  loading,
  emptyMessage = 'No records found',
  maxHeight = 'calc(100vh - 320px)',
}: TwTableProps<T>) {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-burgundy-700 border-t-transparent" />
        <span className="ml-3 text-sm text-glass-muted">Loading...</span>
      </div>
    )
  }

  if (!rows.length) {
    return (
      <div className="flex items-center justify-center py-16">
        <p className="text-sm text-glass-muted">{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className="overflow-auto" style={{ maxHeight }}>
      <table className="w-full border-collapse">
        <thead className="sticky top-0 z-10">
          <tr className="border-b border-white/10 bg-[#0f1a2e]">
            {columns.map((col) => (
              <th
                key={col.key}
                className={cn(
                  'whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted',
                  col.className,
                )}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5">
          {rows.map((row) => (
            <tr
              key={keyExtractor(row)}
              className="transition-colors hover:bg-white/[0.02]"
            >
              {columns.map((col) => (
                <td
                  key={col.key}
                  className={cn(
                    'whitespace-nowrap px-4 py-3 text-sm text-white',
                    col.className,
                  )}
                >
                  {col.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}