# SRM Credit Engine — Especificação funcional v1

## 1. Objetivo e escopo

O SRM Credit Engine recebe recebíveis de cedentes, calcula seu valor presente e registra liquidações auditáveis em BRL ou USD. Esta especificação documenta as premissas adotadas para as ambiguidades do desafio; elas devem ser confirmadas com o negócio antes de um uso produtivo.

O v1 contempla autenticação e autorização via Keycloak/JWT, cadastro individual de cedentes e recebíveis, simulação individual, atualização de câmbio por provedor HTTP mockado, liquidação síncrona em lote e extrato paginado. Importação de recebíveis em lote, administração de identidades dentro da aplicação, estorno e moedas além de BRL/USD ficam fora do escopo.

## 2. Premissas financeiras

- Todo recebível tem valor de face em BRL e pagamento em BRL ou USD.
- Tipos suportados:
  - Duplicata Mercantil: spread de 1,50% a.m.;
  - Cheque Pré-datado: spread de 2,50% a.m.
- A taxa base inicial é 1,00% a.m., configurável por ambiente. O valor efetivamente usado é registrado na liquidação.
- A data de precificação é obtida do relógio do servidor. A data de negócio usa `America/Sao_Paulo`, enquanto timestamps são persistidos em UTC.
- O prazo é contado em meses-calendário iniciados: contam-se os meses completos entre a data de precificação e o vencimento e adiciona-se um mês quando houver fração. Só são aceitos vencimentos futuros, com prazo máximo de 360 meses.
- O valor presente em BRL é calculado por:

  `valor presente = valor de face / (1 + taxa base + spread) ^ prazo em meses`

- A cotação USD/BRL representa quantos BRL equivalem a 1 USD. No pagamento em USD, o valor presente em BRL já arredondado é dividido pela cotação; o deságio permanece em BRL.
- A liquidação usa a cotação mais recente cuja vigência seja anterior ou igual ao instante de processamento e cuja idade não exceda 24 horas.
- Todos os itens USD de um lote usam o mesmo snapshot cambial. A liquidação não consulta o provedor externo; utiliza apenas taxas válidas já persistidas.
- Se não houver cotação válida, itens USD falham sem impedir o processamento de itens BRL do mesmo lote.

## 3. Precisão numérica e arredondamento

