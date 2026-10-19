"use client";


import { FormEvent, useEffect, useState } from "react";
import { ChunksDialog } from "../components/ChunksDialog";
import { FileTable } from "../components/FileTable";
import { FileUploader, SUPPORTED_FILE_EXTENSIONS } from "../components/FileUploader";
import { LoginScreen } from "../components/LoginScreen";
import { Navbar } from "../components/Navbar";
import { PasswordForm } from "../components/PasswordForm";
import { ApplicationSettingsDialog } from "../components/ApplicationSettingsDialog";
import { PreviewDialog } from "../components/PreviewDialog";
import { RegistrationForm } from "../components/RegistrationForm";
import { InviteForm } from "../components/InviteForm";
import { UsersDialog } from "../components/UsersDialog";
import { UrlUploader } from "../components/UrlUploader";
import { useAuth } from "../hooks/useAuth";
import { useFiles } from "../hooks/useFiles";
import { usePasswordPolicies } from "../hooks/usePasswordPolicies";
import { useAuthAccountSettings } from "../hooks/useAuthAccountSettings";
import { api } from "../lib/api";
import { AuthPublicSettings, ChunkItem } from "../lib/types";

declare global {
  namespace JSX {
    interface IntrinsicElements {
      [elementName: string]: any;
    }
  }
}

type Dialog = "invite" | "password" | "applicationSettings" | "chunks" | "preview" | "users" | "url" | null;
type MessageType = "success" | "error";

