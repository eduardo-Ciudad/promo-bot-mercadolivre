# PromoBot

Bot de IA que monitora promoções no Mercado Livre, gera legendas atrativas com IA generativa (Gemini) e distribui as ofertas automaticamente via WhatsApp.

**Autor:** Eduardo Ciudad ([@ciudad_dev](https://instagram.com/ciudad_dev))
**Domínio:** [promobotciudad.duckdns.org](https://promobotciudad.duckdns.org)

---

## Sobre o projeto

O PromoBot busca promoções em marketplaces (inicialmente Mercado Livre), enriquece cada oferta com uma legenda gerada por IA (não um template fixo) e distribui para os usuários via WhatsApp — com planos futuros para outros canais como Telegram.

**Fluxo do pipeline:**

```
Scheduler (periódico)
    ↓
Buscar promoções (Mercado Livre)
    ↓
Filtrar (desconto mínimo, deduplicação)
    ↓
Gerar legenda com IA (Gemini)
    ↓
Enviar via WhatsApp
```

### Diferenciais

- **Legendas geradas por IA** — cada oferta recebe uma descrição única, não um texto engessado
- **Links de afiliado do Mercado Livre** — monetização real via Programa de Afiliados
- **Arquitetura hexagonal** — trocar marketplace, provedor de IA ou canal de distribuição não exige reescrever o núcleo do sistema
- **Operação 24/7** — roda em VPS própria, via Docker

---

## Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Banco de dados | PostgreSQL 16 (Alpine) |
| Migrations | Flyway |
| Browser automation | Playwright (Java) |
| Containerização | Docker + Docker Compose |
| Deploy | VPS (Hostinger) |
| Proxy reverso | Nginx + Certbot (SSL/HTTPS) |
| IA | Google Gemini |
| Mensageria | WhatsApp Cloud API (Meta) |
| Build | Maven |

---

## Arquitetura

O projeto segue **arquitetura hexagonal** (Ports and Adapters), separando regras de negócio (domínio) de integrações externas (marketplace, IA, canal de mensageria). A justificativa: o PromoBot depende de múltiplas integrações externas que podem falhar ou ser substituídas independentemente uma da outra.

```
com.eduar.promobot/
├── domain/
│   ├── model/          → Promocao, Produto, StatusPromocao, CanalDistribuicao
│   ├── port/
│   │   ├── in/          → casos de uso (interfaces)
│   │   └── out/         → PromocaoRepository, BuscadorDePromocoes,
│   │                       GeradorDeDescricao, EnviadorDeMensagem
│   ├── exception/       → TransicaoInvalidaException
│   └── service/         → implementação dos casos de uso
│
├── adapter/
│   ├── in/
│   │   ├── webhook/      → WhatsAppWebhookController
│   │   └── scheduler/    → orquestração periódica
│   └── out/
│       ├── mercadolivre/ → MercadoLivreAdapter (busca via scraping)
│       ├── ia/            → GeminiAdapter
│       ├── whatsapp/      → WhatsAppCloudApiAdapter
│       └── persistence/   → PromocaoJpaAdapter
│
└── config/               → PlaywrightConfig, WhatsAppProperties, JpaAuditingConfig
```

Cada port de saída é uma interface no domínio; a implementação concreta (o adapter) fica isolada e substituível sem impactar o restante do sistema.

### Por que scraping em vez de API para o Mercado Livre

Após investigação extensa, não foi encontrado um endpoint oficial documentado que entregue "promoções em geral" por categoria — `/sites/MLB/search` está bloqueado para esta aplicação, e `/products/search` retorna produtos de catálogo, não anúncios com preço ativo. A estratégia adotada (validada manualmente antes de implementada) é abrir páginas de listagem por categoria num navegador real (Playwright) e extrair os cards renderizados. Essa decisão fica isolada no `MercadoLivreAdapter` — se uma API oficial melhor surgir no futuro, só esse adapter muda.

---

## Rodando localmente

### Pré-requisitos

- Java 17
- Maven
- Docker e Docker Compose
- Conta e aplicação configuradas no [Mercado Livre Developers](https://developers.mercadolivre.com.br)
- Conta e aplicação configuradas no [Meta for Developers](https://developers.facebook.com) (WhatsApp Cloud API)
- Chave de API do Google Gemini

### Variáveis de ambiente

Crie um arquivo `.env` na pasta `backend/` (mesmo nível do `docker-compose.yml`):

```env
DATABASE_USERNAME=
DATABASE_PASSWORD=

WHATSAPP_ACCESS_TOKEN=
WHATSAPP_PHONE_NUMBER_ID=
WHATSAPP_BUSINESS_ACCOUNT_ID=
WHATSAPP_WEBHOOK_VERIFY_TOKEN=

MERCADOLIVRE_APP_ID=
MERCADOLIVRE_SECRET_KEY=

GEMINI_API_KEY=
```

> `DATABASE_URL` não fica no `.env` — é definida diretamente no `docker-compose.yml`.

### Subindo com Docker Compose

```bash
cd backend
docker compose up -d --build
```

A aplicação sobe na porta `8082` (mapeada para `8080` internamente).

### Rodando via Maven (desenvolvimento)

```bash
cd backend
./mvnw spring-boot:run
```

Na primeira execução local, o Playwright baixa os navegadores necessários automaticamente (Chromium, Firefox, WebKit) — isso acontece uma única vez.

---

## Status atual

| Componente | Status |
|---|---|
| Infraestrutura (VPS, Nginx, SSL, Docker) | ✅ Funcionando |
| Domínio (`Promocao`, ports, exceções) | ✅ Implementado |
| Webhook WhatsApp (recebimento) | ✅ Verificado |
| `MercadoLivreAdapter` (scraping) | ✅ Implementado |
| `GeminiAdapter` | ⏳ Planejado |
| `WhatsAppCloudApiAdapter` (envio) | ⏳ Planejado — bloqueado até configuração de produção na Meta |
| `PromocaoJpaAdapter` | ⏳ Planejado |
| Use Cases / Services | ⏳ Planejado |
| Scheduler | ⏳ Planejado |
| Testes automatizados | ⏳ Planejado |

---

## Roadmap

1. `PromocaoJpaAdapter` — persistência via Spring Data JPA
2. `GeminiAdapter` — geração de legenda via IA
3. Use Cases / Services — orquestração do pipeline completo
4. Scheduler — execução periódica automatizada
5. `WhatsAppCloudApiAdapter` — envio de mensagens (após configuração de produção na Meta)
6. Expansão para múltiplas categorias e, futuramente, outros canais (Telegram)

---

## Licença

*A definir.*