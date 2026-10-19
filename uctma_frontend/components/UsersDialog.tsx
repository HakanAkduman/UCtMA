import { UserItem } from "../lib/types";
import { Modal } from "./Modal";

type UsersDialogProps = {
  users: UserItem[];
  busy: boolean;
  currentUserEmail: string;
  onDelete: (email: string) => void;
  onSetEnabled: (email: string, enabled: boolean) => void;
  onClose: () => void;
};

// Kullanıcı listesini gösterir. Silme isteğini doğrudan yapmaz; email'i
// parent callback'ine göndererek API işlemini parent/hook'a bırakır.
export function UsersDialog({ users, busy, currentUserEmail, onDelete, onSetEnabled, onClose }: UsersDialogProps) {
  return (
    <Modal title="Kullanıcılar" onClose={onClose}>
      {users.length ? (
        <ul className="max-h-80 space-y-2 overflow-y-auto text-sm">
          {users.map((user) => (
            <li key={user.email} className="flex flex-col gap-3 rounded bg-slate-50 p-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <p className="break-all">{user.email} <span className="text-xs text-slate-500">({user.role})</span></p>
                <p className="mt-1 text-xs text-slate-500">
                  Durum: {user.enabled ? "Aktif" : "Pasif"} · Hatalı giriş: {user.failedLoginAttempts}
                </p>
              </div>
              <div className="flex shrink-0 gap-3">
                <button
                  className={user.enabled ? "text-amber-700 hover:underline" : "text-emerald-700 hover:underline"}
                  disabled={busy || (user.email.toLocaleLowerCase() === currentUserEmail.toLocaleLowerCase() && user.enabled)}
                  title={user.email.toLocaleLowerCase() === currentUserEmail.toLocaleLowerCase() && user.enabled
                    ? "Kendi hesabınızı devre dışı bırakamazsınız"
                    : undefined}
                  onClick={() => onSetEnabled(user.email, !user.enabled)}
                >
                  {user.enabled ? "Devre dışı bırak" : "Etkinleştir"}
                </button>
                <button className="text-red-600 hover:underline" disabled={busy} onClick={() => onDelete(user.email)}>Sil</button>
              </div>
            </li>
          ))}
        </ul>
      ) : <p className="text-sm text-slate-500">Kullanıcı bulunamadı.</p>}
    </Modal>
  );
}
