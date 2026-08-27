# Arquitetura do PromoBot — referência por classe

Este documento descreve o código existente em `src/main/java` na data desta revisão. A classificação abaixo considera o papel efetivamente desempenhado por cada tipo, e não apenas seu nome ou o roadmap do projeto.

> **Decisão consciente:** `Promocao` e `Produto` são modelos de domínio e também carregam anotações JPA. No estágio atual do PromoBot, a persistência usa o próprio modelo de domínio, sem uma entidade de banco separada. Isso é um acoplamento pragmático deliberado, não uma classificação dessas classes como adapters.

## Estado arquitetural observado

- O domínio contém o modelo, suas transições de estado e quatro ports de saída.
- Não há, no código atual, ports de entrada, serviços de aplicação/casos de uso ou scheduler.
- O único adapter de entrada é o webhook do WhatsApp.
- O único port de saída com implementação concreta é `BuscadorDePromocoes`, implementado por `MercadoLivreAdapter`.
- Ainda não existem implementações de `GeradorDeDescricao`, `EnviadorDeMensagem` e `PromocaoRepository`.
- `GeminiAdapter`, `WhatsAppCloudApiAdapter`, `PromocaoJpaAdapter` e `JpaAuditingConfig` aparecem no contexto/roadmap, mas não existem no código-fonte analisado e, por isso, não recebem seções fictícias neste documento.

## `com.eduar.promobot`

### `PromobotApplication`

- **Resumo:** inicia a aplicação Spring Boot e habilita o binding das propriedades do WhatsApp.
- **Camada:** configuração/bootstrap da aplicação.
- **Responsável por:** estabelecer a raiz de auto-configuração e component scan do Spring; registrar `WhatsAppProperties`; delegar a inicialização a `SpringApplication`.
- **Não é responsável por:** executar o pipeline de promoções, conter regras de negócio, buscar ofertas, processar webhooks ou realizar integrações externas.
- **Depende de:** Spring Boot (`SpringApplication`, `@SpringBootApplication`) e `WhatsAppProperties`.
- **Usada por:** runtime Java como ponto de entrada e pelo teste `PromobotApplicationTests`, que inicializa o contexto criado a partir desta configuração.

#### Funções

- `main(String[] args)`: entrega os argumentos à inicialização do Spring Boot e sobe o contexto da aplicação.

## `com.eduar.promobot.domain.model`

### `Promocao`

- **Resumo:** entidade central que representa uma promoção, calcula seu desconto e protege as transições do seu ciclo de vida.
- **Camada:** domínio, com mapeamento JPA incorporado pela decisão pragmática descrita no início.
- **Responsável por:** armazenar produto, preços, links, descrição, identificadores, canal, status e timestamps; gerar um UUID e iniciar novas instâncias em `ENCONTRADA`; calcular o percentual de desconto; permitir enriquecimento e envio somente a partir dos estados esperados; marcar falha; declarar o mapeamento da tabela `promocao` e a auditoria JPA.
- **Não é responsável por:** buscar ofertas no Mercado Livre, gerar a descrição, criar links afiliados, enviar mensagens, salvar ou consultar o banco, definir transações ou orquestrar o pipeline. Ela apenas registra resultados recebidos por chamadas aos seus métodos.
- **Depende de:** `Produto`, `StatusPromocao`, `CanalDistribuicao`, JPA/Jakarta Persistence, auditoria do Spring Data JPA, Lombok e tipos Java como `UUID`, `BigDecimal` e `Instant`.
- **Usada por:** todos os ports de saída; `MercadoLivreAdapter`, que cria promoções encontradas; e futuras implementações/orquestradores dos ports. No código atual, nenhum repositório concreto a persiste.

#### Funções

