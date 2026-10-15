-- QuickOrder: schema inicial.
--
-- Pré-requisito: papel quickorder_app criado por infra/db/bootstrap(.dev).sql.
-- Tabelas de negócio: RLS por restaurante_id (policy tenant_isolation).
-- Sem RLS: restaurante (registro) e refresh_token, outbox, idempotency_key (técnicas).
-- FKs são compostas com restaurante_id: a validação de FK ignora RLS, então a
-- integridade entre tenants precisa estar no DDL.

CREATE FUNCTION app_tenant_id() RETURNS uuid
    LANGUAGE sql STABLE
AS $$ SELECT nullif(current_setting('app.tenant_id', true), '')::uuid $$;

-- ---------------------------------------------------------------------------
-- Registro de tenants (sem RLS)
-- ---------------------------------------------------------------------------
CREATE TABLE restaurante (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    slug        varchar(60)  NOT NULL,
    nome        varchar(120) NOT NULL,
    ativo       boolean      NOT NULL DEFAULT true,
    criado_em   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_restaurante PRIMARY KEY (id),
    CONSTRAINT uq_restaurante_slug UNIQUE (slug),
    CONSTRAINT ck_restaurante_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

-- ---------------------------------------------------------------------------
-- Negócio (com RLS)
-- ---------------------------------------------------------------------------
CREATE TABLE usuario (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid         NOT NULL,
    nome           varchar(120) NOT NULL,
    login          varchar(80)  NOT NULL,
    senha_hash     varchar(255) NOT NULL,
    papel          varchar(10)  NOT NULL,
    ativo          boolean      NOT NULL DEFAULT true,
    criado_em      timestamptz  NOT NULL DEFAULT now(),
    atualizado_em  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_tenant_id UNIQUE (restaurante_id, id),
    CONSTRAINT uq_usuario_login UNIQUE (restaurante_id, login),
    CONSTRAINT fk_usuario_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT ck_usuario_login CHECK (login = lower(login)),
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('ADMIN', 'GARCOM', 'COPA'))
);

CREATE TABLE mesa (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid        NOT NULL,
    numero         varchar(20) NOT NULL,
    capacidade     smallint    NOT NULL,
    ativa          boolean     NOT NULL DEFAULT true,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    atualizado_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_mesa PRIMARY KEY (id),
    CONSTRAINT uq_mesa_tenant_id UNIQUE (restaurante_id, id),
    CONSTRAINT uq_mesa_numero UNIQUE (restaurante_id, numero),
    CONSTRAINT fk_mesa_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT ck_mesa_capacidade CHECK (capacidade > 0)
);

CREATE TABLE categoria (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid        NOT NULL,
    nome           varchar(80) NOT NULL,
    ordem          integer     NOT NULL DEFAULT 0,
    ativa          boolean     NOT NULL DEFAULT true,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    atualizado_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_categoria PRIMARY KEY (id),
    CONSTRAINT uq_categoria_tenant_id UNIQUE (restaurante_id, id),
    CONSTRAINT uq_categoria_nome UNIQUE (restaurante_id, nome),
    CONSTRAINT fk_categoria_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id)
);

CREATE TABLE produto (
    id             uuid          NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid          NOT NULL,
    categoria_id   uuid          NOT NULL,
    nome           varchar(120)  NOT NULL,
    descricao      varchar(500),
    preco          numeric(12,2) NOT NULL,
    disponivel     boolean       NOT NULL DEFAULT true,
    ativo          boolean       NOT NULL DEFAULT true,
    criado_em      timestamptz   NOT NULL DEFAULT now(),
    atualizado_em  timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_produto PRIMARY KEY (id),
    CONSTRAINT uq_produto_tenant_id UNIQUE (restaurante_id, id),
    CONSTRAINT fk_produto_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_produto_categoria FOREIGN KEY (restaurante_id, categoria_id)
        REFERENCES categoria (restaurante_id, id),
    CONSTRAINT ck_produto_preco CHECK (preco >= 0)
);
CREATE INDEX idx_produto_categoria ON produto (restaurante_id, categoria_id);

