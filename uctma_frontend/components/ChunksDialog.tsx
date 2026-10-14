import { ChunkItem } from "../lib/types";
import { Modal } from "./Modal";

type ChunksDialogProps = {
  chunks?: ChunkItem[];
  busy: boolean;
  onDownload: (chunk: ChunkItem) => void;
  onDelete: (chunkId: number) => void;
  onClose: () => void;
};

// Seçilen dosyanın chunk'larını listeler. İndirme ve silme işlemlerini
// parent callback'leri üzerinden başlatır.
export function ChunksDialog({ chunks, busy, onDownload, onDelete, onClose }: ChunksDialogProps) {


  return (
    <Modal title="Chunklar" onClose={onClose}>
      {chunks ? (chunks.length ? (
        <ul className="max-h-80 space-y-2 overflow-y-auto text-sm text-slate-600">
          {chunks.map((chunk) => (
            <li key={chunk.id} className="flex min-w-0 flex-wrap items-center justify-between gap-3 rounded bg-slate-50 p-2">
              <span className="min-w-0 flex-1 break-words">#{chunk.id} {chunk.chunkName}</span>
              <span className="flex shrink-0 gap-2">
                <button className="button-secondary" onClick={() => onDownload(chunk)}>İndir</button>
                <button className="text-red-600" disabled={busy} onClick={() => onDelete(chunk.id)}>Sil</button>
              </span>
            </li>
          ))}
        </ul>
      ) : <p className="text-sm text-slate-500">Bu dosyada chunk yok.</p>) : <p className="text-sm text-slate-500">Chunklar yükleniyor...</p>}
    </Modal>
  );
}
