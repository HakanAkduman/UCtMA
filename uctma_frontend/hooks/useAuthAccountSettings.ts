import { useCallback, useState } from "react";
import { api } from "../lib/api";
import { AuthAccountSetting } from "../lib/types";

export function useAuthAccountSettings(token: string | null) {
  const [settings, setSettings] = useState<AuthAccountSetting[]>([]);

  const loadSettings = useCallback(async () => {
    if (!token) throw new Error("Oturum bulunamadı.");
    setSettings(await api<AuthAccountSetting[]>("/auth/account-settings", {}, token));
  }, [token]);

  const saveSettings = useCallback(async (nextSettings: AuthAccountSetting[]): Promise<AuthAccountSetting[]> => {
    if (!token) throw new Error("Oturum bulunamadı.");
    const saved = await api<AuthAccountSetting[]>("/auth/account-settings", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        settings: nextSettings.map(({ settingCode, enabled, qty }) => ({ settingCode, enabled, qty })),
      }),
    }, token);
    setSettings(saved);
    return saved;
  }, [token]);

  return { settings, setSettings, loadSettings, saveSettings };
}
