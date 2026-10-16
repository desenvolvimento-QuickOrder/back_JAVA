# QuickOrder — Definições de Arquitetura e Setup

Documento consolidado das decisões da fase de definição. Data de referência das verificações de versão e suporte: 2026-10-15.

Escopo desta fase: definir a base (arquitetura, repositórios, contrato da API, convenções, CI/CD, deploy). Não inclui o desenvolvimento do core de negócio.

---

## 1. Produto

- Sistema web de gerenciamento para restaurantes (comércios do setor alimentício).
- Problema: gargalos no atendimento e gestão complexa.
- Solução: gestão de mesas e comandas em tempo real, com automação de pedidos.
- Foco: agilidade dos pedidos feitos pelo garçom.
- Início com **um restaurante**, mas a base deve ser **multi-tenant ready** e escalável.

## 2. Stack e versões

| Camada | Tecnologia |
|---|---|
| Front-end | React + TypeScript |
| Back-end core | Java 25 (LTS) + Spring Boot 4.1.x |
| Back-end tempo real | Node.js 24 LTS + TypeScript + Fastify |
| Gateway | Spring Cloud Gateway (Spring Cloud 2025.1.x) — **adiado para a fase paga** (seção 10) |
| Banco | PostgreSQL |
| Eventos | Redis Streams |
| Migrations | Flyway |
| CI | GitHub Actions |
| Registry | GHCR privado |
| Deploy | Render (fase inicial, plano gratuito); AWS depois |

Versões verificadas:

- Spring Boot 4.0 sai do suporte open source em 2026-12-31. **Usar 4.1.x** (4.1.1 na verificação; suporte até 2027-07-31).
- Boot 4.1 suporta Java 17 a 26. **Java 25 é LTS** e foi adotado (decisão do projeto: Java 25 no lugar do 21).
- Java 25 permite o **AOT cache** do JDK (JEP 483/514/515), documentado pelo Spring Boot para Dockerfile. Entra como segunda etapa, depois que o baseline funcionar (seção 13).
- Spring Cloud 2025.1.x suporta Boot 4.1 a partir da 2025.1.2. O trem 2025.0 (Boot 3.5) está fora de suporte.
- Node 24 LTS (suporte até 2028-04). Node 26 vira LTS em 2026-10-28; reavaliar depois.
- Preferir versões publicadas há mais de 7 dias.
- Boot 4: starters renomeados (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, etc.). Gateway (quando voltar): `spring-cloud-starter-gateway-server-webflux`, propriedades sob `spring.cloud.gateway.server.webflux`.

## 3. Arquitetura

### Estilo

- **Monolito modular** no core Java, com **DDD tático + hexagonal (portas e adaptadores)** em cada módulo.
- Microsserviços só quando houver motivo concreto (escala diferente, time dedicado, deploy independente).
- Consistência forte (ACID) para mesa, comanda e pedido dentro do core.

### Módulos do core (MVP)

```
com.quickorder.restaurant.core
├── shared/       # eventos base, exceções, tenant context
├── equipe/       # login, JWT, usuários, papéis (ADMIN, GARCOM, COPA)
├── mesa/         # cadastro e status das mesas
├── cardapio/     # produtos, categorias, disponibilidade
└── comanda/      # comanda, pedido, itens, máquina de estados, fila da copa
```

Cada módulo: `domain/` (entidades, value objects, portas), `application/` (casos de uso), `infrastructure/` (JPA, adaptadores), `web/` (controllers e DTOs).

Recomendado: Spring Modulith para verificar fronteiras (`ApplicationModules.verify()`). Confirmar a versão compatível com o Boot 4 antes de adotar.

### Responsabilidades

**back-java (core)**: autenticação, usuários, funcionários, mesas, comandas, pedidos, regras de negócio, cardápio (cadastro e, na fase gratuita, também a leitura pública), financeiro (adiado), dashboard admin (básico).

**back-node**: WebSocket e notificação em tempo real, consumo de eventos, cardápio digital (leitura com cache, a partir da fase paga), integrações futuras (WhatsApp, iFood).

### Dono de cada capacidade

| Capacidade | Dono da escrita | Papel do outro serviço |
|---|---|---|
| Autenticação | Java (emite JWT) | Node valida JWT (HTTP e WebSocket) |
| Usuários e funcionários | Java | — |
| Mesas, comandas, pedidos e status | Java | Node recebe eventos e transmite |
| Cardápio (cadastro) | Java | Node poderá servir leitura com cache depois |
| Financeiro e dashboard | Java | — |
| Notificação em tempo real | Node | — |
| Eventos | Redis Streams | Java publica, Node consome |
| WhatsApp e iFood (futuro) | Node (adaptadores) | Traduz webhooks em comandos para a API Java |
| Logs e monitoramento | Transversal | Ambos instrumentados |

Regra central: **o Java é o único dono do estado do pedido**. O Node nunca altera status; ele transmite e, quando necessário, chama a API Java.

## 4. Autenticação e acesso

- Java gerencia usuário e senha (Spring Security, BCrypt ou Argon2id, rate limit no login).
- JWT assinado com **RS256**; chave pública exposta em endpoint JWKS (`/.well-known/jwks.json`, código a ser escrito; contrato entre serviços, fora do contrato público). Node valida localmente, sem chamar o Java por requisição.
- Claims: `sub`, `roles`, `tenant_id`. Access token curto (ex.: 15 min).
- Papéis: `ADMIN`, `GARCOM`, `COPA`. Não existe papel de cliente.
- O `tenant_id` vem sempre do token, nunca de header ou parâmetro enviado pelo cliente.
- Chave privada do JWT fora do repositório (variável de ambiente ou volume).
- Token do WebSocket enviado no handshake (`auth` do Socket.IO), nunca na query string.

