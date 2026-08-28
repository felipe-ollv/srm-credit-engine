# Uso de IA no desenvolvimento

> Documento vivo — última atualização: 28/08/2026.

## 1. Como a IA foi utilizada

O Codex foi utilizado como ferramenta de apoio para explorar alternativas, estruturar especificações e executar tarefas delimitadas. A saída da IA não foi tratada como fonte de verdade nem aceita automaticamente.

O fluxo adotado foi:

1. delimitar o objetivo e as restrições da tarefa;
2. solicitar análise ou geração à IA;
3. confrontar a saída com o enunciado e com as decisões do domínio;
4. corrigir ou rejeitar propostas inadequadas;
5. aprovar explicitamente o resultado antes de incorporá-lo ao projeto.

Decisões financeiras, critérios de aceite e aprovação final permaneceram sob responsabilidade humana.

## 2. Interações estratégicas

Este registro resume as interações relevantes; ele não reproduz o histórico completo das conversas.

| Etapa | Objetivo e restrições fornecidos à IA | Resultado aproveitado | Validação humana |
|---|---|---|---|
| Scaffold inicial | Auxiliar na criação da base Java/Spring do projeto. A participação da IA ficou restrita à stack; o diagrama inicial não foi atribuído à IA. | Estrutura mínima com Maven, Java 21 e aplicação Spring Boot. | Inspeção do `pom.xml`, da classe principal e da estrutura do repositório. A base ainda não foi considerada pronta para entrega. |
| Análise do desafio | Interpretar o enunciado para uma candidatura sênior, considerando monólito modular, arquitetura hexagonal e SOLID. | Levantamento inicial dos requisitos de precisão, idempotência, concorrência, observabilidade e resiliência. | Comparação direta com as fases e com a rubrica do documento do desafio. |
| Especificação funcional | Estruturar as ambiguidades antes do código e apresentar escolhas com seus impactos. | Organização das premissas de prazo, taxa base, câmbio, precisão decimal, liquidação em lote e idempotência. | As alternativas foram decididas pelo candidato, revisadas em conjunto e aprovadas antes da geração do arquivo. |
| Geração do SPEC | Materializar somente as decisões aprovadas, em português e sem iniciar a implementação. | Criação do [`SPEC.md`](./SPEC.md) como fonte de verdade funcional do v1. | Leitura integral do documento e confirmação explícita antes de avançar para arquitetura ou código. |
| Domínio e motor de pricing | Implementar o plano aprovado com domínio puro, Strategy por tipo de recebível, fórmula única no `DiscountCalculator`, `DECIMAL128` e `HALF_EVEN`, sem JPA ou REST. | Shared kernel tipado, aggregates de recebível e liquidação, `PricingEngine`, `DuplicataPricingStrategy` e `PostDatedCheckPricingStrategy`. | Testes unitários das invariantes, verificação de fronteiras com Spring Modulith e aferição automatizada dos golden cases C1, C2 e C3. Os nomes finais das estratégias e a separação do calculator foram escolhidos pelo candidato antes da implementação. |
| Exposição da simulação | Implementar a primeira fatia vertical em `POST /api/v1/pricing/simulations`, sem expor taxa, spread ou câmbio como entrada; proteger com JWT; usar valores financeiros textuais; padronizar erros e correlação. | Caso de uso e porta cambial, adapter configurado temporário, controller e DTOs internos, Resource Server, CORS explícito, Problem Details, métrica e documentação OpenAPI. | Testes com `Clock` fixo, golden cases pela rota HTTP, perfis `OPERATOR` e `ADMIN`, autenticação, CORS, correlação, indisponibilidade cambial, contrato OpenAPI e PostgreSQL real via Testcontainers. Uma coerção indevida de número JSON para texto foi rejeitada e corrigida antes da conclusão. |
| Frontend e autenticação local | Implementar somente a tela correspondente ao contrato de simulação já existente, com Angular 21, tipagem estrita, Keycloak real, tokens somente em memória e sem criar telas fictícias para contratos futuros. | SPA responsiva em `frontend/`, formulário reativo, facade com debounce e cancelamento, integração HTTP tipada, autorização por perfil, realm Keycloak de demonstração e execução integrada pelo Docker Compose. | Lint, testes unitários e de componente, build de produção e Playwright contra API e Keycloak reais. Os E2E aferiram login, logout, autorização, responsividade e os golden cases C1, C2 e C3. |
| Cadastro persistente | Implementar cedentes e recebíveis como primeira branch sequencial, mantendo domínio sem Spring, entidades JPA nos adapters, CNPJ verificável, valores financeiros textuais e paginação sem expor tipos do framework. | Migration incremental, APIs protegidas, contratos de aplicação, adapters PostgreSQL e Problem Details compartilhado. | Testes de CNPJ e aggregates, PostgreSQL real via Testcontainers, validação do OpenAPI e `ApplicationModules.verify()`. |
| Currency engine | Substituir o câmbio configurado por snapshots imutáveis no PostgreSQL, integrar um provedor HTTP mockado, aplicar timeout/retry e limitar o refresh a `ADMIN`. | Módulo `currency` hexagonal, migration V3, adapter HTTP, WireMock no Compose, APIs de consulta/refresh e integração do pricing à API pública de câmbio. | Testes unitários de vigência e retry, timeout HTTP reproduzível, PostgreSQL real via Testcontainers, autorização/OpenAPI, Compose saudável e consulta direta ao histórico Flyway e ao snapshot persistido. |
| Liquidação em lote | Implementar lotes de 1–100 itens com um instante e um snapshot cambial, transação `REQUIRES_NEW` por item, idempotência persistida e conflito concorrente sem aceitar valores financeiros do cliente. | Migration V4, API `POST /api/v1/settlement-batches`, snapshots cadastral e financeiro imutáveis, hash ordenado do payload, replay da resposta persistida e integração pelas APIs públicas dos módulos. | PostgreSQL real via Testcontainers, teste de rollback forçado, duas liquidações simultâneas, lote misto BRL/USD, ausência cambial, payload divergente, chave em processamento, limites do envelope e `ApplicationModules.verify()`. |
| Extrato analítico | Implementar a consulta paginada sem carregar aggregates JPA, com filtros combináveis, valores financeiros textuais e ordenação limitada a campos conhecidos. | Módulo `reporting`, API `GET /api/v1/settlements`, read model completo via `JdbcClient`, datas inclusivas de São Paulo e migration V5 com índices dedicados. | Testes HTTP de filtros isolados e combinados, paginação, ordenação, segurança e limites; `EXPLAIN` em PostgreSQL real confirmou o índice composto e `ApplicationModules.verify()` protegeu a independência do módulo. |

