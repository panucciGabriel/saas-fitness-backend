CREATE TABLE public.invites (
                                id UUID PRIMARY KEY,
                                tenant_id BIGINT NOT NULL, -- Quem enviou o convite (ID da Academia/Personal)
                                email VARCHAR(255),        -- (Opcional) Email do aluno convidado
                                used BOOLEAN DEFAULT FALSE,
                                expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);