### Refresh token

- `onrender.com` está na Public Suffix List: cada `*.onrender.com` é um site diferente. Front e API ficam cross-site, e cookie de refresh seria de terceiros (Safari bloqueia; o tablet do garçom pode ser um iPad).
- **Fase gratuita**: refresh token **no corpo** da resposta (`TokenResponse.refreshToken`), guardado pelo front. Mitigações: access token curto, **rotação** do refresh token e **detecção de reuso** no servidor (reuso invalida a família de tokens).
- **Produção**: domínio próprio com `app.` e `api.` sob o mesmo domínio (same-site) e refresh token em **cookie HttpOnly**. O Render suporta domínio próprio em serviço gratuito.
- Transição para cookie sem quebra: o servidor envia corpo e cookie por um período, o front migra, e o campo do corpo sai depois.
- **Refresh concorrente** (duas abas, ou retry após resposta perdida no wifi do tablet): single-flight no front entre abas (Web Locks API ou BroadcastChannel; confirmar suporte nos navegadores-alvo) e janela de tolerância no servidor, em que o token recém-rotacionado ainda é aceito; reuso fora da janela revoga a família. **Duração da janela: a definir** (sugestão: 10 a 30 s).

### Cliente final (QR code)

- Cliente **apenas visualiza o cardápio**. Não faz pedido, não faz login.
- QR aponta para URL pública somente leitura: `https://app.../cardapio/{slug-do-restaurante}`. Número da mesa é opcional e apenas informativo (nunca confiável).
- Sem JWT nem sessão anônima. Tenant resolvido por slug ou domínio.
- Rota pública com cache e rate limit.
- Futuro (se o cliente puder pedir): token de sessão de mesa com claims `tenant_id`, `mesa_id`, `comanda_id`; token opaco e rotativo no QR; pedido do cliente entra como pendente até confirmação. O campo `origem` do pedido (`GARCOM` ou `CLIENTE`) é gravado desde já.

## 5. Contrato da API (OpenAPI)

### Abordagem: contract-first

- Arquivo `back-java/api/openapi.yaml`, **OpenAPI 3.1**, dividido com `$ref` (`paths/`, `schemas/`, `components/`) e empacotado (Redocly CLI).
- Descreve a **API pública do sistema**, incluindo o cardápio público (o Node implementará o mesmo schema se passar a servi-lo).
- WebSocket e eventos **não cabem no OpenAPI**: envelope e payloads em JSON Schema (ou AsyncAPI) em `back-java/docs/events`.
- `servers: - url: /api/v1`. Sem gateway, o front usa duas URLs base (core e node).

### Ferramentas

- **Spectral** no CI (regra: `x-roles` obrigatório em toda operação não pública).
- **oasdiff** no CI, comparando com a `main`: mudança incompatível sem versão nova bloqueia o PR.
- **CODEOWNERS** no arquivo do contrato.
- **Geração interface-only** com `openapi-generator` (gerador `spring`): opções `useSpringBoot4` e `useJackson3` existem desde a 7.16.0 (última vista: 7.21.0). Controllers implementam as interfaces geradas. Manter `openApiNullable=true` com `jackson-databind-nullable` a partir da 0.2.10 (traz `JsonNullableJackson3Module`; a 0.2.9 não tinha; a última vista foi a 0.2.12; o Jackson deixou de ser transitivo), módulo registrado como bean (a geração interface-only não gera a classe da aplicação) e teste de PATCH com campo ausente, `null` e valor. Plano B: "ausente = sem mudança, `null` = 400", com ações dedicadas para limpar campos.
- **springdoc-openapi 3.x** suporta Boot 4; uso opcional, só para Swagger UI apontando para o arquivo estático.
- **Distribuição entre repositórios**: a CI do `back-java` anexa o `openapi.yaml` a cada release (tag); front e Node fixam uma tag e rodam um script de sincronização que gera tipos com `openapi-typescript`. Se o atrito crescer, publicar pacote npm privado.

### Convenções

| Tema | Decisão |
|---|---|
| Versionamento | Prefixo `/api/v1`. Mudança aditiva fica na v1; incompatível vira v2. |
| IDs públicos | UUID (evita enumeração em multi-tenant). |
| Tenant | Nunca no path nem no corpo; vem do token (exceção: slug no cardápio público). |
| Datas | ISO-8601 em UTC (`format: date-time`). |
| Nomes | Paths no plural e em kebab-case, JSON em camelCase, enums em MAIÚSCULAS. |
| Dinheiro | **String decimal**: `type: string`, `pattern: '^\d{1,10}\.\d{2}$'` (ex.: `"12.50"`). Gerar como `String` e converter para value object de dinheiro na camada `web` (parse estrito), evitando `BigDecimal` serializado como número JSON. Total sempre calculado no servidor. |
| Enums | Clientes devem tolerar valores desconhecidos. |
| Paginação | `page` e `size` (máx. 100) com envelope `{items, page, size, total}` nas listas administrativas. Cardápio não pagina. |
| Segurança | `bearerAuth` (JWT) global, `security: []` nos endpoints públicos. Papéis por operação em `x-roles` (só documentação; a imposição é do Spring Security). |
| Idempotência | Header `Idempotency-Key` (UUID) obrigatório em `POST /comandas/{id}/pedidos`. |
| Concorrência | MVP **sem `If-Match`**; `@Version` no servidor protege as transições (conflito retorna `409`). `If-Match` poderá entrar depois como header opcional. |
| Correlação | `X-Correlation-Id` aceito e devolvido em todas as respostas. |