## 3. Erros e correções

### 3.1 Erro de processo já observado

Durante o planejamento, a IA começou a avançar para decisões de arquitetura e implementação antes de concluir a Fase 0 exigida pelo desafio.

- **Problema:** a ordem proposta diluía o objetivo de resolver primeiro as ambiguidades financeiras no `SPEC.md`.
- **Risco:** decisões técnicas poderiam cristalizar premissas de negócio ainda não aprovadas e gerar retrabalho.
- **Detecção:** revisão humana do fluxo contra a seção 3 do enunciado. O candidato interrompeu a atividade e determinou que somente o SPEC fosse definido naquele momento.
- **Correção:** o trabalho retornou à especificação; prazo, taxa base, vigência cambial, arredondamento, idempotência e semântica do lote foram decididos antes de qualquer implementação.
- **Evidência:** [`SPEC.md`](./SPEC.md) e o enunciado em `src/docs/desafio-tecnico-srm-credit-engine-v2 (2).md`.

Esse caso demonstra uma correção real do processo de colaboração, mas não será apresentado como substituto do erro técnico verificável solicitado pelo desafio.

### 3.2 Caso técnico verificável

**Status: verificado por teste em 26/08/2026.**

Ao gerar o skeleton solicitado para Spring Boot 4.1.1, PostgreSQL, Docker Compose, Flyway e OpenAPI, a IA adicionou `flyway-core` e o módulo PostgreSQL, seguindo a configuração comum das versões anteriores do Spring Boot.