- `Promocao()`: construtor protegido, sem argumentos, gerado por Lombok para materialização JPA; não cria uma promoção de negócio válida por si só.
- `Promocao(Produto, BigDecimal, BigDecimal, String, String)`: construtor privado usado pelo builder; cria o UUID, copia os dados básicos, calcula o desconto e define o status inicial como `ENCONTRADA`.
- `builder()`: API gerada por Lombok para construir uma nova promoção usando o construtor privado. Aceita somente `produto`, `precoOriginal`, `precoPromocional`, `linkOriginal` e `idExterno`.
- `calcularPercentualDesconto(BigDecimal, BigDecimal)`: retorna zero para preços nulos ou preço original não positivo; nos demais casos calcula `(original - promocional) / original * 100`, com escala intermediária 4 e `HALF_UP`, e devolve apenas a parte inteira por `intValue()`.
- `definirLinkAfiliado(String)`: atribui o link afiliado sem validar conteúdo nem alterar o status.
- `enriquecerComDescricao(String)`: exige status `ENCONTRADA`, grava a descrição recebida e muda o status para `ENRIQUECIDA`; não chama uma IA.
- `marcarComoEnviada(CanalDistribuicao)`: exige status `ENRIQUECIDA`, registra o canal recebido e muda o status para `ENVIADA`; não envia a mensagem.
- `marcarComoFalha()`: muda o status para `FALHA` a partir de qualquer estado, sem guardar causa ou validar a transição.
- `exigirStatus(StatusPromocao, String)`: valida o estado corrente. Apesar de existir `TransicaoInvalidaException`, a implementação atual lança `jakarta.persistence.TransactionRequiredException` com uma mensagem de negócio quando o estado diverge.
- **Getters:** `@Getter` gera leitura para todos os campos; não há setters gerais.

### `Produto`

- **Resumo:** objeto de valor embutível que reúne os dados básicos do produto associado a uma promoção.
- **Camada:** domínio, com mapeamento JPA incorporado.
- **Responsável por:** representar nome, URL da imagem e categoria; definir como esses três valores são embutidos na tabela da entidade proprietária; oferecer construção e comparação por valor por meio do Lombok.
- **Não é responsável por:** representar preços ou desconto, buscar detalhes do produto, validar URLs/categoria, controlar o ciclo de vida da promoção ou persistir-se de forma independente.
- **Depende de:** JPA (`@Embeddable`, `@Column`) e Lombok. Os imports de `BigDecimal` e `UUID` existentes no arquivo não são utilizados.
- **Usada por:** `Promocao` como `@Embedded` e `MercadoLivreAdapter` ao converter um card extraído em modelo de domínio.

#### Funções

- `Produto()`: construtor público sem argumentos gerado por Lombok, necessário para materialização JPA.
- `Produto(String nome, String imagemUrl, String categoria)`: construtor gerado por Lombok que inicializa os três campos.
- **Getters:** expõem os três valores sem setters.
- `equals(Object)` e `hashCode()`: gerados por Lombok considerando os campos do produto.

### `StatusPromocao`

- **Resumo:** enumera os estados possíveis do ciclo de vida de uma promoção.
- **Camada:** domínio.
- **Responsável por:** fornecer os valores `ENCONTRADA`, `ENRIQUECIDA`, `ENVIADA` e `FALHA`, usados nas regras de transição e nas consultas por status.
- **Não é responsável por:** executar ou validar transições; essa lógica está em `Promocao`.
- **Depende de:** nenhum tipo do projeto.
- **Usada por:** `Promocao`, `TransicaoInvalidaException` e `PromocaoRepository`.

### `CanalDistribuicao`

- **Resumo:** identifica o canal pelo qual uma promoção é distribuída.
- **Camada:** domínio.
- **Responsável por:** declarar o canal atualmente suportado pelo modelo: `WHATSAPP`.
- **Não é responsável por:** selecionar um destinatário, formatar ou enviar mensagens, nem configurar a API do WhatsApp.
- **Depende de:** nenhum tipo do projeto.
- **Usada por:** `Promocao` para registrar `canalEnvio`. Há um import não utilizado desse enum em `EnviadorDeMensagem`.

