CREATE TABLE workouts (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL, -- Ex: "Treino de Peito A"
                          description TEXT,
                          week_day VARCHAR(20), -- Ex: "Segunda-feira"
                          student_id BIGINT REFERENCES students(id) ON DELETE CASCADE, -- Vincula ao aluno
                          created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);