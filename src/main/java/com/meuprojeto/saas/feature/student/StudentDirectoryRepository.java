package com.meuprojeto.saas.feature.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentDirectoryRepository extends JpaRepository<StudentDirectory, String> {
    Optional<StudentDirectory> findByEmail(String email);
}
