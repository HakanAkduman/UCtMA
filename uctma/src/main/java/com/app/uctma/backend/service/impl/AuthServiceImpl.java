package com.app.uctma.backend.service.impl;

import com.app.uctma.backend.dto.*;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.UserRepository;
import com.app.uctma.backend.service.AuthService;
import com.app.uctma.backend.service.EmailService;
import com.app.uctma.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Override
    public String register(RegisterReqDTO registerReqDTO) {

        if (userRepository.findByEmail(registerReqDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Bu email adresi zaten kullanımda!");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .email(registerReqDTO.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(registerReqDTO.getRole())
                .newPasswordFlag(true)
                .build();

        userRepository.save(user);

        emailService.sendTempPasswordEmail(user.getEmail(), tempPassword);

        return "Kullanıcı başarıyla oluşturuldu. Geçici şifre e-posta adresine gönderildi.";
    }

    @Override
    public String delete(String email) {
        // İstek atan aktif kullanıcının email'ini al
        String currentUserEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        if (currentUserEmail.equalsIgnoreCase(email)) {
            throw new RuntimeException("Kendi hesabınızı silemezsiniz!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + email));

        userRepository.delete(user);
        return "Kullanıcı başarıyla silindi!";
    }

    @Override
    public List<UserListResDTO> listOfUsers() {
        return userRepository.findAll().stream().map(
                user -> {
                    return new UserListResDTO(user.getEmail(),user.getRole());
                }
        ).toList();
    }

    @Override
    public LoginResDTO login(LoginReqDTO loginReqDTO) {

        Optional<User> optionalUser= userRepository.findByEmail(loginReqDTO.getEmail());
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("Bu email adresine kayıtlı kullanıcı bulunamadı!");
        }
        User user= optionalUser.get();
        if(!passwordEncoder.matches(loginReqDTO.getPassword(),user.getPassword())){
            throw new RuntimeException("Şifreniz hatalı!");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());


        return new LoginResDTO(
                user.getEmail(),
                token,
                "Bearer",
                user.isNewPasswordFlag()
        );
    }

    @Override
    public String changePassword(ChangePasswordReqDTO changePasswordReqDTO) {
        if (!changePasswordReqDTO.getNewPassword().equals(changePasswordReqDTO.getConfirmNewPassword())) {
            throw new RuntimeException("Yeni şifreler birbiriyle uyuşmuyor!");
        }


        String currentUserEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        if (!passwordEncoder.matches(changePasswordReqDTO.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mevcut şifreniz hatalı!");
        }

        user.setPassword(passwordEncoder.encode(changePasswordReqDTO.getNewPassword()));
        user.setNewPasswordFlag(false);

        userRepository.save(user);
        return currentUserEmail;
    }


}
