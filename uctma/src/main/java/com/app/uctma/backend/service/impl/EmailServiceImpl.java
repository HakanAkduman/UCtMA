package com.app.uctma.backend.service.impl;

import com.app.uctma.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {


    private final JavaMailSender mailSender;

    @Override
    public void sendTempPasswordEmail(String targetMail, String password) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(targetMail);
        message.setSubject("UCtMA Sistemine Hoş Geldiniz - Geçici Şifreniz");
        message.setText("Merhaba,\n\n" +
                "UCtMA platformuna hesabınız başarıyla tanımlanmıştır.\n" +
                "Kullnıcı adınız: "+targetMail+" \n" +
                "Geçici Şifreniz: " + password + "\n\n" +
                "Lütfen ilk girişinizde şifrenizi değiştirmeyi unutmayın.");

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetTemporaryPassword(String targetMail, String password, int validForMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(targetMail);
        message.setSubject("UCTMA parola sıfırlama");
        message.setText("Merhaba,\n\n" +
                "Parola sıfırlama talebiniz için geçici parolanız:\n\n" +
                password + "\n\n" +
                "Bu parola " + validForMinutes + " dakika içinde kullanılmalıdır. " +
                "Bu parolayla giriş yaptığınızda yalnızca yeni parola belirleme ekranına erişebilirsiniz; " +
                "geçici parola girişte tek kullanımlıktır.\n\n" +
                "Bu isteği siz yapmadıysanız bu e-postayı yok sayabilirsiniz.");
        mailSender.send(message);
    }

    @Override
    public void sendEmailVerificationCode(String targetMail, String code, int validForMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(targetMail);
        message.setSubject("UCTMA e-posta doğrulama kodu");
        message.setText("Merhaba,\n\n" +
                "Hesabınızı doğrulamak için aşağıdaki kodu kullanın:\n\n" +
                code + "\n\n" +
                "Bu kod " + validForMinutes + " dakika boyunca geçerlidir. " +
                "Bu isteği siz yapmadıysanız bu e-postayı yok sayabilirsiniz.");
        mailSender.send(message);
    }
}
