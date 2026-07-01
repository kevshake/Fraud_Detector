import { cn } from '../../lib/utils'
import type { InputHTMLAttributes, SelectHTMLAttributes } from 'react'

interface TwInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
}

export function TwInput({ label, className, id, ...props }: TwInputProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && <label htmlFor={id} className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">{label}</label>}
      <input
        id={id}
        className={cn(
          'w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-burgundy-700',
          className,
        )}
        {...props}
      />
    </div>
  )
}

interface TwSelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  options: { value: string; label: string }[]
}

export function TwSelect({ label, options, className, id, ...props }: TwSelectProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && <label htmlFor={id} className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">{label}</label>}
      <select
        id={id}
        className={cn(
          'w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-burgundy-700',
          className,
        )}
        {...props}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  )
}