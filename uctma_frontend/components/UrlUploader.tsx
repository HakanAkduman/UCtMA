import { FormEvent, useState } from "react";
import { Modal } from "./Modal";

type UrlUploaderProps = {
  busy: boolean;
  conversionType: string;
  maxTokens: string;
  onConversionTypeChange: (value: string) => void;
  onMaxTokensChange: (value: string) => void;
  onSubmit: (url: string, conversionType: string, maxTokens: string) => void;
  onClose: () => void;
};

export function UrlUploader({
  busy,
  conversionType,
  maxTokens,
  onConversionTypeChange,
  onMaxTokensChange,
  onSubmit,
  onClose,
}: UrlUploaderProps) {
  const [url, setUrl] = useState("");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit(url, conversionType, maxTokens);
  }

  return (
    <Modal title="URL'den yükle" onClose={busy ? undefined : onClose}>
      <form onSubmit={submit} className="space-y-5">
        <label className="block space-y-1 text-sm font-medium">
          Sayfa URL'si
          <input
            className="input w-full"
            type="url"
            inputMode="url"
            autoFocus
            required
            maxLength={2048}
            pattern="https?://.+"
            placeholder="https://example.com/page"
            value={url}
            onChange={(event) => setUrl(event.target.value)}
          />
          <span className="block text-xs font-normal text-slate-500">
            Herkese açık HTTP/HTTPS sayfaları Markdown'a dönüştürülür.
          </span>
        </label>

        <div className="grid gap-3 sm:grid-cols-[1fr_8rem_auto] sm:items-end">
          <label className="space-y-1 text-sm font-medium">
            Dönüşüm türü
            <select
              className="input w-full"
              value={conversionType}
              onChange={(event) => onConversionTypeChange(event.target.value)}
            >
              <option value="CONVERT">Sadece Markdown</option>
              <option value="CONVERT_AND_CHUNK">Markdown + Chunk</option>
            </select>
          </label>
          {conversionType === "CONVERT_AND_CHUNK" && (
            <label className="space-y-1 text-sm font-medium">
              Max token
              <input
                className="input w-full"
                type="number"
                min="1"
                value={maxTokens}
                onChange={(event) => onMaxTokensChange(event.target.value)}
              />
            </label>
          )}
          <button className="button w-full sm:w-auto" disabled={busy}>
            {busy ? "Yükleniyor..." : "Dönüştür"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
