import PspListCrud from "../../../components/Common/PspListCrud";

interface Props { pspId: string; directors: any[]; onRefresh: () => void; }

export default function DirectorsTab({ pspId, directors, onRefresh }: Props) {
  return <PspListCrud title="Directors" items={directors} pspId={pspId} apiPath="psps/directors" onRefresh={onRefresh} />;
}