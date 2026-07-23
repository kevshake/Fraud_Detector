import { useDashboardStats } from "../../features/api/queries";
import HokekaPageShell from "../../components/Layout/HokekaPageShell";
import { Download, Loader2 } from "lucide-react";

export default function ReportsPage() {
  const { data: stats, isLoading } = useDashboardStats();

  const handleExport = (reportType: string) => {
    if (!stats) return;
    let csv = "";
    const today = new Date().toISOString().split("T")[0];

    if (reportType === "cases") {
      const statusRows = stats.casesByStatus ? Object.entries(stats.casesByStatus).map(([s, c]) => `${s},${c}`) : [];
      const trendRows = stats.casesLast7d ? Object.entries(stats.casesLast7d).map(([d, c]) => `${d},${c}`) : [];
      csv = `Cases by Status\nStatus,Count\n${statusRows.join("\n")}\n\nCases Last 7 Days\nDate,Count\n${trendRows.join("\n")}`;
    } else if (reportType === "sars") {
      const statusRows = stats.sarsByStatus ? Object.entries(stats.sarsByStatus).map(([s, c]) => `${s},${c}`) : [];
      const trendRows = stats.sarsLast7d ? Object.entries(stats.sarsLast7d).map(([d, c]) => `${d},${c}`) : [];
      csv = `SARs by Status\nStatus,Count\n${statusRows.join("\n")}\n\nSARs Last 7 Days\nDate,Count\n${trendRows.join("\n")}`;
    } else if (reportType === "audit") {
      csv = `Audit Activity Summary\nMetric,Value\nLast 24h Events,${stats.auditLast24h || 0}`;
    }

    if (!csv) return;
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${reportType}-report-${today}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <HokekaPageShell title="Reports" subtitle="Generate and export compliance reports" noCard>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {[
          { title: "Case Summary Report", desc: "Summary of all compliance cases by status", type: "cases", data: stats?.casesByStatus },
                    { title: "SAR Summary Report", desc: "Summary of all SAR reports by status", type: "sars", data: stats?.sarsByStatus },
                    { title: "Audit Activity Report", desc: "Audit events in the last 24 hours", type: "audit", data: null as Record<string, number> | null },
                  ].map((card) => (
                    <div key={card.type} className="rounded-lg border border-white/10 bg-[var(--surface-2)] p-4">
                      <h4 className="mb-1 text-base font-semibold text-white">{card.title}</h4>
                      <p className="mb-3 text-xs text-glass-muted">{card.desc}</p>
                      {card.data && Object.entries(card.data).map(([status, count]) => (
              <div key={status} className="mb-1 flex justify-between text-sm">
                <span className="text-white/80">{status}:</span>
                <span className="font-semibold text-white">{String(count)}</span>
              </div>
            ))}
            {card.type === "audit" && (
              <div className="mb-1 flex justify-between text-sm">
                <span className="text-white/80">Last 24h:</span>
                <span className="font-semibold text-white">{stats?.auditLast24h || 0}</span>
              </div>
            )}
            <button onClick={() => handleExport(card.type)} className="mt-3 flex items-center gap-1.5 rounded-lg border border-burgundy-700 px-3 py-1.5 text-xs text-burgundy-400 transition-colors hover:bg-burgundy-700/10">
              <Download size={14} /> Export
            </button>
          </div>
        ))}
      </div>

      <div className="mt-4 rounded-lg border border-white/10 bg-[var(--surface-2)] p-4">
        <h4 className="mb-3 text-base font-semibold text-white">Daily Trends</h4>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <p className="mb-2 text-xs text-glass-muted">Cases (Last 7 Days)</p>
            {stats?.casesLast7d ? Object.entries(stats.casesLast7d).map(([date, count]) => (
              <div key={date} className="mb-0.5 flex justify-between text-xs">
                <span className="text-white/80">{date}:</span>
                <span className="text-white">{String(count)}</span>
              </div>
            )) : <p className="text-xs text-glass-muted">No data available</p>}
          </div>
          <div>
            <p className="mb-2 text-xs text-glass-muted">SARs (Last 7 Days)</p>
            {stats?.sarsLast7d ? Object.entries(stats.sarsLast7d).map(([date, count]) => (
              <div key={date} className="mb-0.5 flex justify-between text-xs">
                <span className="text-white/80">{date}:</span>
                <span className="text-white">{String(count)}</span>
              </div>
            )) : <p className="text-xs text-glass-muted">No data available</p>}
          </div>
        </div>
      </div>

      {isLoading && <p className="mt-2 text-sm text-glass-muted"><Loader2 size={14} className="mr-1 inline animate-spin" />Loading report data...</p>}
    </HokekaPageShell>
  );
}