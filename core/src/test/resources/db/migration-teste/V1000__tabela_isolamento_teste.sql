CREATE TABLE isolamento_teste (
    id             uuid PRIMARY KEY,
    restaurante_id uuid NOT NULL REFERENCES restaurante (id),
    valor          text
);

ALTER TABLE isolamento_teste ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON isolamento_teste
    USING (restaurante_id = (SELECT app_tenant_id()))
    WITH CHECK (restaurante_id = (SELECT app_tenant_id()));

GRANT SELECT, INSERT, UPDATE ON isolamento_teste TO quickorder_app;
