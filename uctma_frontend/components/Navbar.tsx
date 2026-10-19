

import { useTheme } from "../hooks/useTheme";

type NavbarProps = {
  isAdmin: boolean;
  onPassword: () => void;
  onInvite: () => void;
  onUsers: () => void;
  onApplicationSettings: () => void;
  onUrlUpload: () => void;
  onLogout: () => void;
};

// Navbar yalnızca navigasyon ve aksiyon butonlarını gösterir.
// Yetki kontrolü için backend'den gelen isAdmin bilgisi kullanılır.
export function Navbar({ isAdmin, onPassword, onInvite, onUsers, onApplicationSettings, onUrlUpload, onLogout }: NavbarProps) {
  const { isDark, toggleTheme } = useTheme();

  return (
    <header className="flex flex-col gap-4 border-b pb-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 className="text-2xl font-bold">UCTMA</h1>
        <p className="text-sm text-slate-500">Dosya dönüştürme paneli</p>
      </div>
      <nav className="flex flex-wrap gap-2" aria-label="Uygulama işlemleri">
        <button className="button-secondary" title={isDark ? "Açık temaya geç" : "Koyu temaya geç"} aria-label={isDark ? "Açık temaya geç" : "Koyu temaya geç"} onClick={toggleTheme}>
          { isDark ? "☀️" : "🌜"}
        </button>
        <button className="button-secondary" onClick={onPassword}>Şifre değiştir</button>
        <button className="button-secondary" onClick={onUrlUpload}>URL'den yükle</button>
        {isAdmin && <button className="button-secondary" onClick={onInvite}>Kullanıcı davet et</button>}
        {isAdmin && <button className="button-secondary" onClick={onUsers}>Kullanıcılar</button>}
        {isAdmin && <button className="button-secondary" onClick={onApplicationSettings}>Uygulama ayarları</button>}
        <button className="button-secondary" onClick={onLogout}>Çıkış</button>
      </nav>
    </header>
  );
}