### Erros (RFC 9457)

Schema `Problem` (`application/problem+json`): `type`, `title`, `status`, `detail`, `instance`, e extensões `code` (estável, para o front), `correlationId` e `errors[]` (validação). Respostas reutilizáveis em `components/responses`.

| Status | Uso |
|---|---|
| 400 | Requisição malformada ou validação de campo |
| 401 / 403 | Autenticação / permissão |
| 404 | Não encontrado |
| 409 | Conflito (versão, duplicidade, idempotência em andamento) |
| 422 | Regra de negócio violada |
| 429 | Rate limit |

### Endpoints do MVP (prefixo `/api/v1`)

| Área | Endpoints |
|---|---|
| Auth | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| Usuários (ADMIN) | `GET/POST /usuarios`, `GET/PATCH /usuarios/{id}`, `POST /usuarios/{id}/desativar` |
| Mesas | `GET /mesas`, `POST /mesas` (ADMIN), `GET/PATCH /mesas/{id}` |
| Cardápio (ADMIN) | CRUD de `/categorias` e `/produtos`, `PATCH /produtos/{id}/disponibilidade` |
| Cardápio público | `GET /cardapio/{slug}` (`security: []`), sob o prefixo `/api/v1/cardapio/**` para roteamento futuro ao Node |
| Comandas | `POST /comandas`, `GET /comandas`, `GET /comandas/{id}`, `POST /comandas/{id}/fechar`, `POST /comandas/{id}/transferir-mesa` |
| Pedidos | `POST /comandas/{id}/pedidos`, `GET /pedidos?status=` (fila da copa), `GET /pedidos/{id}`, ações de transição (seção 6) |

CRUD usa REST direto; a máquina de estados do pedido usa **endpoints de ação** (um por transição).

### Schemas mínimos

| Schema | Campos principais |
|---|---|
| `Pedido` | `id`, `comandaId`, `mesaId`, `status`, `itens[]`, `total`, `observacao`, `criadoEm`, `atualizadoEm`, `criadoPor` |
| `PedidoCriacao` | `itens[]` (`produtoId`, `quantidade` 1 a 99, `observacao` até 200), `observacao` |
| `ItemPedido` | `produtoId`, `nome` e `precoUnitario` (cópias do momento do pedido), `quantidade`, `subtotal`, `observacao` — **sem status** |
| `Comanda` | `id`, `mesaId`, `status`, `abertaEm`, `fechadaEm`, `total`, `abertaPor` |
| `Mesa` | `id`, `numero`, `capacidade`, `status`, `ativa` |
| `Categoria` / `Produto` | `id`, `nome`, `ordem` / `categoriaId`, `nome`, `descricao`, `preco`, `disponivel` |
| `Usuario` | `id`, `nome`, `login`, `papel`, `ativo`; `senha` só na criação (`writeOnly`) |
| Auth | `LoginRequest` (`login`, `senha`), `TokenResponse` (`accessToken`, `refreshToken`, `tokenType`, `expiresIn`) |
| `CardapioPublico` | `restaurante` (`nome`, `slug`), `categorias[]` com `produtos[]` (`id`, `nome`, `descricao`, `preco`, `disponivel`) |
| `Problem` | `type`, `title`, `status`, `detail`, `instance`, `code`, `correlationId`, `errors[]` |

Campos gerados pelo servidor (`id`, datas, `status`, `total`) são `readOnly`.

### Idempotência na criação de pedido

- Mesma chave e mesmo corpo: devolve a resposta original.
- Mesma chave e corpo diferente: `422` com `code: IDEMPOTENCY_KEY_REUSADA`.
- Mesma chave ainda em processamento: `409`.
- Retenção de 24 horas, em tabela no Postgres.

## 6. Pedido, comanda e mesa (regras e estados)

O status existe **somente no pedido**. Itens não têm status.

| Ação | Papel | De | Para | Evento |
|---|---|---|---|---|
| Criar (`POST /comandas/{id}/pedidos`) | GARCOM, ADMIN | (novo) | `ENVIADO_COPA` | `pedido.enviado` |
| `iniciar-preparo` | COPA | `ENVIADO_COPA` | `EM_PREPARO` | `pedido.em_preparo` |
| `marcar-pronto` | COPA | `EM_PREPARO` | `PRONTO` | `pedido.pronto` |
| `entregar` | GARCOM | `PRONTO` | `ENTREGUE` | `pedido.entregue` |
| `cancelar` | GARCOM, ADMIN | `ENVIADO_COPA` | `CANCELADO` | `pedido.cancelado` |

