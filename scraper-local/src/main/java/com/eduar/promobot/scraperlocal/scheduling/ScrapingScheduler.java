package com.eduar.promobot.scraperlocal.scheduling;

import com.eduar.promobot.scraperlocal.application.ScrapingJob;
import com.eduar.promobot.scraperlocal.config.ScraperConfig;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScrapingScheduler implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(ScrapingScheduler.class.getName());
    private static final Duration TEMPO_ENCERRAMENTO = Duration.ofSeconds(30);

    private final Runnable tarefa;
    private final ScraperConfig config;
    private final Clock clock;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean iniciado = new AtomicBoolean();
    private final AtomicBoolean fechado = new AtomicBoolean();
    private final CountDownLatch encerrado = new CountDownLatch(1);
    private volatile ScheduledFuture<?> agendamentoAtual;

    public ScrapingScheduler(ScrapingJob job, ScraperConfig config) {
        this(job::executar, config, Clock.systemUTC(),
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "scraper-scheduler");
                    thread.setDaemon(false);
                    return thread;
                }));
    }

    ScrapingScheduler(Runnable tarefa, ScraperConfig config, Clock clock,
                      ScheduledExecutorService executor) {
        this.tarefa = Objects.requireNonNull(tarefa, "tarefa e obrigatoria");
        this.config = Objects.requireNonNull(config, "config e obrigatoria");
        this.clock = Objects.requireNonNull(clock, "clock e obrigatorio");
        this.executor = Objects.requireNonNull(executor, "executor e obrigatorio");
    }

    public void iniciar() {
        if (fechado.get()) {
            throw new IllegalStateException("O scheduler ja foi fechado");
        }
        if (!iniciado.compareAndSet(false, true)) {
            throw new IllegalStateException("O scheduler ja foi iniciado");
        }
        agendarProxima(config.executarAoIniciar());
    }

    public void aguardarEncerramento() throws InterruptedException {
        encerrado.await();
    }

    ZonedDateTime calcularProximaExecucao(boolean permitirExecucaoImediata) {
        ZonedDateTime agora = ZonedDateTime.ofInstant(clock.instant(), config.timezone());
        LocalDate dataAtual = agora.toLocalDate();
        ZonedDateTime inicioHoje = dataAtual.atTime(config.horaInicio()).atZone(config.timezone());
        ZonedDateTime fimHoje = dataAtual.atTime(config.horaFim()).atZone(config.timezone());

        if (agora.isBefore(inicioHoje)) {
            return inicioHoje;
        }
        if (!agora.isBefore(fimHoje)) {
            return inicioDaJanela(dataAtual.plusDays(1));
        }
        if (permitirExecucaoImediata) {
            return agora;
        }

        ZonedDateTime aposIntervalo = agora.plus(config.intervalo());
        return aposIntervalo.isBefore(fimHoje)
                ? aposIntervalo
                : inicioDaJanela(dataAtual.plusDays(1));
    }

    private ZonedDateTime inicioDaJanela(LocalDate data) {
        return data.atTime(config.horaInicio()).atZone(config.timezone());
    }

    private void agendarProxima(boolean permitirExecucaoImediata) {
        if (fechado.get()) {
            return;
        }

        ZonedDateTime proxima = calcularProximaExecucao(permitirExecucaoImediata);
        Instant agora = clock.instant();
        long atrasoMillis = Math.max(0, Duration.between(agora, proxima.toInstant()).toMillis());
        LOG.info(() -> "Proxima execucao agendada para " + proxima);

        try {
            agendamentoAtual = executor.schedule(this::executarRodada, atrasoMillis, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            if (!fechado.get()) {
                throw e;
            }
        }
    }

    private void executarRodada() {
        try {
            tarefa.run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Falha inesperada na rodada de scraping", e);
        } finally {
            agendarProxima(false);
        }
    }

    @Override
    public void close() {
        if (!fechado.compareAndSet(false, true)) {
            return;
        }

        ScheduledFuture<?> atual = agendamentoAtual;
        if (atual != null) {
            atual.cancel(false);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TEMPO_ENCERRAMENTO.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            encerrado.countDown();
        }
    }
}

