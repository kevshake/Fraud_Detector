import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react'
import { cn } from '../../lib/utils'

interface TwPaginationProps {
  page: number
  totalPages: number
  totalCount: number
  rowsPerPage: number
  onPageChange: (page: number) => void
  onRowsPerPageChange: (rowsPerPage: number) => void
  rowsPerPageOptions?: number[]
}

function getDisplayRange(page: number, rowsPerPage: number, totalCount: number) {
  const start = page * rowsPerPage + 1
  const end = Math.min((page + 1) * rowsPerPage, totalCount)
  return { start, end }
}

const ROWS_OPTIONS = [10, 25, 50, 100]

export default function TwPagination({
  page,
  totalPages,
  totalCount,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  rowsPerPageOptions = ROWS_OPTIONS,
}: TwPaginationProps) {
  if (totalCount === 0) return null

  const { start, end } = getDisplayRange(page, rowsPerPage, totalCount)

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-white/10 bg-[#0f1a2e] px-4 py-3">
      <div className="flex items-center gap-2 text-xs text-glass-muted">
        <span>
          {start}–{end} of {totalCount}
        </span>
        <span className="text-white/20">|</span>
        <span className="flex items-center gap-1">
          Rows:
          <select
            value={rowsPerPage}
            onChange={(e) => onRowsPerPageChange(Number(e.target.value))}
            className="rounded border border-white/10 bg-[#1a2744] px-1.5 py-0.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-burgundy-700"
          >
            {rowsPerPageOptions.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </span>
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(0)}
          disabled={page === 0}
          className={cn(
            'rounded p-1.5 transition-colors',
            page === 0
              ? 'cursor-not-allowed text-white/20'
              : 'text-glass-muted hover:bg-white/10 hover:text-white',
          )}
        >
          <ChevronsLeft size={16} />
        </button>
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className={cn(
            'rounded p-1.5 transition-colors',
            page === 0
              ? 'cursor-not-allowed text-white/20'
              : 'text-glass-muted hover:bg-white/10 hover:text-white',
          )}
        >
          <ChevronLeft size={16} />
        </button>

        <span className="min-w-[4rem] text-center text-xs text-glass-muted">
          Page {page + 1} of {totalPages || 1}
        </span>

        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className={cn(
            'rounded p-1.5 transition-colors',
            page >= totalPages - 1
              ? 'cursor-not-allowed text-white/20'
              : 'text-glass-muted hover:bg-white/10 hover:text-white',
          )}
        >
          <ChevronRight size={16} />
        </button>
        <button
          onClick={() => onPageChange(totalPages - 1)}
          disabled={page >= totalPages - 1}
          className={cn(
            'rounded p-1.5 transition-colors',
            page >= totalPages - 1
              ? 'cursor-not-allowed text-white/20'
              : 'text-glass-muted hover:bg-white/10 hover:text-white',
          )}
        >
          <ChevronsRight size={16} />
        </button>
      </div>
    </div>
  )
}