- Valores monetários usam `BigDecimal` na aplicação e `NUMERIC(19,2)` no banco.
- Taxas e percentuais usam `BigDecimal` na aplicação e `NUMERIC(19,10)` no banco.
- Cálculos intermediários usam alta precisão decimal e nunca `float` ou `double`.
- O arredondamento é `HALF_EVEN` (banker's rounding), com duas casas decimais, somente no resultado monetário final.
- No cross-currency há duas etapas finais: primeiro arredonda-se o valor presente em BRL; depois da conversão, arredonda-se o valor em USD.
- O deságio corresponde ao valor de face menos o valor presente arredondado em BRL.

## 4. Comportamento funcional

### Autenticação e autorização

Todas as rotas de negócio exigem `Authorization: Bearer <JWT>` emitido pelo Keycloak. O perfil `OPERATOR` pode cadastrar, simular, liquidar e consultar extratos. O perfil `ADMIN` possui as mesmas permissões e também pode solicitar atualizações cambiais. Health e readiness são os únicos endpoints técnicos públicos; administração de usuários e credenciais permanece delegada ao Keycloak.

### Simulação

A simulação é individual, efêmera e não reserva taxa. A resposta informa valor presente em BRL, deságio em BRL, valor e moeda de pagamento, prazo, taxa base, spread, cotação aplicada e instante do cálculo. A liquidação sempre recalcula os valores no servidor e não aceita resultados financeiros calculados pelo cliente.

### Atualização cambial

Uma operação administrativa solicita a cotação a um provedor HTTP mockado, com timeout e retry limitados, e persiste a taxa com sua vigência e instante de captura. A indisponibilidade do provedor falha apenas a atualização; não invalida taxas já persistidas que ainda estejam dentro da vigência de 24 horas.

### Liquidação em lote

- Cada requisição exige o header `Idempotency-Key` e contém de 1 a 100 itens, cada um com `receivableId` e moeda de pagamento.
- Um mesmo `receivableId` repetido no lote torna o envelope ambíguo; a requisição inteira é rejeitada com `422 Unprocessable Entity` antes de qualquer efeito.
- JSON inválido, campos obrigatórios ausentes, lote vazio ou acima do limite também impedem qualquer processamento.
- Para um envelope válido, cada item é processado em transação própria. A resposta é `200 OK` e contém o resultado individual de todos os itens, permitindo sucesso parcial.
- A garantia ACID vale por item: uma falha nunca deixa uma liquidação ou a mudança de estado do recebível pela metade.
- A mesma chave com payload semanticamente idêntico reproduz integralmente a resposta original, inclusive as falhas, sem reprocessamento.
- A mesma chave com payload diferente retorna `409 Conflict`.
- Para retentar itens que falharam, o cliente envia apenas esses itens em nova requisição e com nova chave.
- Um recebível só pode ter uma liquidação confirmada. Em tentativas simultâneas com chaves diferentes, exatamente uma vence; as demais recebem conflito no resultado do item, sem efeitos parciais.
- Liquidações e seus snapshots financeiros são imutáveis. Não há operação de alteração, exclusão, cancelamento ou estorno no v1.

### Extrato

O extrato de liquidações é paginado no servidor e permite filtros combináveis por período, cedente e moeda de pagamento. Cada registro expõe os identificadores da liquidação e do recebível, cedente, valores de face, presente, deságio e pagamento, parâmetros financeiros efetivamente usados e timestamps.

## 5. Critérios de aceite

### Corretude e integridade

- Os golden cases C1, C2 e C3 devem ser reproduzidos exatamente ao centavo por testes automatizados.
- Devem existir testes para meses exatos, frações de mês, vencimento passado, prazo acima de 360 meses, arredondamento half-even e cross-currency.
- A ausência ou expiração da taxa de câmbio deve impedir apenas liquidações USD.
- Repetição idempotente deve devolver o resultado original; reutilização da chave com outro payload deve ser rejeitada.
- Um teste concorrente deve demonstrar que duas tentativas para o mesmo recebível não criam duas liquidações.
- Falhas de persistência devem comprovar rollback integral do item, e não deve existir API para modificar liquidações.

### Usabilidade e segurança

- A API deve possuir documentação OpenAPI com exemplos, validações e códigos de resposta.
- Erros devem seguir Problem Details, com código estável, mensagem segura e identificador de correlação.
- Entradas devem ter tamanho e formato validados; consultas SQL devem ser parametrizadas.
- Segredos devem vir do ambiente e logs não devem expor dados financeiros sensíveis.
- Tokens JWT devem ter assinatura, emissor, audiência e expiração validados; endpoints devem aplicar os perfis `OPERATOR` e `ADMIN` conforme sua responsabilidade.
- Credenciais, criação de usuários, recuperação de senha e MFA não pertencem ao motor de crédito e permanecem sob responsabilidade do Keycloak.

### Desempenho e operação

- Em ambiente Docker de referência, a simulação deve atingir p95 menor ou igual a 100 ms e um lote de 100 itens p95 menor ou igual a 2 segundos, sem falha técnica durante a aferição.
- A integração cambial deve demonstrar timeout e retry quando o provedor mockado estiver indisponível.
- Devem ser emitidos logs estruturados e, no mínimo, métricas de quantidade de itens por resultado/moeda e latência do motor de precificação.

## 6. Perguntas ao negócio

1. Qual convenção oficial de prazo deve substituir a premissa de meses iniciados?
2. Qual benchmark, origem e processo de aprovação definem a taxa base?
3. A cotação oficial deve ser PTAX, spot, de compra ou de venda? Existe spread cambial adicional?
4. Como tratar finais de semana, feriados e horários de corte? A validade de 24 horas deve variar por calendário ou moeda?
5. Uma simulação deverá reservar seus valores por algum período?
6. Como devem funcionar correções e estornos compensatórios sem alterar o registro original?
7. Quais moedas, tipos de recebível, volumes e limites financeiros reais são esperados?
8. Quais perfis, alçadas e regras de segregação de funções serão exigidos em produção?
