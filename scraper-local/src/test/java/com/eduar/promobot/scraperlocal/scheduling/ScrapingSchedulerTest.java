package com.eduar.promobot.scraperlocal.scheduling;

import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrapingSchedulerTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void agendaParaInicioDeHojeAntesDaJanela() {
        assertProxima("2026-08-29T09:00:00Z", false,
                "2026-08-29T07:00:00-03:00[America/Sao_Paulo]", SAO_PAULO);
    }

    @Test
    void executaImediatamenteDentroDaJanelaQuandoConfigurado() {
        assertProxima("2026-08-29T14:15:00Z", true,
                "2026-08-29T11:15:00-03:00[America/Sao_Paulo]", SAO_PAULO);
    }

    @Test
    void aguardaIntervaloDentroDaJanelaSemExecucaoImediata() {
        assertProxima("2026-08-29T14:15:00Z", false,
                "2026-08-29T11:45:00-03:00[America/Sao_Paulo]", SAO_PAULO);
    }

    @Test
    void agendaParaAmanhaDepoisDoFimDaJanela() {
        assertProxima("2026-08-29T23:00:00Z", true,
                "2026-08-30T07:00:00-03:00[America/Sao_Paulo]", SAO_PAULO);
    }

    @Test
    void naoAgendaIntervaloQueUltrapassaFimDaJanela() {
        assertProxima("2026-08-29T22:45:00Z", false,
                "2026-08-30T07:00:00-03:00[America/Sao_Paulo]", SAO_PAULO);
    }

    @Test
    void respeitaInicioInclusivoEFimExclusivo() {
        assertProxima("2026-08-29T10:00:00Z", true,
                "2026-08-29T07:00:00-03:00[America/Sao_Paulo]", SAO_PAULO);
        assertProxima("2026-08-29T23:00:00Z", true,
                "2026-08-30T07:00:00-03:00[America/Sao_Paulo]", SAO_PAULO);
    }

    @Test
    void converteInstanteParaTimezoneConfigurado() {
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        assertProxima("2026-08-29T21:30:00Z", false,
                "2026-08-30T07:00:00+09:00[Asia/Tokyo]", tokyo);
    }

    private void assertProxima(String instante, boolean imediata, String esperado, ZoneId timezone) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try (ScrapingScheduler scheduler = new ScrapingScheduler(
                () -> { }, config(timezone), Clock.fixed(Instant.parse(instante), ZoneId.of("UTC")), executor)) {
            assertEquals(ZonedDateTime.parse(esperado), scheduler.calcularProximaExecucao(imediata));
        }
    }

    private ScraperConfig config(ZoneId timezone) {
        return new ScraperConfig(
                URI.create("http://127.0.0.1/api/promocoes/ingestao"),
                "chave-teste",
                true,
                "https://www.mercadolivre.com.br/ofertas?page=%d",
                20,
                10,
                Duration.ofSeconds(1),
                10,
                Duration.ofSeconds(30),
                LocalTime.of(7, 0),
                LocalTime.of(20, 0),
                Duration.ofMinutes(30),
                timezone,
                false);
    }
}
