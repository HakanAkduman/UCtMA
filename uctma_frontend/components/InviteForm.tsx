import { FormEvent } from "react";
import { Modal } from "./Modal";

type InviteFormProps = {
  email: string;
  role: string;
  busy: boolean;
  onEmailChange: (value: string) => void;
  onRoleChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onClose: () => void;
};

// Admin'in yeni kullanıcı oluşturması için kullanılan dialog formudur.
// Backend'in beklediği ROLE_USER ve ROLE_ADMIN değerleri select'ten gönderilir.
export function InviteForm(props: InviteFormProps) {
  return (
    <Modal title="Kullanıcı davet et" onClose={props.onClose}>
      <form onSubmit={props.onSubmit} className="space-y-4">
        <input className="input w-full" type="email" placeholder="Kullanıcı email" onInvalid={(e) => { e.currentTarget.setCustomValidity("Lütfen geçerli bir mail adresi girin."); }} onChange={(e) => { e.currentTarget.setCustomValidity(""); props.onEmailChange(e.target.value); }} required />
        <select className="input w-full" value={props.role} onChange={(e) => props.onRoleChange(e.target.value)}>
          <option value="ROLE_USER">USER</option>
          <option value="ROLE_ADMIN">ADMIN</option>
        </select>
        <button className="button w-full" disabled={props.busy}>Davet gönder</button>
      </form>
    </Modal>
  );
}
