-- Cria o papel restrito da aplicação (sem superusuário, sem BYPASSRLS, não dono das tabelas).
-- Papéis são do cluster, por isso este script fica fora das migrations; as migrations só fazem GRANT.
-- Rodar como o usuário dono do banco, antes da primeira migration:
--   psql "$DATABASE_URL" -v app_password='<senha>' -f bootstrap.sql
-- Para dev local e Testcontainers (sem psql), usar bootstrap-dev.sql.

SELECT 'CREATE ROLE quickorder_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE'
 WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'quickorder_app')
\gexec

ALTER ROLE quickorder_app PASSWORD :'app_password';