CREATE TABLE comanda (
    id             uuid          NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid          NOT NULL,
    mesa_id        uuid          NOT NULL,
    status         varchar(10)   NOT NULL DEFAULT 'ABERTA',
    aberta_em      timestamptz   NOT NULL DEFAULT now(),
    aberta_por     uuid          NOT NULL,
    fechada_em     timestamptz,
    fechada_por    uuid,
    total_final    numeric(12,2),
    version        bigint        NOT NULL DEFAULT 0,
    CONSTRAINT pk_comanda PRIMARY KEY (id),
    CONSTRAINT uq_comanda_tenant_id UNIQUE (restaurante_id, id),
    CONSTRAINT fk_comanda_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_comanda_mesa FOREIGN KEY (restaurante_id, mesa_id)
        REFERENCES mesa (restaurante_id, id),
    CONSTRAINT fk_comanda_aberta_por FOREIGN KEY (restaurante_id, aberta_por)
        REFERENCES usuario (restaurante_id, id),
    CONSTRAINT fk_comanda_fechada_por FOREIGN KEY (restaurante_id, fechada_por)
        REFERENCES usuario (restaurante_id, id),
    CONSTRAINT ck_comanda_status CHECK (status IN ('ABERTA', 'FECHADA')),
    CONSTRAINT ck_comanda_total CHECK (total_final IS NULL OR total_final >= 0),
    CONSTRAINT ck_comanda_fechamento CHECK (
        (status = 'ABERTA'  AND fechada_em IS NULL AND fechada_por IS NULL AND total_final IS NULL) OR
        (status = 'FECHADA' AND fechada_em IS NOT NULL AND fechada_por IS NOT NULL AND total_final IS NOT NULL)
    )
);
CREATE UNIQUE INDEX uq_comanda_mesa_aberta ON comanda (restaurante_id, mesa_id) WHERE status = 'ABERTA';
CREATE INDEX idx_comanda_status ON comanda (restaurante_id, status, aberta_em);

