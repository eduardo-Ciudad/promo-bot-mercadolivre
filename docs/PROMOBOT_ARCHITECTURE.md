# 🤖 PromoBot — Documentação de Arquitetura

> Bot de promoções com IA que monitora ofertas no Mercado Livre, gera descrições inteligentes e distribui para canais de mensageria.

---

## 1. Visão Geral do Projeto

O PromoBot é um serviço backend que opera 24/7, monitorando promoções em marketplaces (inicialmente Mercado Livre), enriquecendo cada oferta com uma legenda/descrição gerada por IA, e distribuindo automaticamente para canais de comunicação (Telegram/WhatsApp).

### Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3 |
| Banco de dados | PostgreSQL |
| Migrations | Flyway |
| Containerização | Docker + Docker Compose |
| Deploy | VPS Hostinger (Ubuntu 24.04) |
| Proxy reverso | Nginx + Certbot (SSL) |
| Marketplace | Mercado Livre API (OAuth 2.0 — Client Credentials) |
| IA | Gemini Flash Lite (geração de legendas) |
| Mensageria | Telegram Bot API (MVP) |

### Domínio público

```
https://promobotciudad.duckdns.org
```

---

## 2. Arquitetura Hexagonal — Conceitos

A arquitetura hexagonal (Ports and Adapters) isola o core de negócio de qualquer tecnologia externa. O domínio define **contratos** (interfaces) e o mundo externo se adapta a eles.

### Analogia: O Restaurante

