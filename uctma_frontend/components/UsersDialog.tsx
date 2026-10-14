import { UserItem } from "../lib/types";
import { Modal } from "./Modal";

type UsersDialogProps = {
  users: UserItem[];
  busy: boolean;
  onDelete: (email: string) => void;
  onClose: () => void;
};

// Kullanıcı listesini gösterir. Silme isteğini doğrudan yapmaz; email'i
// parent callback'ine göndererek API işlemini parent/hook'a bırakır.
export function UsersDialog({ users, busy, onDelete, onClose }: UsersDialogProps) {
  return (
    <Modal title="Kullanıcılar" onClose={onClose}>
      {users.length ? (
        <ul className="max-h-80 space-y-2 overflow-y-auto text-sm">
          {users.map((user) => (
            <li key={user.email} className="flex items-center justify-between gap-3 rounded bg-slate-50 p-3">
              <span>{user.email} <span className="text-xs text-slate-500">({user.role})</span></span>
              <button className="text-red-600" disabled={busy} onClick={() => onDelete(user.email)}>Sil</button>
            </li>
          ))}
        </ul>
      ) : <p className="text-sm text-slate-500">Kullanıcı bulunamadı.</p>}
    </Modal>
  );
}
