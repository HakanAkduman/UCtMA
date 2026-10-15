import { FormEvent, useState } from "react";

type FileUploaderProps = {
  busy: boolean;
  conversionType: string;
  maxTokens: string;
  onConversionTypeChange: (value: string) => void;
  onMaxTokensChange: (value: string) => void;
  onSubmit: (file: File, conversionType: string, maxTokens: string) => void;
};

export const SUPPORTED_FILE_EXTENSIONS = [
  ".csv", ".txt",  ".md",
  ".wav",  ".mp3",  ".m4a",  ".aac",  ".ogg",  ".flac",  ".mp4",  ".avi",  ".mov",  ".mkv",  ".webm",
  ".pdf",  ".docx",  ".pptx",  ".html",  ".xlsx",  
  ".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp", ".webp",
];

// Dosya seçimi component içinde tutulur. Conversion ayarları ise parent'tan
// gelir; bu sayede page.tsx aynı ayarları tekrar dönüştürmede kullanabilir.
export function FileUploader({
  busy,
  conversionType,
  maxTokens,
  onConversionTypeChange,
  onMaxTokensChange,
  onSubmit,
}: FileUploaderProps) {
  const [file, setFile] = useState<File | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    if (file) onSubmit(file, conversionType, maxTokens);
  }

  return (
    <form onSubmit={submit} className="space-y-5 rounded-xl bg-white p-6 shadow">
      <div>
        <h2 className="font-semibold">Yeni dosya</h2>
        <p className="mt-1 text-sm text-slate-500">Dönüştürmek istediğiniz dosyayı seçin.</p>
      </div>
      <label htmlFor="fileInput" className="block w-full cursor-pointer rounded-lg border-2 border-dashed border-slate-300 p-6 text-center text-sm font-medium text-slate-600 transition hover:border-slate-500 hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:border-slate-400 dark:hover:bg-slate-800">
        <span className="block text-2xl" aria-hidden="true">↑</span>
        <span className="mt-2 block truncate">{file ? file.name : "Dosya seçmek için tıklayın"}</span>
        <span className="mt-1 block text-xs font-normal text-slate-500">PDF, DOCX, PPTX, HTML, CSV, XLSX, TXT veya MD</span>
      </label>
      <input
        type="file"
        id="fileInput"
        accept={SUPPORTED_FILE_EXTENSIONS.join(",")}
        onChange={(e) => setFile(e.target.files?.[0] || null)}
        required
        className="hidden"
      />
      <div className="grid gap-3 sm:grid-cols-[1fr_8rem_auto] sm:items-end">
        <label className="space-y-1 text-sm font-medium">
          Dönüşüm türü
          <select className="input w-full" value={conversionType} onChange={(e) => onConversionTypeChange(e.target.value)}>
            <option value="CONVERT">Sadece Markdown</option>
            <option value="CONVERT_AND_CHUNK">Markdown + Chunk</option>
          </select>
        </label>
        { conversionType === "CONVERT_AND_CHUNK" &&(
          <label className="space-y-1 text-sm font-medium">
          Max token
          <input className="input w-full" type="number" min="1" value={maxTokens} onChange={(e) => onMaxTokensChange(e.target.value)} />
        </label>
        )}
        

       
        <button className="button w-full sm:w-auto" disabled={busy}>{busy ? "Yükleniyor..." : "Yükle"}</button>
      </div>
    </form>
  );
}