```
┌─────────────────────────────────────────────────────────────┐
│                      MUNDO EXTERNO                          │
│                                                             │
│   🕐 Scheduler       🌐 REST Controller                    │
│   (cliente regular)   (pedido avulso)                       │
│        │                    │                               │
│        ▼                    ▼                               │
│   ┌─────────────────────────────────┐                       │
│   │      INPUT ADAPTERS             │                       │
│   │      (Garçons)                  │                       │
│   │  Recebem pedidos e traduzem     │                       │
│   │  pro formato da cozinha         │                       │
│   └──────────────┬──────────────────┘                       │
│                  │                                          │
│                  ▼                                          │
│   ┌─────────────────────────────────┐                       │
│   │      INPUT PORTS                │                       │
│   │      (Cardápio)                 │                       │
│   │  Interfaces dos Use Cases       │                       │
│   │  "Você pode pedir X, Y, Z"     │                       │
│   └──────────────┬──────────────────┘                       │
│                  │                                          │
│   ╔══════════════▼══════════════════╗                       │
│   ║      DOMAIN / USE CASES         ║                       │
│   ║      (A Cozinha)                ║                       │
│   ║                                 ║                       │
│   ║  • Buscar promoções             ║                       │
│   ║  • Filtrar relevantes           ║                       │
│   ║  • Gerar descrição via IA       ║                       │
│   ║  • Distribuir para canal        ║                       │
│   ║                                 ║                       │
│   ║  JAVA PURO — ZERO FRAMEWORKS    ║                       │
│   ╚══════════════╤══════════════════╝                       │
│                  │                                          │
│   ┌──────────────▼──────────────────┐                       │
│   │      OUTPUT PORTS               │                       │
│   │      (Lista de fornecedores)    │                       │
│   │  "Preciso de carne, tempero,    │                       │
│   │   e um entregador"              │                       │
│   └──────────────┬──────────────────┘                       │
│                  │                                          │
│   ┌──────────────▼──────────────────┐                       │
│   │      OUTPUT ADAPTERS            │                       │
│   │      (Fornecedores concretos)   │                       │
│   │                                 │                       │
│   │  🛒 MercadoLivreAdapter         │                       │
│   │  🤖 GeminiAdapter               │                       │
│   │  📱 TelegramAdapter             │                       │
│   │  💾 PostgresAdapter             │                       │
│   └─────────────────────────────────┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Os 3 Conceitos Fundamentais

**Domain (Coração)**
Entidades, regras de negócio e lógica pura. Sem anotações de framework, sem JPA, sem HTTP. Java puro. É a cozinha — recebe pedido padronizado, entrega resultado.

**Ports (Contratos)**
Interfaces Java que definem O QUE o sistema faz e O QUE ele precisa, sem dizer COMO.
- **Input Ports (Driving):** o que o mundo externo pode pedir → interfaces dos use cases.
- **Output Ports (Driven):** o que o sistema precisa do mundo externo → interfaces de repositórios, clients de API, serviços de mensageria.

**Adapters (Implementações)**
A cola entre o mundo externo e o domínio. Implementam os ports usando tecnologia concreta.
- **Input Adapters (Driving):** traduzem requests externos pro formato do domínio (Controllers, Schedulers).
- **Output Adapters (Driven):** implementam as interfaces usando tecnologia real (JPA, HTTP clients, APIs).

---

## 3. Estrutura de Pacotes

```
br.com.ciudad.promobot/
│
├── domain/                              ← CORAÇÃO (zero dependências externas)
│   ├── model/
│   │   ├── Promocao.java                ← entidade central
│   │   ├── Produto.java                 ← dados do produto do marketplace
│   │   ├── CanalDistribuicao.java       ← enum: TELEGRAM, WHATSAPP
│   │   └── StatusPromocao.java          ← enum: ENCONTRADA, ENRIQUECIDA, ENVIADA, FALHA
│   │
│   ├── port/
│   │   ├── in/                          ← INPUT PORTS (o que o sistema FAZ)
│   │   │   ├── BuscarPromocoesUseCase.java
│   │   │   ├── EnriquecerPromocaoUseCase.java
│   │   │   └── DistribuirPromocaoUseCase.java
│   │   │
│   │   └── out/                         ← OUTPUT PORTS (o que o sistema PRECISA)
│   │       ├── BuscadorDePromocoes.java          ← "me dá promoções"
│   │       ├── GeradorDeDescricao.java           ← "gera uma legenda pra isso"
│   │       ├── EnviadorDeMensagem.java           ← "manda pro canal"
│   │       └── PromocaoRepository.java           ← "salva/consulta promoções"
│   │
│   └── service/                         ← IMPLEMENTAÇÃO DOS USE CASES
│       ├── BuscarPromocoesService.java
│       ├── EnriquecerPromocaoService.java
│       └── DistribuirPromocaoService.java
│
├── adapter/                             ← MUNDO EXTERNO
│   ├── in/                              ← INPUT ADAPTERS (quem chama o sistema)
│   │   ├── scheduler/
│   │   │   └── PromocaoScheduler.java            ← @Scheduled — dispara o fluxo
│   │   └── rest/
│   │       └── PromocaoController.java           ← endpoint manual (admin)
│   │
│   └── out/                             ← OUTPUT ADAPTERS (implementações concretas)
│       ├── mercadolivre/
│       │   └── MercadoLivreAdapter.java          ← implementa BuscadorDePromocoes
│       ├── ia/
│       │   └── GeminiAdapter.java                ← implementa GeradorDeDescricao
│       ├── telegram/
│       │   └── TelegramAdapter.java              ← implementa EnviadorDeMensagem
│       └── persistence/
│           ├── PromocaoJpaAdapter.java            ← implementa PromocaoRepository
│           ├── PromocaoEntity.java                ← entidade JPA (separada do domain!)
│           └── PromocaoJpaRepository.java         ← Spring Data JPA interface
│
└── config/                              ← SPRING BOOT WIRING
    ├── BeanConfig.java                  ← injeta adapters nos ports
    ├── SecurityConfig.java
    └── SchedulerConfig.java