- O pedido já nasce em `ENVIADO_COPA` (o garçom monta no front e envia uma vez). Novos itens viram um novo pedido (rodada).
- Cancelamento é do **pedido inteiro**, apenas em `ENVIADO_COPA`. Cancelamento por item fica para depois.
- Transição inválida: `422` com `code: PEDIDO_TRANSICAO_INVALIDA`.
- Repetir uma ação cujo destino já foi alcançado retorna `200` com o recurso atual (duplo toque não gera erro).
- Conflito de concorrência: `409`.
- **Comanda**: `ABERTA` e `FECHADA`. Fechar exige todos os pedidos em `ENTREGUE` ou `CANCELADO`, senão `422` com `code: COMANDA_COM_PEDIDOS_PENDENTES`. O fechamento devolve o total, sem pagamento.
- **Mesa**: `LIVRE` e `OCUPADA`, derivadas da comanda aberta. O cliente da API não altera o status diretamente.
- **Uma comanda aberta por mesa**: `@Version` não pega a corrida entre duas aberturas. Índice único parcial: `CREATE UNIQUE INDEX uq_comanda_mesa_aberta ON comanda (restaurante_id, mesa_id) WHERE status = 'ABERTA'`. Violação vira `409` com `code: MESA_JA_TEM_COMANDA_ABERTA` (cobre também a transferência de mesa).
- **Fechar x criar pedido**: criar pedido, fechar e transferir mesa travam a linha da comanda (`SELECT ... FOR UPDATE`). Ordem de locks: comanda, depois pedidos. As demais transições de pedido não precisam do lock, porque um pedido não finalizado já impede o fechamento.

## 7. Eventos e tempo real

### Broker: Redis Streams

Decisão: **Redis Streams** no lugar de RabbitMQ (no Render o RabbitMQ exigiria serviço pago com disco persistente).

- Core: `spring-boot-starter-data-redis`; publicação por porta `EventPublisher`, com o adaptador Redis isolado na infraestrutura; outbox no Postgres.
- Node: `ioredis` (XREADGROUP).
- Convenção: um stream por agregado (`events:pedido`, `events:mesa`), consumer group `realtime`. O Node cria stream e grupo na subida de forma idempotente (`XGROUP CREATE ... MKSTREAM`, ignorando `BUSYGROUP`).
- Envelope: `eventId`, `tipo`, `schemaVersion`, `tenantId`, `payload`. Eventos retrocompatíveis (apenas adicionar campos). Consumidores idempotentes por `eventId`.
- Trocar de broker no futuro (inclusive na AWS) deve ficar restrito a um adaptador.

### Eventos são dicas, a verdade é o REST

- O Postgres é a fonte da verdade. Restart do Key Value gratuito (só memória) **apaga streams e grupo**: eventos pendentes se perdem, não apenas atrasam.
- O front **recarrega o estado via REST** (`GET /pedidos`, `GET /mesas`) ao conectar e reconectar o WebSocket.

### Catálogo de eventos

`pedido.enviado`, `pedido.em_preparo`, `pedido.pronto`, `pedido.entregue`, `pedido.cancelado`, `mesa.status_alterado`.

### Rooms (Socket.IO)

- `tenant:{id}:copa` e `tenant:{id}:salao`. **Todo `pedido.*` vai para as duas rooms**; `mesa.status_alterado` vai para `salao`.
- Payload mínimo: `pedidoId`, `comandaId`, `mesaId`, `status`, `pedidoVersion`, `criadoPor`. O front guarda a última `pedidoVersion` vista por pedido e **descarta evento com versão menor ou igual**; o estado é reconciliado por REST. O front decide o que destacar (por exemplo, "meus pedidos").
- Não há room por garçom (frágil quando outro garçom cobre a mesa).
- Apenas transporte `websocket`; adapter Redis do Socket.IO só quando houver mais de uma instância do Node.

### Fluxo de publicação

1. Mudança de status via API Java.
2. Java persiste e grava o evento na tabela **outbox** na mesma transação.
3. Publicador envia ao stream Redis (após o commit).
4. Node consome e transmite por WebSocket.

Ordem e entrega:

- `pedidoVersion` vem do `@Version` do pedido (monotônico por pedido).
- Publicador **único**, que lê a outbox por `publicado_em IS NULL` ordenado por id, e não por "id maior que o último visto" (ids da sequência não seguem a ordem de commit).
- Com mais de uma instância do publicador seria preciso coordenação (por exemplo, advisory lock).

## 8. Multi-tenancy

- Estratégia: **coluna `restaurante_id`** (tenant) em todas as tabelas de negócio. Tabela `restaurante` desde já (um registro hoje).
- **Hibernate `@Filter` não se aplica a `findById` nem a SQL nativo; `@TenantId` também tem limitação em busca por chave primária** (issue HHH-16830 no Hibernate 6.4; status no Hibernate 7 não verificado). Portanto `@TenantId` serve para preencher o tenant no insert e filtrar JPQL/Criteria, mas **não é a garantia**.
- **A garantia é o RLS (Row-Level Security) no PostgreSQL**:
  - Usuário da aplicação **não pode ser dono das tabelas** (o dono ignora RLS, a menos que se use `FORCE ROW LEVEL SECURITY`).
  - Tenant definido por transação com `SET LOCAL` / `set_config(..., true)` (seguro com pool de conexões), aplicado **no início da transação** (por exemplo, sobrescrevendo `beginTransaction` do `JpaDialect`; validar no Spring 7), nunca em filtro HTTP, que só preenche o `TenantContext`. `spring.jpa.open-in-view=false`. `SET LOCAL` fora de transação não tem efeito. Métodos de consulta declarados em repositórios Spring Data não são transacionais por padrão (confirmar na documentação), então todo caso de uso da camada `application` é `@Transactional`.
  - Tenant não definido: a policy não retorna linhas (falha fechada). A policy usa `nullif(current_setting('app.tenant_id', true), '')::uuid` (em `USING` e `WITH CHECK`), porque, depois de um `SET LOCAL`, a variável volta como string vazia na conexão do pool e o cast direto para `uuid` daria erro em vez de zero linhas.
  - Flyway roda com o usuário dono; a aplicação, com o usuário restrito (`spring.flyway.user` e `spring.flyway.password` separados). No Render, o papel restrito é criado por **SQL** (`CREATE ROLE`, e não pelo painel, para que o usuário padrão continue sendo o dono e as migrations rodem com ele); a connection string da aplicação é montada à mão. Papéis são do cluster: o script fica em `infra/db/bootstrap.sql`, fora das migrations, que só fazem `GRANT`. O Render permite adicionar usuários (painel e API) e `CREATE USER`, mas **confirmar `CREATE ROLE` no primeiro deploy**; plano B: papel único com `FORCE ROW LEVEL SECURITY`.
