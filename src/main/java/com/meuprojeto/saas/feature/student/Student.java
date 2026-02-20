package com.meuprojeto.saas.feature.student;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String password;

    // --- NOVOS CAMPOS ---
    private String phone;

    private Integer age;
    // --------------------

    private String plan;
}