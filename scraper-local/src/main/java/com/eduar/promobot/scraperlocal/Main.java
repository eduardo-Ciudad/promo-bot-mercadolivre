package com.eduar.promobot.scraperlocal;

import com.eduar.promobot.scraperlocal.application.ScrapingJob;
import com.eduar.promobot.scraperlocal.config.PlaywrightConfig;
import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.ingestao.IngestaoClient;
import com.eduar.promobot.scraperlocal.scraping.MercadoLivreScraper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        if (!argumentosValidos(args)) {
            System.err.println("Uso: java -jar scraper-local.jar [--once]");
            System.exit(2);
        }

        try {
            executarUmaVez();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Falha fatal durante a execucao do scraper", e);
            System.exit(1);
        }
    }

    private static boolean argumentosValidos(String[] args) {
        return args.length == 0 || Arrays.equals(args, new String[]{"--once"});
    }

    private static void executarUmaVez() {
        ScraperConfig config = ScraperConfig.carregar();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try (Playwright playwright = Playwright.create();
             Browser browser = PlaywrightConfig.criarBrowser(playwright, config)) {
            MercadoLivreScraper scraper = new MercadoLivreScraper(browser, config);
            IngestaoClient ingestaoClient = new IngestaoClient(
                    httpClient, config.ingestaoEndpointUrl(), config.ingestaoApiKey(), config.timeout());
            ScrapingJob job = new ScrapingJob(scraper, ingestaoClient, config);
            job.executar();
        }
    }
}