- **Teste de integração obrigatório desde o início** (Testcontainers, dois tenants): tentar vazamento por `findById`, JPQL e SQL nativo.
- O teste **precisa rodar a aplicação com o papel restrito**: o usuário padrão do Testcontainers e do compose é superusuário e ignora RLS. Bootstrap por script de init (compose e `withInitScript`).
- Testes-guarda: `current_user` sem `rolsuper` nem `rolbypassrls`, com guarda equivalente na subida fora do perfil de desenvolvimento (desligada no treino do AOT cache); e toda tabela do schema com RLS ativo ou presente numa lista explícita de exceções (ver decisão pendente 2, seção 23).
- Rooms do Socket.IO sempre prefixadas com o tenant.
- Configurações variáveis (taxas, impressoras, horários) em tabela por tenant.
- Logs e métricas com tag `tenant_id`.

## 9. Escalabilidade (sem complexidade prematura)

- Serviços stateless, nenhuma sessão em memória.
- Idempotência na criação de pedido (`Idempotency-Key`) e nos consumidores de evento (`eventId`).
- Controle otimista (`@Version`) em mesa, comanda e pedido.
- Valores monetários em `NUMERIC(12,2)` no banco.
- Preço copiado para o `item_pedido` no momento do lançamento.
- Rate limit distribuído e adapter do Socket.IO só quando houver mais de uma instância.

## 10. Gateway (adiado)

**Fase gratuita: não há gateway.** Core e node são públicos nessa fase, então ele não protegeria nada a mais, e consumiria horas gratuitas.

Consequências na fase gratuita:

- O front tem duas URLs base (core e node).
- **CORS** configurado em core e node, restrito à origem do front.
- **Rate limit** no core (login e cardápio público) e no node, em memória (instância única): Bucket4j no Java (verificar artefato e versão compatíveis) e `@fastify/rate-limit` no Node.
- **IP do cliente**: o Render manda ler `X-Forwarded-For`, mas **não usar o primeiro item às cegas**. Um relato com dados reais do Render mostra `X-Forwarded-For: <cliente>, <edge da Cloudflare>`; a Cloudflare acrescenta o IP de conexão a um header enviado pelo cliente, então, se o header forjado for preservado, o primeiro item é do atacante.
  - O `ClientIpResolver` pega a posição **N a partir da direita**, com N configurável e calibrado por teste. Pelo relato, o cliente seria o penúltimo item (N=2).
  - No Fastify, `trustProxy` numérico (nunca `true`); pela semântica do `proxy-addr`, `1` entregaria o IP do edge da Cloudflare e `2` o do cliente (hipótese a confirmar no teste).
  - Registrar divergência com `CF-Connecting-IP` e `True-Client-IP` (não documentados pelo Render; só como conferência).
  - Throttle por conta no login (chave `slug+login`), para que forjar o IP não anule o limite.
  - **Teste no primeiro deploy**: endpoint temporário que devolve o `X-Forwarded-For` bruto, o IP do par e os headers da Cloudflare; chamar de duas redes (celular em 4G e notebook), com e sem `X-Forwarded-For: 1.2.3.4` forjado, e escolher N pelo resultado.
- O cardápio público é servido pelo core.

**Fase paga** (quando core e node virarem private services): o gateway volta como único ponto de entrada público. Projeto previsto:

| Rota | Destino | Acesso |
|---|---|---|
| `POST /api/v1/auth/login` | Java | Público, rate limit forte |
| `GET /api/v1/cardapio/**` | Node (cache Redis) | Público, rate limit |
| `/api/v1/**` | Java | JWT obrigatório |
| `/ws/**` | Node | JWT obrigatório |

- Spring Cloud Gateway (WebFlux), módulo `gateway` no `back-java`, adicionado nessa fase.
- Rate limit com Redis e **`XForwardedRemoteAddressResolver`** (sem ele o limite vira global atrás do proxy).
- Timeouts do gateway para o core compatíveis com cold start (se algum serviço ainda hibernar).
- CORS no gateway; ordem das rotas importa (específicas antes de `/api/v1/**`); bean `KeyResolver` por IP.
- Serviços continuam validando o JWT; não repassar headers de identidade forjáveis.

## 11. Repositórios

```
frontend/    # React + TypeScript
back-java/   # POM pai + módulo core (módulo gateway entra na fase paga); docker-compose.yml; render.yaml; api/openapi.yaml
back-node/   # Fastify + TypeScript + Socket.IO
```

- `back-java`: POM pai (`packaging pom`) com o módulo `core` por enquanto, para que adicionar o `gateway` depois não obrigue a mover pastas. Pacote: `com.quickorder.restaurant.core`.
- `docker-compose.yml` local na raiz do `back-java` (PostgreSQL 17, Redis 7). O Node depende desse compose no ambiente local (documentar no README).
- Dev local: proxy do Vite do front para os serviços.

### Contratos entre repositórios

