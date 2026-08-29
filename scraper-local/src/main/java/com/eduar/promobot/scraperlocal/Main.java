package com.eduar.promobot.scraperlocal;

import com.eduar.promobot.scraperlocal.application.ScrapingJob;
import com.eduar.promobot.scraperlocal.config.PlaywrightConfig;
import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.ingestao.IngestaoClient;
import com.eduar.promobot.scraperlocal.scraping.MercadoLivreScraper;
import com.eduar.promobot.scraperlocal.scheduling.ScrapingScheduler;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
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
            executar(args);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("Execucao interrompida");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Falha fatal durante a execucao do scraper", e);
            System.exit(1);
        }
    }

    private static boolean argumentosValidos(String[] args) {
        return args.length == 0 || Arrays.equals(args, new String[]{"--once"});
    }

    private static void executar(String[] args) throws InterruptedException {
        ScraperConfig config = ScraperConfig.carregar();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        Playwright playwright = Playwright.create();
        Browser browser = null;
        ScrapingScheduler scheduler = null;
        RecursosAplicacao recursos = null;
        Thread shutdownHook = null;

        try {
            browser = PlaywrightConfig.criarBrowser(playwright, config);
            MercadoLivreScraper scraper = new MercadoLivreScraper(browser, config);
            IngestaoClient ingestaoClient = new IngestaoClient(
                    httpClient, config.ingestaoEndpointUrl(), config.ingestaoApiKey(), config.timeout());
            ScrapingJob job = new ScrapingJob(scraper, ingestaoClient, config);
            scheduler = args.length == 0 ? new ScrapingScheduler(job, config) : null;
            recursos = new RecursosAplicacao(scheduler, browser, playwright);
            RecursosAplicacao recursosDoHook = recursos;
            shutdownHook = new Thread(recursosDoHook::close, "scraper-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            if (args.length == 1) {
                job.executar();
            } else {
                scheduler.iniciar();
                scheduler.aguardarEncerramento();
            }
        } finally {
            removerShutdownHook(shutdownHook);
            if (recursos != null) {
                recursos.close();
            } else {
                fecharComLog(browser, "browser");
                fecharComLog(playwright, "playwright");
            }
        }
    }

    private static void removerShutdownHook(Thread shutdownHook) {
        if (shutdownHook == null) return;
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // O encerramento da JVM ja comecou e o hook esta em execucao.
        }
    }

    private static void fecharComLog(AutoCloseable recurso, String nome) {
        if (recurso == null) return;
        try {
            recurso.close();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Falha ao fechar " + nome, e);
        }
    }

    private static final class RecursosAplicacao implements AutoCloseable {
        private final ScrapingScheduler scheduler;
        private final Browser browser;
        private final Playwright playwright;
        private final AtomicBoolean fechado = new AtomicBoolean();

        private RecursosAplicacao(ScrapingScheduler scheduler, Browser browser, Playwright playwright) {
            this.scheduler = scheduler;
            this.browser = browser;
            this.playwright = playwright;
        }

        @Override
        public void close() {
            if (!fechado.compareAndSet(false, true)) return;
            fecharComLog(scheduler, "scheduler");
            fecharComLog(browser, "browser");
            fecharComLog(playwright, "playwright");
        }
    }
}
