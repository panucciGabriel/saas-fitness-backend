package com.meuprojeto.saas.feature.training;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutRepository workoutRepository;

    public WorkoutController(WorkoutRepository workoutRepository) {
        this.workoutRepository = workoutRepository;
    }

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
