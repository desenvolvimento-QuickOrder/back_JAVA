# QuickOrder — back-java

Core do QuickOrder: monolito modular (DDD tático + hexagonal) em Java 25 / Spring Boot 4.1,
único dono do estado de mesas, comandas e pedidos. PostgreSQL 17 é a fonte da verdade e os
eventos saem por outbox para Redis Streams, consumidos pelo `back-node`.

## Estrutura

```text
api/                     contrato OpenAPI 3.1 (modular, entrada em api/openapi.yaml)
core/                    módulo Maven do core
  src/main/java/com/quickorder/restaurant/core/
    shared/              tenant (RLS), erros RFC 9457, outbox/eventos, web, segurança
    equipe/              usuários, papéis, login, JWT/JWKS, refresh token
    mesa/                mesas e status
    cardapio/            categorias, produtos e cardápio público por slug
    comanda/             comandas e pedidos (ciclo de vida, locks, idempotência)
  src/main/resources/db/migration/   migrations Flyway
docs/events/             contrato dos eventos (Redis Streams / Socket.IO)
infra/db/                bootstrap dos papéis do PostgreSQL (dev e produção)
docker-compose.yml       PostgreSQL, Redis e core; cria a rede "quickorder"
```

Cada módulo de negócio segue `domain/`, `application/`, `infrastructure/` e `web/`. Por
enquanto cada pacote tem só o `package-info.java` descrevendo a responsabilidade.

## Rodando com Docker

```bash
cp .env.example .env        # opcional; os padrões já servem para dev
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

- O PostgreSQL roda `infra/db/bootstrap-dev.sql` na primeira subida e cria o papel restrito
  `quickorder_app` (sem superusuário, sem `BYPASSRLS`, sem ser dono das tabelas).
- O Flyway migra com o usuário dono (`DB_MIGRATION_USER`); a aplicação conecta com
  `quickorder_app`, então o RLS vale de verdade.
- Para recriar o banco do zero: `docker compose down -v`.

### Registry corporativo

O build Maven do `core/Dockerfile` aceita um `settings.xml` opcional como build secret:

```bash
docker build --secret id=maven-settings,src=$HOME/.m2/settings.xml -f core/Dockerfile -t quickorder-core:local .
```

## Convenções

- Configuração só por variável de ambiente; `.env` não é versionado, `.env.example` é.
- Commits no padrão Conventional Commits.
- `spring.jpa.open-in-view=false`; o tenant é aplicado com `set_config(..., true)` no início
  de cada transação.
