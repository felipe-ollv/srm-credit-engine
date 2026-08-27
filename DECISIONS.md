# Decisões e priorização da entrega

> Documento vivo — última atualização: 27/08/2026.
>
> Todas as decisões abaixo estão aceitas. O estado de implementação é registrado individualmente para não apresentar intenção como funcionalidade concluída.

## 1. Direção da entrega

O compromisso é atender aos requisitos acumulativos de **Sênior**: corretude financeira, frontend, robustez, concorrência, operação e capacidade de defesa. Os itens de Staff/Tech Lead não fazem parte do compromisso desta versão.

As escolhas priorizam um núcleo financeiro correto e auditável, executável localmente e simples de explicar e modificar durante a defesa técnica.

## 2. Decisões aceitas

| ID | Decisão | Estado | Motivação | Consequência aceita | Gatilho de evolução |
|---|---|---|---|---|---|
| D01 | Construir um monólito modular com Spring Modulith. | Implementada e verificada estruturalmente. | Preservar limites de negócio verificáveis sem introduzir a operação distribuída de microserviços em um domínio e volume ainda pequenos. | Uma única unidade de implantação e um único ponto de falha, compensados por menor complexidade operacional e transacional. | Módulos com escala, disponibilidade ou ciclos de entrega comprovadamente independentes. |
| D02 | Aplicar arquitetura hexagonal e SOLID dentro de cada módulo. | Implementada no módulo `pricing`; módulos futuros seguirão o mesmo limite. | Isolar regras financeiras de HTTP, banco e provedor cambial, facilitando testes e mudanças ao vivo. | Mais interfaces e mapeamentos nas fronteiras, usados somente quando existe uma dependência externa real. | Revisar abstrações que não protejam uma fronteira ou não simplifiquem testes e evolução. |
| D03 | Usar PostgreSQL como banco relacional. | Cedentes e recebíveis persistidos com Flyway/JPA; liquidação e reporting permanecem pendentes. | ACID, constraints, locking otimista e consultas analíticas são centrais para liquidação e auditoria. | Dependência do modelo relacional e de recursos do PostgreSQL; relatórios poderão usar SQL nativo. | Necessidade comprovada de particionamento, réplicas de leitura ou armazenamento especializado. |
| D04 | Manter API REST síncrona e transação independente por item do lote. | Pendente da fatia de liquidação em lote. | O sucesso parcial definido no `SPEC.md` exige isolamento por item e resposta imediata ao operador. | Lotes muito grandes não serão eficientes e cada item poderá ter resultado diferente. | Volume que exija processamento assíncrono, acompanhamento de jobs ou filas. |
| D05 | Delegar identidade ao Keycloak em Docker e proteger a API com JWT. | Implementada para a API e para o ambiente local. | Evitar implementar armazenamento de credenciais e emissão de tokens dentro do motor de crédito. | O ambiente local ganha um serviço adicional e a autenticação depende da disponibilidade do provedor de identidade. | Integração com o IdP corporativo ou requisitos de federação, MFA e segregação mais granular. |
| D06 | Adotar os perfis `OPERATOR` e `ADMIN`. | Implementada na simulação; permissões futuras dependem de suas rotas. | Demonstrar autorização mínima coerente com uma ferramenta de mesa de operações. | O modelo não representa alçadas, aprovação em quatro olhos ou permissões por cedente. | Regras reais de segregação de funções e aprovação fornecidas pelo negócio. |
| D07 | Criar frontend funcional e enxuto em Angular 21 LTS. | Parcial: simulação implementada; painel completo e grid aguardam contratos backend. | Atender painel e grid com tipagem forte e uma versão suportada, sem competir com o núcleo financeiro. | Não haverá design system próprio, SSR ou estado global complexo. | Crescimento da aplicação que justifique biblioteca visual, store global ou estratégia de renderização adicional. |
| D08 | Integrar com um provedor cambial HTTP mockado. | Pendente; a simulação usa um adapter configurado temporário. | Permitir testar vigência, timeout e retry de forma determinística, sem depender de credenciais ou disponibilidade externa. | A integração não comprova particularidades de uma fonte real, como PTAX, calendários e limites de consumo. | Escolha de um provedor oficial e definição contratual da cotação pelo negócio. |

