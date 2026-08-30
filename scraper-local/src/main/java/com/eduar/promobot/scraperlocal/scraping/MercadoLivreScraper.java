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
        return buscarOfertas(criterios.percentualDescontoMinimo());
    }

    private List<PromocaoEncontrada> buscarOfertas(int percentualDescontoMinimo) {
        try (BrowserContext context = browser.newContext(); Page page = context.newPage()) {
            page.setDefaultTimeout(config.timeout().toMillis());
            page.setDefaultNavigationTimeout(config.timeout().toMillis());

            List<Map<String, Object>> cardsBrutos = coletarCards(
                    config.maxProdutos(), config.maxPaginas(), numeroPagina -> extrairPagina(
                            page, numeroPagina, config.baseUrlTemplate(), config.pageDelay()));
            return mapearParaPromocoes(cardsBrutos, CATEGORIA_OFERTAS, percentualDescontoMinimo);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Falha ao buscar ofertas", e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> extrairPagina(
            Page page, int numeroPagina, String baseUrlTemplate, Duration pageDelay) {
        page.navigate(baseUrlTemplate.formatted(numeroPagina));
        page.waitForSelector(".poly-card");
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
        int quantidadeParaAnalisar = Math.min(cardsBrutos.size(), config.maxProdutos());
        for (int indice = 0; indice < quantidadeParaAnalisar; indice++) {
            Map<String, Object> cardBruto = cardsBrutos.get(indice);
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
