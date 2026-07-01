import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { apiClient } from "../../lib/apiClient";
import HokekaPageShell from "../../components/Layout/HokekaPageShell";
import TwSnackbar from "../../components/Common/TwSnackbar";
import { Loader2, Save, DollarSign } from "lucide-react";

export default function LimitsAmlPage() {
  const [transactionLimit, setTransactionLimit] = useState("");
  const [dailyLimit, setDailyLimit] = useState("");
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: "success" | "error" }>({
    open: false, message: "", severity: "success",
  });

  const saveLimits = useMutation({
    mutationFn: () => apiClient.post("limits/aml", {
      transactionLimit: transactionLimit ? parseFloat(transactionLimit) : undefined,
      dailyLimit: dailyLimit ? parseFloat(dailyLimit) : undefined,
    }),
    onSuccess: () => { setSnackbar({ open: true, message: "AML limits saved successfully.", severity: "success" }); },
    onError: (err: any) => { setSnackbar({ open: true, message: err?.message || "Failed to save limits.", severity: "error" }); },
  });

  return (
    <HokekaPageShell title="Transaction Limits" subtitle="Configure AML transaction and daily volume limits">
      <div className="rounded-lg border border-white/10 bg-[#0f1a2e] p-6">
        <h3 className="mb-4 flex items-center gap-2 text-base font-semibold text-white">
          <DollarSign size={20} className="text-burgundy-400" /> AML Limits Configuration
        </h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">Transaction Limit</label>
            <input type="number" value={transactionLimit} onChange={(e) => setTransactionLimit(e.target.value)}
              placeholder="e.g. 10000"
              className="mt-1 w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-burgundy-700" />
            <p className="mt-1 text-xs text-glass-muted">Maximum amount per single transaction (USD)</p>
          </div>
          <div>
            <label className="text-[11px] font-semibold uppercase tracking-wider text-glass-muted">Daily Limit</label>
            <input type="number" value={dailyLimit} onChange={(e) => setDailyLimit(e.target.value)}
              placeholder="e.g. 50000"
              className="mt-1 w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-burgundy-700" />
            <p className="mt-1 text-xs text-glass-muted">Maximum total transaction volume per day (USD)</p>
          </div>
        </div>
        <div className="mt-6">
          <button onClick={() => saveLimits.mutate()} disabled={saveLimits.isPending || (!transactionLimit && !dailyLimit)}
            className="flex items-center gap-2 rounded-lg bg-burgundy-700 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-burgundy-800 disabled:opacity-50">
            {saveLimits.isPending ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
            Save Limits
          </button>
        </div>
      </div>
      <TwSnackbar open={snackbar.open} message={snackbar.message} severity={snackbar.severity} onClose={() => setSnackbar(prev => ({ ...prev, open: false }))} />
    </HokekaPageShell>
  );
}