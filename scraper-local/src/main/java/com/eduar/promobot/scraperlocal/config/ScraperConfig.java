package com.eduar.promobot.scraperlocal.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public record ScraperConfig(
        URI ingestaoEndpointUrl,
        String ingestaoApiKey,
        boolean headless,
        String baseUrlTemplate,
        int maxProdutos,
        int maxPaginas,
        int alvoMinimoNovas,
        int alvoMaximoNovas,
        int maxPaginasSegurancaPorCiclo,
        String categoriaId,
        Duration pageDelay,
        int descontoMinimo,
        Duration timeout,
        LocalTime horaInicio,
        LocalTime horaFim,
        Duration intervalo,
        ZoneId timezone,
        boolean executarAoIniciar) {

    private static final Path DEFAULT_CONFIG_PATH = Path.of("config", "scraper.properties");
    private static final Pattern CATEGORIA_ID_PATTERN = Pattern.compile("MLB\\d+");

    public ScraperConfig {
        if (!"http".equalsIgnoreCase(ingestaoEndpointUrl.getScheme())
                && !"https".equalsIgnoreCase(ingestaoEndpointUrl.getScheme())) {
            throw new IllegalArgumentException("A URL de ingestao deve usar HTTP ou HTTPS");
        }
        exigirTexto(ingestaoApiKey, "SCRAPER_INGESTAO_API_KEY");
        exigirTexto(baseUrlTemplate, "scraping.base-url-template");
        if (!baseUrlTemplate.contains("%s") || !baseUrlTemplate.contains("%d")) {
            throw new IllegalArgumentException("scraping.base-url-template deve conter %s e %d");
        }
        exigirPositivo(maxProdutos, "scraping.max-produtos");
        exigirPositivo(maxPaginas, "scraping.max-paginas");
        exigirPositivo(alvoMinimoNovas, "busca.alvo-minimo-novas");
        exigirPositivo(alvoMaximoNovas, "busca.alvo-maximo-novas");
        if (alvoMinimoNovas > alvoMaximoNovas) {
            throw new IllegalArgumentException(
                    "busca.alvo-minimo-novas deve ser menor ou igual a busca.alvo-maximo-novas");
        }
        exigirPositivo(maxPaginasSegurancaPorCiclo, "busca.max-paginas-seguranca-por-ciclo");
        exigirTexto(categoriaId, "scraping.categoria-id");
        if (!CATEGORIA_ID_PATTERN.matcher(categoriaId).matches()) {
            throw new IllegalArgumentException("scraping.categoria-id deve usar o formato MLB seguido de digitos");
        }
        exigirDuracaoPositiva(pageDelay, "scraping.page-delay-ms");
        if (descontoMinimo < 0 || descontoMinimo > 100) {
            throw new IllegalArgumentException("scraping.desconto-minimo deve estar entre 0 e 100");
        }
        exigirDuracaoPositiva(timeout, "scraping.timeout-segundos");
        if (!horaInicio.isBefore(horaFim)) {
            throw new IllegalArgumentException("scheduler.hora-inicio deve ser anterior a scheduler.hora-fim");
        }
        exigirDuracaoPositiva(intervalo, "scheduler.intervalo-minutos");
    }

    public static ScraperConfig carregar() {
        return carregar(System.getenv());
    }

    static ScraperConfig carregar(Map<String, String> ambiente) {
        Properties properties = carregarProperties(caminhoConfiguracao(ambiente));

        return new ScraperConfig(
                uriObrigatoria(valor(ambiente, properties,
                        "SCRAPER_VPS_ENDPOINT_URL", "ingestao.endpoint-url", null)),
                exigirTexto(ambiente.get("SCRAPER_INGESTAO_API_KEY"), "SCRAPER_INGESTAO_API_KEY"),
                parseBoolean(valor(ambiente, properties, "SCRAPER_HEADLESS", "scraping.headless", "true"),
                        "scraping.headless"),
                valor(ambiente, properties, "SCRAPER_ML_BASE_URL_TEMPLATE", "scraping.base-url-template",
                        "https://www.mercadolivre.com.br/ofertas?category=%s&page=%d"),
                parseInt(valor(ambiente, properties,
                        "SCRAPER_MAX_PRODUTOS", "scraping.max-produtos", "20"),
                        "scraping.max-produtos"),
                parseInt(valor(ambiente, properties,
                        "SCRAPER_MAX_PAGINAS", "scraping.max-paginas", "10"), "scraping.max-paginas"),
                parseInt(valor(ambiente, properties,
                        "SCRAPER_ALVO_MINIMO_NOVAS", "busca.alvo-minimo-novas", "5"),
                        "busca.alvo-minimo-novas"),
                parseInt(valor(ambiente, properties,
                        "SCRAPER_ALVO_MAXIMO_NOVAS", "busca.alvo-maximo-novas", "8"),
                        "busca.alvo-maximo-novas"),
                parseInt(valor(ambiente, properties,
                        "SCRAPER_MAX_PAGINAS_SEGURANCA_POR_CICLO",
                        "busca.max-paginas-seguranca-por-ciclo", "12"),
                        "busca.max-paginas-seguranca-por-ciclo"),
                valor(ambiente, properties,
                        "SCRAPER_ML_CATEGORIA_ID", "scraping.categoria-id", "MLB1648"),
                Duration.ofMillis(parseLong(valor(ambiente, properties,
                        "SCRAPER_PAGE_DELAY_MS", "scraping.page-delay-ms", "1000"),
                        "scraping.page-delay-ms")),
                parseInt(valor(ambiente, properties,
                        "SCRAPER_DESCONTO_MINIMO", "scraping.desconto-minimo", "10"),
                        "scraping.desconto-minimo"),
                Duration.ofSeconds(parseLong(valor(ambiente, properties,
                        "SCRAPER_TIMEOUT_SEGUNDOS", "scraping.timeout-segundos", "30"),
                        "scraping.timeout-segundos")),
                parseTime(valor(ambiente, properties,
                        "SCRAPER_HORA_INICIO", "scheduler.hora-inicio", "07:00"), "scheduler.hora-inicio"),
                parseTime(valor(ambiente, properties,
                        "SCRAPER_HORA_FIM", "scheduler.hora-fim", "20:00"), "scheduler.hora-fim"),
                Duration.ofMinutes(parseLong(valor(ambiente, properties,
                        "SCRAPER_INTERVALO_MINUTOS", "scheduler.intervalo-minutos", "40"),
                        "scheduler.intervalo-minutos")),
                parseZoneId(valor(ambiente, properties,
                        "SCRAPER_TIMEZONE", "scheduler.timezone", "America/Sao_Paulo")),
                parseBoolean(valor(ambiente, properties,
                        "SCRAPER_EXECUTAR_AO_INICIAR", "scheduler.executar-ao-iniciar", "false"),
                        "scheduler.executar-ao-iniciar"));
    }

    private static Path caminhoConfiguracao(Map<String, String> ambiente) {
        String configurado = ambiente.get("SCRAPER_CONFIG_FILE");
        return configurado == null || configurado.isBlank() ? DEFAULT_CONFIG_PATH : Path.of(configurado.trim());
    }

    private static Properties carregarProperties(Path path) {
        Properties properties = new Properties();
        if (!Files.exists(path)) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel ler o arquivo de configuracao: " + path, e);
        }
    }

    private static String valor(Map<String, String> ambiente, Properties properties,
                                String variavel, String property, String padrao) {
        String valorAmbiente = ambiente.get(variavel);
        if (valorAmbiente != null && !valorAmbiente.isBlank()) {
            return valorAmbiente.trim();
        }
        String valorProperty = properties.getProperty(property);
        if (valorProperty != null && !valorProperty.isBlank()) {
            return valorProperty.trim();
        }
        return padrao;
    }

    private static URI uriObrigatoria(String valor) {
        exigirTexto(valor, "SCRAPER_VPS_ENDPOINT_URL ou ingestao.endpoint-url");
        try {
            return URI.create(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("URL de ingestao invalida", e);
        }
    }

    private static boolean parseBoolean(String valor, String nome) {
        if ("true".equalsIgnoreCase(valor)) return true;
        if ("false".equalsIgnoreCase(valor)) return false;
        throw new IllegalArgumentException(nome + " deve ser true ou false");
    }

    private static int parseInt(String valor, String nome) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nome + " deve ser um numero inteiro", e);
        }
    }

    private static long parseLong(String valor, String nome) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nome + " deve ser um numero inteiro", e);
        }
    }

    private static LocalTime parseTime(String valor, String nome) {
        try {
            return LocalTime.parse(valor);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(nome + " deve usar o formato HH:mm", e);
        }
    }

    private static ZoneId parseZoneId(String valor) {
        try {
            return ZoneId.of(valor);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("scheduler.timezone invalido", e);
        }
    }

    private static String exigirTexto(String valor, String nome) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nome + " e obrigatorio");
        }
        return valor;
    }

    private static void exigirPositivo(int valor, String nome) {
        if (valor <= 0) {
            throw new IllegalArgumentException(nome + " deve ser maior que zero");
        }
    }

    private static void exigirDuracaoPositiva(Duration valor, String nome) {
        if (valor.isZero() || valor.isNegative()) {
            throw new IllegalArgumentException(nome + " deve ser maior que zero");
        }
    }
}
