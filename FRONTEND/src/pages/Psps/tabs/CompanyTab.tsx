import { useState } from "react";
import { apiClient } from "../../../lib/apiClient";
import { useQueryClient } from "@tanstack/react-query";
import TwSnackbar from "../../../components/Common/TwSnackbar";
import { Save, Loader2 } from "lucide-react";

interface CompanyTabProps { pspId: string; psp: any }

export default function CompanyTab({ pspId, psp }: CompanyTabProps) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    legalName: psp?.legalName ?? "", tradingName: psp?.tradingName ?? "",
    country: psp?.country ?? "", registrationNumber: psp?.registrationNumber ?? "",
    taxId: psp?.taxId ?? "", contactEmail: psp?.contactEmail ?? "",
    contactPhone: psp?.contactPhone ?? "", contactAddress: psp?.contactAddress ?? "",
  });
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<{ open: boolean; severity: "success" | "error"; message: string }>({ open: false, severity: "success", message: "" });

  const set = (field: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm(f => ({ ...f, [field]: e.target.value }));

  const handleSave = async () => {
    setSaving(true);
    try {
      await apiClient.put(`psps/${pspId}`, form);
      queryClient.invalidateQueries({ queryKey: ["psp", pspId] });
      queryClient.invalidateQueries({ queryKey: ["psps"] });
      setToast({ open: true, severity: "success", message: "Company details saved." });
    } catch { setToast({ open: true, severity: "error", message: "Save failed. Please try again." }); }
    finally { setSaving(false); }
  };

  const inputClass = "w-full rounded-lg border border-white/10 bg-[#1a2744] px-3 py-2 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-burgundy-700";
  const labelClass = "text-[11px] font-semibold uppercase tracking-wider text-glass-muted";

  return (
    <div>
      <h4 className="mb-3 text-base font-semibold text-white">Company Details</h4>
      <div className="grid grid-cols-2 gap-4">
        <div><label className={labelClass}>Legal Name</label><input value={form.legalName} onChange={set("legalName")} className={inputClass} /></div>
        <div><label className={labelClass}>Trading Name</label><input value={form.tradingName} onChange={set("tradingName")} className={inputClass} /></div>
        <div><label className={labelClass}>Country</label><input value={form.country} onChange={set("country")} className={inputClass} /></div>
        <div><label className={labelClass}>Registration Number</label><input value={form.registrationNumber} onChange={set("registrationNumber")} className={inputClass} /></div>
        <div><label className={labelClass}>Tax ID / PIN</label><input value={form.taxId} onChange={set("taxId")} className={inputClass} /></div>
        <div><label className={labelClass}>Contact Email</label><input type="email" value={form.contactEmail} onChange={set("contactEmail")} className={inputClass} /></div>
        <div><label className={labelClass}>Contact Phone</label><input value={form.contactPhone} onChange={set("contactPhone")} className={inputClass} /></div>
        <div className="col-span-2"><label className={labelClass}>Contact Address</label><textarea value={form.contactAddress} onChange={set("contactAddress")} rows={2} className={inputClass} /></div>
      </div>
      <div className="mt-4 flex justify-end">
        <button onClick={handleSave} disabled={saving}
          className="flex items-center gap-2 rounded-lg bg-burgundy-700 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-burgundy-800 disabled:opacity-50">
          {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />} Save Changes
        </button>
      </div>
      <TwSnackbar open={toast.open} message={toast.message} severity={toast.severity} onClose={() => setToast(t => ({ ...t, open: false }))} />
    </div>
  );
}