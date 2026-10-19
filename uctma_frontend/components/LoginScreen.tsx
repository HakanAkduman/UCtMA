import { FormEvent, useState } from "react";
import { useTheme } from "../hooks/useTheme";

type LoginScreenProps = {
  email: string;
  password: string;
  busy: boolean;
  message: string;
  messageType: "success" | "error";
  registrationEnabled: boolean;
  onEmailChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onForgotPassword: (event: FormEvent) => void;
  onRegister: () => void;
};

// Login ekranı kendi auth işlemini yapmaz; alan değerlerini ve submit olayını
// parent component'e callback olarak iletir.
export function LoginScreen(props: LoginScreenProps) {
  const { isDark, toggleTheme } = useTheme();
  const [forgotPassword, setForgotPassword] = useState(false);

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-6">
      <form onSubmit={forgotPassword ? props.onForgotPassword : props.onSubmit} className="w-full space-y-4 rounded-xl bg-white p-6 shadow">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">{forgotPassword ? "Parolamı unuttum" : "UCTMA"}</h1>
          <button type="button" className="button-secondary" onClick={toggleTheme}>
            {isDark ? "☀️" : "🌙"}
          </button>
        </div>
        {!forgotPassword && <p className="text-sm text-slate-500">Dosyaları RAG için dönüştürme platformu</p>}
        <input className="input w-full" type="email" placeholder="Email" value={props.email} onChange={(e) => props.onEmailChange(e.target.value)} required />
        {forgotPassword ? (
          <p className="text-sm text-slate-500">
            E-posta adresi sistemde kayıtlıysa tek kullanımlık geçici parola gönderilir. Bu parolayla giriş yaptıktan sonra yeni parolanızı belirlemeniz gerekir.
          </p>
        ) : (
          <input className="input w-full" type="password" placeholder="Şifre" autoComplete="current-password" value={props.password} onChange={(e) => props.onPasswordChange(e.target.value)} required />
        )}
        <button className="button w-full" disabled={props.busy}>
          {forgotPassword ? "Geçici parola gönder" : "Giriş yap"}
        </button>
        {!forgotPassword && (
          <button type="button" className="w-full text-sm text-blue-700 hover:underline" onClick={() => setForgotPassword(true)}>
            Parolamı unuttum
          </button>
        )}
        {!forgotPassword && props.registrationEnabled && (
          <button type="button" className="button-secondary w-full" onClick={props.onRegister}>
            Hesabınız yok mu? Kayıt olun
          </button>
        )}
        {forgotPassword && (
          <button type="button" className="button-secondary w-full" onClick={() => setForgotPassword(false)}>
            Giriş ekranına dön
          </button>
        )}
        {props.message && (
          <p role={props.messageType === "error" ? "alert" : "status"}
             className={`text-sm ${props.messageType === "error" ? "text-red-600" : "text-emerald-700"}`}>
            {props.message}
          </p>
        )}
      </form>
    </main>
  );
}
