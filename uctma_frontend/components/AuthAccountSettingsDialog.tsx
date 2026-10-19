import { AuthAccountSetting } from "../lib/types";
import { Modal } from "./Modal";

type AuthAccountSettingsDialogProps = {
  settings: AuthAccountSetting[];
  busy: boolean;
  onSettingChange: (code: string, change: Partial<Pick<AuthAccountSetting, "enabled" | "qty">>) => void;
  onSave: () => void;
  onClose: () => void;
};

export function AuthAccountSettingsDialog({
  settings,
  busy,
  onSettingChange,
  onSave,
  onClose,
}: AuthAccountSettingsDialogProps) {
  return (
    <Modal title="Hesap ve kayıt ayarları" onClose={onClose}>
      <form
        className="max-h-[70vh] space-y-4 overflow-y-auto"
        onSubmit={(event) => {
          event.preventDefault();
          onSave();
        }}
      >
        <p className="text-sm text-slate-500">
          Hesap oluşturma yöntemini seçin. Kayıt açıldığında herkes hesap oluşturabilir;
          davet yönteminde kullanıcıları yalnızca adminler davet edebilir.
        </p>
        <fieldset className="space-y-3 rounded-lg border p-4">
          <legend className="px-1 font-medium">Hesap oluşturma yöntemi</legend>
          <label className="flex items-start gap-2">
            <input
              type="radio"
              name="account-creation-mode"
              checked={!settings.find((setting) => setting.settingCode === "REGISTRATION_ENABLED")?.enabled}
              onChange={() => onSettingChange("REGISTRATION_ENABLED", { enabled: false })}
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
              checked={settings.find((setting) => setting.settingCode === "REGISTRATION_ENABLED")?.enabled ?? false}
              onChange={() => onSettingChange("REGISTRATION_ENABLED", { enabled: true })}
            />
            <span>
              <span className="block font-medium">Kayıt</span>
              <span className="text-sm text-slate-500">Herkes kayıt olabilir; kayıtlar kullanıcı rolüyle oluşturulur.</span>
            </span>
          </label>
        </fieldset>
        {settings.filter((setting) => setting.settingCode !== "REGISTRATION_ENABLED").map((setting) => (
          <fieldset key={setting.settingCode} className="grid gap-3 rounded-lg border p-4 sm:grid-cols-[1fr_auto] sm:items-center">
            <legend className="sr-only">{setting.description}</legend>
            <div>
              <label className="flex items-center gap-2 font-medium">
                <input
                  type="checkbox"
                  checked={setting.enabled}
                  onChange={(event) => onSettingChange(setting.settingCode, { enabled: event.target.checked })}
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
                  onChange={(event) => onSettingChange(setting.settingCode, { qty: Number(event.target.value) })}
                  required
                />
              </label>
            )}
          </fieldset>
        ))}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="button-secondary" onClick={onClose}>İptal</button>
          <button className="button" disabled={busy || settings.length === 0}>
            {busy ? "Kaydediliyor..." : "Kaydet"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
