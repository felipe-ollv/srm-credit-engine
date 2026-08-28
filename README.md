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

As APIs atuais permitem simular pricing, cadastrar e consultar cedentes e recebíveis, consultar ou atualizar a cotação, liquidar recebíveis em lote e consultar o extrato. Todas usam Bearer JWT; valores monetários são representados como strings decimais no JSON.

- `GET /api/v1/exchange-rates/current` aceita `OPERATOR` e `ADMIN`.
- `POST /api/v1/exchange-rates/refresh` aceita somente `ADMIN`.
- `POST /api/v1/settlement-batches` aceita `OPERATOR` e `ADMIN`, exige `Idempotency-Key` e processa de 1 a 100 itens.
- `GET /api/v1/settlements` aceita `OPERATOR` e `ADMIN`, com filtros por período, cedente e moeda, paginação server-side e ordenação controlada.
- O Compose inicia o WireMock e captura uma cotação USD/BRL no startup da API. O pricing em USD consulta exclusivamente snapshots persistidos com até 24 horas.

Na liquidação, o backend recalcula o pricing com um único instante e um único snapshot USD/BRL por lote. Cada item executa em transação independente: sucessos permanecem confirmados mesmo quando outro item falha. Repetir a mesma chave com o mesmo payload devolve a resposta persistida; reutilizá-la com payload diferente retorna `409`.

No extrato, `from` e `to` são datas inclusivas na zona `America/Sao_Paulo`. O parâmetro `sort` aceita `settledAt`, `assignorLegalName` ou `paymentAmount`, seguido de `asc` ou `desc`; o padrão é `settledAt,desc`.

O Swagger exige um Bearer token válido, assim como as rotas de negócio.

### Fluxos da aplicação web

- **Simulação:** calcula automaticamente após a validação do formulário, sem enviar taxa, spread ou câmbio como entrada.
- **Cedentes e recebíveis:** cadastra e consulta os dados operacionais, incluindo validação local de CNPJ, valor e vencimento.
- **Liquidação:** seleciona até 100 recebíveis disponíveis e define BRL ou USD por item. Uma falha preserva a chave idempotente para repetição segura da mesma tentativa.
- **Extrato:** consulta snapshots imutáveis com filtros e paginação executados no servidor.
- **Câmbio:** aparece somente para `ADMIN` e permite capturar um novo snapshot USD/BRL do provedor mockado.

O frontend usa contratos TypeScript manuais e facades por feature. Tokens permanecem somente na memória do adapter Keycloak; nenhum resultado de pricing é calculado ou aceito como autoridade no navegador.

### Usuários de demonstração

| Usuário | Senha | Perfil |
|---|---|---|
| `operator` | `operator123` | `OPERATOR` |
| `admin` | `admin123` | `ADMIN` |
| `viewer` | `viewer123` | sem permissão operacional |

Essas credenciais existem somente no realm local importado pelo Docker Compose. Não devem ser reutilizadas em outros ambientes. O administrador do Keycloak local usa `admin/admin`.

O realm também contém o client confidencial `srm-credit-engine-load-test`, com perfil `OPERATOR`, exclusivamente para os testes k6 locais. Seu secret de demonstração não deve ser promovido para outro ambiente.

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

O E2E pressupõe que o Compose esteja em execução e usa Chromium para validar login, autorização, responsividade, golden cases financeiros, cadastro, liquidação, extrato e atualização cambial pela interface real.

## Observabilidade

No Compose, a API escreve logs JSON no formato Logstash. Cada término de requisição registra somente método, path sem query string, status, duração, correlation ID e, quando válido, `Idempotency-Key`. Corpo, valores financeiros, token e demais headers não são registrados.

As métricas disponíveis incluem:

- `pricing.simulation.duration`, por resultado e moeda;
- `settlement.batch.duration`, por resultado global;
- `settlement.items.total`, por resultado individual e moeda;
- `fx.refresh.total`, por resultado.

`/actuator/health` e os probes permanecem públicos. `/actuator/prometheus` exige Bearer JWT com perfil `ADMIN`; use um token administrativo válido no header `Authorization` para consultar o endpoint.

## Teste de carga

Com o Compose saudável e [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) instalado:

```bash
k6 run performance/pricing-simulation.js
k6 run performance/settlement-batch.js
```

O primeiro cenário sustenta 50 simulações/s por 30 segundos e exige p95 de até 100 ms. O segundo cria deterministicamente um cedente e 100 recebíveis, liquida todo o lote em uma chamada e exige p95 de até 2 segundos. Ambos rejeitam falhas HTTP. `RATE`, `DURATION`, `API_BASE_URL`, `KEYCLOAK_URL`, `CLIENT_ID` e `CLIENT_SECRET` podem ser sobrescritos por ambiente.

## Integração contínua e fluxo Git

O workflow [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) executa três gates: Maven com Java 21 e PostgreSQL/Testcontainers; lint, testes e build Angular com Node 24; e Compose completo com Playwright/Chromium. Logs dos containers são publicados no job quando o E2E falha.

O desenvolvimento segue GitHub Flow:

1. atualizar `main` e criar uma branch curta por objetivo (`feat/...`, `fix/...`, `docs/...`);
2. registrar commits pequenos em [Conventional Commits](https://www.conventionalcommits.org/), por exemplo `feat(settlements): add batch metrics`;
3. abrir PR com motivação, riscos, migrations afetadas e evidências dos comandos de validação;
4. exigir CI verde e revisão antes do merge;
5. criar a próxima branch somente a partir da `main` já atualizada.

## Decisões e arquitetura

- [`SPEC.md`](./SPEC.md): premissas financeiras e semântica funcional.
- [`DECISIONS.md`](./DECISIONS.md): escolhas, simplificações e gatilhos de evolução.
- [`src/docs/architecture.md`](./src/docs/architecture.md): diagramas C4 e limites modulares.
- [`AI_USAGE.md`](./AI_USAGE.md): registro vivo da colaboração com IA.
