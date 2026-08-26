# Arquitetura do SRM Credit Engine

Este documento apresenta a arquitetura planejada para a entrega Sênior. Os diagramas usam a notação C4 nos níveis de contexto e contêiner e detalham, em seguida, os módulos e as fronteiras hexagonais do backend.

## C4 — Nível 1: contexto do sistema

```mermaid
flowchart LR
    operator["Operador<br/>Simula, liquida e consulta extratos"]
    administrator["Administrador<br/>Atualiza taxas de câmbio"]
    credit_engine["SRM Credit Engine<br/>Precificação e liquidação auditável<br/>de recebíveis em BRL e USD"]
    keycloak["Keycloak<br/>Identidade, autenticação e perfis"]
    fx_provider["Provedor cambial mockado<br/>Cotações USD/BRL com vigência"]

    operator -->|"Opera recebíveis e liquidações"| credit_engine
    administrator -->|"Administra o câmbio"| credit_engine
    operator -->|"Autentica-se"| keycloak
    administrator -->|"Autentica-se"| keycloak
    credit_engine -->|"Valida identidade e autorização"| keycloak
    credit_engine -->|"Obtém novas cotações"| fx_provider
```

## C4 — Nível 2: contêineres

```mermaid
flowchart LR
    operator["Operador"]
    administrator["Administrador"]

    subgraph platform["SRM Credit Engine"]
        spa["Web App<br/>Angular 21 LTS<br/>Painel, simulação e extrato"]
        api["Backend API<br/>Java 21 + Spring Boot<br/>REST, OpenAPI, logs e métricas"]
        database[("PostgreSQL<br/>Dados transacionais, câmbio<br/>e trilha auditável")]
    end

    keycloak["Keycloak<br/>OIDC, JWT e perfis<br/>OPERATOR / ADMIN"]
    fx_provider["FX Provider Mock<br/>API HTTP USD/BRL"]

    operator -->|"HTTPS"| spa
    administrator -->|"HTTPS"| spa
    spa <-->|"Authorization Code + PKCE"| keycloak
    spa -->|"REST/JSON + Bearer JWT"| api
    api -->|"Valida issuer, audience e assinatura"| keycloak
    api -->|"JPA/JDBC em transações ACID"| database
    api -->|"HTTP com timeout e retry<br/>somente na atualização cambial"| fx_provider
```

### Responsabilidades dos contêineres

- O Angular mantém o token apenas durante a sessão, aplica guards por perfil e nunca calcula valores financeiros autoritativos.
- O backend recalcula todas as simulações e liquidações, valida o JWT e concentra as regras de negócio.
- O PostgreSQL assegura constraints, idempotência, locking otimista e registros imutáveis de liquidação.
- O Keycloak administra usuários e credenciais; o motor de crédito consome somente identidade e perfis.
- O provedor mockado é chamado no refresh de câmbio. A liquidação usa apenas taxas vigentes já persistidas.

## Backend — módulos e dependências permitidas

```mermaid
flowchart LR
    rest["Adapters de entrada<br/>Controllers REST + validação"]
    security["Adapter de segurança<br/>Spring Security + JWT"]

    subgraph backend["Monólito modular — Spring Modulith"]
        shared["Shared Kernel<br/>Money, moedas, câmbio<br/>tipos e IDs"]
        receivables["Receivables<br/>Cedentes, recebíveis e status"]
        pricing["Pricing<br/>Strategies, prazo, precisão<br/>e arredondamento"]
        currency["Currency<br/>Cotações, vigência e refresh"]
        settlements["Settlements<br/>Lote, idempotência, concorrência<br/>e auditoria"]
        reporting["Reporting<br/>Extrato paginado e filtros"]
    end

    postgres[("PostgreSQL")]
    fx_provider["FX Provider Mock"]

    security -->|"Autoriza chamadas"| rest
    rest -->|"Comandos e consultas"| receivables
    rest -->|"Simulação"| pricing
    rest -->|"Refresh e consulta"| currency
    rest -->|"Liquidação em lote"| settlements
    rest -->|"Extrato"| reporting

    settlements -->|"Consulta e marca como liquidado"| receivables
    settlements -->|"Calcula valores autoritativos"| pricing
    settlements -->|"Obtém snapshot vigente"| currency

    receivables -->|"Value objects estáveis"| shared
    pricing -->|"Value objects estáveis"| shared
    currency -->|"Value objects estáveis"| shared
    settlements -->|"Value objects estáveis"| shared

    receivables -->|"Persistence port + JPA adapter"| postgres
    currency -->|"Persistence port + JPA adapter"| postgres
    settlements -->|"Persistence port + JPA adapter"| postgres
    reporting -->|"Read port + SQL/JdbcClient"| postgres
    currency -->|"Provider port + HTTP adapter"| fx_provider
```

As setas entre módulos representam as únicas dependências síncronas permitidas. O `shared` é um shared kernel mínimo e estável, limitado a value objects financeiros, vocabulário comum e IDs tipados; ele não contém regra de orquestração nem depende dos módulos de negócio. Cada módulo expõe sua API de aplicação e mantém domínio, portas e adapters internos encapsulados. O módulo `settlements` orquestra a liquidação, mas não acessa entidades ou repositories internos de outros módulos. `reporting` é um read model e pode consultar o banco diretamente com SQL otimizado, sem reutilizar aggregates transacionais.

## Estrutura hexagonal aplicada por módulo

```mermaid
flowchart LR
    http["Adapter de entrada<br/>REST / DTO / validação"]
    input["Porta de entrada<br/>Use case"]
    application["Aplicação<br/>Orquestração e transação"]
    domain["Domínio<br/>Entidades, value objects<br/>e políticas"]
    output["Portas de saída<br/>Repository, clock ou provider"]
    persistence["Adapter de persistência<br/>JPA ou JdbcClient"]
    external["Adapter externo<br/>HTTP / Keycloak"]

    http --> input
    input --> application
    application --> domain
    application --> output
    output --> persistence
    output --> external
```

### Regras arquiteturais

1. O domínio não depende de Spring, HTTP, JPA, banco ou Keycloak.
2. Adapters dependem das portas; portas não dependem dos adapters.
3. Controllers convertem DTOs e delegam ao caso de uso, sem conter regra financeira.
4. Transações são delimitadas na camada de aplicação, uma por item da liquidação em lote.
5. Dependências entre módulos passam apenas por APIs públicas verificadas pelo Spring Modulith.
6. O motor financeiro usa `BigDecimal`; arredondamento e conversão seguem o `SPEC.md`.
7. Logs estruturados, correlation ID e métricas atravessam as fronteiras como preocupações operacionais, sem contaminar o domínio.
