package com.meuprojeto.saas.feature.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", schema = "public") // Forçamos o schema 'public' para esta tabela
@Data // Lombok: Gera Getters, Setters, ToString, etc.
@Builder // Lombok: Permite criar objetos com Builder Pattern
@NoArgsConstructor // Lombok: Construtor vazio (obrigatório para JPA)
@AllArgsConstructor // Lombok: Construtor com todos os argumentos
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Ex: "Academia Ironberg"

    @Column(nullable = false, unique = true)
    private String schemaName; // Ex: "tenant_ironberg" (O nome técnico do banco)

    @Column(nullable = false)
    private String ownerEmail; // Email do dono (para login futuro)

    @Column(nullable = false)
    private boolean active; // Se o cliente pagou a mensalidade

    private LocalDateTime createdAt;

    // Método utilitário para preencher a data antes de salvar
    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
}
