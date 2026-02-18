package com.meuprojeto.saas.feature.training;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade Workout — representa um treino de um aluno dentro do schema do
 * tenant.
 * Corresponde à tabela criada em db/tenants/V2__create_workouts_table.sql
 */
@Entity
@Table(name = "workouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Ex: "Treino de Peito A"

    private String description;

    private String weekDay; // Ex: "Segunda-feira"

    @Column(name = "student_id")
    private Long studentId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
