import { useCallback, useState } from "react";
import { api, downloadApiFile } from "../lib/api";
import { ChunkItem, FileItem, UserItem } from "../lib/types";

export function useFiles(token: string | null) {
  // Backend'den gelen dosyalar ana tablo için tutulur.
  const [files, setFiles] = useState<FileItem[]>([]);
  // Chunk'lar file ID ile cache'lenir. Aynı dialog tekrar açılırsa gereksiz
  // bir API isteği yapılmaz.
  const [chunks, setChunks] = useState<Record<number, ChunkItem[]>>({});
  // Admin kullanıcı dialog'unda gösterilecek kullanıcılar.
  const [users, setUsers] = useState<UserItem[]>([]);

  // Dosya listesini backend'den alır. currentToken parametresi, token state'i
  // henüz güncellenmemişken çağrı yapılması gereken durumlar için desteklenir.
  const loadFiles = useCallback(async (currentToken = token) => {
    if (currentToken) 
      setFiles(await api<FileItem[]>(
          "/file", 
          {}, 
          currentToken
        )
      );
  }, [token]);

  // Dosyayı multipart/form-data olarak yükler. Backend dosyayı kuyruğa
  // gönderir; işlem tamamlanınca güncel liste tekrar alınır.
  async function upload(file: File, conversionType: string, maxTokens: string) {
    if (!token) return;
    const form = new FormData();
    form.append("file", file);
    form.append("conversionType", conversionType);
    form.append("maxTokens", maxTokens);
    await api("/file/upload", { method: "POST", body: form }, token);
    await loadFiles();
  }

  async function uploadUrl(url: string, conversionType: string, maxTokens: string) {
    if (!token) return;
    await api("/file/url", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url, conversionType, maxTokens: Number(maxTokens) }),
    }, token);
    await loadFiles();
  }

  // Mevcut dosyanın dönüşümünü yeniden başlatır. Eski çıktı/chunk temizleme
  // işlemi backend'e aittir; frontend yalnızca isteği gönderir.
  async function reconvert(fileId: number, conversionType: string, maxTokens: string) {
    if (!token) return;
    await api(`/file/${fileId}/convert?conversionType=${conversionType}&maxTokens=${maxTokens}`, { method: "POST" }, token);
    await loadFiles();
  }

  // Dosyayı backend'den siler ve başarılı cevap sonrası frontend cache'inden
  // dosyayı ve ona ait chunk listesini kaldırır.
  async function removeFile(fileId: number) {
    if (!token) return;
    await api(`/file/${fileId}`, { method: "DELETE" }, token);
    setFiles((current) => current.filter((file) => file.id !== fileId));
    setChunks((current) => {
      const next = { ...current };
      delete next[fileId];
      return next;
    });
  }

  // Chunk listesi yalnızca ilk açılışta yüklenir ve file ID altında saklanır.
  async function loadChunks(fileId: number) {
    if (!token) return;
    if (!chunks[fileId]) {
      const result = await api<ChunkItem[]>(`/file/${fileId}/chunks`, {}, token);
      setChunks((current) => ({ ...current, [fileId]: result }));
    }
  }

  // Chunk silme başarılı olursa yalnızca ilgili dosyanın cache'i güncellenir.
  async function removeChunk(fileId: number, chunkId: number) {
    if (!token) return;
    await api(`/file/chunks/${chunkId}`, { method: "DELETE" }, token);
    setChunks((current) => ({ ...current, [fileId]: current[fileId].filter((chunk) => chunk.id !== chunkId) }));
  }

  // Admin kullanıcı listesi endpoint'ini çağırır.
  async function loadUsers() {
    if (token) setUsers(await api<UserItem[]>("/auth/list-users", {}, token));
  }

  // Kullanıcı silme isteğinden sonra kullanıcıyı dialog listesinden de kaldırır.
  async function removeUser(email: string) {
    if (!token) return;
    await api(`/auth/delete?email=${encodeURIComponent(email)}`, { method: "DELETE" }, token);
    setUsers((current) => current.filter((user) => user.email !== email));
  }

  // Admin tarafından yeni kullanıcı oluşturur. Geçici şifre backend tarafından
  // oluşturulup e-posta ile gönderilir.
  async function registerUser(email: string, role: string) {
    if (!token) return;
    await api("/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, role }),
    }, token);
  }

  // Blob response'unu tarayıcıda indirilebilir dosyaya dönüştürür.
  async function download(path: string, name: string) {
    if (token) await downloadApiFile(path, name, token);
  }

  return { files, chunks, users, loadFiles, upload, uploadUrl, reconvert, removeFile, loadChunks, removeChunk, loadUsers, removeUser, registerUser, download };
}
