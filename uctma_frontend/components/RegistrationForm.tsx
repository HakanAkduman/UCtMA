import { FormEvent, useState } from "react";
import { api } from "../lib/api";
import { useTheme } from "../hooks/useTheme";

type RegistrationFormProps = {
  message: string;
  messageType: "success" | "error";
  busy: boolean;
  onBackToLogin: (email: string) => void;
  onBusy: (action: () => Promise<void>, successMessage?: string) => void;
};

export function RegistrationForm(props: RegistrationFormProps) {
  const { isDark, toggleTheme } = useTheme();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [stage, setStage] = useState<"register" | "verify" | "complete">("register");
  const [localMessage, setLocalMessage] = useState("");

  async function register(event: FormEvent) {
    event.preventDefault();
    setLocalMessage("");
    props.onBusy(async () => {
      const result = await api<{ message: string; emailVerificationRequired: boolean }>("/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      setPassword("");
      if (result.emailVerificationRequired) {
        setStage("verify");
        setLocalMessage(result.message);
      } else {
        setStage("complete");
        setLocalMessage(result.message);
      }
    });
  }

  async function verify(event: FormEvent) {
    event.preventDefault();
    setLocalMessage("");
    props.onBusy(async () => {
      const response = await api<string>("/auth/verify-email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code }),
      });
      setStage("complete");
      setLocalMessage(response);
    });
  }

  async function resendCode() {
    setLocalMessage("");
    props.onBusy(async () => {
      const response = await api<string>(`/auth/resend-verification?email=${encodeURIComponent(email)}`, { method: "POST" });
      setLocalMessage(response);
    });
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-6">
      <form
        onSubmit={(event) => void (stage === "register" ? register(event) : stage === "verify" ? verify(event) : undefined)}
        className="w-full space-y-4 rounded-xl bg-white p-6 shadow"
      >
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">{stage === "verify" ? "E-posta doğrulama" : "Hesap oluştur"}</h1>
          <button type="button" className="button-secondary" onClick={toggleTheme}>{isDark ? "☀️" : "🌙"}</button>
        </div>
        {stage === "register" && (
          <>
            <p className="text-sm text-slate-500">Kayıt sırasında seçtiğiniz parola ile e-posta adresinizi doğruladıktan sonra giriş yapabilirsiniz.</p>
            <input className="input w-full" type="email" placeholder="Email" value={email} onChange={(event) => setEmail(event.target.value)} required />
            <input className="input w-full" type="password" placeholder="Şifre" value={password} onChange={(event) => setPassword(event.target.value)} required />
            <button className="button w-full" disabled={props.busy}>Kayıt ol</button>
          </>
        )}
        {stage === "verify" && (
          <>
            <p className="text-sm text-slate-500">{email} adresine gönderilen 6 haneli kodu girin. Kayıt sırasında belirlediğiniz parolayı değiştirmek gerekmez.</p>
            <input className="input w-full" type="text" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="6 haneli doğrulama kodu" value={code} onChange={(event) => setCode(event.target.value)} required />
            <button className="button w-full" disabled={props.busy}>E-postayı doğrula</button>
            <button type="button" className="button-secondary w-full" disabled={props.busy} onClick={() => void resendCode()}>Yeni kod gönder</button>
          </>
        )}
        {stage === "complete" && <button type="button" className="button w-full" onClick={() => props.onBackToLogin(email)}>Giriş yap</button>}
        {stage !== "complete" && <button type="button" className="button-secondary w-full" onClick={() => props.onBackToLogin(email)}>Giriş ekranına dön</button>}
        {(localMessage || props.message) && (
          <p
            role={props.messageType === "error" ? "alert" : "status"}
            className={`text-sm ${props.messageType === "error" ? "text-red-600" : "text-emerald-700"}`}
          >
            {props.messageType === "error" ? props.message : localMessage || props.message}
          </p>
        )}
      </form>
    </main>
  );
}
