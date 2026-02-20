CREATE TABLE workout_history (
                                 id BIGSERIAL PRIMARY KEY,
                                 workout_id BIGINT NOT NULL,
                                 student_id BIGINT NOT NULL,
                                 completed_at TIMESTAMP NOT NULL,
                                 notes VARCHAR(255),
                                 CONSTRAINT fk_history_workout FOREIGN KEY (workout_id) REFERENCES workouts(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_history_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);