## `com.eduar.promobot.domain.exception`

### `TransicaoInvalidaException`

- **Resumo:** exceção de domínio destinada a descrever uma tentativa inválida de transição de promoção.
- **Camada:** domínio.
- **Responsável por:** montar uma mensagem com ação, promoção, estado atual e esperado; conservar `promocaoId`, `statusAtual` e `statusEsperado` para tratamento estruturado.
- **Não é responsável por:** decidir quais transições são válidas, alterar o status ou converter a falha em resposta HTTP.
- **Depende de:** `StatusPromocao`, `UUID` e `RuntimeException`.
- **Usada por:** nenhuma classe no código atual. Em particular, `Promocao.exigirStatus` lança `TransactionRequiredException`, não esta exceção.

#### Funções

- `TransicaoInvalidaException(UUID, String, StatusPromocao, StatusPromocao)`: inicializa a mensagem da exceção e os três campos estruturados.
- `getPromocaoId()`: retorna o UUID da promoção relacionada.
- `getStatusAtual()`: retorna o estado encontrado.
- `getStatusEsperado()`: retorna o estado exigido pela ação.

## `com.eduar.promobot.domain.port.out`

### `BuscadorDePromocoes`

- **Resumo:** contrato pelo qual o núcleo solicita promoções a uma fonte externa sem conhecer a tecnologia de coleta.
- **Camada:** port de saída.
- **Responsável por:** definir a operação de busca e o conjunto mínimo de critérios aceito por ela.
- **Não é responsável por:** escolher quando a busca ocorre, deduplicar ou persistir resultados, gerar descrições, enviar mensagens ou prescrever scraping/API.
- **Depende de:** `Promocao` e coleções Java.
- **Usada/implementada por:** `MercadoLivreAdapter`. Não há caso de uso chamando o port no código atual.

#### Funções e tipos internos

- `buscarPromocoes(CriteriosBusca)`: solicita uma lista de promoções que atendam aos critérios; a interface não define política de erro, ordenação ou unicidade.

### `BuscadorDePromocoes.CriteriosBusca`

- **Resumo:** record aninhado que transporta os filtros de uma solicitação de busca.
- **Pacote:** `com.eduar.promobot.domain.port.out`, como tipo membro de `BuscadorDePromocoes`.
- **Camada:** port de saída, como parte do contrato de busca.
- **Responsável por:** agrupar `percentualDescontoMinimo` e `categorias` em um valor de entrada imutável.
- **Não é responsável por:** validar o percentual, normalizar categorias, substituir uma lista nula ou executar a filtragem. A implementação atual do adapter pressupõe que `categorias` não seja nula.
- **Depende de:** `List<String>`.
- **Usada por:** `BuscadorDePromocoes.buscarPromocoes` e `MercadoLivreAdapter.buscarPromocoes`.

#### Funções

- `CriteriosBusca(int, List<String>)`: construtor canônico, sem validações nem cópia defensiva da lista.
- `percentualDescontoMinimo()` e `categorias()`: accessors gerados pelo record.
- `equals`, `hashCode` e `toString`: implementações geradas pelo record.

### `GeradorDeDescricao`

- **Resumo:** contrato para obter uma descrição textual para uma promoção.
- **Camada:** port de saída.
- **Responsável por:** desacoplar o domínio do provedor e da técnica usados para gerar texto.
- **Não é responsável por:** gravar a descrição em `Promocao`, mudar seu status, persistir a entidade ou determinar quando a geração ocorre.
- **Depende de:** `Promocao`.
- **Usada/implementada por:** nenhuma classe no código atual; o `GeminiAdapter` citado no roadmap ainda não existe.

#### Funções

- `gerarDescricao(Promocao)`: recebe a promoção como contexto e devolve uma `String`; o contrato atual não especifica formato, validação ou tratamento de falhas.

### `EnviadorDeMensagem`

