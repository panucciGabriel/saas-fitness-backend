package com.meuprojeto.saas.feature.training;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workout_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workout_id", nullable = false)
    private Long workoutId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    private String notes; // Espaço para o aluno deixar feedback futuramente

    @PrePersist
    protected void onCreate() {
        this.completedAt = LocalDateTime.now();
    }
}