package com.eduar.promobot.adapter.out.mercadolivre;

import com.eduar.promobot.adapter.in.webhook.WhatsAppWebhookController;
import com.eduar.promobot.domain.model.Produto;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.BuscadorDePromocoes;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Component
public class MercadoLivreAdapter implements BuscadorDePromocoes {


    private static final Logger log = LoggerFactory.getLogger(MercadoLivreAdapter.class);

    private final Browser browser;
    private final String baseUrlTemplate;
    private final String categoriaPadrao;
    private final int maxPorCategoria;
    private final int maxScrolls;

    public MercadoLivreAdapter(
            Browser browser,
            @Value("${mercadolivre.scraping.base-url-template}") String baseUrlTemplate,
            @Value("${mercadolivre.scraping.categoria-padrao}") String categoriaPadrao,
            @Value("${mercadolivre.scraping.max-por-categoria:20}") int maxPorCategoria,
            @Value("${mercadolivre.scraping.max-scrolls:10}") int maxScrolls) {
        this.browser = browser;
        this.baseUrlTemplate = baseUrlTemplate;
        this.categoriaPadrao = categoriaPadrao;
        this.maxPorCategoria = maxPorCategoria;
        this.maxScrolls = maxScrolls;
    }

    @Override
    public List<Promocao> buscarPromocoes(CriteriosBusca criterios) {
        List<String> categorias = criterios.categorias().isEmpty()
                ? List.of(categoriaPadrao)
                : criterios.categorias();

        List<Promocao> resultado = new ArrayList<>();
        for (String categoria : categorias) {
            resultado.addAll(buscarPorCategoria(categoria, criterios.percentualDescontoMinimo()));
        }
        return resultado;
    }

    private List<Promocao> buscarPorCategoria(String categoria, int percentualDescontoMinimo) {
        String url = baseUrlTemplate.formatted(categoria);

        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            page.navigate(url);
            page.waitForSelector(".ui-search-layout__item");

            int scrolls = 0;
            long cardsAtuais = ((Number) page.evaluate(OfertaExtractionScripts.CONTAR_CARDS)).longValue();            while (cardsAtuais < maxPorCategoria && scrolls < maxScrolls) {
                page.evaluate(OfertaExtractionScripts.SCROLL_PARA_BAIXO);
                page.waitForTimeout(1000);
                cardsAtuais = ((Number) page.evaluate(OfertaExtractionScripts.CONTAR_CARDS)).longValue();
                scrolls++;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cardsBrutos =
                    (List<Map<String, Object>>) page.evaluate(OfertaExtractionScripts.EXTRAIR_CARDS);

            return mapearParaPromocoes(cardsBrutos, categoria, percentualDescontoMinimo);

        }  catch (Exception e) {
        log.error("Falha ao buscar promoções na categoria {}", categoria, e);
        return List.of();
    }
    }

    private List<Promocao> mapearParaPromocoes(List<Map<String, Object>> cardsBrutos,
                                               String categoria,
                                               int percentualDescontoMinimo) {
        List<Promocao> promocoes = new ArrayList<>();

        for (Map<String, Object> cardBruto : cardsBrutos) {
            OfertaCard card = new OfertaCard(
                    (String) cardBruto.get("titulo"),
                    (String) cardBruto.get("url"),
                    (String) cardBruto.get("imagemUrl"),
                    (String) cardBruto.get("precoAtualLabel"),
                    (String) cardBruto.get("precoAnteriorLabel"),
                    (String) cardBruto.get("descontoLabel")
            );

            if (card.titulo() == null || card.url() == null || card.precoAnteriorLabel() == null) {
                continue;
            }

            String idExterno = extrairIdExterno(card.url());
            if (idExterno == null) {
                continue;
            }

            BigDecimal precoAtual = parsePrecoLabel(card.precoAtualLabel());
            BigDecimal precoAnterior = parsePrecoLabel(card.precoAnteriorLabel());
            if (precoAtual == null || precoAnterior == null || precoAtual.compareTo(precoAnterior) >= 0) {
                continue;
            }

            Produto produto = new Produto(card.titulo(), card.imagemUrl(), categoria);
            Promocao promocao = Promocao.builder()
                    .produto(produto)
                    .precoOriginal(precoAnterior)
                    .precoPromocional(precoAtual)
                    .linkOriginal(card.url())
                    .idExterno(idExterno)
                    .build();

            if (promocao.getPercentualDesconto() >= percentualDescontoMinimo) {
                promocoes.add(promocao);
            }
        }

        return promocoes;
    }

    private static final Pattern ID_EXTERNO_PATTERN = Pattern.compile("MLB-?(\\d+)");

    private String extrairIdExterno(String url) {
        if (url == null) return null;
        Matcher matcher = ID_EXTERNO_PATTERN.matcher(url);
        return matcher.find() ? "MLB" + matcher.group(1) : null;
    }

    private static final Pattern PRECO_PATTERN =
            Pattern.compile("(\\d[\\d.]*)\\s*reais(?:\\s*com\\s*(\\d+)\\s*centavos)?", Pattern.CASE_INSENSITIVE);

    private BigDecimal parsePrecoLabel(String label) {
        if (label == null) return null;
        Matcher matcher = PRECO_PATTERN.matcher(label);
        if (!matcher.find()) return null;

        String reaisStr = matcher.group(1).replace(".", "");
        String centavosStr = matcher.group(2) != null ? matcher.group(2) : "0";
        if (centavosStr.length() == 1) centavosStr = "0" + centavosStr;

        try {
            return new BigDecimal(reaisStr + "." + centavosStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
