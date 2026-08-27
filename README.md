# SRM Credit Engine

Motor de precificação e liquidação de recebíveis em BRL e USD, estruturado como monólito modular com Spring Boot, Spring Modulith e Angular.

## Executar o ambiente completo

Pré-requisitos: Docker Desktop com Docker Compose v2.

```bash
docker compose up --build
```

Serviços locais:

| Serviço | URL |
|---|---|
| Frontend Angular | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| OpenAPI | http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:8081 |
| PostgreSQL | localhost:5432 |

O Swagger exige um Bearer token válido, assim como as rotas de negócio.

### Usuários de demonstração

| Usuário | Senha | Perfil |
|---|---|---|
| `operator` | `operator123` | `OPERATOR` |
| `admin` | `admin123` | `ADMIN` |
| `viewer` | `viewer123` | sem permissão operacional |

Essas credenciais existem somente no realm local importado pelo Docker Compose. Não devem ser reutilizadas em outros ambientes. O administrador do Keycloak local usa `admin/admin`.

## Desenvolvimento

Backend, com Java 21:

```bash
./mvnw test
./mvnw spring-boot:run
```

Frontend, com Node.js 20.19+, 22.12+ ou 24:

```bash
cd frontend
npm install
npm start
```

O frontend carrega os endereços da API e do Keycloak por `config.json` antes de inicializar o Angular. No container, esse arquivo é gerado pelas variáveis `API_BASE_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM` e `KEYCLOAK_CLIENT_ID`.

## Validação

```bash
./mvnw test
cd frontend
npm run lint
npm test
npm run build
npm run e2e
```

O E2E pressupõe que o Compose esteja em execução e usa Chromium para validar login, autorização, responsividade e os golden cases financeiros pela interface real.

## Decisões e arquitetura

- [`SPEC.md`](./SPEC.md): premissas financeiras e semântica funcional.
- [`DECISIONS.md`](./DECISIONS.md): escolhas, simplificações e gatilhos de evolução.
- [`src/docs/architecture.md`](./src/docs/architecture.md): diagramas C4 e limites modulares.
- [`AI_USAGE.md`](./AI_USAGE.md): registro vivo da colaboração com IA.
