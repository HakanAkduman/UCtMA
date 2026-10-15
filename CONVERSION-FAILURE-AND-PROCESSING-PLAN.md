# Dönüşüm Hataları ve İşleniyor Durumu

## Amaç

Dönüşüm başarısız olduğunda kullanıcıya hata nedenini göstermek ve dosya
işlenirken `PENDING` yerine `PROCESSING` durumunu göstermek.

## Hata mesajı çözüm yolu

1. `FileMetadata` modeline nullable bir `errorMessage` alanı ekle.
2. Flyway kullandığımız ve Hibernate şemayı doğruladığı için yeni bir migration
   ile `files` tablosuna nullable `error_message` sütunu ekle.
3. Converter'ın başarısız callback'inde gönderdiği `errorMessage` değerini
   backend callback handler'ında metadata'ya kaydet. Boş mesajı `null` olarak
   ele al; başarılı callback'te ve yeniden dönüştürme başlatılırken önceki
   hata mesajını temizle.
4. `FileUploadResDTO` alanını ve dosya listesi eşlemesini güncelleyerek hata
   mesajını `GET /file` cevabına dahil et.
5. Frontend `FileItem` tipine alanı ekle ve dosya tablosunda sadece mesaj dolu
   olduğunda, güvenli metin olarak göster.
6. Başarısız callback'in mesajı sakladığını; başarılı callback ve yeniden
   dönüştürmenin eski mesajı temizlediğini, ayrıca arayüzün boş mesaj
   göstermediğini test et.

Converter şu anda Python exception'ının `str(e)` değerini gönderiyor. Bu metin
kullanıcıya gösterilmeden önce dosya yolu veya dahili teknik ayrıntı içerip
içermediği değerlendirilmeli; gerekirse kullanıcıya uygun, güvenli bir hata
mesajına dönüştürülmeli.

## `PROCESSING` durumunun uygulanışı

Backend'de `FileStatus.PROCESSING` zaten mevcut ve callback handler gelen status
değerini metadata'ya yazıyor. Bu nedenle converter consumer mesajı aldığında,
dosya/URL işleme başlamadan önce `PROCESSING` callback'i gönderebilir; işlem
sonunda mevcut akış `COMPLETED` veya `FAILED` callback'ini gönderir. Frontend
dosya listesini periyodik yenilediğinden bu ara durum da görünür.

İlk callback başarısız olursa dönüşüm başlatılmaz: geçici callback hatalarında
RabbitMQ mesajı yeniden kuyruğa alınır, kalıcı 4xx hatalarında mevcut callback
politikasına uygun olarak mesaj onaylanır. Böylece backend'e `PROCESSING`
durumunu bildiremeden işleme başlanmaz.

Bu ilk durum callback'i consumer'a eklendi. Hata mesajını metadata'ya kaydetme
ve arayüzde gösterme adımları bu dokümanda plan olarak duruyor; henüz
uygulanmadı.