CREATE TABLE pedido (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid         NOT NULL,
    comanda_id     uuid         NOT NULL,
    rodada         integer      NOT NULL,
    status         varchar(15)  NOT NULL DEFAULT 'ENVIADO_COPA',
    origem         varchar(10)  NOT NULL,
    observacao     varchar(200),
    criado_por     uuid,
    criado_em      timestamptz  NOT NULL DEFAULT now(),
    atualizado_em  timestamptz  NOT NULL DEFAULT now(),
    version        bigint       NOT NULL DEFAULT 0,
    CONSTRAINT pk_pedido PRIMARY KEY (id),
    CONSTRAINT uq_pedido_tenant_id UNIQUE (restaurante_id, id),
    CONSTRAINT uq_pedido_rodada UNIQUE (comanda_id, rodada),
    CONSTRAINT fk_pedido_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_pedido_comanda FOREIGN KEY (restaurante_id, comanda_id)
        REFERENCES comanda (restaurante_id, id),
    CONSTRAINT fk_pedido_criado_por FOREIGN KEY (restaurante_id, criado_por)
        REFERENCES usuario (restaurante_id, id),
    CONSTRAINT ck_pedido_rodada CHECK (rodada > 0),
    CONSTRAINT ck_pedido_status CHECK (status IN ('ENVIADO_COPA', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO')),
    CONSTRAINT ck_pedido_origem CHECK (origem IN ('GARCOM', 'CLIENTE')),
    CONSTRAINT ck_pedido_criado_por CHECK (origem = 'CLIENTE' OR criado_por IS NOT NULL)
);
CREATE INDEX idx_pedido_fila ON pedido (restaurante_id, status, criado_em)
    WHERE status IN ('ENVIADO_COPA', 'EM_PREPARO');

CREATE TABLE item_pedido (
    id             uuid          NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid          NOT NULL,
    pedido_id      uuid          NOT NULL,
    produto_id     uuid          NOT NULL,
    posicao        smallint      NOT NULL,
    nome_produto   varchar(120)  NOT NULL,
    preco_unitario numeric(12,2) NOT NULL,
    quantidade     smallint      NOT NULL,
    observacao     varchar(200),
    subtotal       numeric(12,2) GENERATED ALWAYS AS (preco_unitario * quantidade) STORED,
    CONSTRAINT pk_item_pedido PRIMARY KEY (id),
    CONSTRAINT uq_item_pedido_posicao UNIQUE (pedido_id, posicao),
    CONSTRAINT fk_item_pedido_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (restaurante_id, pedido_id)
        REFERENCES pedido (restaurante_id, id),
    CONSTRAINT fk_item_pedido_produto FOREIGN KEY (restaurante_id, produto_id)
        REFERENCES produto (restaurante_id, id),
    CONSTRAINT ck_item_pedido_posicao CHECK (posicao > 0),
    CONSTRAINT ck_item_pedido_preco CHECK (preco_unitario >= 0),
    CONSTRAINT ck_item_pedido_quantidade CHECK (quantidade BETWEEN 1 AND 99)
);
CREATE INDEX idx_item_pedido_produto ON item_pedido (restaurante_id, produto_id);

CREATE TABLE pedido_status_historico (
    id             bigint      GENERATED ALWAYS AS IDENTITY,
    restaurante_id uuid        NOT NULL,
    pedido_id      uuid        NOT NULL,
    status_de      varchar(15),
    status_para    varchar(15) NOT NULL,
    usuario_id     uuid,
    em             timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_pedido_status_historico PRIMARY KEY (id),
    CONSTRAINT fk_psh_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_psh_pedido FOREIGN KEY (restaurante_id, pedido_id)
        REFERENCES pedido (restaurante_id, id),
    CONSTRAINT fk_psh_usuario FOREIGN KEY (restaurante_id, usuario_id)
        REFERENCES usuario (restaurante_id, id),
    CONSTRAINT ck_psh_status_de CHECK (status_de IS NULL OR status_de IN
        ('ENVIADO_COPA', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO')),
    CONSTRAINT ck_psh_status_para CHECK (status_para IN
        ('ENVIADO_COPA', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO'))
);
CREATE INDEX idx_psh_pedido ON pedido_status_historico (pedido_id, em);

-- ---------------------------------------------------------------------------
-- Técnicas (sem RLS; nunca expostas por listagem)
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_token (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid        NOT NULL,
    usuario_id     uuid        NOT NULL,
    token_hash     char(64)    NOT NULL,
    familia_id     uuid        NOT NULL,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    expira_em      timestamptz NOT NULL,
    usado_em       timestamptz,
    revogado_em    timestamptz,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (restaurante_id, usuario_id)
        REFERENCES usuario (restaurante_id, id),
    CONSTRAINT ck_refresh_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$')
);
CREATE INDEX idx_refresh_token_familia ON refresh_token (familia_id);
CREATE INDEX idx_refresh_token_expira ON refresh_token (expira_em);

CREATE TABLE outbox (
    id             bigint       GENERATED ALWAYS AS IDENTITY,
    event_id       uuid         NOT NULL DEFAULT gen_random_uuid(),
    restaurante_id uuid         NOT NULL,
    agregado_tipo  varchar(30)  NOT NULL,
    agregado_id    uuid         NOT NULL,
    evento_tipo    varchar(60)  NOT NULL,
    schema_version integer      NOT NULL DEFAULT 1,
    payload        jsonb        NOT NULL,
    criado_em      timestamptz  NOT NULL DEFAULT now(),
    publicado_em   timestamptz,
    CONSTRAINT pk_outbox PRIMARY KEY (id),
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id),
    CONSTRAINT fk_outbox_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id)
);
CREATE INDEX idx_outbox_pendentes ON outbox (id) WHERE publicado_em IS NULL;
CREATE INDEX idx_outbox_publicados ON outbox (publicado_em) WHERE publicado_em IS NOT NULL;

CREATE TABLE idempotency_key (
    restaurante_id uuid        NOT NULL,
    chave          uuid        NOT NULL,
    request_hash   char(64)    NOT NULL,
    pedido_id      uuid,
    status         varchar(20) NOT NULL,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    expira_em      timestamptz NOT NULL,
    CONSTRAINT pk_idempotency_key PRIMARY KEY (restaurante_id, chave),
    CONSTRAINT fk_idempotency_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante (id),
    CONSTRAINT fk_idempotency_pedido FOREIGN KEY (restaurante_id, pedido_id)
        REFERENCES pedido (restaurante_id, id),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('EM_PROCESSAMENTO', 'CONCLUIDA')),
    CONSTRAINT ck_idempotency_pedido CHECK (status = 'EM_PROCESSAMENTO' OR pedido_id IS NOT NULL)
);
CREATE INDEX idx_idempotency_expira ON idempotency_key (expira_em);

-- ---------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------
ALTER TABLE usuario ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON usuario
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE mesa ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON mesa
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE categoria ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON categoria
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE produto ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON produto
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE comanda ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON comanda
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE pedido ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON pedido
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE item_pedido ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON item_pedido
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

ALTER TABLE pedido_status_historico ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON pedido_status_historico
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

-- ---------------------------------------------------------------------------
-- Privilégios do papel da aplicação (mínimo necessário; sem DELETE em dados de negócio)
-- Migrations futuras devem conceder privilégios explicitamente.
-- ---------------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO quickorder_app;

GRANT SELECT ON restaurante TO quickorder_app;
GRANT SELECT, INSERT, UPDATE ON usuario, mesa, categoria, produto, comanda, pedido, item_pedido TO quickorder_app;
GRANT SELECT, INSERT ON pedido_status_historico TO quickorder_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_token, outbox, idempotency_key TO quickorder_app;
