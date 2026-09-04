package com.eduar.promobot.scraperlocal.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScraperConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void carregaDefaultsDaBuscaAdaptativa() {
        ScraperConfig config = ScraperConfig.carregar(ambienteBase(tempDir.resolve("inexistente.properties")));

        assertEquals(5, config.alvoMinimoNovas());
        assertEquals(8, config.alvoMaximoNovas());
        assertEquals(12, config.maxPaginasSegurancaPorCiclo());
        assertEquals("MLB1648", config.categoriaId());
        assertEquals("https://www.mercadolivre.com.br/ofertas?category=%s&page=%d",
                config.baseUrlTemplate());
        assertEquals(Duration.ofMinutes(40), config.intervalo());
    }

    @Test
    void priorizaAmbienteSobrePropertiesEDefaults() throws IOException {
        Path properties = tempDir.resolve("scraper.properties");
        Files.writeString(properties, """
                busca.alvo-minimo-novas=3
                busca.alvo-maximo-novas=7
                busca.max-paginas-seguranca-por-ciclo=15
                scraping.categoria-id=MLB1051
                """);
        Map<String, String> ambiente = new HashMap<>(ambienteBase(properties));
        ambiente.put("SCRAPER_ALVO_MAXIMO_NOVAS", "9");
        ambiente.put("SCRAPER_ML_CATEGORIA_ID", "MLB1144");

        ScraperConfig config = ScraperConfig.carregar(ambiente);

        assertEquals(3, config.alvoMinimoNovas());
        assertEquals(9, config.alvoMaximoNovas());
        assertEquals(15, config.maxPaginasSegurancaPorCiclo());
        assertEquals("MLB1144", config.categoriaId());
    }

    @Test
    void rejeitaAlvoMinimoMaiorQueMaximo() {
        Map<String, String> ambiente = new HashMap<>(ambienteBase(tempDir.resolve("inexistente.properties")));
        ambiente.put("SCRAPER_ALVO_MINIMO_NOVAS", "9");
        ambiente.put("SCRAPER_ALVO_MAXIMO_NOVAS", "8");

        assertThrows(IllegalArgumentException.class, () -> ScraperConfig.carregar(ambiente));
    }

    @Test
    void rejeitaLimiteDePaginasNaoPositivo() {
        Map<String, String> ambiente = new HashMap<>(ambienteBase(tempDir.resolve("inexistente.properties")));
        ambiente.put("SCRAPER_MAX_PAGINAS_SEGURANCA_POR_CICLO", "0");

        assertThrows(IllegalArgumentException.class, () -> ScraperConfig.carregar(ambiente));
    }

    @Test
    void rejeitaCategoriaForaDoFormatoMlb() {
        Map<String, String> ambiente = new HashMap<>(ambienteBase(tempDir.resolve("inexistente.properties")));
        ambiente.put("SCRAPER_ML_CATEGORIA_ID", "informatica");

        assertThrows(IllegalArgumentException.class, () -> ScraperConfig.carregar(ambiente));
    }

    private Map<String, String> ambienteBase(Path configPath) {
        return Map.of(
                "SCRAPER_CONFIG_FILE", configPath.toString(),
                "SCRAPER_VPS_ENDPOINT_URL", "http://127.0.0.1/api/promocoes/ingestao",
                "SCRAPER_INGESTAO_API_KEY", "chave-teste");
    }
}
