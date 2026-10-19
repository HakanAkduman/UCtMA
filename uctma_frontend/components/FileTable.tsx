import { useState } from "react";
import { FileItem } from "../lib/types";

type ActionIconName = "reconvert" | "preview" | "markdown" | "chunks" | "archive" | "delete";

function ActionIcon({ name }: { name: ActionIconName }) {
  const commonProps = {
    "aria-hidden": true as const,
    className: "h-5 w-5",
    fill: "none",
    stroke: "currentColor",
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    strokeWidth: 1.8,
    viewBox: "0 0 24 24",
  };

  switch (name) {
    case "reconvert":
      return <svg {...commonProps}><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.6 9a7 7 0 0 1 11.6-2L20 12M4 12l2.8 5a7 7 0 0 0 11.6-2" /></svg>;
    case "preview":
      return <svg {...commonProps}><path d="M2.5 12s3.4-6 9.5-6 9.5 6 9.5 6-3.4 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.5" /></svg>;
    case "markdown":
      return <svg {...commonProps}><path d="M6 2.75h8l4 4v5.5M14 2.75v4h4M6 2.75v18.5h6" /><path d="M15 14v7m-3-3 3 3 3-3" /><path d="M8.5 11h5M8.5 14h3" /></svg>;
    case "chunks":
      return <svg {...commonProps}><path d="M9 6h11M9 12h11M9 18h11" /><circle cx="4.5" cy="6" r="1" /><circle cx="4.5" cy="12" r="1" /><circle cx="4.5" cy="18" r="1" /></svg>;
    case "archive":
      return <svg {...commonProps}><path d="M4 7.5h16v13H4zM3 3.5h18v4H3z" /><path d="M12 9v8m-2-2 2 2 2-2M10 11h4" /></svg>;
    case "delete":
      return <svg {...commonProps}><path d="M4 7h16M10 11v6m4-6v6M5.5 7l1 14h11l1-14M9 7V4h6v3" /></svg>;
  }
}

type FileTableProps = {
  files: FileItem[];
  busy: boolean;
  onReconvert: (id: number) => void;
  onDownload: (path: string, name: string) => void;
  onPreview: (id: number, name: string) => void;
  onChunks: (id: number) => void;
  onDelete: (id: number) => void;
};

// Bu component sadece dosya listesini ve butonları render eder.
// API çağrıları parent'tan gelen callback'ler üzerinden çalıştırılır.
export function FileTable({ files, busy, onReconvert, onDownload, onPreview, onChunks, onDelete }: FileTableProps) {
  const [expandedErrorFileId, setExpandedErrorFileId] = useState<number | null>(null);

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
            <th className="w-1/2 p-4">Dosya</th>
            <th className="w-1/6 p-4">Durum</th>
            <th className="p-4">İşlemler</th>
          </tr>
        </thead>
        <tbody>
          {files.map((file) => <tr key={file.id} className="border-b">
            <td className="max-w-0 break-words p-4 align-top">
              {file.originalFileName}
              {file.fileType && (
                <span className="ml-2 rounded bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-800 dark:bg-sky-900/40 dark:text-sky-200">
                  {file.fileType}
                </span>
              )}
            </td>
            <td className="break-words p-4 align-top">
              {file.errorMessage?.trim() ? (
                <>
                  <button
                    type="button"
                    className={`inline-flex cursor-pointer rounded-full px-2.5 py-1 text-xs font-semibold focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 ${statusClass(file.status)}`}
                    aria-expanded={expandedErrorFileId === file.id}
                    aria-controls={`file-error-${file.id}`}
                    aria-label={`${statusLabel(file.status)}. Hata nedenini ${expandedErrorFileId === file.id ? "gizle" : "göster"}`}
                    onClick={() => setExpandedErrorFileId(
                      expandedErrorFileId === file.id ? null : file.id,
                    )}
                  >
                    {statusLabel(file.status)}
                  </button>
                  <p
                    id={`file-error-${file.id}`}
                    hidden={expandedErrorFileId !== file.id}
                    role="note"
                    className="mt-2 whitespace-pre-wrap break-words text-xs text-red-700 dark:text-red-300"
                  >
                    <span className="font-semibold">Hata nedeni: </span>
                    {file.errorMessage.trim()}
                  </p>
                </>
              ) : (
                <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass(file.status)}`}>
                  {statusLabel(file.status)}
                </span>
              )}
            </td>
            <td className="p-4 align-top">
              <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
                <button
                  type="button"
                  className="button-secondary inline-flex h-10 w-full items-center justify-center p-0"
                  title="Tekrar dönüştür"
                  aria-label={`${file.originalFileName}: tekrar dönüştür`}
                  disabled={busy || (file.status !== "COMPLETED" && file.status !== "FAILED")}
                  onClick={() => onReconvert(file.id)}
                >
                  <ActionIcon name="reconvert" />
                </button>
                <button
                  type="button"
                  className="button-secondary inline-flex h-10 w-full items-center justify-center p-0"
                  title="Önizle"
                  aria-label={`${file.originalFileName}: önizle`}
                  disabled={busy || file.status !== "COMPLETED"}
                  onClick={() => onPreview(file.id, file.originalFileName)}
                >
                  <ActionIcon name="preview" />
                </button>
                <button
                  type="button"
                  className="button-secondary inline-flex h-10 w-full items-center justify-center p-0"
                  title="Markdown indir"
                  aria-label={`${file.originalFileName}: Markdown indir`}
                  onClick={() => onDownload(`/file/${file.id}/download-md`, `${file.originalFileName}.md`)}
                >
                  <ActionIcon name="markdown" />
                </button>
                <button
                  type="button"
                  className="button-secondary inline-flex h-10 w-full items-center justify-center p-0"
                  title="Chunkları görüntüle"
                  aria-label={`${file.originalFileName}: chunkları görüntüle`}
                  onClick={() => onChunks(file.id)}
                >
                  <ActionIcon name="chunks" />
                </button>
                <button
                  type="button"
                  className="button-secondary inline-flex h-10 w-full items-center justify-center p-0"
                  title="Tüm chunkları ZIP olarak indir"
                  aria-label={`${file.originalFileName}: tüm chunkları ZIP olarak indir`}
                  onClick={() => onDownload(`/file/chunks/${file.id}/download-all`, `${file.originalFileName}-chunks.zip`)}
                >
                  <ActionIcon name="archive" />
                </button>
                <button
                  type="button"
                  className="button-secondary inline-flex h-10 w-full items-center justify-center p-0 text-red-600"
                  title="Sil"
                  aria-label={`${file.originalFileName}: sil`}
                  disabled={busy}
                  onClick={() => onDelete(file.id)}
                >
                  <ActionIcon name="delete" />
                </button>
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
