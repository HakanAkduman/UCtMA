package com.app.uctma.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Data
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "files")
@EqualsAndHashCode(exclude = "files")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Email
    @NotEmpty(message = "Email can not be empty")
    @Column(unique = true)
    private String email;

    @NotEmpty(message = "Password can not be empty")
    @Length(min = 3,message = "password can not be less than 3 character")
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileMetadata> files;

    @NotNull(message = "Role can not be empty")
    @Enumerated(EnumType.STRING)
    private Role role;

    @NotNull
    @Column
    private boolean newPasswordFlag = true ;

    @Column(name = "password_changed_at", nullable = false)
    private LocalDateTime passwordChangedAt;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "password_reset_hash")
    private String passwordResetHash;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "password_reset_requested_at")
    private LocalDateTime passwordResetRequestedAt;

    @Column(name = "password_reset_session_id")
    private String passwordResetSessionId;

    @PrePersist
    void initializePasswordChangedAt() {
        if (passwordChangedAt == null) {
            passwordChangedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
