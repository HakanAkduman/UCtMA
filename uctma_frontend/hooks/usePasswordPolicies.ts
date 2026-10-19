import { useCallback, useState } from "react";
import { api } from "../lib/api";
import { PasswordPolicy } from "../lib/types";

export function usePasswordPolicies(token: string | null) {
  const [policies, setPolicies] = useState<PasswordPolicy[]>([]);

  const loadPolicies = useCallback(async () => {
    if (!token) throw new Error("Oturum bulunamadı.");
    setPolicies(await api<PasswordPolicy[]>("/auth/password-policies", {}, token));
  }, [token]);

  const savePolicies = useCallback(async (nextPolicies: PasswordPolicy[]) => {
    if (!token) throw new Error("Oturum bulunamadı.");
    const saved = await api<PasswordPolicy[]>("/auth/password-policies", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        policies: nextPolicies.map(({ passwordPolicyCode, enabled, qty }) => ({
          passwordPolicyCode,
          enabled,
          qty,
        })),
      }),
    }, token);
    setPolicies(saved);
  }, [token]);

  return { policies, setPolicies, loadPolicies, savePolicies };
}