1. API HTTP: `back-java/api/openapi.yaml` (seção 5); front e Node geram tipos a partir dele.
2. Eventos: envelope padrão documentado em `back-java/docs/events`; `back-node` replica os tipos (ou extrair repositório `contracts` depois).
3. Configuração comum (URL do JWKS, nomes de claims, nomes de eventos) documentada em um único lugar.

## 12. Convenções comuns aos backs

- Git: `main` protegida, PR obrigatório, CI obrigatório, squash merge, histórico linear, sem force push.
- **Conventional Commits** (`feat:`, `fix:`).
- Configuração por variáveis de ambiente (12-factor); `.env.example` versionado; nenhum segredo no repositório.
- Erros no padrão **RFC 9457 (Problem Details)**.
- Logs JSON com `correlationId` e `tenant_id`; health check e métricas Prometheus.
- Windows: `.gitattributes` com `* text=auto eol=lf`, `.editorconfig`; marcar `mvnw` como executável (`git update-index --chmod=+x mvnw`).
- Dependabot ou Renovate com intervalo mínimo de 7 dias (`cooldown` / `minimumReleaseAge`).
- SonarCloud adiado.

## 13. back-java: base

- Maven Wrapper (`mvnw`) versionado. Java 25.
- Dependências do `core`: `starter-webmvc`, `starter-validation`, `starter-security`, `starter-security-oauth2-resource-server`, `starter-data-jpa`, `starter-flyway`, `flyway-database-postgresql` (runtime), `postgresql` (runtime), `starter-data-redis`, `starter-actuator`, `starter-test` (test); Bucket4j para rate limit. Testcontainers (PostgreSQL e Redis) nos testes de integração.
- O `spring-boot-maven-plugin` declarado no `<build>` do módulo.
- Qualidade: Spotless (google-java-format, **verificar suporte a Java 25**) checado no CI, JaCoCo, JUnit 5; unitários no Surefire e integração `*IT` no Failsafe.
- Base mínima no código (sem regra de negócio): handler global de erros (ProblemDetail), filtro de `correlationId`, `ClientIpResolver`, configuração de CORS, `TenantContext` e mecanismo de `SET LOCAL` por transação, Actuator expondo só `health`, `info` e `prometheus`, migration `V1` validando o Flyway (com papéis e policies de RLS), teste do Modulith e **teste de isolamento entre tenants**.
- Migration inicial (a detalhar depois): `restaurante`, `usuario`, `papel`, `mesa`, `categoria`, `produto`, `comanda`, `pedido`, `item_pedido`, `outbox`, `idempotency_key`.
- Dockerfile multi-stage: Temurin 25 (JRE), usuário não-root, jar em camadas (`jarmode=tools extract --layers`), build `linux/amd64`.
- **AOT cache (segunda etapa)**: treino no build (`java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar application.jar`) e execução com `-XX:AOTCache=app.aot`. O JDK rejeita o cache em silêncio em vários casos (jar reconstruído, arquivos copiados, ZGC): gerar no mesmo estágio do Dockerfile e **confirmar nos logs que foi aceito**. Medir o cold start no Render com e sem cache.
- **Treino do AOT cache sem banco**: o treino (`-Dspring.context.exit=onRefresh`) sobe DataSource, Flyway e Hibernate, e não há banco no `docker build`. Usar um perfil `aot-training`: `spring.flyway.enabled=false` (deve funcionar, porque não usamos o processamento AOT do Spring; se não, um `FlywayMigrationStrategy` vazio, como indicado numa issue do Boot), JPA sem metadata JDBC, `ddl-auto=none`, `spring.sql.init.mode=never` e a guarda de papel desligada. Seguir a seção "training run configuration" do README do `spring-lifecycle-smoke-tests`; confirmar os nomes das propriedades do Hibernate 7.
- JVM no plano gratuito (0,1 CPU / 512 MB): `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC` e pool Hikari pequeno. Testar cedo.
- `application.yml` com valores padrão apenas para desenvolvimento (`${VAR:default}`); `server.port: ${PORT:8080}`.

## 14. back-node: base

- Fastify, TypeScript em modo `strict`, ESM.
- Estrutura:

```
src/
├── server.ts / app.ts
├── config/       # validação das variáveis de ambiente (falha rápido)
├── plugins/      # JWT (JWKS), Socket.IO, Redis, CORS, rate limit
├── modules/      # realtime, cardapio, integracoes
└── shared/
```

- Bibliotecas (últimas estáveis na instalação): `pino`, `jose`, `zod`, `ioredis`, `socket.io`, `@fastify/cors`, `@fastify/rate-limit`.
- Atrás do proxy do Render: `trustProxy` configurado e IP do cliente pelo mesmo critério do core.
- Qualidade: ESLint, Prettier, Vitest.
- Scripts: `dev`, `build`, `start`, `lint`, `format:check`, `typecheck`, `test`.
- `.nvmrc` e `engines` no `package.json`; gerenciador **npm**, `package-lock.json` versionado, `npm ci` no CI, `save-exact=true` no `.npmrc`.
- Dockerfile multi-stage: `node:24-slim`, usuário não-root, `npm ci --omit=dev`, build `linux/amd64`.
- Health: `/health` e `/ready`.

## 15. CI (GitHub Actions)

Um pipeline por repositório, em PR e em push na `main`.

- `permissions: contents: read`; `concurrency` cancelando execuções antigas.
- Actions de terceiros fixadas por **SHA do commit**.
- `back-java`: setup-java (Temurin 25, cache Maven), `./mvnw -B -ntp spotless:check verify`, upload de relatórios (Surefire, Failsafe, JaCoCo). Spectral e oasdiff sobre `api/openapi.yaml`.
- `back-node`: setup-node (via `.nvmrc`, cache npm), `npm ci`, lint, typecheck, test, build.
- Em PR: `dependency-review-action`, secret scanning, Trivy na imagem. CodeQL se disponível.
- Branch protection com checks obrigatórios.
- Release do `back-java`: anexa `api/openapi.yaml` à release (tag).

