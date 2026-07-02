import PspListCrud from "../../../components/Common/PspListCrud";
interface Props { pspId: string; shareholders: any[]; onRefresh: () => void; }
export default function ShareholdersTab({ pspId, shareholders, onRefresh }: Props) {
  return <PspListCrud title="Shareholders" items={shareholders} pspId={pspId} apiPath="psps/shareholders" onRefresh={onRefresh} />;
}