- **Erro:** no Spring Boot 4, a auto-configuração do Flyway foi movida para um módulo próprio. Apenas `flyway-core` no classpath não registrou `FlywayAutoConfiguration`, portanto nenhuma migration foi executada.
- **Impacto potencial:** a aplicação poderia iniciar conectada a um banco com schema vazio ou desatualizado. Em produção, isso causaria falhas tardias de persistência e quebraria a premissa de que migrations são aplicadas antes do acesso JPA.
- **Detecção:** o teste de contexto iniciou PostgreSQL 17 real com Testcontainers e consultou `flyway_schema_history`. A execução falhou com `relation "flyway_schema_history" does not exist`.
- **Correção:** `flyway-core` foi substituído por `spring-boot-starter-flyway`, mantendo `flyway-database-postgresql`. O teste foi repetido e confirmou a aplicação da migration `V1__baseline.sql` antes da inicialização do JPA.
- **Evidências:** [`pom.xml`](./pom.xml), [`SrmApplicationTests.java`](./src/test/java/com/credit/engine/srm/SrmApplicationTests.java) e [`V1__baseline.sql`](./src/main/resources/db/migration/V1__baseline.sql).

O mesmo ciclo de compilação também detectou que Testcontainers 2 renomeou os artifacts Maven para `testcontainers-junit-jupiter` e `testcontainers-postgresql`. As coordenadas foram corrigidas antes de a alteração ser considerada concluída.

### 3.3 Borda financeira detectada na revisão do domínio

Na primeira implementação do motor de pricing, a IA exigiu que o valor presente monetário fosse estritamente positivo.

- **Erro:** a invariante ignorava que, sem um teto artificial para `InterestRate`, um valor matematicamente positivo pode legitimamente arredondar para `0,00` após 360 períodos.
- **Impacto potencial:** uma precificação válida seria rejeitada depois do cálculo, apesar de taxa e prazo atenderem às regras aprovadas.
- **Detecção:** revisão das invariantes contra a decisão de aceitar qualquer taxa não negativa, seguida de um teste reproduzível com face de `R$ 0,01`, taxa mensal de `100%` e prazo de 360 meses.
- **Correção:** `DiscountCalculation` e `PricingResult` passaram a exigir valor presente não negativo, preservando a exigência de face estritamente positiva.
- **Evidências:** [`DiscountCalculator.java`](./src/main/java/com/credit/engine/srm/pricing/internal/DiscountCalculator.java), [`PricingResult.java`](./src/main/java/com/credit/engine/srm/pricing/PricingResult.java) e [`DiscountCalculatorTest.java`](./src/test/java/com/credit/engine/srm/pricing/internal/DiscountCalculatorTest.java).

### 3.4 Coerção indevida no contrato financeiro HTTP

Na primeira versão do DTO da simulação, a IA declarou `faceValue` como `String` e aplicou validação por expressão regular, mas não desabilitou a coerção padrão do Jackson.

- **Erro:** o JSON `"faceValue": 100000.00`, sem aspas no valor, era convertido silenciosamente para texto e aceito com `200 OK`, embora o contrato exija uma string decimal.
- **Impacto potencial:** clientes poderiam depender de um formato não documentado e transmitir valores financeiros como números JSON, sujeitos à representação numérica da linguagem cliente.
- **Detecção:** um teste HTTP enviou o valor sem aspas e esperou `400 REQUEST_INVALID`; antes da correção, o MockMvc recebeu `200 OK` e uma simulação calculada.
- **Correção:** foi adicionado um desserializador local que aceita exclusivamente o token JSON textual. O teste foi repetido e confirmou `400`; C1, C2 e C3 permaneceram verdes.
- **Evidências:** [`PricingSimulationRequestDto.java`](./src/main/java/com/credit/engine/srm/pricing/internal/adapter/in/web/PricingSimulationRequestDto.java), [`StrictStringDeserializer.java`](./src/main/java/com/credit/engine/srm/config/web/StrictStringDeserializer.java) e [`SrmApplicationTests.java`](./src/test/java/com/credit/engine/srm/SrmApplicationTests.java).

### 3.5 Realm local incompleto detectado pelo E2E

Na primeira composição do realm de demonstração, a IA criou os usuários locais sem preencher os atributos de e-mail esperados pelo fluxo inicial do Keycloak.

- **Erro:** após autenticar, o Keycloak interrompia o redirecionamento para a SPA e exibia a etapa obrigatória `Update Account Information`.
- **Impacto potencial:** nenhum perfil de demonstração conseguia concluir automaticamente o login usado nos testes e na avaliação local.
- **Detecção:** o primeiro teste Playwright não encontrou a aplicação após enviar as credenciais; a captura da página mostrou o formulário adicional do Keycloak.
- **Correção:** os usuários do realm receberam e-mails de demonstração e o import foi recriado antes da repetição do E2E.
- **Evidências:** [`srm-credit-engine-realm.json`](./infra/keycloak/srm-credit-engine-realm.json) e [`pricing.spec.ts`](./frontend/e2e/pricing.spec.ts).

