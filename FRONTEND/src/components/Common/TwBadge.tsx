import { cn } from '../../lib/utils'
import type { ReactNode } from 'react'

interface TwBadgeProps {
  children: ReactNode
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info'
  size?: 'sm' | 'md'
  className?: string
}

const VARIANT_CLASSES = {
  default: 'bg-white/10 text-glass-muted border-white/10',
  success: 'bg-emerald-900/30 text-emerald-300 border-emerald-700/30',
  warning: 'bg-amber-900/30 text-amber-300 border-amber-700/30',
  danger: 'bg-red-900/30 text-red-300 border-red-700/30',
  info: 'bg-sky-900/30 text-sky-300 border-sky-700/30',
}

export default function TwBadge({ children, variant = 'default', size = 'sm', className }: TwBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-0.5 font-semibold',
        size === 'sm' ? 'text-[11px]' : 'text-xs',
        VARIANT_CLASSES[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}