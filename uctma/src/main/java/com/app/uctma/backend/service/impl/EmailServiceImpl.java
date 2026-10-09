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
                "Geçici Şifreniz: " + password + "\n\n" +
                "Lütfen ilk girişinizde şifrenizi değiştirmeyi unutmayın.");

        mailSender.send(message);
    }
}
