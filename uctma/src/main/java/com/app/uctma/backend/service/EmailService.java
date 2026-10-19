package com.app.uctma.backend.service;

public interface EmailService {
    void sendTempPasswordEmail(String targetMail, String password);

    void sendPasswordResetTemporaryPassword(String targetMail, String password, int validForMinutes);

    void sendEmailVerificationCode(String targetMail, String code, int validForMinutes);
}
