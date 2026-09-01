# 🤖 PromoBot — Monitoramento e Distribuição de Promoções

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-green?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue?style=flat-square&logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-orange?style=flat-square&logo=rabbitmq)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

Bot que monitora promoções no Mercado Livre, enriquece cada oferta com uma legenda gerada por IA (Gemini) e distribui automaticamente para um grupo do Telegram — rodando 24/7 em VPS própria, com scraping executado localmente para contornar bloqueio de IP de datacenter.

**Domínio:** [promobotciudad.duckdns.org](https://promobotciudad.duckdns.org)

---

## 🧩 Como o projeto é dividido

O repositório é um monorepo com dois projetos Java **independentes**, sem multi-module Maven:

```
promo-bot-mercadolivre/
├── backend/         # Spring Boot — roda na VPS
└── scraper-local/   # Java puro (sem Spring) — roda na máquina do usuário
```

### Por que separar o scraper do backend?

O Mercado Livre retorna **403 Forbidden via CloudFront** para requisições feitas a partir de IPs de datacenter (confirmado em testes diretos na VPS) — scraping só funciona de IP residencial. A solução foi extrair o scraper para um processo independente, que roda no PC do usuário e envia as promoções encontradas para a VPS via HTTP autenticado. Postgres e RabbitMQ nunca ficam expostos à internet.

---

## 🔄 Pipeline completo

```
scraper-local (PC do usuário, janela 07h-20h, a cada 30 min)
    ↓ HTTPS + API key
POST /api/promocoes/ingestao (backend na VPS)
    ↓ deduplicação por idExterno
RabbitMQ (fila.enriquecimento.promocao)
    ↓
Gemini (gera legenda única por oferta — fallback genérico se falhar/expirar)
    ↓
Outbox (mensagem_outbox — lease/timeout de 120s, backoff, max. tentativas)
    ↓
Telegram Bot API
    ↓
Grupo do Telegram
```

Todo o fluxo roda automaticamente via schedulers — sem intervenção manual — desde o scraping até a entrega da mensagem no grupo.

---

## 🚀 Tecnologias

### Backend (VPS)
- **Java 17** + **Spring Boot 4.1** — arquitetura hexagonal (`domain` / `application` / `adapter in-out`)
- **PostgreSQL 16** + **Flyway**
- **RabbitMQ** — fila de enriquecimento assíncrono das promoções
- **Google Gemini** (`gemini-3.5-flash-lite`) — geração de legenda por IA
- **Telegram Bot API** — canal de distribuição ativo
- **Docker + Docker Compose** — 3 serviços (db, rabbitmq, app) com healthchecks e limites de memória
- **Nginx + Certbot** — proxy reverso com SSL/HTTPS

### Scraper local (máquina do usuário)
- **Java 17 puro** — sem Spring Boot, decisão consciente de simplicidade
- **Playwright (Java) 1.48.0** — automação de navegador
- **`java.net.http.HttpClient`** nativo — envio das promoções à VPS
- **`ScheduledExecutorService`** nativo — agendamento com reagendamento após cada ciclo (evita sobreposição e lida com virada de dia/horário de verão)
- **Maven Shade Plugin** — empacotado como JAR executável único
- Sem Spring, Lombok ou biblioteca de JSON — serializador JSON próprio para os 7 campos do contrato de ingestão
- Logging via `java.util.logging`

---

## 📦 Funcionalidades

- Scraping do Mercado Livre (página de ofertas, com paginação) com filtro por desconto mínimo e deduplicação por `idExterno`
- Ingestão autenticada por API key (`X-Ingestao-Api-Key`, comparação via `MessageDigest.isEqual`) na VPS
- Enriquecimento assíncrono via RabbitMQ + Gemini, com fallback de descrição genérica em caso de falha/timeout
- Padrão Outbox para envio de mensagens: lease com timeout, backoff exponencial e limite de tentativas — evita duplicidade e mensagens perdidas
- Entrega no Telegram via bot dedicado, com suporte a webhook e deduplicação de updates processados (`telegram_update_processado`)
- Suporte legado a WhatsApp Cloud API (canal original do projeto, hoje substituído pelo Telegram como canal ativo)
- Scheduler do scraper local com janela de horário configurável (07h-20h, America/Sao_Paulo) e modo `--once` para execução manual/teste
- Configuração híbrida em ambos os projetos: variável de ambiente → arquivo de properties → valor padrão

---

## 🗂️ Estrutura do projeto

### `backend/`

```
src/main/java/com/eduar/promobot
├── domain
│   ├── model              # Promocao, DestinoDistribuicao, MensagemOutbox...
│   ├── port/out            # Interfaces (portas de saída)
│   └── exception
├── application              # Casos de uso (ex: EnviarMensagensPendentesUseCase)
├── config                    # Beans de configuração (RabbitMQ, Gemini, Telegram...)
└── adapter
    ├── in
    │   ├── web                 # IngestaoPromocaoController, ScrapingController
    │   ├── webhook               # TelegramWebhookController, WhatsAppWebhookController
    │   ├── messaging              # Consumers do RabbitMQ
    │   └── scheduler                # Schedulers (outbox, scraping)
    └── out
        ├── ia                       # Integração com Gemini
        ├── mercadolivre               # Scraper embutido (uso original, pré scraper-local)
        ├── messaging                   # Publisher RabbitMQ
        ├── persistence                   # Repositórios JPA
        └── telegram                       # Cliente da Telegram Bot API
```

### `scraper-local/`

```
src/main/java/com/eduar/promobot/scraperlocal
├── config          # ScraperConfig (validação de todas as properties)
├── model           # PromocaoEncontrada, CriteriosBusca (records)
├── scraping        # MercadoLivreScraper, OfertaCard, OfertaExtractionScripts
├── ingestao        # IngestaoClient, JsonSerializer, ResultadoIngestao
├── scheduling      # ScrapingScheduler
├── application     # ScrapingJob (orquestra scraping + envio)
└── Main            # Composition root — monta as dependências manualmente
```

---

## ⚙️ Como rodar

### Backend (Docker, recomendado)

```bash
cd backend
```

Crie um `.env` com as variáveis necessárias (banco, RabbitMQ, Gemini, Telegram, API key de ingestão — veja `application.properties` para a lista completa):

```bash
docker compose up --build
```

As migrações Flyway rodam automaticamente. A aplicação roda na porta configurada no `docker-compose.yml`.

### Scraper local

**Pré-requisitos:** Java 17.

```bash
cd scraper-local
cp config/scraper.properties.example config/scraper.properties
# edite scraper.properties com a URL de ingestão da VPS
# defina SCRAPER_INGESTAO_API_KEY como variável de ambiente (nunca em arquivo)

./mvnw package
java -jar target/scraper-local-0.0.1-SNAPSHOT.jar --once   # execução única, para teste
java -jar target/scraper-local-0.0.1-SNAPSHOT.jar           # modo scheduler contínuo
```

> ⚠️ `scraping.headless=true` é necessário para rodar despercebido, mas pode disparar detecção anti-bot do Mercado Livre dependendo da configuração do navegador — validar localmente antes de rodar em produção.

### Testes

```bash
# backend
cd backend && ./mvnw test

# scraper-local
cd scraper-local && ./mvnw test
```

---

## 📡 Endpoints principais (backend)

| Método | Rota | Descrição | Acesso |
|--------|------|-----------|--------|
| POST | `/api/promocoes/ingestao` | Recebe promoções enviadas pelo scraper local | API key (header `X-Ingestao-Api-Key`) |
| POST | `/admin/scraping/executar` | Dispara o scraper embutido no backend manualmente | Admin |
| POST | `/webhook/telegram` | Webhook de updates do Telegram | Telegram |
| GET / POST | `/webhook/whatsapp` | Webhook legado do WhatsApp Cloud API | Meta |

---

## 🧪 Testes

### Backend

| Classe de teste | Cobertura |
|----------------|-----------|
| `IngestaoPromocaoServiceTest` / `IngestaoPromocaoControllerTest` | Ingestão de promoções vindas do scraper local |
| `EnviarMensagensPendentesUseCaseRateLimitTest` | Rate limiting no envio de mensagens pendentes |
| `EnviarMensagensPendentesUseCaseUrlInvalidaTest` | Tratamento de URL inválida |
| `MensagemOutboxConcurrencyTest` / `MensagemOutboxRepositoryAdapterLeaseTest` | Concorrência e lease do padrão Outbox |
| `TelegramBotAdapterTest` / `TelegramUpdateProcessorTest` / `TelegramWebhookControllerTest` | Integração e webhook do Telegram |

### Scraper local

| Classe de teste | Cobertura |
|----------------|-----------|
| `ScrapingJobTest` | Consolidação de resultados, continuidade após falha isolada de envio |
| `ScrapingSchedulerTest` | Agendamento e janela de horário |
| `MercadoLivreScraperTest` / `OfertaExtractionScriptsTest` | Extração de ofertas da página do Mercado Livre |
| `IngestaoClientTest` | Envio HTTP autenticado à VPS |

---

## 🔜 Próximos passos

- [ ] Incluir preço (De/Por) na mensagem enviada ao Telegram, hoje só título + descrição + link
- [ ] Remover menção residual ao WhatsApp no prompt de geração de legenda (Gemini)
- [ ] Throttling por `chat_id` no scheduler do outbox (pausado, ainda não implementado)
- [ ] Resolver detecção anti-bot em modo `headless=true` no scraper local sem depender de janela visível

---

## 👨‍💻 Autor

Desenvolvido por [Eduardo](https://github.com/eduardo-Ciudad)