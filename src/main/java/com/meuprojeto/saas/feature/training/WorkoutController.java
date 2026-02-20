package com.meuprojeto.saas.feature.training;

import com.meuprojeto.saas.feature.student.Student;
import com.meuprojeto.saas.feature.student.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutRepository workoutRepository;
    private final StudentRepository studentRepository;

    // Injetamos o StudentRepository para podermos achar o aluno logado
    public WorkoutController(WorkoutRepository workoutRepository, StudentRepository studentRepository) {
        this.workoutRepository = workoutRepository;
        this.studentRepository = studentRepository;
    }

    // --- ENDPOINT DO ALUNO ---
    @GetMapping("/my")
    public ResponseEntity<List<Workout>> getMyWorkouts() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

        return ResponseEntity.ok(workoutRepository.findByStudentId(student.getId()));
    }

    // --- ENDPOINTS DO PERSONAL ---
    @GetMapping
    public ResponseEntity<List<Workout>> listAll() {
        return ResponseEntity.ok(workoutRepository.findAll());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Workout>> listByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(workoutRepository.findByStudentId(studentId));
    }

    @PostMapping
    public ResponseEntity<Workout> create(@RequestBody Workout workout) {
        return ResponseEntity.ok(workoutRepository.save(workout));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workout> update(@PathVariable Long id, @RequestBody Workout updatedData) {
        return workoutRepository.findById(id)
                .map(workout -> {
                    workout.setName(updatedData.getName());
                    workout.setDescription(updatedData.getDescription());
                    workout.setWeekDay(updatedData.getWeekDay());
                    return ResponseEntity.ok(workoutRepository.save(workout));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (workoutRepository.existsById(id)) {
            workoutRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}