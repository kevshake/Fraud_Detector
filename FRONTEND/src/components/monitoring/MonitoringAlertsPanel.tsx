import { Bell, CheckCircle2 } from 'lucide-react'
import GlassCard from '../Common/GlassCard'
import { useMonitoringAlerts } from '../../features/api/queries'

function timeAgo(iso?: string) {
  if (!iso) return '—'
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return '—'
  const diffSec = Math.max(1, Math.round((Date.now() - then) / 1000))
  if (diffSec < 60) return `${diffSec}s ago`
  if (diffSec < 3600) return `${Math.round(diffSec / 60)}m ago`
  if (diffSec < 86400) return `${Math.round(diffSec / 3600)}h ago`
  return `${Math.round(diffSec / 86400)}d ago`
}

export default function MonitoringAlertsPanel() {
  const { data, isLoading, error } = useMonitoringAlerts(0, 8)
  const rows = data?.content ?? []

  return (
    <GlassCard className="flex min-h-0 flex-col gap-2 p-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/20">
            <Bell className="h-4 w-4 text-amber-300" />
          </div>
          <div>
            <p className="text-sm font-medium text-white">Monitoring Alerts</p>
            <p className="text-xs text-white/50">Rescreening & risk changes</p>
          </div>
        </div>
        {data?.totalElements != null && (
          <span className="rounded-full bg-white/10 px-2 py-0.5 text-xs text-white/70">
            {data.totalElements}
          </span>
        )}
      </div>

      {isLoading && <p className="text-xs text-white/50">Loading…</p>}
      {error && <p className="text-xs text-red-300">Could not load monitoring alerts</p>}

      <ul className="flex min-h-0 flex-1 flex-col gap-1.5 overflow-y-auto">
        {rows.length === 0 && !isLoading && (
          <li className="text-xs text-white/45">No monitoring alerts</li>
        )}
        {rows.map((row) => (
          <li
            key={row.alertId}
            className="flex items-start justify-between gap-2 rounded-lg border border-white/8 bg-white/[0.03] px-2.5 py-2"
          >
            <div className="min-w-0">
              <p className="truncate text-xs font-medium text-white">
                {row.alertType || 'Risk change'}
              </p>
              <p className="text-[11px] text-white/45">
                {row.merchantId != null ? `Merchant ${row.merchantId}` : '—'}
                {' · '}
                {timeAgo(row.createdAt)}
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-1">
              <span
                className={`rounded px-1.5 py-0.5 text-[10px] font-medium uppercase ${
                  row.alertSeverity === 'HIGH' || row.alertSeverity === 'CRITICAL'
                    ? 'bg-red-500/20 text-red-300'
                    : 'bg-amber-500/15 text-amber-200'
                }`}
              >
                {row.alertSeverity ?? 'MEDIUM'}
              </span>
              {row.acknowledged && (
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" aria-label="Acknowledged" />
              )}
            </div>
          </li>
        ))}
      </ul>
    </GlassCard>
  )
}
