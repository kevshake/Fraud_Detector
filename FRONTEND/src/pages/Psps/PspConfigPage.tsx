import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { usePsp } from "../../features/api/queries";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Loader2 } from "lucide-react";
import { apiClient } from "../../lib/apiClient";
import CompanyTab from "./tabs/CompanyTab";
import CbkReportingTab from "./tabs/CbkReportingTab";
import DirectorsTab from "./tabs/DirectorsTab";
import ShareholdersTab from "./tabs/ShareholdersTab";
import TrusteesTab from "./tabs/TrusteesTab";
import SeniorManagementTab from "./tabs/SeniorManagementTab";
import ProductsTab from "./tabs/ProductsTab";
import TrustAccountsTab from "./tabs/TrustAccountsTab";
import TariffsTab from "./tabs/TariffsTab";
import BillingTab from "./tabs/BillingTab";
import HokekaPageShell from "../../components/Layout/HokekaPageShell";
import { useQueryClient } from "@tanstack/react-query";

const TAB_LABELS = [
  "Company", "CBK Reporting", "Directors", "Shareholders", "Trustees",
  "Senior Management", "Products", "Trust Accounts", "Tariffs", "Billing",
];

const LIST_QUERIES: Record<string, string> = {
  directors: "directors", shareholders: "shareholders", trustees: "trustees",
  "senior-management": "senior-management", products: "products",
  "trust-accounts": "trust-accounts", tariffs: "tariffs",
};

export default function PspConfigPage() {
  const { pspId } = useParams<{ pspId: string }>();
  const navigate = useNavigate();
  const [tab, setTab] = useState(0);
  const queryClient = useQueryClient();

  const numericId = Number(pspId ?? 0);
  const { data: psp, isLoading, isError } = usePsp(numericId);

  const listKeys = Object.keys(LIST_QUERIES);
  const listIndex = tab >= 2 ? tab - 2 : -1;
  const listKey = listIndex >= 0 && listIndex < listKeys.length ? listKeys[listIndex] : null;

  const { data: listData, refetch: refetchList } = useQuery({
    queryKey: ["psp", pspId, "list", listKey],
    queryFn: async () => {
      if (!listKey || !pspId) return [];
      return apiClient.get<any[]>(`psps/${pspId}/${LIST_QUERIES[listKey]}`);
    },
    enabled: listKey !== null && pspId !== undefined,
  });

  const onRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ["psp", pspId, "list"] });
    if (refetchList) refetchList();
  };

  if (!pspId) return <div className="rounded-lg border border-red-700/30 bg-red-900/30 px-4 py-3 text-sm text-red-200">Invalid PSP ID.</div>;

  const pspName = (psp as any)?.legalName ?? (psp as any)?.tradingName ?? (psp as any)?.name ?? `PSP ${pspId}`;

  return (
    <HokekaPageShell title={pspName} subtitle="PSP configuration and compliance settings" noCard>
      <div className="mb-4 flex items-center gap-2">
        <button onClick={() => navigate("/psps")} className="flex items-center gap-1 rounded p-1 text-xs text-burgundy-400 transition-colors hover:bg-white/10">
          <ArrowLeft size={16} /> Back
        </button>
        <h3 className="text-base font-semibold text-white">{isLoading ? "Loading…" : `Configure ${pspName}`}</h3>
      </div>

      {isError && <div className="mb-3 rounded-lg border border-amber-700/30 bg-amber-900/30 px-4 py-3 text-sm text-amber-200">Could not load PSP details. You can still manage entities below.</div>}

      {isLoading ? (
        <div className="flex justify-center py-8"><Loader2 size={28} className="animate-spin text-glass-muted" /></div>
      ) : (
        <>
          <div className="mb-4 flex gap-0 overflow-x-auto border-b border-white/10">
            {TAB_LABELS.map((label, i) => (
              <button key={label} onClick={() => setTab(i)}
                className={`whitespace-nowrap px-4 py-2.5 text-xs font-medium transition-colors ${
                  tab === i ? "border-b-2 border-burgundy-700 text-burgundy-400" : "text-glass-muted hover:text-white"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          <div className="mt-2">
            {tab === 0 && <CompanyTab pspId={pspId} psp={psp} />}
            {tab === 1 && <CbkReportingTab pspId={pspId} psp={psp} />}
            {tab >= 2 && listKey && (() => {
              const items = listData || [];
              const props = { pspId, items, onRefresh };
              switch (listKey) {
                case "directors": return <DirectorsTab {...props} directors={items} />;
                case "shareholders": return <ShareholdersTab {...props} shareholders={items} />;
                case "trustees": return <TrusteesTab {...props} trustees={items} />;
                case "senior-management": return <SeniorManagementTab {...props} seniorMgmt={items} />;
                case "products": return <ProductsTab {...props} products={items} />;
                case "trust-accounts": return <TrustAccountsTab {...props} trustAccounts={items} />;
                case "tariffs": return <TariffsTab {...props} tariffs={items} />;
                default: return <p className="text-sm text-glass-muted">Tab not implemented.</p>;
              }
            })()}
            {tab === 9 && <BillingTab pspId={pspId} />}
          </div>
        </>
      )}
    </HokekaPageShell>
  );
}