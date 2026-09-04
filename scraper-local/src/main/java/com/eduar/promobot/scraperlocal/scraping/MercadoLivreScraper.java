package com.eduar.promobot.scraperlocal.scraping;

import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MercadoLivreScraper {
    private static final Logger LOG = Logger.getLogger(MercadoLivreScraper.class.getName());
    // TODO: suportar filtros de ofertas por IDs oficiais de categoria (ex.: category=MLB1648).
    private static final String CATEGORIA_OFERTAS = "ofertas";
    private static final Pattern ID_EXTERNO_PATTERN = Pattern.compile("MLB-?(\\d+)");
    private static final Pattern PRECO_PATTERN = Pattern.compile(
            "(\\d[\\d.]*)\\s*reais(?:\\s*com\\s*(\\d+)\\s*centavos)?",
            Pattern.CASE_INSENSITIVE);

    private final Browser browser;
    private final ScraperConfig config;

    public MercadoLivreScraper(Browser browser, ScraperConfig config) {
        this.browser = Objects.requireNonNull(browser, "browser e obrigatorio");
        this.config = Objects.requireNonNull(config, "config e obrigatoria");
    }

    public List<PromocaoEncontrada> buscarPromocoes(CriteriosBusca criterios) {
        Objects.requireNonNull(criterios, "criterios e obrigatorio");
        List<PromocaoEncontrada> acumuladas = new ArrayList<>();
        try (SessaoBusca sessao = iniciarBusca(criterios)) {
            for (int numeroPagina = 1;
                 numeroPagina <= config.maxPaginas() && acumuladas.size() < config.maxProdutos();
                 numeroPagina++) {
                PaginaPromocoes pagina = sessao.buscarPagina(numeroPagina);
                if (pagina.semCards()) {
                    break;
                }
                for (PromocaoEncontrada promocao : pagina.promocoes()) {
                    acumuladas.add(promocao);
                    if (acumuladas.size() >= config.maxProdutos()) {
                        break;
                    }
                }
            }
        }
        return List.copyOf(acumuladas);
    }

    public SessaoBusca iniciarBusca(CriteriosBusca criterios) {
        Objects.requireNonNull(criterios, "criterios e obrigatorio");
        BrowserContext context = browser.newContext();
        try {
            Page page = context.newPage();
            page.setDefaultTimeout(config.timeout().toMillis());
            page.setDefaultNavigationTimeout(config.timeout().toMillis());
            return new SessaoMercadoLivre(context, page, criterios.percentualDescontoMinimo());
        } catch (RuntimeException e) {
            context.close();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> extrairPagina(
            Page page, int numeroPagina, String baseUrlTemplate, String categoriaId, Duration pageDelay) {
        page.navigate(baseUrlTemplate.formatted(categoriaId, numeroPagina));
        page.waitForTimeout(pageDelay.toMillis());
        return (List<Map<String, Object>>) page.evaluate(OfertaExtractionScripts.EXTRAIR_CARDS);
    }

    static List<Map<String, Object>> coletarCards(
            int maxProdutos,
            int maxPaginas,
            Function<Integer, List<Map<String, Object>>> extrairPagina) {
        Objects.requireNonNull(extrairPagina, "extrairPagina e obrigatorio");
        List<Map<String, Object>> acumulados = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();

        for (int numeroPagina = 1;
             numeroPagina <= maxPaginas && acumulados.size() < maxProdutos;
             numeroPagina++) {
            List<Map<String, Object>> cardsDaPagina = Objects.requireNonNull(
                    extrairPagina.apply(numeroPagina), "A extracao da pagina retornou uma lista nula");
            int novosNaPagina = 0;

            for (Map<String, Object> card : cardsDaPagina) {
                Object url = card.get("url");
                String idExterno = url instanceof String ? extrairIdExterno((String) url) : null;
                if (idExterno == null || !idsVistos.add(idExterno)) {
                    continue;
                }

                acumulados.add(card);
                novosNaPagina++;
                if (acumulados.size() >= maxProdutos) {
                    break;
                }
            }

            if (novosNaPagina == 0) {
                break;
            }
        }

        return List.copyOf(acumulados);
    }

    private List<PromocaoEncontrada> mapearParaPromocoes(List<Map<String, Object>> cardsBrutos,
                                                          String categoria,
                                                          int percentualDescontoMinimo) {
        List<PromocaoEncontrada> promocoes = new ArrayList<>();
        for (Map<String, Object> cardBruto : cardsBrutos) {
            OfertaCard card = new OfertaCard(
                    (String) cardBruto.get("titulo"), (String) cardBruto.get("url"),
                    (String) cardBruto.get("imagemUrl"), (String) cardBruto.get("precoAtualLabel"),
                    (String) cardBruto.get("precoAnteriorLabel"), (String) cardBruto.get("descontoLabel"));

            if (card.titulo() == null || card.titulo().isBlank()
                    || card.url() == null || card.url().isBlank()
                    || card.precoAnteriorLabel() == null) {
                continue;
            }

            String idExterno = extrairIdExterno(card.url());
            if (idExterno == null) continue;

            BigDecimal precoAtual = parsePrecoLabel(card.precoAtualLabel());
            BigDecimal precoAnterior = parsePrecoLabel(card.precoAnteriorLabel());
            if (precoAtual == null || precoAnterior == null
                    || precoAtual.signum() <= 0 || precoAnterior.signum() <= 0
                    || precoAtual.compareTo(precoAnterior) >= 0) {
                continue;
            }

            try {
                PromocaoEncontrada promocao = new PromocaoEncontrada(
                        card.titulo(), card.imagemUrl(), categoria, precoAnterior,
                        precoAtual, card.url(), idExterno);
                if (promocao.percentualDesconto() >= percentualDescontoMinimo) {
                    promocoes.add(promocao);
                }
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Oferta ignorada por dados invalidos: " + idExterno, e);
            }
        }
        return List.copyOf(promocoes);
    }

    private final class SessaoMercadoLivre implements SessaoBusca {
        private final BrowserContext context;
        private final Page page;
        private final int percentualDescontoMinimo;
        private final Set<String> idsVistos = new HashSet<>();
        private boolean fechada;

        private SessaoMercadoLivre(BrowserContext context, Page page, int percentualDescontoMinimo) {
            this.context = context;
            this.page = page;
            this.percentualDescontoMinimo = percentualDescontoMinimo;
        }

        @Override
        public PaginaPromocoes buscarPagina(int numeroPagina) {
            if (fechada) {
                throw new IllegalStateException("A sessao de busca ja foi fechada");
            }
            if (numeroPagina <= 0) {
                throw new IllegalArgumentException("numeroPagina deve ser maior que zero");
            }

            List<Map<String, Object>> cardsBrutos = Objects.requireNonNull(
                    extrairPagina(page, numeroPagina, config.baseUrlTemplate(),
                            config.categoriaId(), config.pageDelay()),
                    "A extracao da pagina retornou uma lista nula");
            List<Map<String, Object>> cardsNaoVistos = new ArrayList<>();
            for (Map<String, Object> card : cardsBrutos) {
                Object url = card.get("url");
                String idExterno = url instanceof String ? extrairIdExterno((String) url) : null;
                if (idExterno != null && idsVistos.add(idExterno)) {
                    cardsNaoVistos.add(card);
                }
            }

            List<PromocaoEncontrada> promocoes = mapearParaPromocoes(
                    cardsNaoVistos, CATEGORIA_OFERTAS, percentualDescontoMinimo);
            return new PaginaPromocoes(numeroPagina, cardsBrutos.size(), promocoes);
        }

        @Override
        public void close() {
            if (fechada) return;
            fechada = true;
            context.close();
        }
    }

    static String extrairIdExterno(String url) {
        if (url == null) return null;
        Matcher matcher = ID_EXTERNO_PATTERN.matcher(url);
        return matcher.find() ? "MLB" + matcher.group(1) : null;
    }

    static BigDecimal parsePrecoLabel(String label) {
        if (label == null) return null;
        Matcher matcher = PRECO_PATTERN.matcher(label);
        if (!matcher.find()) return null;

        String reais = matcher.group(1).replace(".", "");
        String centavos = matcher.group(2) != null ? matcher.group(2) : "0";
        if (centavos.length() == 1) centavos = "0" + centavos;
        try {
            return new BigDecimal(reais + "." + centavos);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
