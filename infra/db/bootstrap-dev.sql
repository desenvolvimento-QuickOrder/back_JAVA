-- Somente dev local e testes: senha fixa. Compatível com psql (/docker-entrypoint-initdb.d) e com
-- o init script do Testcontainers (JDBC). Em ambientes reais usar bootstrap.sql.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'quickorder_app') THEN
    CREATE ROLE quickorder_app LOGIN PASSWORD 'quickorder_app_dev'
      NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
  END IF;
END
$$;
