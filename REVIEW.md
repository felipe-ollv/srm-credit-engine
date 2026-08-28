# Code review reverso — endpoint de liquidação do Anexo A

Revisão do trecho `settlement.controller.ts` fornecido no desafio. A ordem abaixo considera primeiro perda financeira, duplicidade e corrupção de dados; depois segurança, semântica financeira e operabilidade.

## Bloqueadores

### 1. Liquidação não é atômica

O `INSERT` da liquidação e o `UPDATE` do recebível são executados fora de uma transação. Se a segunda instrução falhar, a liquidação permanece registrada enquanto o recebível continua disponível. O `catch` ainda ignora deliberadamente o erro e responde sucesso.

- **Impacto:** pagamento duplicado, divergência contábil e resposta falsa ao operador.
- **Correção:** executar criação do registro imutável e transição do recebível na mesma transação ACID; propagar falhas e responder um status coerente. Para lotes com sucesso parcial, delimitar uma transação independente por item.
- **Prevenção:** teste de integração que force falha entre as duas escritas e confirme rollback completo; proibir `catch` vazio em lint/review.

### 2. Não há idempotência

Um retry de rede ou duplo clique executa novamente toda a operação. Não existe chave idempotente, hash do payload nem replay da resposta original.

- **Impacto:** duas liquidações e potencial pagamento em duplicidade.
- **Correção:** exigir `Idempotency-Key`, reivindicá-la atomicamente sob constraint única, associar um hash SHA-256 do request normalizado e persistir a resposta. Mesma chave e payload devem reproduzir a resposta; payload diferente deve retornar `409`.
- **Prevenção:** testes de replay, chave concorrente em processamento e reutilização com payload diferente.

### 3. Condição de corrida no recebível

Duas requisições podem ler o mesmo recebível disponível, calcular e inserir liquidações antes de qualquer atualização de status. O `UPDATE` não condiciona o estado anterior e não há versão otimista ou unicidade por recebível.

- **Impacto:** liquidações concorrentes do mesmo ativo.
- **Correção:** usar versão otimista no recebível, transição `AVAILABLE -> SETTLED` condicionada e constraint única em `settlements.receivable_id`. Traduzir o conflito para resultado explícito, sem esconder a disputa.
- **Prevenção:** teste concorrente real com duas transações e barreira de sincronização; manter a constraint como última linha de defesa.

### 4. SQL injection em todas as consultas

`receivableId`, `finalAmount` e `currency` são interpolados diretamente no SQL. Mesmo campos aparentemente tipados ou calculados não devem compor instruções por concatenação.

- **Impacto:** leitura ou alteração indevida do banco, corrupção de dados e comprometimento completo da aplicação.
- **Correção:** SQL parametrizado/prepared statements, validação de UUID e enum, usuário de banco com menor privilégio e mensagens de erro sem detalhes internos.
- **Prevenção:** regra estática contra SQL concatenado, testes de entrada hostil e revisão obrigatória de adapters de persistência.

## Severidade alta

### 5. Matemática financeira está em unidade incorreta e usa ponto flutuante

As constantes `1.0`, `1.5` e `2.5` são usadas diretamente em `1 + taxa + spread`, resultando em 100%, 150% e 250%, embora o enunciado defina 1%, 1,5% e 2,5%. JavaScript calcula com `number` binário e `toFixed(2)` apenas mascara parte da perda de precisão.

- **Impacto:** preço materialmente incorreto e divergências de centavos ou ordens de grandeza.
- **Correção:** representar taxas como decimais `0.01`, `0.015` e `0.025`; usar decimal arbitrário/decimal de banco; centralizar a fórmula e aplicar `HALF_EVEN` no ponto definido pelo SPEC.
- **Prevenção:** golden cases C1, C2 e C3 ao centavo, testes de borda e proibição de `float`/`double`/`number` no cálculo monetário autoritativo.

### 6. Prazo e elegibilidade são confiados ao registro sem validação de domínio

O código usa `receivable.term` sem demonstrar origem, unidade ou relação com a data de liquidação e o vencimento. Também não valida existência, status, valor positivo, vencimento futuro ou prazo máximo.

