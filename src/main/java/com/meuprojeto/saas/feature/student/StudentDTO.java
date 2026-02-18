package com.meuprojeto.saas.feature.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para Student — nunca expõe o campo password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer age;
    private String plan;

    public static StudentDTO from(Student student) {
        return new StudentDTO(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getAge(),
                student.getPlan());
    }
}
