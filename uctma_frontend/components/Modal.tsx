import { ReactNode } from "react";

type ModalProps = {
  title: string;
  onClose?: () => void;
  children: ReactNode;
};

// Tüm dialog'lar aynı modal kabını kullanır. İçerik children ile dışarıdan
// verilir; böylece her dialog için tekrar overlay ve başlık kodu yazılmaz.
export function Modal({ title, onClose, children }: ModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        className="max-h-[90vh] w-full max-w-2xl overflow-hidden rounded-xl bg-white p-6 shadow-xl"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          {onClose && (
            <button type="button" className="text-slate-500" onClick={onClose}>
              ✕
            </button>
          )}
        </div>
        {children}
      </div>
    </div>
  );
}
