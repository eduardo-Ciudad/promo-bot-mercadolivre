package com.eduar.promobot.scraperlocal.scraping;

import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                page, 2, "https://www.mercadolivre.com.br/ofertas?category=%s&page=%d",
                "MLB1648", Duration.ofMillis(750));

        assertEquals(List.of(
                "navigate:https://www.mercadolivre.com.br/ofertas?category=MLB1648&page=2",
                "waitForTimeout:750.0",
                "evaluate:" + OfertaExtractionScripts.EXTRAIR_CARDS), chamadas);
    }

    @Test
    void sessaoDiferenciaPaginaVaziaDePaginaSemPromocoesValidasEDeduplicaIds() {
        AtomicBoolean contextoFechado = new AtomicBoolean();
        Browser browser = browserComPaginas(List.of(
                List.of(cardValido("MLB1"), cardValido("MLB2")),
                List.of(cardValido("MLB2"), card("MLB3")),
                List.of()), contextoFechado, false);
        MercadoLivreScraper scraper = new MercadoLivreScraper(browser, config());

        try (SessaoBusca sessao = scraper.iniciarBusca(new CriteriosBusca(10))) {
            PaginaPromocoes primeira = sessao.buscarPagina(1);
            PaginaPromocoes segunda = sessao.buscarPagina(2);
            PaginaPromocoes terceira = sessao.buscarPagina(3);

            assertEquals(2, primeira.quantidadeCards());
            assertEquals(List.of("MLB1", "MLB2"), idsPromocoes(primeira.promocoes()));
            assertEquals(2, segunda.quantidadeCards());
            assertTrue(segunda.promocoes().isEmpty());
            assertEquals(0, terceira.quantidadeCards());
            assertTrue(terceira.semCards());
        }

        assertTrue(contextoFechado.get());
    }

    @Test
    void sessaoPropagaFalhaTecnicaEmVezDeRetornarPaginaVazia() {
        Browser browser = browserComPaginas(List.of(List.of()), new AtomicBoolean(), true);
        MercadoLivreScraper scraper = new MercadoLivreScraper(browser, config());

        try (SessaoBusca sessao = scraper.iniciarBusca(new CriteriosBusca(10))) {
            assertThrows(IllegalStateException.class, () -> sessao.buscarPagina(1));
        }
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

    private Map<String, Object> cardValido(String idExterno) {
        return Map.of(
                "titulo", "Notebook " + idExterno,
                "url", "https://produto.mercadolivre.com.br/" + idExterno + "-produto",
                "imagemUrl", "https://example.com/" + idExterno + ".jpg",
                "precoAtualLabel", "80 reais",
                "precoAnteriorLabel", "100 reais",
                "descontoLabel", "20% OFF");
    }

    private List<String> idsPromocoes(List<PromocaoEncontrada> promocoes) {
        return promocoes.stream().map(PromocaoEncontrada::idExterno).toList();
    }

    private Browser browserComPaginas(List<List<Map<String, Object>>> paginas,
                                      AtomicBoolean contextoFechado,
                                      boolean falharAoExtrair) {
        AtomicInteger paginaAtual = new AtomicInteger(-1);
        Page page = (Page) Proxy.newProxyInstance(
                Page.class.getClassLoader(), new Class<?>[]{Page.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("navigate")) {
                        paginaAtual.incrementAndGet();
                    }
                    if (method.getName().equals("evaluate")) {
                        if (falharAoExtrair) throw new IllegalStateException("falha simulada");
                        return paginas.get(paginaAtual.get());
                    }
                    return null;
                });
        BrowserContext context = (BrowserContext) Proxy.newProxyInstance(
                BrowserContext.class.getClassLoader(), new Class<?>[]{BrowserContext.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("newPage")) return page;
                    if (method.getName().equals("close")) contextoFechado.set(true);
                    return null;
                });
        return (Browser) Proxy.newProxyInstance(
                Browser.class.getClassLoader(), new Class<?>[]{Browser.class},
                (proxy, method, args) -> method.getName().equals("newContext") ? context : null);
    }

    private ScraperConfig config() {
        return new ScraperConfig(
                URI.create("http://127.0.0.1/api/promocoes/ingestao"),
                "chave-teste",
                true,
                "https://www.mercadolivre.com.br/ofertas?category=%s&page=%d",
                5,
                8,
                12,
                "MLB1648",
                Duration.ofMillis(1),
                10,
                Duration.ofSeconds(30),
                LocalTime.of(7, 0),
                LocalTime.of(20, 0),
                Duration.ofMinutes(40),
                ZoneId.of("America/Sao_Paulo"),
                false);
    }
}
