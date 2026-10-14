import { FormEvent, useEffect, useState } from "react";
import { api } from "../lib/api";
import { LoginResult } from "../lib/types";

const TOKEN_KEY = "uctma_token";
const ROLE_KEY = "uctma_role";

export function useAuth() {
  // Auth bilgileri hem React state'inde hem de localStorage'da tutulur.
  // Böylece sayfa yenilense bile kullanıcı oturumu korunur.
  const [token, setToken] = useState<string | null>(null);
  // Rol backend login response'undan gelir; admin kontrolleri JWT decode edilmeden yapılır.
  const [isAdmin, setIsAdmin] = useState(false);
  // Login formunun alanları.
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  // Backend geçici şifre kullanıldığını bildirirse password dialog'u zorunlu açılır.
  const [mustChangePassword, setMustChangePassword] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // İlk render'da kayıtlı oturum bilgilerini geri yükler.
  useEffect(() => {
    const savedToken = window.localStorage.getItem(TOKEN_KEY);
    if (savedToken) {
      setToken(savedToken);
      setIsAdmin(window.localStorage.getItem(ROLE_KEY) === "ROLE_ADMIN");
    }
  }, []);

  // Login başarılı olunca token ve rol saklanır. page.tsx bu token değişimini
  // izleyerek dosya listesini yükler.
  async function login(event: FormEvent) {
    event.preventDefault();
    const result = await api<LoginResult>("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    window.localStorage.setItem(TOKEN_KEY, result.token);
    window.localStorage.setItem(ROLE_KEY, result.role);
    setToken(result.token);
    setIsAdmin(result.role === "ROLE_ADMIN");
    setMustChangePassword(result.newPasswordRequired);
    return result.newPasswordRequired;
  }

  // Hem mevcut şifreyi hem yeni şifre alanlarını backend'e gönderir.
  // Başarılı olursa form alanları temizlenir ve zorunlu şifre durumu kapanır.
  async function changePassword() {
    if (!token) return;
    if (newPassword !== confirmPassword) throw new Error("Yeni şifreler aynı olmalı.");
    await api("/auth/change-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        currentPassword: password,
        newPassword,
        confirmNewPassword: confirmPassword,
      }),
    }, token);
    setPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setMustChangePassword(false);
  }

  // Oturumu hem React state'inden hem de localStorage'dan temizler.
  function logout() {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(ROLE_KEY);
    setToken(null);
    setIsAdmin(false);
  }

  return {
    token, isAdmin, email, password, mustChangePassword, newPassword, confirmPassword,
    setEmail, setPassword, setNewPassword, setConfirmPassword, setMustChangePassword,
    login, changePassword, logout,
  };
}
