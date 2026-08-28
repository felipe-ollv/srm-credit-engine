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
| Provedor cambial mockado (WireMock) | http://localhost:8082 |
| PostgreSQL | localhost:5432 |

As APIs atuais permitem simular pricing, cadastrar e consultar cedentes e recebíveis, consultar ou atualizar a cotação e liquidar recebíveis em lote. Todas usam Bearer JWT; valores monetários são representados como strings decimais no JSON.

- `GET /api/v1/exchange-rates/current` aceita `OPERATOR` e `ADMIN`.
- `POST /api/v1/exchange-rates/refresh` aceita somente `ADMIN`.
- `POST /api/v1/settlement-batches` aceita `OPERATOR` e `ADMIN`, exige `Idempotency-Key` e processa de 1 a 100 itens.
- O Compose inicia o WireMock e captura uma cotação USD/BRL no startup da API. O pricing em USD consulta exclusivamente snapshots persistidos com até 24 horas.

Na liquidação, o backend recalcula o pricing com um único instante e um único snapshot USD/BRL por lote. Cada item executa em transação independente: sucessos permanecem confirmados mesmo quando outro item falha. Repetir a mesma chave com o mesmo payload devolve a resposta persistida; reutilizá-la com payload diferente retorna `409`.

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
npm ci
npm start
```

O frontend carrega os endereços da API e do Keycloak por `config.json` antes de inicializar o Angular. No container, esse arquivo é gerado pelas variáveis `API_BASE_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM` e `KEYCLOAK_CLIENT_ID`.

A integração cambial usa `FX_PROVIDER_BASE_URL`, timeouts de conexão e leitura de 500 ms, três tentativas totais com backoff de 250 ms e vigência máxima de 24 horas. Esses valores podem ser sobrescritos pelas variáveis `FX_CONNECT_TIMEOUT`, `FX_READ_TIMEOUT`, `FX_MAX_ATTEMPTS`, `FX_RETRY_BACKOFF` e `FX_MAX_AGE`.

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
