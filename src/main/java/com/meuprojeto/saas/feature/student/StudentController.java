package com.meuprojeto.saas.feature.student;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository repository;

    public StudentController(StudentRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<StudentDTO> create(@RequestBody Student student) {
        Student saved = repository.save(student);
        return ResponseEntity.ok(StudentDTO.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<StudentDTO>> list() {
        List<StudentDTO> students = repository.findAll()
                .stream()
                .map(StudentDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> update(@PathVariable Long id, @RequestBody Student updatedData) {
        return repository.findById(id)
                .map(student -> {
                    student.setName(updatedData.getName());
                    student.setEmail(updatedData.getEmail());
                    student.setPlan(updatedData.getPlan());
                    student.setPhone(updatedData.getPhone());
                    student.setAge(updatedData.getAge());
                    return ResponseEntity.ok(StudentDTO.from(repository.save(student)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalStudents = repository.count();
        return ResponseEntity.ok(Map.of(
                "totalStudents", totalStudents,
                "activePlans", totalStudents));
    }
}