O mesmo ciclo revelou que a política padrão `sslRequired=external` recusava o discovery OIDC no ambiente HTTP local com `HTTPS required`. Como o Compose usa exclusivamente o modo de desenvolvimento, o realm foi limitado a `sslRequired=none`; produção permanece fora desse perfil. A correção foi validada pelo discovery OIDC e pelos fluxos reais de login do Playwright.

### 3.6 Healthcheck do frontend inconsistente com o listener

Na validação final do Compose, a aplicação respondia pela porta publicada e todos os testes Playwright passavam, mas o container do frontend permanecia em `health: starting`.

- **Erro:** o healthcheck gerado pela IA consultava `localhost:8080`, resolvido no container para um endereço no qual o Nginx não estava ouvindo.
- **Impacto potencial:** orquestradores e dependências poderiam considerar indisponível uma aplicação funcional.
- **Detecção:** `docker compose ps` mostrou o estado inconclusivo e `docker inspect` registrou tentativas repetidas com `Connection refused`.
- **Correção:** o probe passou a consultar explicitamente `127.0.0.1:8080` e o container foi recriado.
- **Evidência:** [`compose.yaml`](./compose.yaml).

### 3.7 Fronteira web não exposta no Modulith

Ao extrair correlação, Problem Details e desserialização estrita para uma infraestrutura web compartilhada, a IA permitiu inicialmente que `pricing` e `receivables` dependessem diretamente de tipos internos do módulo `config`.

- **Erro:** declarar `config` como dependência permitida não torna automaticamente seus subpackages uma API pública do módulo.
- **Impacto potencial:** a solução compilava, mas enfraquecia o encapsulamento e impedia a verificação estrutural da arquitetura.
- **Detecção:** `ApplicationModules.verify()` listou cada acesso aos tipos não expostos.
- **Correção:** `config.web` passou a ser uma named interface explícita e os módulos consumidores foram limitados a `config :: web`.
- **Evidências:** [`package-info.java`](./src/main/java/com/credit/engine/srm/config/web/package-info.java), [`ModuleStructureTest.java`](./src/test/java/com/credit/engine/srm/ModuleStructureTest.java) e os `package-info.java` de `pricing` e `receivables`.

### 3.8 Fixture cambial sem tipo temporal explícito

Na primeira versão do teste integrado do currency engine, a IA inseriu o snapshot com `JdbcTemplate` passando objetos `Instant` diretamente aos parâmetros `TIMESTAMPTZ`.

- **Erro:** o driver PostgreSQL não conseguiu inferir o tipo SQL de `Instant` no statement JDBC cru.
- **Impacto potencial:** os 13 cenários integrados falharam durante a preparação do fixture, impedindo validar migration, segurança, APIs e golden cases, embora o código de produção já compilasse.
- **Detecção:** `./mvnw test` com Testcontainers aplicou a V3 e então retornou `Não pode inferir um tipo SQL a ser usado para uma instância de java.time.Instant` em `seedCurrentExchangeRate`.
- **Correção:** o fixture passou a converter explicitamente os instantes para `OffsetDateTime` em UTC antes do bind. A suíte integrada foi repetida e os 13 cenários passaram.
- **Evidências:** [`SrmApplicationTests.java`](./src/test/java/com/credit/engine/srm/SrmApplicationTests.java) e [`V3__exchange_rates.sql`](./src/main/resources/db/migration/V3__exchange_rates.sql).

### 3.9 Packages `adapter/out` ignorados pelo Git

Na revisão final da branch, a IA comparou os arquivos usados na compilação com o índice Git e detectou que adapters essenciais não apareciam no diff.

