import { useState } from "react";
import { AuthAccountSetting, PasswordPolicy } from "../lib/types";
import { Modal } from "./Modal";

type ApplicationSettingsDialogProps = {
  accountSettings: AuthAccountSetting[];
  passwordPolicies: PasswordPolicy[];
  busy: boolean;
  onAccountSettingChange: (
    code: string,
    change: Partial<Pick<AuthAccountSetting, "enabled" | "qty">>,
  ) => void;
  onPasswordPolicyChange: (
    code: string,
    change: Partial<Pick<PasswordPolicy, "enabled" | "qty">>,
  ) => void;
  onSaveAccountSettings: () => void;
  onSavePasswordPolicies: () => void;
  onClose: () => void;
};

type Section = "accounts" | "passwords";

export function ApplicationSettingsDialog({
  accountSettings,
  passwordPolicies,
  busy,
  onAccountSettingChange,
  onPasswordPolicyChange,
  onSaveAccountSettings,
  onSavePasswordPolicies,
  onClose,
}: ApplicationSettingsDialogProps) {
  const [section, setSection] = useState<Section>("accounts");
  const registrationEnabled = accountSettings.find(
    (setting) => setting.settingCode === "REGISTRATION_ENABLED",
  )?.enabled ?? false;

  return (
    <Modal title="Uygulama ayarları" onClose={onClose}>
      <div className="space-y-5">
        <nav className="flex gap-2 border-b pb-3" aria-label="Uygulama ayarı kategorileri">
          <button
            type="button"
            className={section === "accounts" ? "button" : "button-secondary"}
            aria-pressed={section === "accounts"}
            onClick={() => setSection("accounts")}
          >
            Hesap ve kayıt
          </button>
          <button
            type="button"
            className={section === "passwords" ? "button" : "button-secondary"}
            aria-pressed={section === "passwords"}
            onClick={() => setSection("passwords")}
          >
            Parola politikaları
          </button>
        </nav>

        {section === "accounts" ? (
          <form
            className="max-h-[65vh] space-y-4 overflow-y-auto"
            onSubmit={(event) => {
              event.preventDefault();
              onSaveAccountSettings();
            }}
          >
            <p className="text-sm text-slate-500">
              Hesap oluşturma yöntemini seçin. Kayıt seçeneği açıldığında herkes hesap oluşturabilir;
              davet yönteminde kullanıcıları yalnızca adminler davet edebilir.
            </p>
            <fieldset className="space-y-3 rounded-lg border p-4">
              <legend className="px-1 font-medium">Hesap oluşturma yöntemi</legend>
              <label className="flex items-start gap-2">
                <input
                  type="radio"
                  name="account-creation-mode"
                  checked={!registrationEnabled}
                  onChange={() => onAccountSettingChange("REGISTRATION_ENABLED", { enabled: false })}
                />
                <span>
                  <span className="block font-medium">Davet</span>
                  <span className="text-sm text-slate-500">Herkese açık kayıt kapalıdır; admin kullanıcıları davet eder.</span>
                </span>
              </label>
              <label className="flex items-start gap-2">
                <input
                  type="radio"
                  name="account-creation-mode"
                  checked={registrationEnabled}
                  onChange={() => onAccountSettingChange("REGISTRATION_ENABLED", { enabled: true })}
                />
                <span>
                  <span className="block font-medium">Kayıt</span>
                  <span className="text-sm text-slate-500">Herkes kayıt olabilir; kayıtlar kullanıcı rolüyle oluşturulur.</span>
                </span>
              </label>
            </fieldset>
            {accountSettings.filter((setting) => setting.settingCode !== "REGISTRATION_ENABLED").map((setting) => (
              <fieldset
                key={setting.settingCode}
                className="grid gap-3 rounded-lg border p-4 sm:grid-cols-[1fr_auto] sm:items-center"
              >
                <legend className="sr-only">{setting.description}</legend>
                <div>
                  <label className="flex items-center gap-2 font-medium">
                    <input
                      type="checkbox"
                      checked={setting.enabled}
                      onChange={(event) => onAccountSettingChange(setting.settingCode, { enabled: event.target.checked })}
                    />
                    {setting.settingCode === "EMAIL_VERIFICATION_REQUIRED"
                      ? "Kayıt için e-posta doğrulaması zorunlu"
                      : setting.description}
                  </label>
                  <p className="mt-1 text-sm text-slate-500">
                    {setting.settingCode === "EMAIL_VERIFICATION_REQUIRED"
                      ? "Kayıt sırasında e-posta doğrulamasını zorunlu tutar."
                      : setting.settingCode === "LOGIN_FAILURE_LOCKOUT"
                        ? "Belirlenen hatalı parola denemesi sınırına ulaşınca hesabı devre dışı bırakır."
                      : setting.settingCode}
                  </p>
                </div>
                {setting.settingCode !== "EMAIL_VERIFICATION_REQUIRED" && (
                  <label className="flex items-center gap-2 text-sm">
                    {setting.settingCode === "LOGIN_FAILURE_LOCKOUT"
                      ? "Hatalı giriş sayısı"
                      : setting.settingCode.includes("MINUTES")
                        ? "Dakika"
                        : setting.settingCode.includes("SECONDS")
                          ? "Saniye"
                          : "Sayı"}
                    <input
                      className="input w-24"
                      type="number"
                      min={setting.settingCode === "LOGIN_FAILURE_LOCKOUT" && setting.enabled ? 1 : 0}
                      max={setting.settingCode === "LOGIN_FAILURE_LOCKOUT" ? 20 : 10080}
                      value={setting.qty}
                      onChange={(event) => onAccountSettingChange(
                        setting.settingCode,
                        { qty: Number(event.target.value) },
                      )}
                      required
                    />
                  </label>
                )}
              </fieldset>
            ))}
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" className="button-secondary" onClick={onClose}>Kapat</button>
              <button className="button" disabled={busy || accountSettings.length === 0}>
                {busy ? "Kaydediliyor..." : "Hesap ayarlarını kaydet"}
              </button>
            </div>
          </form>
        ) : (
          <form
            className="max-h-[65vh] space-y-4 overflow-y-auto"
            onSubmit={(event) => {
              event.preventDefault();
              onSavePasswordPolicies();
            }}
          >
            <p className="text-sm text-slate-500">
              Etkin kurallar parola değişiminde Backend tarafından zorunlu tutulur.
            </p>
            {passwordPolicies.map((policy) => (
              <fieldset
                key={policy.passwordPolicyCode}
                className="grid gap-3 rounded-lg border p-4 sm:grid-cols-[1fr_auto] sm:items-center"
              >
                <legend className="sr-only">{policy.description}</legend>
                <div>
                  <label className="flex items-center gap-2 font-medium">
                    <input
                      type="checkbox"
                      checked={policy.enabled}
                      onChange={(event) => onPasswordPolicyChange(
                        policy.passwordPolicyCode,
                        { enabled: event.target.checked },
                      )}
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
                    onChange={(event) => onPasswordPolicyChange(
                      policy.passwordPolicyCode,
                      { qty: Number(event.target.value) },
                    )}
                    required
                  />
                </label>
              </fieldset>
            ))}
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" className="button-secondary" onClick={onClose}>Kapat</button>
              <button className="button" disabled={busy || passwordPolicies.length === 0}>
                {busy ? "Kaydediliyor..." : "Parola politikalarını kaydet"}
              </button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  );
}