## 16. CD (Render + GHCR)

Fluxo em push na `main`:

1. Job `verify`.
2. Job `image`: build `linux/amd64` e push para o GHCR com tag igual ao SHA do commit (core no `back-java`; node no `back-node`). Login com `GITHUB_TOKEN` (`packages: write`).
3. Job `deploy`: chama o **deploy hook** do Render com `imgURL` (imagem codificada como URL) do SHA. Environment `dev`.
4. Verificação pós-deploy: `curl --retry` no `/actuator/health` público do core (o hook só inicia o deploy; o primeiro acesso pode demorar por causa do cold start).

Pontos:

- Serviços baseados em imagem não fazem redeploy automático quando a tag muda; o deploy hook com `imgURL` é o caminho documentado. Testar uma chamada manual antes de automatizar.
- Deploy hooks (`RENDER_DEPLOY_HOOK_*`) como secrets do GitHub dentro de um Environment.
- GHCR privado: o Render usa um PAT com `read:packages` (expiração longa, anotar a rotação, ativar notificação de "Image Pull Failed"). O Render baixa a imagem a cada deploy e em alguns restarts.
- Não apagar tags recentes do GHCR (o rollback depende delas).
- Não usar ECR no Render (a senha expira a cada 12 h); só na AWS.
- Migrations: Flyway na subida do core (uma instância). Com mais instâncias, usar o pre-deploy command do Render.
- `healthCheckPath: /actuator/health` nos web services.
- Alternativa descartada: build direto no Render a partir do Dockerfile (consome minutos de build, artefato diferente do testado pelo CI).
- `render.yaml` na raiz do `back-java`; validar com `render blueprints validate render.yaml`.

## 17. Topologia no Render (fase gratuita)

| Componente | Tipo |
|---|---|
| core | web service gratuito, público |
| node | web service gratuito, público |
| Postgres | Render Postgres gratuito |
| Redis | Render Key Value gratuito |
| frontend | static site gratuito |

Todos na **mesma região**. Plano `free`: 0,1 CPU / 512 MB. Primeiro plano pago de web service: `0.5c-512mb` (0,5 CPU / 512 MB); `1c-2g` tem 1 CPU / 2 GB.

Consequências da fase gratuita (verificadas na documentação do Render):

- Private services não têm plano gratuito, e web services gratuitos não recebem tráfego da rede privada. Core e node ficam **públicos** (aceito), protegidos por JWT em cada serviço, sem CORS aberto, Actuator só com `health`, e **nenhum dado real de cliente neste ambiente**. Core e node falam com Postgres e Key Value pela rede interna.
- **Cota de 750 horas por mês para todo o workspace**: um mês tem 744 h, então core e node ligados 24 h (1.488 h) já passam disso, e o Render suspende os serviços até o mês seguinte. Mensagens de WebSocket de conexões existentes contam como tráfego, e o ping do Socket.IO é uma mensagem do cliente: **uma aba aberta mantém o node acordado**. Tratar o ambiente como demonstração e fechar as abas.
- Hibernação após 15 min sem tráfego; retorno em cerca de 1 min por serviço, mais a subida da JVM em 0,1 CPU. O **front precisa de estado "acordando o servidor" e timeout alto**, e o cliente HTTP do Node para o core também.
- Postgres gratuito expira em 30 dias (1 GB, sem backup) e é apagado após 14 dias de carência. Tratar como descartável: migrations Flyway, script de dados de desenvolvimento, lembrete antes do dia 30.
- Key Value gratuito é só memória: restart apaga os streams (seção 7).
- Confirmar na validação do Blueprint se `plan: free` é aceito para o banco.

## 18. Saída do modo gratuito

Gatilhos: aproximar-se do dia 30 do Postgres, necessidade de disponibilidade 24 h, ou primeiro restaurante real.

1. Postgres pago.
2. Core e node viram private services.
3. Key Value com persistência.
4. Gateway entra como único serviço público (módulo `gateway`, seção 10).
5. Domínio próprio com `app.` e `api.`; refresh token em cookie HttpOnly (seção 4).
6. Cardápio público passa a ser servido pelo Node, se fizer sentido.

Nenhum código de negócio muda (apenas configuração, o gateway e o cookie).

## 19. Portabilidade para a AWS

- Específico do Render: apenas o `render.yaml` e o job `deploy`.
- Imagens Docker, variáveis de ambiente, `PORT`, health checks e Flyway migram sem alteração.
- Registry pode continuar no GHCR ou ir para o ECR; o job `deploy` troca o hook por `aws ecs update-service` ou equivalente.
- Redis: ElastiCache (ou equivalente).

## 20. Adiado

- Pagamento e financeiro (a comanda fica `FECHADA` com o total calculado).
- Pedido pelo cliente e sessão de mesa.
- Cancelamento por item.
- `If-Match` nas atualizações.
- Integrações WhatsApp e iFood.
- Dashboard além do básico.
- Gateway (fase paga).
- SonarCloud.

## 21. Definição de pronto da base

