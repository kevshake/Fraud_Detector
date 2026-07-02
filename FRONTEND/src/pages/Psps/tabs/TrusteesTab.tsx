import PspListCrud from "../../../components/Common/PspListCrud";
interface Props { pspId: string; trustees: any[]; onRefresh: () => void; }
export default function TrusteesTab({ pspId, trustees, onRefresh }: Props) {
  return <PspListCrud title="Trustees" items={trustees} pspId={pspId} apiPath="psps/trustees" onRefresh={onRefresh} />;
}