- **Impacto:** liquidação de ativo inexistente, vencido, já liquidado ou precificado com prazo diferente da política aprovada.
- **Correção:** carregar uma visão consistente do recebível, calcular meses-calendário iniciados na data de negócio e aplicar invariantes de 1 a 360 meses antes de qualquer escrita.
- **Prevenção:** domínio tipado e testes para vencimento presente/passado, fronteira de mês e prazo máximo.

### 7. Câmbio sem semântica temporal ou snapshot auditável

`getLatestRate("USD")` não informa par completo, vigência, idade máxima, instante de captura ou direção da cotação. O valor usado não é salvo na liquidação.

- **Impacto:** valor não reproduzível, uso de cotação vencida/invertida e impossibilidade de auditoria.
- **Correção:** selecionar snapshot USD/BRL vigente no instante do lote, validar idade máxima, converter o PV BRL já arredondado e persistir taxa, par e timestamps. Falta de taxa deve produzir indisponibilidade explícita, nunca fallback silencioso.
- **Prevenção:** testes de direção, expiração, indisponibilidade e lote misto usando um único snapshot.

### 8. Registro de liquidação não contém evidência suficiente

São persistidos apenas recebível, valor final e moeda. Faltam snapshots do cedente, face, PV BRL, deságio, taxas, prazo, câmbio e timestamps.

- **Impacto:** a operação não pode ser reconstruída quando cadastro ou políticas mudarem; conciliação e auditoria ficam comprometidas.
- **Correção:** tornar a liquidação imutável e salvar todo o contexto cadastral e financeiro efetivamente utilizado. Não oferecer update/delete; correções futuras devem ser compensatórias.
- **Prevenção:** constraints de coerência no banco e teste que leia o snapshot completo após liquidar.

## Severidade média

### 9. Contrato HTTP e validação são insuficientes

O endpoint retorna `200 {ok: true}` inclusive após falha e não distingue JSON inválido, regra de negócio, conflito ou indisponibilidade cambial. `currency` não possui allow-list e o recebível inexistente causará erro não tratado antes do `try`.

- **Impacto:** clientes não sabem se podem repetir, corrigir dados ou interromper a operação.
- **Correção:** DTO validado, enums estritos e Problem Details com códigos estáveis; usar `400`, `401`, `403`, `409`, `422` e `503` conforme a causa. Uma criação concluída pode responder `201`/`200`, mas nunca sucesso após exceção.
- **Prevenção:** testes de contrato/OpenAPI para todos os estados relevantes.

### 10. Segurança e autorização não aparecem no fluxo

Não há autenticação, autorização por perfil nem segregação da atualização cambial.

- **Impacto:** qualquer cliente alcançável poderia liquidar ativos ou escolher entradas inválidas.
- **Correção:** proteger a API com JWT validando assinatura, emissor, audiência e validade; permitir a operação somente aos perfis definidos e manter credenciais fora da aplicação.
- **Prevenção:** testes `401`, `403` e autorização positiva por perfil.

### 11. Ausência de observabilidade segura

Não há correlation ID, métrica, log estruturado ou evidência de duração/resultado. O `catch` destrói a única pista do erro.

- **Impacto:** incidentes de duplicidade ou divergência financeira não podem ser diagnosticados rapidamente.
- **Correção:** logs estruturados com correlação e chave idempotente, sem token ou valores financeiros; métricas de duração e resultado; alerta para conflitos e falhas cambiais.
- **Prevenção:** teste de ausência de dados sensíveis e dashboards/alertas baseados em códigos de resultado.

## Desenho corretivo resumido

O controller deve apenas validar o envelope e delegar a um caso de uso. O caso de uso reivindica a idempotência, captura um único instante e um snapshot cambial quando necessário, e processa cada item em transação própria. Dentro da transação, o servidor recarrega o recebível, recalcula o pricing com decimal exato, cria o snapshot imutável da liquidação e efetua a transição otimista do recebível. Constraints únicas protegem recebível e chave idempotente; a resposta final é persistida para replay.

Esse desenho está materializado nesta entrega em `POST /api/v1/settlement-batches`, nas migrations V2–V4 e nos testes integrados de replay, rollback, lote misto e concorrência.
