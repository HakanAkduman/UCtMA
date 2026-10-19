import { PasswordPolicy } from "../lib/types";
import { Modal } from "./Modal";

type PasswordPoliciesDialogProps = {
  policies: PasswordPolicy[];
  busy: boolean;
  onPolicyChange: (code: string, change: Partial<Pick<PasswordPolicy, "enabled" | "qty">>) => void;
  onSave: () => void;
  onClose: () => void;
};

export function PasswordPoliciesDialog({
  policies,
  busy,
  onPolicyChange,
  onSave,
  onClose,
}: PasswordPoliciesDialogProps) {
  return (
    <Modal title="Parola politikaları" onClose={onClose}>
      <form
        className="max-h-[70vh] space-y-4 overflow-y-auto"
        onSubmit={(event) => {
          event.preventDefault();
          onSave();
        }}
      >
        <p className="text-sm text-slate-500">
          Etkin kurallar parola değişiminde Backend tarafından zorunlu tutulur.
        </p>
        {policies.map((policy) => (
          <fieldset key={policy.passwordPolicyCode} className="grid gap-3 rounded-lg border p-4 sm:grid-cols-[1fr_auto] sm:items-center">
            <legend className="sr-only">{policy.description}</legend>
            <div>
              <label className="flex items-center gap-2 font-medium">
                <input
                  type="checkbox"
                  checked={policy.enabled}
                  onChange={(event) => onPolicyChange(policy.passwordPolicyCode, { enabled: event.target.checked })}
                />
                {policy.description}
              </label>
              <p className="mt-1 text-sm text-slate-500">{policy.passwordPolicyCode}</p>
            </div>
            <label className="flex items-center gap-2 text-sm">
              Miktar
              <input
                className="input w-24"
                type="number"
                min={0}
                max={512}
                value={policy.qty}
                onChange={(event) => onPolicyChange(policy.passwordPolicyCode, { qty: Number(event.target.value) })}
                required
              />
            </label>
          </fieldset>
        ))}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="button-secondary" onClick={onClose}>İptal</button>
          <button className="button" disabled={busy || policies.length === 0}>
            {busy ? "Kaydediliyor..." : "Kaydet"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
