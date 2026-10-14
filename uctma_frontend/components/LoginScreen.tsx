import { FormEvent } from "react";
import { useTheme } from "../hooks/useTheme";

type LoginScreenProps = {
  email: string;
  password: string;
  busy: boolean;
  message: string;
  onEmailChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
};

// Login ekranı kendi auth işlemini yapmaz; alan değerlerini ve submit olayını
// parent component'e callback olarak iletir.
export function LoginScreen(props: LoginScreenProps) {
  const { isDark, toggleTheme } = useTheme();

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-6">
      <form onSubmit={props.onSubmit} className="w-full space-y-4 rounded-xl bg-white p-6 shadow">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">UCTMA</h1>
          <button type="button" className="button-secondary" onClick={toggleTheme}>
            {isDark ? "☀️" : "🌙"}
          </button>
        </div>
        <p className="text-sm text-slate-500">Dosyaları RAG için dönüştürme platformu</p>
        <input className="input w-full" type="email" placeholder="Email" value={props.email} onChange={(e) => props.onEmailChange(e.target.value)} required />
        <input className="input w-full" type="password" placeholder="Şifre" value={props.password} onChange={(e) => props.onPasswordChange(e.target.value)} required />
        <button className="button w-full" disabled={props.busy}>Giriş yap</button>
        {props.message && <p className="text-sm text-red-600">{props.message}</p>}
      </form>
    </main>
  );
}
