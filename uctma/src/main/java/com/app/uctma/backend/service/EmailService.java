package com.app.uctma.backend.service;

public interface EmailService {
    void sendTempPasswordEmail(String targetMail, String password);
}