**back-java**
- `core` sobe; `/actuator/health` responde 200.
- Teste de fumaça; IT com Testcontainers rodando a migration `V1` (com RLS); teste do Modulith passando; **teste de isolamento entre tenants** passando.
- Geração das interfaces a partir de `api/openapi.yaml` funcionando; Spectral e oasdiff no CI.
- CI verde; Dockerfile `linux/amd64`; README; Dependabot ou Renovate configurado.

**back-node**
- Servidor sobe; `/health` responde 200.
- Teste de fumaça; validação de configuração falha corretamente quando falta variável.
- CI verde; Dockerfile; README; Dependabot ou Renovate configurado.

**Deploy**
- Job `image` publicando no GHCR; job `deploy` com hook; `render.yaml` validado.
- Deploy de ponta a ponta com o `/actuator/health` do core respondendo e o IP do cliente resolvido corretamente atrás do proxy.
- Checklist do primeiro deploy: (1) teste do `X-Forwarded-For` com header forjado e calibragem do N; (2) teste de `CREATE ROLE` no Postgres do Render; (3) confirmar nos logs que o AOT cache foi aceito.

## 22. Registro de mudanças de decisão

| Antes | Depois | Motivo |
|---|---|---|
| Spring Boot 4.0.x | 4.1.x | 4.0 sai do suporte em 2026-12-31 |
| Java 21 | Java 25 (LTS) | Suporte do Boot 4.1; AOT cache para reduzir cold start |
| RabbitMQ | Redis Streams | RabbitMQ no Render exige serviço pago com disco |
| Monorepo | Repositórios separados | Preferência do projeto |
| Gateway + core no `back-java` | Só `core` agora; gateway na fase paga | Core e node públicos; economiza horas gratuitas; gateway não protege nada a mais |
| code-first (springdoc) | contract-first (`openapi.yaml`) | Contrato é a fronteira entre três repositórios |
| Dinheiro em centavos inteiros | String decimal | Decisão do projeto |
| `@TenantId` como proteção | RLS como garantia, `@TenantId` como apoio | `@TenantId` também tem limitação em busca por chave primária |
| Room por garçom | Rooms `copa` e `salao` | Room por garçom é frágil |
| Eventos atrasam se o Redis reinicia | Eventos se perdem; front recarrega por REST | Key Value gratuito é só memória |
| Cancelamento por item | Cancelamento do pedido inteiro | Status só no pedido; simplicidade |
| Refresh token em cookie | Corpo na fase gratuita, cookie com domínio próprio depois | `onrender.com` está na Public Suffix List |
| IP do cliente: primeiro item do `X-Forwarded-For` | Posição N a partir da direita, calibrada por teste | A Cloudflare acrescenta ao header enviado pelo cliente; o primeiro item pode ser forjado |
| `openApiNullable=false` como saída | `openApiNullable=true` com `jackson-databind-nullable` 0.2.10 ou superior | O módulo Jackson 3 existe desde a 0.2.10 |
| Envelope com `versao` | `schemaVersion` no envelope e `pedidoVersion` no payload | Evitar colisão de nomes e permitir descartar evento fora de ordem |

## 23. Pendências e itens não verificados

Verificados nesta fase: springdoc-openapi 3.x suporta Boot 4; `openapi-generator` 7.16+ tem `useSpringBoot4` e `useJackson3`; especificações do plano `free` do Render; `onrender.com` na Public Suffix List; `X-Forwarded-For` com o IP do cliente em primeiro; AOT cache documentado pelo Spring Boot para Java 25.

A validar durante a implementação:

- Versão do Spring Modulith compatível com o Boot 4.
- Suporte do Spotless/google-java-format e disponibilidade da imagem Temurin 25.
- Artefato e versão do Bucket4j compatíveis com o Boot 4.
- Se o Hibernate 7 corrigiu a filtragem por chave primária no `@TenantId`.
- Se o gerador deriva o prefixo `/api/v1` do `servers`; registro do `JsonNullableJackson3Module` como bean (teste de PATCH).
- Cotas do GitHub Packages para imagens privadas no plano usado.
- Codificação exata do `imgURL` no deploy hook.
- `plan: free` para o banco no Blueprint.
- Nome do módulo `spring-boot-docker-compose` no Boot 4.
- Comportamento real do Render com o ping do Socket.IO (documentado como tráfego; não testado).
- Ganho real do AOT cache em 0,1 CPU (benchmark de terceiros mostrou 3,9 s para 2,0 s em outra máquina).

### Decisões a definir depois

1. **Login e tenant**: login com slug no corpo (`LoginRequest.restaurante`, com `login` único por tenant, e o front enviando o slug de uma variável de ambiente na fase de um restaurante) ou outra forma de resolver o tenant antes do login (por exemplo, subdomínio).
2. **Fluxos sem tenant e RLS**: proposta de três categorias de tabela, em vez de `SECURITY DEFINER` ou `BYPASSRLS`: (a) com RLS, todo dado de negócio; (b) registro sem RLS, `restaurante` (id, slug, nome, ativo), que resolve o tenant pelo slug; (c) técnicas sem RLS, `refresh_token` (busca por hash), `outbox` e `idempotency_key` (guarda `request_hash` e `pedido_id`, não o corpo da resposta; chave única `(restaurante_id, key)`), nunca expostas por listagem. Jobs só tocam tabelas técnicas; job com dado de negócio itera os tenants do registro, cada um em sua transação. Teste-guarda para a lista de exceções.
3. **Duração da janela de tolerância do refresh token** (sugestão: 10 a 30 s).

Até a decisão, o modelo de dados assume as propostas 1 e 2 como hipótese de trabalho.

Pendências de produto: forma de pagamento (adiado); modelo de dados (MER) em análise.
