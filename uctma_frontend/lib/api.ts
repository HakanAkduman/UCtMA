import { ApiResponse } from "./types";

function authorizationHeaders(token: string): Headers {
  // Dosya indirme gibi JSON olmayan response'lar için Authorization header'ı.
  const headers = new Headers();
  headers.set("Authorization", ["Bearer", token].join(" "));
  return headers;
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
  token: string | null = null,
): Promise<T> {
  // Request header'ları burada birleştirilir; token varsa tüm korumalı
  // backend endpoint'lerine Bearer token eklenir.
  const headers = new Headers(options.headers);
  if (token) {
    headers.set("Authorization", ["Bearer", token].join(" "));
  }

  const response = await fetch(`/api${path}`, { ...options, headers });
  // Backend standart ApiResponse yapısı döndürür. HTTP başarılı olsa bile
  // success=false ise bunu hata olarak frontend'e taşırız.
  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !body.success) {
    throw new Error(body.error?.errorMessage || "İstek başarısız oldu.");
  }

  return body.data as T;
}

export async function downloadApiFile(
  path: string,
  filename: string,
  token: string,
): Promise<void> {
  // Download endpoint'leri JSON değil binary/blob döndürdüğü için api<T>
  // helper'ı yerine ayrı bir istek kullanılır.
  const response = await fetch(`/api${path}`, {
    headers: authorizationHeaders(token),
  });

  if (!response.ok) {
    throw new Error("Dosya indirilemedi.");
  }

  const url = URL.createObjectURL(await response.blob());
  // Geçici link ile tarayıcının dosya indirmesini başlatır, sonra URL'yi temizler.
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
