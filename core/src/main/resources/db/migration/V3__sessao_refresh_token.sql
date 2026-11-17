ALTER TABLE refresh_token
    ADD COLUMN familia_expira_em timestamptz NOT NULL;

CREATE INDEX idx_refresh_token_restaurante_usuario
    ON refresh_token (restaurante_id, usuario_id);
