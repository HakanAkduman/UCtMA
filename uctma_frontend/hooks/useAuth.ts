import { FormEvent, useEffect, useState } from "react";
import { api } from "../lib/api";
import { LoginResult } from "../lib/types";

const TOKEN_KEY = "uctma_token";
const ROLE_KEY = "uctma_role";
const MUST_CHANGE_PASSWORD_KEY = "uctma_must_change_password";
const PASSWORD_RESET_SESSION_KEY = "uctma_password_reset_session";

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
  const [passwordResetSession, setPasswordResetSession] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // İlk render'da kayıtlı oturum bilgilerini geri yükler.
  useEffect(() => {
    const savedToken = window.localStorage.getItem(TOKEN_KEY);
    if (savedToken) {
      setToken(savedToken);
      setIsAdmin(window.localStorage.getItem(ROLE_KEY) === "ROLE_ADMIN");
      setMustChangePassword(window.localStorage.getItem(MUST_CHANGE_PASSWORD_KEY) === "true");
      setPasswordResetSession(window.localStorage.getItem(PASSWORD_RESET_SESSION_KEY) === "true");
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
    window.localStorage.setItem(MUST_CHANGE_PASSWORD_KEY, String(result.newPasswordRequired));
    window.localStorage.setItem(PASSWORD_RESET_SESSION_KEY, String(result.passwordResetSession));
    setToken(result.token);
    setIsAdmin(result.role === "ROLE_ADMIN");
    setMustChangePassword(result.newPasswordRequired);
    setPasswordResetSession(result.passwordResetSession);
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
    window.localStorage.removeItem(MUST_CHANGE_PASSWORD_KEY);
    window.localStorage.removeItem(PASSWORD_RESET_SESSION_KEY);
    setPasswordResetSession(false);
    return passwordResetSession;
  }

  // Oturumu hem React state'inden hem de localStorage'dan temizler.
  function logout() {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(ROLE_KEY);
    window.localStorage.removeItem(MUST_CHANGE_PASSWORD_KEY);
    window.localStorage.removeItem(PASSWORD_RESET_SESSION_KEY);
    setToken(null);
    setIsAdmin(false);
    setPassword("");
    setMustChangePassword(false);
    setPasswordResetSession(false);
  }

  return {
    token, isAdmin, email, password, mustChangePassword, passwordResetSession, newPassword, confirmPassword,
    setEmail, setPassword, setNewPassword, setConfirmPassword, setMustChangePassword,
    login, changePassword, logout,
  };
}
