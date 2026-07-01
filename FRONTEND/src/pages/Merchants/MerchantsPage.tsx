import { useState } from "react";
import { useMerchants } from "../../features/api/queries";
import { useCreateMerchant, type CreateMerchantRequest } from "../../features/api/mutations";
import type { Merchant } from "../../types";
import HokekaPageShell from "../../components/Layout/HokekaPageShell";
import TwBadge from "../../components/Common/TwBadge";
import TwPagination from "../../components/Common/TwPagination";
import TwSnackbar from "../../components/Common/TwSnackbar";
import { TwInput } from "../../components/Common/TwInput";
import { Download, Eye, Loader2, Plus, X } from "lucide-react";

const riskBadge = (level: string | undefined): "danger" | "warning" | "success" | "default" => {
  if (level === "HIGH" || level === "CRITICAL") return "danger";
  if (level === "MEDIUM") return "warning";
  if (level === "LOW") return "success";
  return "default";
};

const formatScore = (score: number | null | undefined): string => {
  if (score === null || score === undefined || isNaN(score)) return "—";
  return score.toFixed(1);
};

const kycStatuses = ["PENDING", "VERIFIED", "REJECTED", "UNDER_REVIEW"];
const contractStatuses = ["ACTIVE", "INACTIVE", "SUSPENDED", "PENDING"];

