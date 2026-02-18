package com.meuprojeto.saas.feature.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_directory", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDirectory {

    @Id
    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private Long tenantId;
}
