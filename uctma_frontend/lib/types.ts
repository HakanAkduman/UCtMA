// Backend'in dosya işleme yaşam döngüsündeki durumlar.
export type FileStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

// Ana dosya tablosunda kullanılan backend dosya modeli.
export type FileItem = {
  id: number;
  originalFileName: string;
  fileType: string;
  status: FileStatus;
  errorMessage?: string | null;
  maxTokens: number | null;
  createdAt: string;
};

// Chunk dialog'unda kullanılan minimum chunk bilgisi.
export type ChunkItem = {
  id: number;
  chunkName: string;
};

// Backend enum değerleri ROLE_ prefix'i ile gönderilir.
export type UserItem = {
  email: string;
  role: "ROLE_USER" | "ROLE_ADMIN";
};

// Login response'u frontend'in oturum ve admin görünürlüğünü belirler.
export type LoginResult = {
  token: string;
  newPasswordRequired: boolean;
  role: "ROLE_USER" | "ROLE_ADMIN";
};

// Tüm JSON API çağrılarında kullanılan ortak response zarfı.
export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: { errorMessage?: string };
};
