package com.eduar.promobot.scraperlocal.scraping;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MercadoLivreScraperTest {

    @Test
    void esperaPolyCardAntesDaPausaDeEstabilidadeEDaExtracao() {
        List<String> chamadas = new ArrayList<>();
        Page page = (Page) Proxy.newProxyInstance(
                Page.class.getClassLoader(),
                new Class<?>[]{Page.class},
                (proxy, method, args) -> {
                    chamadas.add(method.getName() + (args == null ? "" : ":" + args[0]));
                    return method.getName().equals("evaluate") ? List.of() : null;
                });

        MercadoLivreScraper.extrairPagina(
                page, 2, "https://www.mercadolivre.com.br/ofertas?page=%d", Duration.ofMillis(750));

        assertEquals(List.of(
                "navigate:https://www.mercadolivre.com.br/ofertas?page=2",
                "waitForSelector:.poly-card",
                "waitForTimeout:750.0",
                "evaluate:" + OfertaExtractionScripts.EXTRAIR_CARDS), chamadas);
    }

    @Test
    void acumulaPaginasDeduplicaPorIdEParaQuandoNaoHaCardsNovos() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        Map<Integer, List<Map<String, Object>>> paginas = Map.of(
                1, List.of(card("MLB100"), card("MLB200")),
                2, List.of(card("MLB200"), card("MLB300")),
                3, List.of(card("MLB100"), card("MLB300")),
                4, List.of(card("MLB400")));

        List<Map<String, Object>> resultado = MercadoLivreScraper.coletarCards(10, 10, pagina -> {
            paginasVisitadas.add(pagina);
            return paginas.getOrDefault(pagina, List.of());
        });

        assertEquals(List.of(1, 2, 3), paginasVisitadas);
        assertEquals(List.of("MLB100", "MLB200", "MLB300"), ids(resultado));
    }

    @Test
    void paraAoAtingirMaximoDeProdutos() {
        List<Integer> paginasVisitadas = new ArrayList<>();

        List<Map<String, Object>> resultado = MercadoLivreScraper.coletarCards(3, 10, pagina -> {
            paginasVisitadas.add(pagina);
            return pagina == 1
                    ? List.of(card("MLB1"), card("MLB2"))
                    : List.of(card("MLB3"), card("MLB4"));
        });

        assertEquals(List.of(1, 2), paginasVisitadas);
        assertEquals(List.of("MLB1", "MLB2", "MLB3"), ids(resultado));
    }

    @Test
    void respeitaMaximoDePaginasEIgnoraCardsSemIdMlb() {
        List<Map<String, Object>> resultado = MercadoLivreScraper.coletarCards(10, 2, pagina ->
                pagina == 1
                        ? List.of(card("MLB10"), Map.of("url", "https://example.com/produto"))
                        : List.of(card("MLB20")));

        assertEquals(List.of("MLB10", "MLB20"), ids(resultado));
    }

    @Test
    void extraiIdExternoNosFormatosComESemHifen() {
        assertEquals("MLB123456", MercadoLivreScraper.extrairIdExterno(
                "https://produto.mercadolivre.com.br/MLB-123456-produto"));
        assertEquals("MLB987", MercadoLivreScraper.extrairIdExterno(
                "https://www.mercadolivre.com.br/item/MLB987"));
        assertNull(MercadoLivreScraper.extrairIdExterno("https://www.mercadolivre.com.br/ofertas"));
    }

    @Test
    void converteLabelsDePrecoComMilharesECentavos() {
        assertEquals(new BigDecimal("1234.56"),
                MercadoLivreScraper.parsePrecoLabel("1.234 reais com 56 centavos"));
        assertEquals(new BigDecimal("99.00"), MercadoLivreScraper.parsePrecoLabel("99 reais"));
        assertNull(MercadoLivreScraper.parsePrecoLabel("preco indisponivel"));
    }

    private Map<String, Object> card(String idExterno) {
        return Map.of("url", "https://produto.mercadolivre.com.br/" + idExterno + "-produto");
    }

    private List<String> ids(List<Map<String, Object>> cards) {
        return cards.stream()
                .map(card -> MercadoLivreScraper.extrairIdExterno((String) card.get("url")))
                .toList();
    }
}
