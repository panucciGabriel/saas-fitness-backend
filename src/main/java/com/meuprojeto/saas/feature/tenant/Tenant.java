package com.meuprojeto.saas.feature.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", schema = "public")
@Data
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "schema_name", nullable = false, unique = true)
    private String schemaName;

    @Column(name = "owner_email", nullable = false, unique = true)
    private String ownerEmail;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phone;

    private String plan = "FREE";

    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Tenant(String name, String schemaName, String ownerEmail, String password, String phone) {
        this.name = name;
        this.schemaName = schemaName;
        this.ownerEmail = ownerEmail;
        this.password = password;
        this.phone = phone; // 🌟 Apanha o telefone no construtor
    }
}