```

---

## 4. Fluxo de Execução

```
  ┌──────────┐
  │ Scheduler │  ← dispara a cada X minutos
  │ (Input    │
  │  Adapter) │
  └─────┬─────┘
        │ chama
        ▼
  ┌──────────────────────────┐
  │ BuscarPromocoesUseCase   │  ← Input Port (interface)
  │ → BuscarPromocoesService │  ← Implementação (domain)
  └─────┬────────────────────┘
        │ chama
        ▼
  ┌──────────────────────────┐
  │ BuscadorDePromocoes      │  ← Output Port (interface)
  │ → MercadoLivreAdapter    │  ← Output Adapter
  │   (chama API do ML)      │
  └─────┬────────────────────┘
        │ retorna List<Produto>
        ▼
  ┌──────────────────────────┐
  │ Service filtra:          │
  │ • desconto > X%          │
  │ • não duplicada          │
  │ • salva via Repository   │
  └─────┬────────────────────┘
        │
        ▼
  ┌──────────────────────────────┐
  │ EnriquecerPromocaoUseCase    │
  │ → EnriquecerPromocaoService  │
  └─────┬────────────────────────┘
        │ chama
        ▼
  ┌──────────────────────────┐
  │ GeradorDeDescricao       │  ← Output Port (interface)
  │ → GeminiAdapter          │  ← Output Adapter
  │   (chama API do Gemini)  │
  └─────┬────────────────────┘
        │ retorna String (legenda gerada)
        ▼
  ┌──────────────────────────────┐
  │ DistribuirPromocaoUseCase    │
  │ → DistribuirPromocaoService  │
  └─────┬────────────────────────┘
        │ chama
        ▼
  ┌──────────────────────────┐
  │ EnviadorDeMensagem       │  ← Output Port (interface)
  │ → TelegramAdapter        │  ← Output Adapter
  │   (chama Telegram API)   │
  └─────┬────────────────────┘
        │
        ▼
  ✅ Promoção enviada ao canal!
```

### Passo a passo detalhado

1. **Scheduler dispara** → a cada N minutos, chama o use case de busca.
2. **BuscarPromocoesService** chama `BuscadorDePromocoes.buscar()` — interface. Quem responde é o `MercadoLivreAdapter`, que consulta a API do ML, parseia o JSON e devolve `List<Produto>` (objeto do domínio).
3. **Filtragem** → o service aplica regras de negócio: desconto mínimo, deduplicação (não enviar a mesma promoção duas vezes), categorias de interesse.
4. **Persistência** → promoções válidas são salvas via `PromocaoRepository` com status `ENCONTRADA`.
5. **EnriquecerPromocaoService** pega promoções com status `ENCONTRADA` e chama `GeradorDeDescricao.gerar(promocao)` — interface. O `GeminiAdapter` manda os dados pra IA e devolve a legenda formatada. Status atualizado para `ENRIQUECIDA`.
6. **DistribuirPromocaoService** pega promoções `ENRIQUECIDA` e chama `EnviadorDeMensagem.enviar(promocao)` — interface. O `TelegramAdapter` posta no canal/grupo. Status atualizado para `ENVIADA`.

---

## 5. Modelo de Domínio

### Promocao (entidade central)

| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador único |
| produto | Produto | Dados do produto (embedded ou relação) |
| precoOriginal | BigDecimal | Preço antes do desconto |
| precoPromocional | BigDecimal | Preço com desconto |
| percentualDesconto | Integer | % de desconto calculado |
| linkOriginal | String | Link do produto no ML |
| linkAfiliado | String | Link com tag de afiliado |
| descricaoGerada | String | Legenda gerada pela IA |
| status | StatusPromocao | ENCONTRADA → ENRIQUECIDA → ENVIADA / FALHA |
| canalEnvio | CanalDistribuicao | TELEGRAM, WHATSAPP |
| dataEncontrada | LocalDateTime | Quando o bot encontrou |
| dataEnviada | LocalDateTime | Quando foi distribuída |
| idExterno | String | ID do item no ML (deduplicação) |

### StatusPromocao (ciclo de vida)

```
ENCONTRADA ──→ ENRIQUECIDA ──→ ENVIADA
     │               │
     └──→ FALHA ←─────┘
