package com.eduar.promobot.scraperlocal.scraping;

import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MercadoLivreScraper {
    private static final Logger LOG = Logger.getLogger(MercadoLivreScraper.class.getName());
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
        List<String> categorias = criterios.categorias().isEmpty()
                ? List.of(config.categoriaPadrao()) : criterios.categorias();

        List<PromocaoEncontrada> resultado = new ArrayList<>();
        for (String categoria : categorias) {
            resultado.addAll(buscarPorCategoria(categoria, criterios.percentualDescontoMinimo()));
        }
        return List.copyOf(resultado);
    }

    private List<PromocaoEncontrada> buscarPorCategoria(String categoria, int percentualDescontoMinimo) {
        String url = config.baseUrlTemplate().formatted(categoria);
        try (BrowserContext context = browser.newContext(); Page page = context.newPage()) {
            page.setDefaultTimeout(config.timeout().toMillis());
            page.setDefaultNavigationTimeout(config.timeout().toMillis());
            page.navigate(url);
            page.waitForSelector(".ui-search-layout__item");

            int scrolls = 0;
            long cardsAtuais = contarCards(page);
            while (cardsAtuais < config.maxPorCategoria() && scrolls < config.maxScrolls()) {
                page.evaluate(OfertaExtractionScripts.SCROLL_PARA_BAIXO);
                page.waitForTimeout(config.scrollDelay().toMillis());
                cardsAtuais = contarCards(page);
                scrolls++;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cardsBrutos =
                    (List<Map<String, Object>>) page.evaluate(OfertaExtractionScripts.EXTRAIR_CARDS);
            return mapearParaPromocoes(cardsBrutos, categoria, percentualDescontoMinimo);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Falha ao buscar promocoes na categoria " + categoria, e);
            return List.of();
        }
    }

    private long contarCards(Page page) {
        return ((Number) page.evaluate(OfertaExtractionScripts.CONTAR_CARDS)).longValue();
    }

    private List<PromocaoEncontrada> mapearParaPromocoes(List<Map<String, Object>> cardsBrutos,
                                                          String categoria,
                                                          int percentualDescontoMinimo) {
        List<PromocaoEncontrada> promocoes = new ArrayList<>();
        int quantidadeParaAnalisar = Math.min(cardsBrutos.size(), config.maxPorCategoria());
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

    private String extrairIdExterno(String url) {
        if (url == null) return null;
        Matcher matcher = ID_EXTERNO_PATTERN.matcher(url);
        return matcher.find() ? "MLB" + matcher.group(1) : null;
    }

    private BigDecimal parsePrecoLabel(String label) {
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
