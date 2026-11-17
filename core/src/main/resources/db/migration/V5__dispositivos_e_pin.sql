ALTER TABLE usuario
    ADD COLUMN codigo char(4),
    ADD COLUMN pin_hash varchar(255),
    ADD COLUMN deve_trocar_pin boolean NOT NULL DEFAULT false,
    ALTER COLUMN login DROP NOT NULL,
    ALTER COLUMN senha_hash DROP NOT NULL,
    ADD CONSTRAINT ck_usuario_credenciais_por_papel CHECK (
        (papel = 'ADMIN' AND login IS NOT NULL AND senha_hash IS NOT NULL
            AND codigo IS NULL AND pin_hash IS NULL) OR
        (papel IN ('GARCOM', 'COPA') AND login IS NULL AND senha_hash IS NULL
            AND codigo IS NOT NULL AND pin_hash IS NOT NULL)
    ),
    ADD CONSTRAINT ck_usuario_codigo CHECK (codigo ~ '^[0-9]{4}$'),
    ADD CONSTRAINT uq_usuario_codigo UNIQUE (restaurante_id, codigo);

CREATE TABLE dispositivo (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid         NOT NULL,
    nome           varchar(80)  NOT NULL,
    token_hash     char(64)     NOT NULL,
    criado_por     uuid         NOT NULL,
    criado_em      timestamptz  NOT NULL DEFAULT now(),
    ultimo_uso_em  timestamptz,
    revogado_em    timestamptz,
    CONSTRAINT pk_dispositivo PRIMARY KEY (id),
    CONSTRAINT uq_dispositivo_restaurante_id UNIQUE (restaurante_id, id),
    CONSTRAINT uq_dispositivo_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_dispositivo_restaurante FOREIGN KEY (restaurante_id)
        REFERENCES restaurante (id),
    CONSTRAINT fk_dispositivo_criado_por FOREIGN KEY (restaurante_id, criado_por)
        REFERENCES usuario (restaurante_id, id),
    CONSTRAINT ck_dispositivo_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$')
);

ALTER TABLE dispositivo ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON dispositivo
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

GRANT SELECT, INSERT, UPDATE ON dispositivo TO quickorder_app;

ALTER TABLE refresh_token
    ADD COLUMN dispositivo_id uuid,
    ADD CONSTRAINT fk_refresh_token_dispositivo
        FOREIGN KEY (restaurante_id, dispositivo_id)
        REFERENCES dispositivo (restaurante_id, id);

CREATE INDEX idx_refresh_token_dispositivo ON refresh_token (dispositivo_id);
