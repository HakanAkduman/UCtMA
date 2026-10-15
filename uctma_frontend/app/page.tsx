"use client";


import { FormEvent, useEffect, useState } from "react";
import { ChunksDialog } from "../components/ChunksDialog";
import { FileTable } from "../components/FileTable";
import { FileUploader, SUPPORTED_FILE_EXTENSIONS } from "../components/FileUploader";
import { LoginScreen } from "../components/LoginScreen";
import { Navbar } from "../components/Navbar";
import { PasswordForm } from "../components/PasswordForm";
import { RegisterForm } from "../components/RegisterForm";
import { UsersDialog } from "../components/UsersDialog";
import { UrlUploader } from "../components/UrlUploader";
import { useAuth } from "../hooks/useAuth";
import { useFiles } from "../hooks/useFiles";
import { ChunkItem } from "../lib/types";

declare global {
  namespace JSX {
    interface IntrinsicElements {
      [elementName: string]: any;
    }
  }
}

type Dialog = "register" | "password" | "chunks" | "users" | "url" | null;
type MessageType = "success" | "error";

export default function Home() {
  // Aynı anda yalnızca bir dialog açık olur. İçeriğin kendisi aşağıdaki ayrı
  // component'lerde bulunur; page.tsx sadece hangi dialog'un açılacağını belirler.
  const [dialog, setDialog] = useState<Dialog>(null);
  // Chunk dialog'unda hangi dosyanın chunk'larını göstereceğimizi tutar.
  const [selectedFileId, setSelectedFileId] = useState<number | null>(null);
  // Kullanıcıya gösterilecek başarılı işlem veya hata mesajı.
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState<MessageType>("success");
  // Upload, silme, indirme ve dialog içi işlemler sırasında butonları kilitler.
  const [busy, setBusy] = useState(false);
  // Kullanıcı oluşturma dialog'undaki form alanları.
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerRole, setRegisterRole] = useState("ROLE_USER");
  // Kullanıcının seçtiği dönüşüm ayarları hem yeni yüklemede hem de
  // "Tekrar dönüştür" işleminde aynı şekilde kullanılır.
  const [conversionType, setConversionType] = useState("CONVERT");
  const [maxTokens, setMaxTokens] = useState("400");

  // useAuth login, logout, rol ve şifre değiştirme akışını yönetir.
  const auth = useAuth();
  // useFiles backend'deki dosya, chunk ve kullanıcı endpoint'lerini çağırır.
  // Token değiştiğinde hook yeni token ile çalışır.
  const authenticatedFiles = useFiles(auth.token);

  // Kullanıcı login olduğunda veya sayfa yenilendiğinde localStorage'daki
  // token geri yüklendiğinde güncel dosya listesi backend'den alınır.
  useEffect(() => {
    if (auth.token) void authenticatedFiles.loadFiles();
  }, [auth.token, authenticatedFiles.loadFiles]);

  // Converter işlemi RabbitMQ üzerinden asenkron tamamlandığı için dosya
  // durumunu belirli aralıklarla yenileriz. Böylece PENDING durumu,
  // backend callback'i geldikten sonra COMPLETED veya FAILED olarak görünür.
  useEffect(() => {
    if (!auth.token) return;

    const refreshInterval = window.setInterval(() => {
      void authenticatedFiles.loadFiles();
    }, 5000);

    return () => window.clearInterval(refreshInterval);
  }, [auth.token, authenticatedFiles.loadFiles]);

  // Tüm async işlemler bu yardımcıdan geçirilir. Böylece her işlemde
  // loading state'i, hata mesajı ve başarılı işlem mesajı aynı şekilde yönetilir.
  async function run(action: () => Promise<void>, successMessage?: string) {
    setBusy(true);
    try {
      await action();
      if (successMessage) {
        setMessage(successMessage);
        setMessageType("success");
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "İşlem başarısız.");
      setMessageType("error");
    } finally {
      setBusy(false);
    }
  }

  // LoginScreen form submit olayını auth hook'una iletir.
  // Backend yeni şifre zorunluluğu dönerse şifre dialog'u açılır.
  async function login(event: FormEvent) {
    await run(async () => {
      const mustChange = await auth.login(event);
      if (mustChange) setDialog("password");
    });
  }

  // Geçerli token yoksa kullanıcı dashboard'a erişemez ve yalnızca giriş
  // ekranını görür. Token oluştuğunda aşağıdaki dashboard render edilir.
  if (!auth.token) {
    return <LoginScreen email={auth.email} password={auth.password} busy={busy} message={message} onEmailChange={auth.setEmail} onPasswordChange={auth.setPassword} onSubmit={(event) => void login(event)} />;
  }

  async function changePassword(event: FormEvent) {
    // Formun browser tarafından sayfayı yenilemesini engelleriz.
    event.preventDefault();
    await run(async () => {
      await auth.changePassword();
      setDialog(null);
      // Şifre değişimi tamamlandıktan sonra dosya listesini tazeleriz.
      await authenticatedFiles.loadFiles();
    }, "Şifreniz değiştirildi.");
  }

  // FileUploader'dan gelen dosya ve dönüşüm ayarlarını backend'e gönderir.
  async function upload(file: File, conversionType: string, maxTokens: string) {
    await run(async () => {
      // Backend'e göndermeden önce uzantıyı kontrol ederiz. Böylece desteklenmeyen
      // dosya kuyruğa girmez ve kullanıcı PENDING durumunda takılı kalmaz.
      const extensionStart = file.name.lastIndexOf(".");
      const extension = extensionStart >= 0
        ? file.name.substring(extensionStart).toLowerCase()
        : "";
      if (!SUPPORTED_FILE_EXTENSIONS.includes(extension)) {
        throw new Error(
          `Desteklenmeyen dosya formatı: ${extension || "uzantısız"}. Desteklenen formatlar: ${SUPPORTED_FILE_EXTENSIONS.join(", ")}`,
        );
      }

      await authenticatedFiles.upload(file, conversionType, maxTokens);
    }, "Dosyanız işleme alındı.");
  }

  async function uploadUrl(url: string, type: string, tokens: string) {
    await run(async () => {
      await authenticatedFiles.uploadUrl(url, type, tokens);
      setDialog(null);
    }, "URL işleme alındı.");
  }

  // Dosyanın chunk'larını yükler ve ardından chunk dialog'unu açar.
  async function openChunks(fileId: number) {
    setSelectedFileId(fileId);
    setDialog("chunks");
    await run(() => authenticatedFiles.loadChunks(fileId));
  }

  // RegisterForm submit olayını backend'deki kullanıcı oluşturma endpoint'ine gönderir.
  async function registerUser(event: FormEvent) {
    // Kullanıcı kayıt formunun normal browser submit davranışını engelleriz.
    event.preventDefault();
    await run(() => authenticatedFiles.registerUser(registerEmail, registerRole), "Kullanıcı oluşturuldu. Geçici şifre e-posta ile gönderildi.");
    setRegisterEmail("");
    setDialog(null);
  }

  // Seçili dosyaya ait chunk'lar cache'den alınır. Henüz yüklenmediyse
  // undefined olabilir; ChunksDialog bu durumu loading olarak gösterebilir.
  const selectedChunks: ChunkItem[] | undefined = selectedFileId === null ? undefined : authenticatedFiles.chunks[selectedFileId];

  return (
    <main className="mx-auto min-h-screen max-w-6xl space-y-6 px-6 py-10">
      {/* Navbar: şifre değiştirme ve logout herkese açıktır.
          Kullanıcı oluşturma ve kullanıcı listesi butonları isAdmin değerine
          göre Navbar component'i tarafından gösterilir veya gizlenir. */}
      <Navbar isAdmin={auth.isAdmin} onPassword={() => setDialog("password")} onRegister={() => setDialog("register")} onUsers={() => void run(async () => { await authenticatedFiles.loadUsers(); setDialog("users"); })} onUrlUpload={() => setDialog("url")} onLogout={auth.logout} />
      {/* FileUploader yalnızca form görünümünü yönetir. Form gönderildiğinde
          dosya ve seçilen ayarlar upload fonksiyonuna callback ile gelir. */}
      <FileUploader
        busy={busy}
        conversionType={conversionType}
        maxTokens={maxTokens}
        onConversionTypeChange={setConversionType}
        onMaxTokensChange={setMaxTokens}
        onSubmit={(file, type, tokens) => void upload(file, type, tokens)}
      />
      {message && (
        <div
          role={messageType === "error" ? "alert" : "status"}
          className={`flex items-start justify-between gap-4 rounded-lg border p-3 text-sm ${
            messageType === "error"
              ? "border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200"
              : "border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200"
          }`}
        >
          <span>{message}</span>
          <button
            type="button"
            className="shrink-0 font-semibold opacity-70 hover:opacity-100"
            aria-label="Mesajı kapat"
            onClick={() => setMessage("")}
          >
            ×
          </button>
        </div>
      )}
      {/* FileTable dosyaları ve butonları gösterir. API çağrıları burada doğrudan
          yapılmaz; ilgili işlemler callback olarak FileTable'a aktarılır. */}
      <FileTable files={authenticatedFiles.files} busy={busy} onReconvert={(id) => void run(() => authenticatedFiles.reconvert(id, conversionType, maxTokens), "Dosya tekrar kuyruğa gönderildi.")} onDownload={(path, name) => void run(() => authenticatedFiles.download(path, name))} onChunks={(id) => void openChunks(id)} onDelete={(id) => void run(async () => { if (window.confirm("Bu dosya ve tüm çıktıları silinsin mi?")) await authenticatedFiles.removeFile(id); })} />
      {/* Dialog component'leri yalnızca ilgili state seçildiğinde oluşturulur.
          Böylece kullanıcı listesi, şifre formu ve chunk listesi ana sayfa
          akışını büyütmeden ayrı dosyalarda kalır. */}
      {dialog === "register" && <RegisterForm email={registerEmail} role={registerRole} busy={busy} onEmailChange={setRegisterEmail} onRoleChange={setRegisterRole} onSubmit={registerUser} onClose={() => setDialog(null)} />}
      {dialog === "password" && <PasswordForm required={auth.mustChangePassword} currentPassword={auth.password} newPassword={auth.newPassword} confirmPassword={auth.confirmPassword} busy={busy} onCurrentPasswordChange={auth.setPassword} onNewPasswordChange={auth.setNewPassword} onConfirmPasswordChange={auth.setConfirmPassword} onSubmit={changePassword} onClose={() => setDialog(null)} />}
      {dialog === "users" && <UsersDialog users={authenticatedFiles.users} busy={busy} onDelete={(email) => void run(() => authenticatedFiles.removeUser(email))} onClose={() => setDialog(null)} />}
      {dialog === "chunks" && selectedFileId !== null && <ChunksDialog chunks={selectedChunks} busy={busy} onDownload={(chunk) => void run(() => authenticatedFiles.download(`/file/chunks/${chunk.id}/download`, chunk.chunkName))} onDelete={(id) => void run(() => authenticatedFiles.removeChunk(selectedFileId, id))} onClose={() => setDialog(null)} />}
      {dialog === "url" && <UrlUploader busy={busy} conversionType={conversionType} maxTokens={maxTokens} onConversionTypeChange={setConversionType} onMaxTokensChange={setMaxTokens} onSubmit={(url, type, tokens) => void uploadUrl(url, type, tokens)} onClose={() => setDialog(null)} />}
    </main>
  );
}
