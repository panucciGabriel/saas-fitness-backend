package com.meuprojeto.saas.feature.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // Método adicionado para buscar o aluno pelo email do token de login
    Optional<Student> findByEmail(String email);
}