package com.app.uctma.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Entity
@Data
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

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

    @NotNull(message = "Role can not be empty")
    @Enumerated(EnumType.STRING)
    private Role role;

    @NotNull
    @Column
    private boolean newPasswordFlag = true ;
}
