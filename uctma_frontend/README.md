# UCTMA Frontend

Bu klasör, backend'e dokunmadan çalışan basit Next.js + React + Tailwind arayüzüdür.

Faz 1 ve Faz 2 teknik dokümantasyonunun tamamı:

- [`UCTMA-TECHNICAL-DOCUMENTATION.md`](../UCTMA-TECHNICAL-DOCUMENTATION.md)

## Çalıştırma

Backend compose'u önce `uctma_default` network'ünü oluşturacak şekilde çalıştır:

```bash
cd ../uctma
docker compose up -d

cd ../uctma_frontend
docker compose up --build
```

Arayüz: http://localhost:3000

## Şu an neler var?

- JWT ile giriş
- İlk girişte zorunlu yeni şifre belirleme
- Admin için kullanıcı tanımlama
- Kullanıcıları dialog içinde listeleme ve silme
- Üst navbar üzerinden kullanıcı ve şifre dialogları
- Dosya yükleme
- Navbar üzerinden HTTP/HTTPS URL'lerini Markdown'a dönüştürme
- Markdown veya Markdown + Chunk seçimi
- Dosya listesi
- Tekrar dönüştürme
- Markdown indirme
- Chunk listesini dialog içinde görüntüleme
- Her chunk'ı ayrı ayrı indirme
- Chunk silme
- Tüm chunkları ZIP olarak indirme
- Dosya silme
- Açık/koyu tema seçimi ve tercihi tarayıcıda hatırlama

## Devralırken bakılacak yerler

- API istekleri ve token: `lib/api.ts`
- Tipler: `lib/types.ts`
- Modal ve form parçaları: `components/`
- Authentication ve dosya işlemleri: `hooks/`
- Sayfa akışı ve state: `app/page.tsx`
- Backend proxy/CORS çözümü: `next.config.mjs`
- Basit stiller: `app/globals.css`
- Tema state'i: `hooks/useTheme.ts`
- Tailwind sınıf taraması: `tailwind.config.ts` (`app`, `components` ve `lib`)
- Yeni sayfalar için `app/` altında route ekleyebilirsin.

## Klasör yapısı

```text
app/          Next.js route ve sayfa akışı
components/   Tekrar kullanılabilir UI parçaları ve dialoglar
hooks/        Authentication ve dosya işlemlerinin state hook'ları
lib/          API istemcisi ve backend tipleri
```

`app/page.tsx` yalnızca ekran state'ini ve endpoint çağrılarını orkestre eder.
Modal içeriği veya API ayrıntıları için ilgili component/lib dosyasına bakılmalıdır.

Not: Bu ilk sürümde kayıt ekranı, chunk listesi, loading skeleton, pagination ve gelişmiş hata yönetimi özellikle eklenmedi. Bunlar sonraki geliştirme adımları olabilir.

## Henüz frontend'de kullanılmayan endpointler

- `GET /uctma/api/file/{fileId}/download`: Orijinal dosyayı indirmek için dosya satırına
  “Orijinali indir” butonu eklenebilir.
- `POST /uctma/api/file/callback`: Frontend'in çağırmaması gerekir; converter service'in
  backend'e gönderdiği internal callback endpoint'idir.
- `GET /uctma/api/auth/list-users` ve `DELETE /uctma/api/auth/delete` artık
  kullanıcılar dialogunda kullanılmaktadır. Bu endpointler admin yetkisi ister.