- **Resumo:** contrato para enviar externamente uma mensagem baseada em uma promoção.
- **Camada:** port de saída.
- **Responsável por:** expor a operação de envio sem acoplar o núcleo a uma API de mensageria específica.
- **Não é responsável por:** marcar a promoção como enviada, escolher explicitamente o canal ou destinatário no contrato, persistir resultados ou receber webhooks.
- **Depende de:** `Promocao`. O import de `CanalDistribuicao` existe, mas não é utilizado pela interface.
- **Usada/implementada por:** nenhuma classe no código atual; o `WhatsAppCloudApiAdapter` citado no roadmap ainda não existe.

#### Funções

- `enviar(Promocao)`: solicita o envio da promoção e não retorna confirmação; o contrato atual não declara canal, destinatário, idempotência ou modelo de erro.

### `PromocaoRepository`

- **Resumo:** contrato de persistência das promoções necessário ao núcleo.
- **Camada:** port de saída.
- **Responsável por:** definir gravação, consulta por UUID, verificação de duplicidade pelo identificador externo e consulta por status.
- **Não é responsável por:** definir SQL/JPA, transações, mapeamento concreto, regras de transição ou a ordem em que as operações são chamadas.
- **Depende de:** `Promocao`, `StatusPromocao`, `UUID`, `Optional` e `List`.
- **Usada/implementada por:** nenhuma classe no código atual; `PromocaoJpaAdapter` ainda não existe. As anotações JPA de `Promocao` não constituem, sozinhas, uma implementação deste port.

#### Funções

- `salvar(Promocao)`: persiste ou atualiza uma promoção e devolve a representação resultante.
- `buscarPorId(UUID)`: procura uma promoção pelo identificador interno, representando ausência com `Optional`.
- `existePorIdExterno(String)`: consulta se o identificador da fonte externa já está registrado, dando suporte à deduplicação sem implementar a política de deduplicação.
- `buscarPorStatus(StatusPromocao)`: devolve promoções que possuam o status informado; o contrato não garante ordenação ou paginação.

## `com.eduar.promobot.adapter.in.webhook`

### `WhatsAppWebhookController`

- **Resumo:** expõe os endpoints HTTP de verificação e recebimento do webhook do WhatsApp.
- **Camada:** adapter de entrada.
- **Responsável por:** comparar o token de verificação recebido com a configuração; devolver o `challenge` em verificações válidas; responder `403` a verificações inválidas; receber, registrar em log e confirmar eventos POST.
- **Não é responsável por:** validar assinatura/autenticidade dos POSTs, desserializar ou processar seu payload, executar casos de uso, responder mensagens, buscar promoções ou gerar descrições. O payload completo é apenas registrado no log atualmente.
- **Depende de:** `WhatsAppProperties`, Spring Web MVC, SLF4J e `ResponseEntity`.
- **Usada por:** Spring MVC, que a registra como controller em `/webhook/whatsapp`; externamente, pelos callbacks de verificação e eventos do WhatsApp/Meta.

#### Funções

- `WhatsAppWebhookController(WhatsAppProperties)`: recebe por injeção as propriedades tipadas usadas na verificação.
- `verificarWebhook(String mode, String verifyToken, String challenge)`: atende `GET /webhook/whatsapp`; retorna `200` com o challenge somente quando `hub.mode` é `subscribe` e `hub.verify_token` coincide com `webhookVerifyToken`; caso contrário retorna `403` com `Verification failed`.
- `receberEvento(String payload)`: atende `POST /webhook/whatsapp`, registra o corpo bruto e responde `200` sem processamento adicional.

## `com.eduar.promobot.adapter.out.mercadolivre`

### `MercadoLivreAdapter`

