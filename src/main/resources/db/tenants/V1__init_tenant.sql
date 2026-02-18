CREATE TABLE students (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255),
                          email VARCHAR(255),
                          password VARCHAR(255), -- Adicionado
                          phone VARCHAR(255),    -- Adicionado
                          age INTEGER,           -- Adicionado
                          plan VARCHAR(255)
);