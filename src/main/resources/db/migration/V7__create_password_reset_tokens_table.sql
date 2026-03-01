CREATE TABLE public.password_reset_tokens (
                                              id SERIAL PRIMARY KEY,
                                              token VARCHAR(255) NOT NULL UNIQUE,
                                              email VARCHAR(255) NOT NULL,
                                              user_type VARCHAR(50) NOT NULL,
                                              expiry_date TIMESTAMP NOT NULL
);