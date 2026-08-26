# Uso de IA no desenvolvimento

> Documento vivo — última atualização: 26/08/2026.

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
- Revisar e completar o caso técnico da seção 3.2 antes da entrega final.