export default function Home() {
  // Aynı anda yalnızca bir dialog açık olur. İçeriğin kendisi aşağıdaki ayrı
  // component'lerde bulunur; page.tsx sadece hangi dialog'un açılacağını belirler.
  const [dialog, setDialog] = useState<Dialog>(null);
  const [showRegistration, setShowRegistration] = useState(false);
  const [publicAuthSettings, setPublicAuthSettings] = useState<AuthPublicSettings>({
    registrationEnabled: false,
    emailVerificationRequired: true,
  });
  // Chunk dialog'unda hangi dosyanın chunk'larını göstereceğimizi tutar.
  const [selectedFileId, setSelectedFileId] = useState<number | null>(null);
  const [previewFileName, setPreviewFileName] = useState("");
  const [previewContent, setPreviewContent] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  // Kullanıcıya gösterilecek başarılı işlem veya hata mesajı.
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState<MessageType>("success");
  // Upload, silme, indirme ve dialog içi işlemler sırasında butonları kilitler.
  const [busy, setBusy] = useState(false);
  // Admin davet dialog'undaki form alanları.
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState("ROLE_USER");
  // Kullanıcının seçtiği dönüşüm ayarları hem yeni yüklemede hem de
  // "Tekrar dönüştür" işleminde aynı şekilde kullanılır.
  const [conversionType, setConversionType] = useState("CONVERT");
  const [maxTokens, setMaxTokens] = useState("400");

  // useAuth login, logout, rol ve şifre değiştirme akışını yönetir.
  const auth = useAuth();
  // useFiles backend'deki dosya, chunk ve kullanıcı endpoint'lerini çağırır.
  // Token değiştiğinde hook yeni token ile çalışır.
  const authenticatedFiles = useFiles(auth.token);
  const passwordPolicies = usePasswordPolicies(auth.token);
  const accountSettings = useAuthAccountSettings(auth.token);

  useEffect(() => {
    if (auth.token && auth.mustChangePassword) setDialog("password");
  }, [auth.token, auth.mustChangePassword]);

  useEffect(() => {
    void api<AuthPublicSettings>("/auth/public-settings")
      .then(setPublicAuthSettings)
      .catch((error: unknown) => {
        setMessage(error instanceof Error ? error.message : "Kimlik doğrulama ayarları yüklenemedi.");
        setMessageType("error");
      });
  }, []);

  useEffect(() => {
    if (auth.token && auth.isAdmin && !auth.passwordResetSession) {
      void passwordPolicies.loadPolicies().catch((error: unknown) => {
        setMessage(error instanceof Error ? error.message : "Parola politikaları yüklenemedi.");
        setMessageType("error");
      });
    }
  }, [auth.token, auth.isAdmin, auth.passwordResetSession, passwordPolicies.loadPolicies]);

  // Kullanıcı login olduğunda veya sayfa yenilendiğinde localStorage'daki
  // token geri yüklendiğinde güncel dosya listesi backend'den alınır.
  useEffect(() => {
    if (auth.token && !auth.mustChangePassword) void authenticatedFiles.loadFiles();
  }, [auth.token, auth.mustChangePassword, authenticatedFiles.loadFiles]);

  // Converter işlemi RabbitMQ üzerinden asenkron tamamlandığı için dosya
  // durumunu belirli aralıklarla yenileriz. Böylece PENDING durumu,
  // backend callback'i geldikten sonra COMPLETED veya FAILED olarak görünür.
  useEffect(() => {
    if (!auth.token || auth.mustChangePassword) return;

    const refreshInterval = window.setInterval(() => {
      void authenticatedFiles.loadFiles();
    }, 5000);

    return () => window.clearInterval(refreshInterval);
  }, [auth.token, auth.mustChangePassword, authenticatedFiles.loadFiles]);

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

  async function forgotPassword(event: FormEvent) {
    event.preventDefault();
    await run(async () => {
      await api<string>("/auth/forgot-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: auth.email }),
      });
    }, "E-posta adresi sistemde kayıtlıysa parola sıfırlama bilgileri gönderildi.");
  }

  // Geçerli token yoksa kullanıcı dashboard'a erişemez ve yalnızca giriş
  // ekranını görür. Token oluştuğunda aşağıdaki dashboard render edilir.
  if (!auth.token) {
    if (showRegistration && publicAuthSettings.registrationEnabled) {
      return <RegistrationForm
        message={message}
        messageType={messageType}
        busy={busy}
        onBackToLogin={(email) => {
          auth.setEmail(email);
          setShowRegistration(false);
          setMessage("");
        }}
        onBusy={(action, successMessage) => {
          setMessage("");
          void run(action, successMessage);
        }}
      />;
    }
    return <LoginScreen
      email={auth.email}
      password={auth.password}
      busy={busy}
      message={message}
      messageType={messageType}
      registrationEnabled={publicAuthSettings.registrationEnabled}
      onEmailChange={auth.setEmail}
      onPasswordChange={auth.setPassword}
      onSubmit={(event) => void login(event)}
      onForgotPassword={(event) => void forgotPassword(event)}
      onRegister={() => { setMessage(""); setShowRegistration(true); }}
    />;
  }

  async function changePassword(event: FormEvent) {
    // Formun browser tarafından sayfayı yenilemesini engelleriz.
    event.preventDefault();
    await run(async () => {
      const resetSession = await auth.changePassword();
      setDialog(null);
      if (resetSession) {
        auth.logout();
        return;
      }
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

  async function openPreview(fileId: number, fileName: string) {
    setPreviewFileName(fileName);
    setPreviewContent(null);
    setPreviewError(null);
    setDialog("preview");
    setBusy(true);
    try {
      setPreviewContent(await authenticatedFiles.preview(fileId));
    } catch (error) {
      setPreviewError(error instanceof Error ? error.message : "Önizleme yüklenemedi.");
    } finally {
      setBusy(false);
    }
  }

  // InviteForm submit olayını admin davet endpoint'ine gönderir.
  async function inviteUser(event: FormEvent) {
    event.preventDefault();
    await run(() => authenticatedFiles.inviteUser(inviteEmail, inviteRole), "Davet gönderildi. Geçici şifre e-posta ile gönderildi.");
    setInviteEmail("");
    setDialog(null);
  }

  // Seçili dosyaya ait chunk'lar cache'den alınır. Henüz yüklenmediyse
  // undefined olabilir; ChunksDialog bu durumu loading olarak gösterebilir.
  const selectedChunks: ChunkItem[] | undefined = selectedFileId === null ? undefined : authenticatedFiles.chunks[selectedFileId];

  return (
    <main className="mx-auto min-h-screen max-w-6xl space-y-6 px-6 py-10">
      {/* Navbar: şifre değiştirme ve logout herkese açıktır.
          Kullanıcı davet etme ve kullanıcı listesi butonları isAdmin değerine
          göre Navbar component'i tarafından gösterilir veya gizlenir. */}
      <Navbar
        isAdmin={auth.isAdmin}
        onPassword={() => setDialog("password")}
        onApplicationSettings={() => void run(async () => {
          await Promise.all([passwordPolicies.loadPolicies(), accountSettings.loadSettings()]);
          setDialog("applicationSettings");
        })}
        onInvite={() => setDialog("invite")}
        onUsers={() => void run(async () => { await authenticatedFiles.loadUsers(); setDialog("users"); })}
        onUrlUpload={() => setDialog("url")}
        onLogout={auth.logout}
      />
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
      <FileTable files={authenticatedFiles.files} busy={busy} onReconvert={(id) => void run(() => authenticatedFiles.reconvert(id, conversionType, maxTokens), "Dosya tekrar kuyruğa gönderildi.")} onDownload={(path, name) => void run(() => authenticatedFiles.download(path, name))} onPreview={(id, name) => void openPreview(id, name)} onChunks={(id) => void openChunks(id)} onDelete={(id) => void run(async () => { if (window.confirm("Bu dosya ve tüm çıktıları silinsin mi?")) await authenticatedFiles.removeFile(id); })} />
      {/* Dialog component'leri yalnızca ilgili state seçildiğinde oluşturulur.
          Böylece kullanıcı listesi, şifre formu ve chunk listesi ana sayfa
          akışını büyütmeden ayrı dosyalarda kalır. */}
      {dialog === "invite" && <InviteForm email={inviteEmail} role={inviteRole} busy={busy} onEmailChange={setInviteEmail} onRoleChange={setInviteRole} onSubmit={inviteUser} onClose={() => setDialog(null)} />}
      {dialog === "password" && <PasswordForm required={auth.mustChangePassword} currentPassword={auth.password} newPassword={auth.newPassword} confirmPassword={auth.confirmPassword} busy={busy} policies={passwordPolicies.policies} onCurrentPasswordChange={auth.setPassword} onNewPasswordChange={auth.setNewPassword} onConfirmPasswordChange={auth.setConfirmPassword} onSubmit={changePassword} onClose={() => setDialog(null)} />}
      {dialog === "applicationSettings" && <ApplicationSettingsDialog
        accountSettings={accountSettings.settings}
        passwordPolicies={passwordPolicies.policies}
        busy={busy}
        onAccountSettingChange={(code, change) => accountSettings.setSettings((current) => current.map(
          (setting) => setting.settingCode === code ? { ...setting, ...change } : setting,
        ))}
        onPasswordPolicyChange={(code, change) => passwordPolicies.setPolicies((current) => current.map(
          (policy) => policy.passwordPolicyCode === code ? { ...policy, ...change } : policy,
        ))}
        onSaveAccountSettings={() => void run(async () => {
          const savedSettings = await accountSettings.saveSettings(accountSettings.settings);
          setPublicAuthSettings({
            registrationEnabled: savedSettings.some(
              (setting) => setting.settingCode === "REGISTRATION_ENABLED" && setting.enabled,
            ),
            emailVerificationRequired: savedSettings.some(
              (setting) => setting.settingCode === "EMAIL_VERIFICATION_REQUIRED" && setting.enabled,
            ),
          });
          setDialog(null);
        }, "Hesap ayarları kaydedildi.")}
        onSavePasswordPolicies={() => void run(async () => {
          await passwordPolicies.savePolicies(passwordPolicies.policies);
          setDialog(null);
        }, "Parola politikaları kaydedildi.")}
        onClose={() => setDialog(null)}
      />}
      {dialog === "users" && <UsersDialog
        users={authenticatedFiles.users}
        busy={busy}
        currentUserEmail={auth.email}
        onDelete={(email) => void run(() => authenticatedFiles.removeUser(email))}
        onSetEnabled={(email, enabled) => void run(
          () => authenticatedFiles.setUserEnabled(email, enabled),
          enabled ? "Kullanıcı hesabı etkinleştirildi." : "Kullanıcı hesabı devre dışı bırakıldı.",
        )}
        onClose={() => setDialog(null)}
      />}
      {dialog === "chunks" && selectedFileId !== null && <ChunksDialog chunks={selectedChunks} busy={busy} onDownload={(chunk) => void run(() => authenticatedFiles.download(`/file/chunks/${chunk.id}/download`, chunk.chunkName))} onDelete={(id) => void run(() => authenticatedFiles.removeChunk(selectedFileId, id))} onClose={() => setDialog(null)} />}
      {dialog === "preview" && <PreviewDialog fileName={previewFileName} content={previewContent} loading={busy && previewContent === null && previewError === null} error={previewError} onClose={() => setDialog(null)} />}
      {dialog === "url" && <UrlUploader busy={busy} conversionType={conversionType} maxTokens={maxTokens} onConversionTypeChange={setConversionType} onMaxTokensChange={setMaxTokens} onSubmit={(url, type, tokens) => void uploadUrl(url, type, tokens)} onClose={() => setDialog(null)} />}
    </main>
  );
}
