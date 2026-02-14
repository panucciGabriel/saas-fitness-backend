package com.meuprojeto.saas.feature.student;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    // AQUI: O nome oficial é "repository"
    private final StudentRepository repository;

    public StudentController(StudentRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Student> create(@RequestBody Student student) {
        return ResponseEntity.ok(repository.save(student));
    }

    @GetMapping
    public ResponseEntity<List<Student>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> update(@PathVariable Long id, @RequestBody Student updatedData) {
        // CORRIGIDO: Usando "repository" em vez de "studentRepository"
        return repository.findById(id)
                .map(student -> {
                    student.setName(updatedData.getName());
                    student.setEmail(updatedData.getEmail());
                    student.setPlan(updatedData.getPlan());
                    return ResponseEntity.ok(repository.save(student));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // CORRIGIDO: Usando "repository" em vez de "studentRepository"
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Adicione este método ao final da classe StudentController
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalStudents = repository.count();

        // Retorna um mapa com os números
        return ResponseEntity.ok(Map.of(
                "totalStudents", totalStudents,
                "activePlans", totalStudents // Por enquanto, todos são ativos
        ));
    }
}