import PspListCrud from "../../../components/Common/PspListCrud";
interface Props { pspId: string; seniorMgmt: any[]; onRefresh: () => void; }
export default function SeniorManagementTab({ pspId, seniorMgmt, onRefresh }: Props) {
  return <PspListCrud title="Senior Management" items={seniorMgmt} pspId={pspId} apiPath="psps/senior-management" onRefresh={onRefresh}
    extraFields={[{ key: "title", label: "Title", placeholder: "e.g. CEO" }]} />;
}