```

---

## 6. Integrações Externas

### Mercado Livre API

- **Autenticação:** OAuth 2.0 — Client Credentials (server-to-server)
- **Endpoint principal:** `/sites/MLB/search?q={query}&discount={range}`
- **Dados extraídos:** título, preço, desconto, thumbnail, link, id do item
- **Rate limiting:** respeitar limites da API (tratar 429 com retry)
- **Link de afiliado:** anexar tag de afiliado ao link do produto

### IA — Geração de Legendas

- **Provedor:** Gemini Flash Lite (custo baixo, texto curto)
- **Input:** dados da promoção (nome, preço original, preço promocional, %, categoria)
- **Output:** legenda formatada para o canal (emojis, call-to-action, link)
- **Resiliência:** retry com backoff para 429 (padrão Stack Over Flowers — 3 tentativas)

### Telegram Bot API

- **Tipo:** Bot oficial via BotFather (gratuito, sem risco de ban)
- **Funcionalidade:** envio de mensagens para canal/grupo
- **Formato:** texto com Markdown + imagem do produto (thumbnail do ML)

---

## 7. Infraestrutura

### Docker Compose (produção na VPS)

```yaml
# Estrutura esperada
services:
  promobot-app:
    # Spring Boot JAR — multi-stage build
    # porta: 8082 (ou a disponível)
    # mem_limit: 300m
    # JVM: -Xmx256m

  # PostgreSQL já compartilhado com GabiKids/Controle Financeiro
  # (database separado: promobot_db)
```

### Nginx

```
promobotciudad.duckdns.org → proxy_pass → localhost:8082
```

### Recursos estimados na VPS

| Recurso | Estimativa |
|---|---|
| RAM (app) | 200–350 MB |
| Disco (JAR + Docker image) | ~80 MB |
| Disco (banco) | Cresce com histórico — baixo inicialmente |
| CPU | Baixo — picos apenas durante chamadas de API |

---

## 8. Por que Hexagonal faz sentido aqui

Diferente do ATSReady (stateless, request-response), o PromoBot tem:

- **Estado persistente** — promoções salvas, histórico de envios, deduplicação.
- **Agendamento** — roda sozinho 24/7 via `@Scheduled`.
- **Múltiplas integrações externas** que falham independentemente (ML API, Gemini, Telegram).

A hexagonal isola cada ponto de falha num adapter. Se amanhã:
- Troca Mercado Livre por Amazon → novo adapter, domínio intacto.
- Troca Gemini por Claude → novo adapter, domínio intacto.
- Troca Telegram por WhatsApp → novo adapter, domínio intacto.
- Adiciona um segundo marketplace → novo adapter, mesma interface.

O domínio (regras de filtragem, ciclo de vida da promoção, lógica de deduplicação) **nunca muda** por causa de tecnologia externa.

---

## 9. Decisões Técnicas Pendentes

| Decisão | Opções | Status |
|---|---|---|
| Canal de distribuição MVP | Telegram (gratuito, sem ban) vs WhatsApp Business API (custo, verificação) | Pendente |
| Frequência de polling | A cada 5min? 15min? 30min? | Pendente |
| Categorias monitoradas | Todas vs configurável vs hardcoded inicial | Pendente |
| Desconto mínimo para envio | 20%? 30%? Configurável? | Pendente |
| Formato da legenda | Template fixo com dados vs 100% gerado por IA | Pendente |
| Monetização | Links de afiliado (comissão por venda) | Planejado |
| Testes | JUnit 5 + Mockito (adapters mockados) | Planejado |

---

## 10. Próximos Passos

1. **Spike da API do ML** — validar que os dados retornados (preço, desconto, link) servem para o bot.
2. **Setup do projeto** — Spring Initializr, estrutura de pacotes hexagonal, Docker Compose, Flyway.
3. **Adapter do ML** — primeira integração funcional (Client Credentials → busca → parse).
4. **Adapter da IA** — gerar legenda a partir dos dados da promoção.
5. **Adapter do Telegram** — enviar mensagem para um canal de teste.
6. **Scheduler** — orquestrar o fluxo completo.
7. **Deploy na VPS** — Docker, Nginx, monitoramento.

---

*Documento criado em: Agosto 2026*
*Autor: Eduardo Ciudad (@ciudad_dev)*
*Repositório: a ser criado*
