import { useState } from "react";
import { useRegulatoryReport } from "../../features/api/queries";
import { Download, Loader2 } from "lucide-react";
import CbkSubmissionsTab from "./tabs/CbkSubmissionsTab";
import HokekaPageShell from "../../components/Layout/HokekaPageShell";

type MainTab = "reports" | "cbk-submissions";

export default function RegulatoryReportsPage() {
  const [mainTab, setMainTab] = useState<MainTab>("reports");
  const [reportType, setReportType] = useState<"ctr" | "lctr" | "iftr">("ctr");
  const { data: report, isLoading } = useRegulatoryReport(reportType);

  const handleExport = () => {
    if (!report) return;
    const today = new Date().toISOString().split("T")[0];
    const transactions = report.transactions && Array.isArray(report.transactions) ? report.transactions : [];
    const headers = ["Transaction ID", "Merchant ID", "Amount (USD)", "Date"];
    const rows = transactions.map((txn: any) => [
      txn.id || txn.transactionId || "", txn.merchantId || "",
      txn.amountCents != null ? (txn.amountCents / 100).toFixed(2) : "0.00",
      txn.txnTs || txn.timestamp ? new Date(txn.txnTs || txn.timestamp).toISOString().split("T")[0] : "",
    ]);
    const summary = [`${reportType.toUpperCase()} Report`, `Total Transactions,${report.totalTransactions || report.transactionCount || 0}`,
      `Total Amount,$${report.totalAmount ? (report.totalAmount / 100).toFixed(2) : "0.00"}`, "", headers.join(","),
      ...rows.map((r: string[]) => r.join(",")),
    ].join("\n");
    const blob = new Blob([summary], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = `${reportType.toUpperCase()}-report-${today}.csv`;
    a.click(); URL.revokeObjectURL(url);
  };

  const tabs: MainTab[] = ["reports", "cbk-submissions"];
  const subTabs = ["ctr", "lctr", "iftr"] as const;

  return (
    <HokekaPageShell title="Regulatory Reports" subtitle="FIU reports and CBK submissions" noCard>
      <div className="mb-4 flex border-b border-white/10">
        {tabs.map(t => (
          <button key={t} onClick={() => setMainTab(t)}
            className={`px-5 py-2.5 text-sm font-medium transition-colors ${mainTab === t ? "border-b-2 border-burgundy-700 text-burgundy-400" : "text-glass-muted hover:text-white"}`}>
            {t === "reports" ? "FIU Reports" : "CBK Submissions"}
          </button>
        ))}
      </div>

      {mainTab === "cbk-submissions" && <CbkSubmissionsTab />}

      {mainTab === "reports" && (
        <>
          <div className="mb-4 flex border-b border-white/10">
            {subTabs.map(t => (
              <button key={t} onClick={() => setReportType(t)}
                className={`px-4 py-2 text-xs font-medium transition-colors ${reportType === t ? "border-b-2 border-burgundy-700 text-burgundy-400" : "text-glass-muted hover:text-white"}`}>
                {t.toUpperCase()} {t === "ctr" ? "(Currency Transaction Report)" : t === "lctr" ? "(Large Cash Transaction Report)" : "(International Funds Transfer Report)"}
              </button>
            ))}
          </div>

          <div className="rounded-lg border border-white/10 bg-[#0f1a2e] p-4">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-semibold text-white">{reportType.toUpperCase()} Report</h3>
              <div className="flex gap-2">
                <button onClick={handleExport} disabled={!report} className="flex items-center gap-1.5 rounded-lg border border-burgundy-700 px-3 py-1.5 text-xs text-burgundy-400 transition-colors hover:bg-burgundy-700/10 disabled:opacity-30">
                  <Download size={14} /> Export
                </button>
              </div>
            </div>

            {isLoading ? (
              <div className="flex items-center gap-2 py-8 text-sm text-glass-muted"><Loader2 size={16} className="animate-spin" /> Generating report...</div>
            ) : report ? (
              <>
                <div className="mb-4 grid grid-cols-3 gap-3">
                  {[
                    { label: "Total Transactions", value: (report.totalTransactions || report.transactionCount || 0).toLocaleString() },
                    { label: "Total Amount", value: `$${report.totalAmount ? (report.totalAmount / 100).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : "0.00"}` },
                    { label: "Report Period", value: report.startDate && report.endDate ? `${new Date(report.startDate).toLocaleDateString()} - ${new Date(report.endDate).toLocaleDateString()}` : "Last 30 days" },
                  ].map(s => (
                    <div key={s.label} className="rounded-lg border border-white/10 bg-[#0f1a2e] p-3">
                      <p className="text-xs text-glass-muted">{s.label}</p>
                      <p className="text-lg font-bold text-white">{s.value}</p>
                    </div>
                  ))}
                </div>

                {report.transactions && Array.isArray(report.transactions) && report.transactions.length > 0 && (
                  <div className="overflow-hidden rounded-lg border border-white/10">
                    <table className="w-full border-collapse">
                      <thead><tr className="border-b border-white/10 bg-[#1a2744]">
                        <th className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Transaction ID</th>
                        <th className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Merchant</th>
                        <th className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Amount</th>
                        <th className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Date</th>
                      </tr></thead>
                      <tbody className="divide-y divide-white/5">
                        {(report.transactions as any[]).slice(0, 20).map((txn: any, idx: number) => (
                          <tr key={txn.id || txn.transactionId || idx} className="transition-colors hover:bg-white/[0.02]">
                            <td className="px-4 py-2 text-sm text-white">#{txn.id || txn.transactionId || idx}</td>
                            <td className="px-4 py-2 text-sm text-white/80">{txn.merchantId || "N/A"}</td>
                            <td className="px-4 py-2 text-sm text-white/80">${txn.amountCents ? (txn.amountCents / 100).toFixed(2) : "0.00"}</td>
                            <td className="px-4 py-2 text-sm text-glass-muted">{txn.txnTs || txn.timestamp ? new Date(txn.txnTs || txn.timestamp).toLocaleDateString() : "N/A"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                {report.summary && (
                  <div className="mt-4 rounded-lg border border-white/10 bg-[#0f1a2e] p-3">
                    <h4 className="mb-1 text-sm font-semibold text-white">Report Summary</h4>
                    <p className="whitespace-pre-wrap text-xs text-glass-muted">{typeof report.summary === "string" ? report.summary : JSON.stringify(report.summary, null, 2)}</p>
                  </div>
                )}
              </>
            ) : (
              <p className="py-8 text-sm text-glass-muted">Click "Generate Report" to create a {reportType.toUpperCase()} report</p>
            )}
          </div>
        </>
      )}
    </HokekaPageShell>
  );
}