- **Erro:** a regra `out/` do `.gitignore`, destinada ao diretório de build da IDE, correspondia também a qualquer package hexagonal chamado `adapter/out`. Os adapters JPA de recebíveis já mergeados e os novos adapters de currency existiam localmente, mas permaneciam ignorados.
- **Impacto potencial:** o projeto passava nos testes da máquina de desenvolvimento, porém um clone limpo não teria implementações exigidas pelo wiring Spring e falharia na compilação.
- **Detecção:** `git check-ignore -v` apontou a regra e `git ls-tree -r HEAD` confirmou que nenhum arquivo sob `adapter/out` estava no commit mergeado.
- **Correção:** a regra foi limitada a `/out/`, representando apenas o diretório de build na raiz, e todos os adapters de saída necessários passaram a ser versionados. A suíte completa foi repetida depois da correção.
- **Evidências:** [`.gitignore`](./.gitignore), [`JpaReceivableRepositoryAdapter.java`](./src/main/java/com/credit/engine/srm/receivables/internal/adapter/out/persistence/JpaReceivableRepositoryAdapter.java) e [`JpaExchangeRateRepositoryAdapter.java`](./src/main/java/com/credit/engine/srm/currency/internal/adapter/out/persistence/JpaExchangeRateRepositoryAdapter.java).

### 3.10 Tipo físico incompatível no registro idempotente

Na primeira validação da migration de liquidação, a IA definiu `request_hash` como `CHAR(64)`, enquanto o mapeamento JPA declarava uma string variável de 64 caracteres.

- **Erro:** o schema físico e o contrato validado pelo Hibernate representavam o mesmo conteúdo com tipos SQL diferentes.
- **Impacto potencial:** a aplicação não iniciaria com `ddl-auto=validate`, impedindo o processamento e o replay idempotente.
- **Detecção:** o teste integrado aplicou a V4 em PostgreSQL 17 real e a validação do Hibernate reportou a incompatibilidade da coluna.
- **Correção:** a migration ainda não publicada foi ajustada para `VARCHAR(64)`, preservando o `CHECK` de SHA-256 hexadecimal e o limite de tamanho.
- **Evidências:** [`V4__settlement_batches.sql`](./src/main/resources/db/migration/V4__settlement_batches.sql), [`IdempotencyJpaEntity.java`](./src/main/java/com/credit/engine/srm/settlements/internal/adapter/out/persistence/IdempotencyJpaEntity.java) e [`SrmApplicationTests.java`](./src/test/java/com/credit/engine/srm/SrmApplicationTests.java).

### 3.11 Snapshot cambial aplicado indevidamente a item BRL

Na primeira execução de um lote misto, a IA repassou o snapshot USD/BRL do lote tanto ao item USD quanto ao item BRL.

- **Erro:** o aggregate de pricing rejeitou corretamente uma liquidação em BRL acompanhada de câmbio.
- **Impacto potencial:** um lote misto produziria falha no item BRL apesar de a cotação ser necessária somente para USD.
- **Detecção:** o teste HTTP do lote misto esperava sucesso nos dois itens e recebeu `RULE_VIOLATION` no item BRL.
- **Correção:** o snapshot continua sendo selecionado uma única vez, mas é aplicado somente aos comandos em USD; comandos BRL recebem câmbio ausente.
- **Evidências:** [`SettlementItemTransaction.java`](./src/main/java/com/credit/engine/srm/settlements/internal/application/SettlementItemTransaction.java) e [`SrmApplicationTests.java`](./src/test/java/com/credit/engine/srm/SrmApplicationTests.java).

## 4. O que não foi delegado

- A interpretação final das ambiguidades financeiras e a aceitação das premissas.
- A escolha da política de prazo, taxa base, vigência cambial e arredondamento.
- A semântica de sucesso parcial, idempotência e concorrência da liquidação em lote.
- A aprovação final do [`SPEC.md`](./SPEC.md).
- A aferição dos golden cases e a decisão de aceitar ou rejeitar o motor de cálculo.
- As decisões finais de integridade, segurança e operação.
- O code review, a organização dos commits e a responsabilidade pelo que será incorporado ao repositório.
- A preparação e a defesa técnica ao vivo.

Esses pontos exigem julgamento de domínio, responsabilização e capacidade de explicar as consequências das decisões; a IA pode oferecer alternativas ou apontar riscos, mas não assume sua aprovação.

## 5. Manutenção deste registro

- Adicionar uma entrada apenas quando uma saída da IA tiver sido utilizada, modificada ou explicitamente rejeitada.
- Atualizar a data do documento e apontar evidências reproduzíveis no repositório.
- Registrar erros com contexto e impacto, evitando descrições genéricas.
- Não incluir conversas completas, prompts irrelevantes ou afirmações sem evidência.
- Revalidar os casos técnicos referenciados antes da entrega final.
