import { FormEvent } from "react";
import { PasswordPolicy } from "../lib/types";
import { Modal } from "./Modal";

type PasswordFormProps = {
  required: boolean;
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
  busy: boolean;
  policies: PasswordPolicy[];
  onCurrentPasswordChange: (value: string) => void;
  onNewPasswordChange: (value: string) => void;
  onConfirmPasswordChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onClose?: () => void;
};

// Hem ilk girişte zorunlu şifre belirleme hem de normal şifre değiştirme
// akışı aynı form üzerinden yönetilir. required değeri alanları ve kapanmayı belirler.
export function PasswordForm(props: PasswordFormProps) {
  const minLength = props.policies.find((policy) => policy.passwordPolicyCode === "MIN_LENGTH" && policy.enabled)?.qty;
  const maxLength = props.policies.find((policy) => policy.passwordPolicyCode === "MAX_LENGTH" && policy.enabled)?.qty;

  return (
    <Modal title={props.required ? "Yeni şifre belirle" : "Şifre değiştir"} onClose={props.required ? undefined : props.onClose}>
      <form onSubmit={props.onSubmit} className="space-y-4">
        {props.required && <p className="text-sm text-slate-500">Güvenlik için yeni bir şifre belirlemelisiniz.</p>}
        {!props.required && (
          <input className="input w-full" type="password" placeholder="Mevcut şifre" value={props.currentPassword} onChange={(e) => props.onCurrentPasswordChange(e.target.value)} required />
        )}
        {props.policies.some((policy) => policy.enabled) && (
          <ul className="list-inside list-disc text-sm text-slate-500">
            {props.policies.filter((policy) => policy.enabled).map((policy) => (
              <li key={policy.passwordPolicyCode}>{policy.description}: {policy.qty}</li>
            ))}
          </ul>
        )}
        <input className="input w-full" type="password" placeholder="Yeni şifre" value={props.newPassword} onChange={(e) => props.onNewPasswordChange(e.target.value)} minLength={minLength} maxLength={maxLength} required />
        <input className="input w-full" type="password" placeholder="Yeni şifre tekrar" value={props.confirmPassword} onChange={(e) => props.onConfirmPasswordChange(e.target.value)} minLength={minLength} maxLength={maxLength} required />
        <button className="button w-full" disabled={props.busy}>{props.busy ? "Kaydediliyor..." : "Şifreyi kaydet"}</button>
      </form>
    </Modal>
  );
}