- **Resumo:** implementa a busca de promoções por scraping das páginas de listagem do Mercado Livre com Playwright.
- **Camada:** adapter de saída.
- **Responsável por:** escolher categorias efetivas; navegar por categoria; provocar scroll para carregar cards; extrair dados do DOM; converter labels de preço e URLs em valores estruturados; descartar cards incompletos/inválidos; criar `Produto` e `Promocao`; filtrar pelo desconto mínimo.
- **Não é responsável por:** agendar buscas, deduplicar contra o banco, persistir promoções, gerar link afiliado ou descrição, enviar mensagens, manter contexto/página entre buscas ou propagar detalhes de falhas. Atualmente qualquer exceção em uma categoria é absorvida e resulta em lista vazia para aquela categoria.
- **Depende de:** port `BuscadorDePromocoes`, modelos `Promocao` e `Produto`, `OfertaCard`, `OfertaExtractionScripts`, beans Playwright `Browser`/`BrowserContext`/`Page`, propriedades Spring e utilitários Java.
- **Usada por:** Spring, como `@Component`, para satisfazer injeções de `BuscadorDePromocoes`; não existe consumidor desse port no código atual. O `Browser` injetado é produzido por `PlaywrigthConfig`.

#### Funções

- `MercadoLivreAdapter(Browser, String, String, int, int)`: injeta navegador e configurações de URL, categoria padrão, limite de cards e limite de scrolls.
- `buscarPromocoes(CriteriosBusca)`: usa a categoria padrão quando `criterios.categorias()` está vazia, busca cada categoria sequencialmente e concatena os resultados. Não limita o total combinado nem elimina duplicatas entre categorias.
- `buscarPorCategoria(String, int)`: formata a URL, abre um contexto e uma página isolados, navega, aguarda o primeiro card, faz scroll até atingir o limite de cards ou de tentativas, extrai mapas via JavaScript e os converte. Fecha página/contexto via `try-with-resources`; em qualquer exceção retorna lista vazia sem log.
- `mapearParaPromocoes(List<Map<String,Object>>, String, int)`: cria um `OfertaCard` para cada mapa, exige título, URL e preço anterior, extrai ID externo e preços, exige preço promocional menor que o original, cria os modelos de domínio e conserva promoções cujo desconto calculado alcança o mínimo. `descontoLabel` é capturado, mas não participa do cálculo.
- `extrairIdExterno(String)`: procura a primeira ocorrência compatível com `MLB-?(\\d+)` na URL e normaliza o retorno para `MLB` seguido dos dígitos; retorna `null` quando ausente.
- `parsePrecoLabel(String)`: interpreta labels em português no formato de reais e centavos, remove pontos de milhar, completa centavo de um dígito e cria um `BigDecimal`; retorna `null` para entrada ausente, formato incompatível ou número inválido.

### `OfertaCard`

- **Resumo:** DTO interno do adapter que representa os campos brutos extraídos de um card de oferta.
- **Camada:** adapter de saída.
- **Responsável por:** transportar título, URL, imagem e labels textuais de preço/desconto entre a extração JavaScript e o mapeamento para o domínio.
- **Não é responsável por:** validar ou converter valores, calcular desconto, representar uma entidade de domínio ou persistir dados.
- **Depende de:** apenas recursos nativos de records do Java.
- **Usada por:** `MercadoLivreAdapter.mapearParaPromocoes`.

#### Funções

- `OfertaCard(...)`: construtor canônico do record, sem validações.
- `titulo()`, `url()`, `imagemUrl()`, `precoAtualLabel()`, `precoAnteriorLabel()` e `descontoLabel()`: acessores gerados pelo record.
- `equals`, `hashCode` e `toString`: implementações de valor geradas pelo record.

### `OfertaExtractionScripts`

- **Resumo:** centraliza os scripts JavaScript usados pelo scraping da listagem do Mercado Livre.
- **Camada:** adapter de saída; é um detalhe privado do pacote da integração Mercado Livre.
- **Responsável por:** fornecer scripts para contar cards, rolar a página e projetar elementos do DOM em objetos simples com os campos esperados pelo adapter.
- **Não é responsável por:** executar scripts, navegar, aguardar carregamento, mapear objetos para o domínio ou tratar mudanças no DOM em runtime.
- **Depende de:** seletores e APIs DOM executados dentro da página do Mercado Livre; não depende de classes do projeto.
- **Usada por:** `MercadoLivreAdapter` através das constantes package-private.