O Angular 21 foi escolhido em vez do Angular 17 por permanecer em LTS até 2027, conforme a [política oficial de versões do Angular](https://angular.dev/reference/releases).

## 3. Cortes e simplificações deliberados

| Item adiado ou excluído | Justificativa | Consequência e evolução possível |
|---|---|---|
| ADRs extensos, design para um milhão de transações/minuto, proposta EDA e post-mortem do Anexo B | São expectativas de Staff e desviariam esforço da implementação e operação exigidas para Sênior. | As decisões essenciais permanecem neste documento; os artefatos poderão ser produzidos quando houver objetivo de escala ou prevenção sistêmica. |
| Microserviços, broker, outbox, Kubernetes e alta disponibilidade | O case não apresenta volume nem independência organizacional que paguem a complexidade distribuída. | O monólito mantém consistência local; métricas e limites de módulos fornecerão evidência para uma futura extração. |
| Moedas além de BRL/USD e novos tipos de recebível | Não são necessários para os fluxos e golden cases fornecidos. | Estratégias e objetos de valor devem permitir extensão sem alterar as regras existentes. |
| Importação de recebíveis em lote | A liquidação em lote já cobre a necessidade operacional priorizada; importar arquivos adicionaria validação e reconciliação próprias. | O cadastro permanece individual; uma futura importação deverá produzir resultados rastreáveis por item. |
| Reserva ou validade contratual de simulação | O v1 define simulações como informativas e recalcula tudo na liquidação. | O valor exibido pode mudar; uma evolução exigirá entidade de cotação, expiração e vínculo com a liquidação. |
| Cancelamento e estorno | Alterar liquidações conflita com a imutabilidade exigida e o negócio ainda não definiu a semântica de reversão. | Correções exigirão futuramente uma operação compensatória auditável, nunca edição do registro original. |
| Provedor cambial real | A fonte oficial, lado da cotação e política de feriados ainda são perguntas abertas. | O adapter HTTP será substituível quando o contrato de negócio estiver definido. |
| Design system, SSR e estado global no frontend | Painel e grid não exigem essas camadas para serem claros, responsivos e testáveis. | Componentes locais e serviços de estado serão promovidos apenas quando houver reutilização ou complexidade comprovada. |

## 4. Itens que não serão cortados

- Golden cases e testes de borda do motor de precificação.
- Precisão decimal, arredondamento half-even e conversão cambial na ordem especificada.
- Persistência ACID por item, idempotência e auditoria imutável.
- Locking otimista com teste concorrente reproduzível.
- Painel do operador e grid paginado com filtros server-side.
- Docker Compose com aplicação, PostgreSQL, Keycloak e provedor cambial mockado.
- Validação de entrada, Problem Details e OpenAPI.
- Logs estruturados, métricas de negócio e resiliência da integração cambial.
- CI executando testes e linter.
- Diagramas C4 nos níveis 1 e 2.
- `REVIEW.md`, `AI_USAGE.md`, README e histórico Git defensável.

## 5. Contrato de segurança assumido

- Todas as rotas de negócio exigem `Authorization: Bearer <JWT>` emitido pelo Keycloak.
- `OPERATOR` pode cadastrar cedentes e recebíveis, simular, liquidar e consultar extratos.
- `ADMIN` herda as permissões de operador e pode solicitar atualização cambial.
- Health e readiness são os únicos endpoints técnicos públicos; informações sensíveis não serão expostas neles.
- Criação de usuários, credenciais, recuperação de senha e MFA permanecem sob responsabilidade do Keycloak.