export default function MerchantsPage() {
  const [page, setPage] = useState({ index: 0, size: 25 });
  const [viewMerchant, setViewMerchant] = useState<Merchant | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [formData, setFormData] = useState<CreateMerchantRequest>({
    merchantId: "", businessName: "", mcc: "", kycStatus: "", contractStatus: "",
  });
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: "success" | "error" }>({
    open: false, message: "", severity: "success",
  });

  const { data: merchants, isLoading, isError, error } = useMerchants({
    page: page.index, size: page.size,
  });
  const createMerchant = useCreateMerchant();

  const content = merchants?.content || [];
  const totalElements = merchants?.totalElements ?? 0;
  const totalPages = merchants?.totalPages ?? 1;

  const handleExportCSV = () => {
    if (!content.length) return;
    const headers = ["Merchant ID", "Business Name", "MCC", "Risk Level", "KRS", "CRA", "KYC Status", "Contract Status"];
    const rows = content.map((m: any) => [
      m.merchantId, (m.businessName || "").replace(/,/g, ";"), m.mcc || "",
      m.riskLevel || "", formatScore(m.krs), formatScore(m.cra),
      m.kycStatus || "", m.contractStatus || "",
    ]);
    const csv = [headers, ...rows].map(r => r.join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `merchants-${new Date().toISOString().split("T")[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleAddMerchant = async () => {
    if (!formData.merchantId.trim() || !formData.businessName.trim()) return;
    try {
      await createMerchant.mutateAsync(formData);
      setAddOpen(false);
      setFormData({ merchantId: "", businessName: "", mcc: "", kycStatus: "", contractStatus: "" });
      setSnackbar({ open: true, message: "Merchant onboarded successfully.", severity: "success" });
    } catch (err: any) {
      setSnackbar({ open: true, message: err?.message || "Failed to onboard merchant.", severity: "error" });
    }
  };

  return (
    <HokekaPageShell title="Merchants" subtitle="Onboard and monitor merchant risk profiles" noCard>
      {/* Toolbar */}
      <div className="flex items-center justify-between pb-3">
        <button
          onClick={handleExportCSV}
          disabled={!content.length}
          className="flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-1.5 text-xs text-glass-muted transition-colors hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-30"
        >
          <Download size={14} /> Export CSV
        </button>
        <button
          onClick={() => setAddOpen(true)}
          className="flex items-center gap-1.5 rounded-lg bg-burgundy-700 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-burgundy-800"
        >
          <Plus size={14} /> Add Merchant
        </button>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg border border-white/10 bg-[#0f1a2e]">
        <div className="overflow-auto" style={{ maxHeight: "calc(100vh - 320px)" }}>
          {isLoading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 size={24} className="animate-spin text-glass-muted" />
            </div>
          ) : isError ? (
            <div className="px-4 py-8 text-center text-sm text-red-400">
              Error loading merchants: {error instanceof Error ? error.message : "Unknown error"}
            </div>
          ) : (
            <table className="w-full border-collapse">
              <thead className="sticky top-0 z-10">
                <tr className="border-b border-white/10 bg-[#0f1a2e]">
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Merchant ID</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Business Name</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">MCC</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Risk Level</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">KRS</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">CRA</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">KYC</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Contract</th>
                  <th className="whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-glass-muted">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {content.map((merchant: any) => (
                  <tr key={merchant.id} className="transition-colors hover:bg-white/[0.02]">
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-sm text-white">{merchant.merchantId}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{merchant.businessName}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white/80">{merchant.mcc || "—"}</td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {merchant.riskLevel ? <TwBadge variant={riskBadge(merchant.riskLevel)}>{merchant.riskLevel}</TwBadge> : <span className="text-glass-muted">—</span>}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white/80">{formatScore(merchant.krs)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white/80">{formatScore(merchant.cra)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white/80">{merchant.kycStatus || "—"}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white/80">{merchant.contractStatus || "—"}</td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <button onClick={() => setViewMerchant(merchant)} className="flex items-center gap-1 text-xs text-burgundy-400 transition-colors hover:text-burgundy-300">
                        <Eye size={14} /> View
                      </button>
                    </td>
                  </tr>
                ))}
                {!content.length && (
                  <tr><td colSpan={9} className="px-4 py-8 text-center text-sm text-glass-muted">No merchants found</td></tr>
                )}
              </tbody>
            </table>
          )}
        </div>
        <TwPagination
          page={page.index} totalPages={totalPages} totalCount={totalElements} rowsPerPage={page.size}
          onPageChange={(p) => setPage(prev => ({ ...prev, index: p }))}
          onRowsPerPageChange={(s) => setPage({ index: 0, size: s })}
        />
      </div>

      {/* View Merchant Modal */}
      {viewMerchant && (
        <>
          <div className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm" onClick={() => setViewMerchant(null)} />
          <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2">
            <div className="overflow-hidden rounded-xl border border-white/10 bg-[#0f1a2e] shadow-2xl">
              <div className="flex items-start justify-between border-b border-white/10 px-6 py-4">
                <div>
                  <h3 className="text-lg font-semibold text-white">{viewMerchant.businessName}</h3>
                  <p className="mt-0.5 font-mono text-xs text-glass-muted">{viewMerchant.merchantId}</p>
                </div>
                <div className="flex items-center gap-2">
                  {viewMerchant.riskLevel && <TwBadge variant={riskBadge(viewMerchant.riskLevel)}>{viewMerchant.riskLevel}</TwBadge>}
                  <button onClick={() => setViewMerchant(null)} className="rounded p-1 text-glass-muted transition-colors hover:bg-white/10"><X size={18} /></button>
                </div>
              </div>
              <div className="space-y-4 px-6 py-4">
                <div className="grid grid-cols-2 gap-4">
                  {viewMerchant.mcc && <div><p className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">MCC Code</p><p className="mt-0.5 font-mono text-sm text-white">{viewMerchant.mcc}</p></div>}
                  <div><p className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">KYC Status</p><p className="mt-0.5 text-sm text-white/80">{viewMerchant.kycStatus || "Not set"}</p></div>
                  <div><p className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">Contract Status</p><p className="mt-0.5 text-sm text-white/80">{viewMerchant.contractStatus || "Not set"}</p></div>
                </div>
                <div className="border-t border-white/10 pt-4">
                  <p className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-glass-muted">Risk Scores</p>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-xs text-glass-muted">KRS</p>
                      <p className={`text-xl font-bold ${viewMerchant.krs && viewMerchant.krs > 7 ? "text-red-400" : viewMerchant.krs && viewMerchant.krs > 4 ? "text-amber-400" : "text-white"}`}>
                        {formatScore(viewMerchant.krs)}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-glass-muted">CRA</p>
                      <p className={`text-xl font-bold ${viewMerchant.cra && viewMerchant.cra > 7 ? "text-red-400" : viewMerchant.cra && viewMerchant.cra > 4 ? "text-amber-400" : "text-white"}`}>
                        {formatScore(viewMerchant.cra)}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
              <div className="flex justify-end border-t border-white/10 px-6 py-3">
                <button onClick={() => setViewMerchant(null)} className="rounded-lg border border-white/10 px-4 py-1.5 text-xs text-white transition-colors hover:bg-white/5">Close</button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* Add Merchant Modal */}
      {addOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm" onClick={() => setAddOpen(false)} />
          <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2">
            <div className="overflow-hidden rounded-xl border border-white/10 bg-[#0f1a2e] shadow-2xl">
              <div className="flex items-center justify-between border-b border-white/10 px-6 py-4">
                <h3 className="text-lg font-semibold text-white">Onboard New Merchant</h3>
                <button onClick={() => setAddOpen(false)} className="rounded p-1 text-glass-muted transition-colors hover:bg-white/10"><X size={18} /></button>
              </div>
              <div className="space-y-4 px-6 py-4">
                <div className="grid grid-cols-2 gap-3">
                  <TwInput label="Merchant ID" value={formData.merchantId} onChange={(e) => setFormData(prev => ({ ...prev, merchantId: e.target.value }))} placeholder="e.g. MRC-00123" />
                  <TwInput label="Business Name" value={formData.businessName} onChange={(e) => setFormData(prev => ({ ...prev, businessName: e.target.value }))} />
                  <TwInput label="MCC Code" value={formData.mcc} onChange={(e) => setFormData(prev => ({ ...prev, mcc: e.target.value }))} placeholder="e.g. 5411" />
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">KYC Status</label>
                    <select value={formData.kycStatus} onChange={(e) => setFormData(prev => ({ ...prev, kycStatus: e.target.value }))} className="w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-burgundy-700">
                      <option value="">Not Set</option>
                      {kycStatuses.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">Contract Status</label>
                    <select value={formData.contractStatus} onChange={(e) => setFormData(prev => ({ ...prev, contractStatus: e.target.value }))} className="w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white focus:outline-none focus:ring-1 focus:ring-burgundy-700">
                      <option value="">Not Set</option>
                      {contractStatuses.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                </div>
              </div>
              <div className="flex justify-end gap-2 border-t border-white/10 px-6 py-3">
                <button onClick={() => setAddOpen(false)} className="rounded-lg border border-white/10 px-4 py-1.5 text-xs text-white transition-colors hover:bg-white/5">Cancel</button>
                <button
                  onClick={handleAddMerchant}
                  disabled={!formData.merchantId.trim() || !formData.businessName.trim() || createMerchant.isPending}
                  className="flex items-center gap-1.5 rounded-lg bg-burgundy-700 px-4 py-1.5 text-xs font-medium text-white transition-colors hover:bg-burgundy-800 disabled:opacity-30"
                >
                  {createMerchant.isPending ? <Loader2 size={14} className="animate-spin" /> : null}
                  Onboard Merchant
                </button>
              </div>
            </div>
          </div>
        </>
      )}

      <TwSnackbar open={snackbar.open} message={snackbar.message} severity={snackbar.severity} onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} />
    </HokekaPageShell>
  );
}