#### Membros

- `OfertaExtractionScripts()`: construtor privado que impede instanciação; a classe funciona apenas como contêiner estático.
- `CONTAR_CARDS`: expressão que conta elementos `.ui-search-layout__item`.
- `EXTRAIR_CARDS`: script que percorre os cards e extrai título, URL, imagem e labels de preço/desconto; campos inexistentes viram `null`.
- `SCROLL_PARA_BAIXO`: script que rola a janela até o fim do documento.

## `com.eduar.promobot.config`

### `PlaywrigthConfig`

- **Resumo:** cria e gerencia os objetos Playwright e Chromium compartilhados pela aplicação.
- **Camada:** configuração.
- **Responsável por:** registrar beans `Playwright` e `Browser`; lançar o Chromium com modo headless configurável e argumentos `--disable-gpu` e `--no-sandbox`; fechar ambos no encerramento do contexto Spring.
- **Não é responsável por:** navegar em páginas, executar scraping, criar contextos/páginas por busca ou definir critérios de promoção.
- **Depende de:** Playwright e Spring Configuration. O valor de `mercadolivre.scraping.headless` vem de `application.properties`, com fallback `true` na própria injeção.
- **Usada por:** Spring para composição da infraestrutura; o bean `Browser` é injetado em `MercadoLivreAdapter` e depende do bean `Playwright`.

> O nome existente no código é `PlaywrigthConfig` (sem o segundo “i” de “Playwright”); esta documentação preserva a grafia real.

#### Funções

- `playwright()`: cria a instância raiz com `Playwright.create()` e declara `close` como método de destruição.
- `browser(Playwright, boolean)`: lança um Chromium a partir do bean raiz, aplica configuração headless e argumentos de processo, e também declara `close` no shutdown.

### `WhatsAppProperties`

- **Resumo:** representa de forma tipada as propriedades de configuração prefixadas por `whatsapp`.
- **Camada:** configuração.
- **Responsável por:** receber do Spring os valores `accessToken`, `phoneNumberId`, `businessAccountId`, `webhookVerifyToken` e `apiUrl` e disponibilizá-los por accessors imutáveis.
- **Não é responsável por:** validar presença/formato dos valores, esconder o token em logs, realizar chamadas à Meta, verificar assinaturas ou processar webhooks.
- **Depende de:** Spring Boot `@ConfigurationProperties`; seu registro é habilitado por `PromobotApplication`.
- **Usada por:** `WhatsAppWebhookController`, atualmente apenas por meio de `webhookVerifyToken()`. Os demais valores aguardam um adapter de envio ainda não implementado.

#### Funções

- `WhatsAppProperties(...)`: construtor canônico usado no binding das cinco propriedades.
- `accessToken()`, `phoneNumberId()`, `businessAccountId()`, `webhookVerifyToken()` e `apiUrl()`: accessors gerados pelo record.
- `equals`, `hashCode` e `toString`: implementações geradas pelo record; o código atual não as sobrescreve para mascarar `accessToken`.

## Teste existente

### `com.eduar.promobot.PromobotApplicationTests`

- **Resumo:** teste de fumaça que verifica se o contexto Spring Boot consegue ser carregado.
- **Camada:** teste de integração de configuração; não integra o hexágono de produção.
- **Responsável por:** solicitar a criação do contexto completo via `@SpringBootTest` e falhar caso a inicialização lance uma exceção.
- **Não é responsável por:** testar regras de `Promocao`, scraping, endpoints, ports, banco ou comportamento das integrações.
- **Depende de:** JUnit 5, Spring Boot Test e da configuração iniciada por `PromobotApplication`.
- **Usada por:** Maven/Surefire durante a fase de testes.

#### Funções

- `contextLoads()`: teste sem corpo; seu sucesso significa somente que o contexto foi criado sem erro.
