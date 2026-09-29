# ☕ QuickOrder — Back-end

Back-end principal da aplicação, responsável pelas **regras de negócio, autenticação, persistência e operações centrais do restaurante**.

## 🛠️ Tecnologias

* Java 17
* Spring Boot
* Spring Security
* JPA/Hibernate
* PostgreSQL

## 🚀 Execução local

```bash
git clone <URL_DO_REPOSITORIO>
cd back_JAVA
./mvnw spring-boot:run
```

Configure as variáveis de ambiente necessárias utilizando o `.env.example`.

## 🌿 Branches

A `main` contém apenas versões estáveis.

Para iniciar uma tarefa:

```bash
git checkout main
git pull origin main
git checkout -b feature/nome-da-tarefa
```

Exemplos:

```text
feature/user-authentication
feature/order-management
feature/table-management
feature/payment
```

Tipos:

* `feature/` — nova funcionalidade
* `fix/` — correção
* `refactor/` — refatoração
* `docs/` — documentação

Ao finalizar:

```bash
git add .
git commit -m "feat: implement order creation"
git push origin feature/order-management
```

Abra um **Pull Request para `main`**.

## 📝 Commits

```text
tipo: descrição
```

Principais tipos:

* `feat`
* `fix`
* `refactor`
* `test`
* `docs`
* `style`
* `build`
* `perf`

Exemplo:

```text
feat: implement order creation
```

## 📂 Estrutura

```text
src/
├── controller/
├── service/
├── repository/
├── dto/
├── entity/
├── security/
└── config/
```

## ⚠️ Regras

* Não realizar `push` diretamente na `main`.
* Não versionar credenciais ou `.env`.
* Alterações no banco devem ser documentadas.
* Regras de negócio devem permanecer no back-end.
* Testar os endpoints antes de abrir o PR.
