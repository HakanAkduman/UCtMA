import { FileItem } from "../lib/types";

type FileTableProps = {
  files: FileItem[];
  busy: boolean;
  onReconvert: (id: number) => void;
  onDownload: (path: string, name: string) => void;
  onChunks: (id: number) => void;
  onDelete: (id: number) => void;
};

// Bu component sadece dosya listesini ve butonları render eder.
// API çağrıları parent'tan gelen callback'ler üzerinden çalıştırılır.
export function FileTable({ files, busy, onReconvert, onDownload, onChunks, onDelete }: FileTableProps) {
  function statusLabel(status: FileItem["status"]) {
    return {
      PENDING: "Bekliyor",
      PROCESSING: "İşleniyor",
      COMPLETED: "Tamamlandı",
      FAILED: "Başarısız",
    }[status];
  }

  function statusClass(status: FileItem["status"]) {
    return {
      PENDING: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200",
      PROCESSING: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200",
      COMPLETED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200",
      FAILED: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200",
    }[status];
  }

  return (
    <section className="overflow-hidden rounded-xl bg-white shadow">
      <table className="w-full table-fixed text-left text-sm">
        <thead className="border-b bg-slate-50">
          <tr>
            <th className="w-1/4 p-4">Dosya</th>
            <th className="w-1/6 p-4">Durum</th>
            <th className="p-4">İşlemler</th>
          </tr>
        </thead>
        <tbody>
          {files.map((file) => <tr key={file.id} className="border-b">
            <td className="max-w-0 break-words p-4 align-top">
              {file.originalFileName}
              {file.fileType === "URL" && (
                <span className="ml-2 rounded bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-800 dark:bg-sky-900/40 dark:text-sky-200">
                  URL
                </span>
              )}
            </td>
            <td className="break-words p-4 align-top">
              <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass(file.status)}`}>
                {statusLabel(file.status)}
              </span>
            </td>
            <td className="p-4 align-top">
              <div className="flex flex-wrap gap-2">
                <button className="button-secondary" disabled={busy} onClick={() => onReconvert(file.id)}>Tekrar dönüştür</button>
                <button className="button-secondary" onClick={() => onDownload(`/file/${file.id}/download-md`, `${file.originalFileName}.md`)}>MD indir</button>
                <button className="button-secondary" onClick={() => onChunks(file.id)}>Chunkları görüntüle</button>
                <button className="button-secondary" onClick={() => onDownload(`/file/chunks/${file.id}/download-all`, `${file.originalFileName}-chunks.zip`)}>Tüm chunkları indir</button>
                <button className="text-red-600" disabled={busy} onClick={() => onDelete(file.id)}>Sil</button>
              </div>
            </td>
          </tr>)}
        </tbody>
      </table>
      {files.length === 0 && (
        <div className="p-10 text-center">
          <p className="font-medium">Henüz dosya yok</p>
          <p className="mt-1 text-sm text-slate-500">İlk dosyanızı yukarıdaki alandan yükleyin.</p>
        </div>
      )}
    </section>
  );
}
