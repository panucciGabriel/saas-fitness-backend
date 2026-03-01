package com.meuprojeto.saas.feature.password;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", schema = "public")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String userType;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, String email, String userType, LocalDateTime expiryDate) {
        this.token = token;
        this.email = email;
        this.userType = userType;
        this.expiryDate = expiryDate;
    }

    private Long getId() {return id;}
    public String getToken() {return token;}
    public String getEmail() {return email;}
    public String getUserType() {return userType;}
    public LocalDateTime getExpiryDate() {return expiryDate;}

}
